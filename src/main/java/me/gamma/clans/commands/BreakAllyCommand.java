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

		Clan target = cm.getClanByName(args[0]);
		if (target == null || !myClan.isAlliedWith(target.getId())) {
			msg(player, "ally.not-allies", "{clan}", args[0]);
			return;
		}

		final String myName = myClan.getName();
		final String targetName = target.getName();

		cm.breakAlliance(myClan.getId(), target.getId())
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
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
}