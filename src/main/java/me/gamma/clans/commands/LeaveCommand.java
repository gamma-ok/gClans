package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class LeaveCommand extends AbstractClanCommand {

	public LeaveCommand(Clans plugin) {
		super(plugin, "leave", "gclans.use", null, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (cp.hasRankAtLeast(Rank.LEADER)) {
			msg(player, "member.leave.leader-cannot-leave");
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;
		final String clanName = clan.getName();

		cm.removeMember(player.getUniqueId()).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
			msg(player, "member.leave.success-self", "{clan}", clanName);
			clan.getMembers().keySet().forEach(mid -> {
				Player m = plugin.getServer().getPlayer(mid);
				if (m != null)
					m.sendMessage(cfg.getMessage("member.leave.success-broadcast", "{player}", player.getName()));
			});
		})).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}
}