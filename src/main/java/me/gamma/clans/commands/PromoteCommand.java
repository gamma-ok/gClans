package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class PromoteCommand extends AbstractClanCommand {

	public PromoteCommand(Clans plugin) {
		super(plugin, "promote", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		if (args[0].equalsIgnoreCase(player.getName())) {
			msg(player, "rank.promote.cannot-self");
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
		Rank next = current != null ? current.next() : null;

		// LEADER no se puede asignar con promote; usa setleader
		if (next == null || next == Rank.LEADER) {
			msg(player, "rank.promote.already-max", "{target}", target.getName());
			return;
		}

		cm.setMemberRank(clan.getId(), target.getUniqueId(), next)
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
					msg(player, "rank.promote.success-self", "{target}", target.getName(), "{rank}",
							next.getDisplayName());
					if (target.isOnline())
						target.sendMessage(cfg.getMessage("rank.promote.success-target", "{rank}",
								next.getDisplayName(), "{player}", player.getName()));
				})).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}