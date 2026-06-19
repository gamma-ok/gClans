package me.gamma.clans.managers;

import me.gamma.clans.Clans;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvitationManager {

	private static class Invitation {
		final String clanId;
		final String clanName;
		final UUID inviterUuid;
		final String inviterName;
		final long expiresAt;
		int taskId = -1;

		Invitation(String clanId, String clanName, UUID inviterUuid, String inviterName, long expiresAt) {
			this.clanId = clanId;
			this.clanName = clanName;
			this.inviterUuid = inviterUuid;
			this.inviterName = inviterName;
			this.expiresAt = expiresAt;
		}
	}

	private final Clans plugin;
	private final Map<UUID, Invitation> invitations = new HashMap<>();
	private final int timeoutSeconds;

	public InvitationManager(Clans plugin) {
		this.plugin = plugin;
		this.timeoutSeconds = plugin.getConfigManager().getInviteTimeout();
	}

	public void invite(UUID invited, String clanId, String clanName, UUID inviterUuid, String inviterName) {
		cancel(invited);

		long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
		Invitation inv = new Invitation(clanId, clanName, inviterUuid, inviterName, expiresAt);

		int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			Invitation expired = invitations.remove(invited);
			if (expired == null)
				return;

			Player inviterPlayer = plugin.getServer().getPlayer(inviterUuid);
			if (inviterPlayer != null && inviterPlayer.isOnline()) {
				Player invitedPlayer = plugin.getServer().getPlayer(invited);
				String targetName = invitedPlayer != null ? invitedPlayer.getName()
						: invited.toString().substring(0, 8);
				inviterPlayer
						.sendMessage(plugin.getConfigManager().getMessage("invite.expired", "{target}", targetName));
			}

			Player invitedPlayer = plugin.getServer().getPlayer(invited);
			if (invitedPlayer != null && invitedPlayer.isOnline()) {
				invitedPlayer.sendMessage(plugin.getConfigManager().getMessage("confirmation.expired", "{action}",
						"invite al clan " + expired.clanName, "{time}", String.valueOf(timeoutSeconds)));
			}
		}, timeoutSeconds * 20L).getTaskId();

		inv.taskId = taskId;
		invitations.put(invited, inv);
	}

	public void invite(UUID invited, String clanId, String clanName, UUID inviterUuid) {
		Player inviter = plugin.getServer().getPlayer(inviterUuid);
		String name = inviter != null ? inviter.getName() : inviterUuid.toString().substring(0, 8);
		invite(invited, clanId, clanName, inviterUuid, name);
	}

	public boolean hasInvitationFor(UUID invited, String clanId) {
		Invitation inv = invitations.get(invited);
		if (inv == null)
			return false;
		if (System.currentTimeMillis() > inv.expiresAt) {
			invitations.remove(invited);
			return false;
		}
		return inv.clanId.equals(clanId);
	}

	public boolean hasInvitationFromPlayer(UUID invited, String inviterName) {
		Invitation inv = invitations.get(invited);
		if (inv == null)
			return false;
		if (System.currentTimeMillis() > inv.expiresAt) {
			invitations.remove(invited);
			return false;
		}
		return inv.inviterName.equalsIgnoreCase(inviterName);
	}

	public boolean hasAnyInvitation(UUID invited) {
		Invitation inv = invitations.get(invited);
		if (inv == null)
			return false;
		if (System.currentTimeMillis() > inv.expiresAt) {
			invitations.remove(invited);
			return false;
		}
		return true;
	}

	public String getInvitedClanId(UUID invited) {
		Invitation inv = invitations.get(invited);
		return inv != null ? inv.clanId : null;
	}

	public String getInvitedClanName(UUID invited) {
		Invitation inv = invitations.get(invited);
		return inv != null ? inv.clanName : null;
	}

	public String getInviterName(UUID invited) {
		Invitation inv = invitations.get(invited);
		return inv != null ? inv.inviterName : null;
	}

	public void cancelAndNotify(UUID invited, String cancellerName) {
		Invitation inv = invitations.get(invited);
		if (inv == null)
			return;

		cancel(invited);

		Player invitedPlayer = plugin.getServer().getPlayer(invited);
		if (invitedPlayer != null && invitedPlayer.isOnline()) {
			invitedPlayer.sendMessage(plugin.getConfigManager().getMessage("uninvite.cancelled-target", "{clan}",
					inv.clanName, "{player}", cancellerName));
		}
	}

	public void cancel(UUID invited) {
		Invitation inv = invitations.remove(invited);
		if (inv != null && inv.taskId != -1)
			plugin.getServer().getScheduler().cancelTask(inv.taskId);
	}

	public void clearAll() {
		invitations.values().forEach(inv -> {
			if (inv.taskId != -1)
				plugin.getServer().getScheduler().cancelTask(inv.taskId);
		});
		invitations.clear();
	}
}