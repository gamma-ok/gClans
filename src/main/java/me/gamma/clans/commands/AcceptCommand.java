package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.managers.InvitationManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

public class AcceptCommand extends AbstractClanCommand {

	public AcceptCommand(Clans plugin) {
		super(plugin, "accept", "gclans.use", (RankPermission) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		if (cp.hasClan()) {
			msg(player, "general.already-in-clan");
			return;
		}

		InvitationManager im = plugin.getInvitationManager();
		String input = args[0];
		String resolvedClanId = null;

		Clan clan = cm.getClanByName(input);
		if (clan != null && im.hasInvitationFor(player.getUniqueId(), clan.getId())) {
			resolvedClanId = clan.getId();
		}

		if (resolvedClanId == null && im.hasInvitationFromPlayer(player.getUniqueId(), input)) {
			resolvedClanId = im.getInvitedClanId(player.getUniqueId());
			if (resolvedClanId != null) {
				clan = cm.getClan(resolvedClanId);
			}
		}

		if (resolvedClanId == null || clan == null) {
			msg(player, "accept.not-invited");
			return;
		}

		if (clan.isFull()) {
			msg(player, "general.invalid-usage");
			return;
		}

		im.cancel(player.getUniqueId());
		final String clanName = clan.getName();
		final String clanId = clan.getId();
		final Clan finalClan = clan;

		cm.addMember(clanId, player.getUniqueId())
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
					msg(player, "accept.success-self", "{clan}", clanName);
					finalClan.getMembers().keySet().forEach(mid -> {
						if (!mid.equals(player.getUniqueId())) {
							Player m = plugin.getServer().getPlayer(mid);
							if (m != null)
								m.sendMessage(cfg.getMessage("accept.success-broadcast", "{player}", player.getName()));
						}
					});
				})).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}