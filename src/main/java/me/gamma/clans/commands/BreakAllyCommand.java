package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class BreakAllyCommand extends AbstractClanCommand {

	public BreakAllyCommand(Clans plugin) {
		super(plugin, "breakally", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		Clan myClan = cm.getClan(cp.getClanId());
		if (myClan == null)
			return;

		Clan target = cm.findClanByNameOrLeader(args[0]);

		if (target == null || !myClan.isAlliedWith(target.getId())) {
			msg(player, "breakally.not-found");
			return;
		}

		final String myName = myClan.getName();
		final String targetName = target.getName();
		final String myId = myClan.getId();
		final String targetId = target.getId();

		cm.breakAlliance(myId, targetId).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
			disableAllyChatIfNeeded(myClan);
			disableAllyChatIfNeeded(target);

			msg(player, "breakally.success-self", "{clan}", targetName);
			target.getMembers().keySet().forEach(uuid -> {
				Player m = plugin.getServer().getPlayer(uuid);
				if (m != null)
					m.sendMessage(cfg.getMessage("breakally.success-other", "{clan}", myName));
			});
		})).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}

	private void disableAllyChatIfNeeded(Clan clan) {
		if (!clan.getAllies().isEmpty())
			return;

		clan.getMembers().keySet().forEach(uuid -> {
			ClanPlayer cp = cm.getPlayer(uuid);
			if (cp != null && cp.isAllyChatActive()) {
				cp.setAllyChatActive(false);
				Player m = plugin.getServer().getPlayer(uuid);
				if (m != null)
					m.sendMessage(cfg.getMessage("chat.ally.auto-disabled"));
			}
		});
	}
}