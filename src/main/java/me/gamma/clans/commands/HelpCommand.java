package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommand extends AbstractClanCommand {

	public HelpCommand(Clans plugin) {
		super(plugin, "help", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		FileConfiguration help = cfg.getHelpConfig();

		// Contar páginas dinámicamente
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

		// Header
		String header = help.getString("header", "&8&m--------&r &6gClans Help &8&m--------");
		player.sendMessage(color(header, ps, ms, ns));

		// Líneas de la página
		List<String> lines = help.getStringList("pages.page-" + page);
		for (String line : lines)
			player.sendMessage(color(line, ps, ms, ns));

		// Footer
		boolean last = page >= total;
		String fKey = last ? "footer-last" : "footer";
		String footer = help.getString(fKey, "&8&m-----------------------------------------");
		player.sendMessage(color(footer, ps, ms, ns));
	}

	private String color(String text, String page, String max, String next) {
		return ConfigManager.color(text.replace("{page}", page).replace("{max}", max).replace("{next}", next));
	}
}