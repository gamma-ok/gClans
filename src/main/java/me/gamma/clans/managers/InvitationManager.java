package me.gamma.clans.managers;

import me.gamma.clans.Clans;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona invitaciones pendientes con TTL automático.
 */
public class InvitationManager {

	private static class Invitation {
		final String clanId;
		final String clanName;
		final UUID inviterUuid;
		final long expiresAt;
		int taskId = -1;

		Invitation(String clanId, String clanName, UUID inviterUuid, long expiresAt) {
			this.clanId = clanId;
			this.clanName = clanName;
			this.inviterUuid = inviterUuid;
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

	/**
	 * Registra una invitación y programa su expiración.
	 */
	public void invite(UUID invited, String clanId, String clanName, UUID inviter) {
		cancel(invited); // cancelar invitación previa si existe

		long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
		Invitation inv = new Invitation(clanId, clanName, inviter, expiresAt);

		int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			invitations.remove(invited);
			// Notificar al invitador que la invitación caducó
			Player inviterPlayer = plugin.getServer().getPlayer(inviter);
			if (inviterPlayer != null && inviterPlayer.isOnline()) {
				Player invitedPlayer = plugin.getServer().getPlayer(invited);
				String targetName = invitedPlayer != null ? invitedPlayer.getName()
						: invited.toString().substring(0, 8);
				inviterPlayer
						.sendMessage(plugin.getConfigManager().getMessage("invite.expired", "{target}", targetName));
			}
		}, timeoutSeconds * 20L).getTaskId();

		inv.taskId = taskId;
		invitations.put(invited, inv);
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