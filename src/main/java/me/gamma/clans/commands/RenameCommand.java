package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class RenameCommand extends AbstractClanCommand {

	public RenameCommand(Clans plugin) {
		super(plugin, "rename", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;
		String error = cm.validateName(args[0]);
		if (error != null) {
			msg(player, error, "{min}", String.valueOf(cfg.getNameMin()), "{max}", String.valueOf(cfg.getNameMax()),
					"{clan}", args[0]);
			return;
		}
		cm.renameClan(cp.getClanId(), args[0]).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
				() -> msg(player, "clan.rename.success", "{clan}", args[0]))).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}