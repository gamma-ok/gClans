package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class DemoteCommand extends AbstractClanCommand {

	public DemoteCommand(Clans plugin) {
		super(plugin, "demote", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;
		if (args[0].equalsIgnoreCase(player.getName())) {
			msg(player, "rank.demote.cannot-self");
			return;
		}

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null || !clan.isMember(target.getUniqueId())) {
			msg(player, "general.target-not-in-clan", "{target}", args[0]);
			return;
		}

		Rank current = clan.getMemberRank(target.getUniqueId());
		Rank previous = current != null ? current.previous() : null;
		if (previous == null) {
			msg(player, "rank.demote.already-min", "{target}", target.getName());
			return;
		}

		cm.setMemberRank(clan.getId(), target.getUniqueId(), previous)
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
					msg(player, "rank.demote.success-self", "{target}", target.getName(), "{rank}",
							previous.getDisplayName());
					if (target.isOnline())
						target.sendMessage(cfg.getMessage("rank.demote.success-target", "{rank}",
								previous.getDisplayName(), "{player}", player.getName()));
				})).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}