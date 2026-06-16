package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class KickCommand extends AbstractClanCommand {

	public KickCommand(Clans plugin) {
		super(plugin, "kick", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		if (args[0].equalsIgnoreCase(player.getName())) {
			msg(player, "member.kick.cannot-kick-self");
			return;
		}

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null || !clan.isMember(target.getUniqueId())) {
			msg(player, "general.target-not-in-clan", "{target}", target.getName());
			return;
		}

		Rank targetRank = clan.getMemberRank(target.getUniqueId());
		if (targetRank != null && targetRank.isAtLeast(cp.getRank())) {
			msg(player, "member.kick.cannot-kick-higher");
			return;
		}

		final String clanName = clan.getName();
		cm.removeMember(target.getUniqueId()).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
			msg(player, "member.kick.success-self", "{target}", target.getName());
			if (target.isOnline())
				target.sendMessage(cfg.getMessage("member.kick.success-target", "{clan}", clanName));
			clan.getMembers().keySet().forEach(mid -> {
				Player m = plugin.getServer().getPlayer(mid);
				if (m != null && !m.getUniqueId().equals(player.getUniqueId()))
					m.sendMessage(cfg.getMessage("member.kick.success-broadcast", "{target}", target.getName(),
							"{player}", player.getName()));
			});
		})).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}
}