package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommand extends AbstractClanCommand {

	public HelpCommand(Clans plugin) {
		super(plugin, "help", "gclans.use", (RankPermission) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		FileConfiguration help = cfg.getHelpConfig();

		int total = 0;
		while (help.contains("pages.page-" + (total + 1)))
			total++;
		if (total == 0) {
			msg(player, "general.invalid-usage");
			return;
		}

		int page = 1;
		if (args.length >= 1) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (NumberFormatException ignored) {
			}
		}
		page = Math.max(1, Math.min(page, total));

		String ps = String.valueOf(page);
		String ms = String.valueOf(total);
		String ns = String.valueOf(Math.min(page + 1, total));

		List<String> lines = help.getStringList("pages.page-" + page);
		for (String line : lines) {
			player.sendMessage(
					ConfigManager.color(line.replace("{page}", ps).replace("{max}", ms).replace("{next}", ns)));
		}
	}
}