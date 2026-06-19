package me.gamma.clans.commands.admin;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminHelpCommand {

	private final Clans plugin;

	public AdminHelpCommand(Clans plugin) {
		this.plugin = plugin;
	}

	public void execute(Player player, String[] args) {
		FileConfiguration help = plugin.getConfigManager().getAdminHelpConfig();

		int total = 0;
		while (help.contains("pages.page-" + (total + 1)))
			total++;
		if (total == 0) {
			player.sendMessage(ConfigManager.color("&cNo hay páginas de ayuda configuradas."));
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