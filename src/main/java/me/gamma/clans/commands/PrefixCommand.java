package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class PrefixCommand extends AbstractClanCommand {

	public PrefixCommand(Clans plugin) {
		super(plugin, "prefix", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;
		String newPrefix = String.join(" ", args);
		String error = cm.validatePrefix(newPrefix);
		if (error != null) {
			msg(player, error, "{min}", String.valueOf(cfg.getPrefixMin()), "{max}",
					String.valueOf(cfg.getPrefixMax()));
			return;
		}
		String colored = ConfigManager.color(newPrefix);
		cm.setClanPrefix(cp.getClanId(), colored).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
				() -> msg(player, "clan.prefix.success", "{prefix}", colored))).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}