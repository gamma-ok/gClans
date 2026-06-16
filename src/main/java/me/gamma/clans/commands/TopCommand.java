package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * /clan top [points|kills|level] Default: points.
 */
public class TopCommand extends AbstractClanCommand {

	public TopCommand(Clans plugin) {
		super(plugin, "top", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		String type = args.length >= 1 ? args[0].toLowerCase() : "points";

		int limit = cfg.getTopCount();
		CompletableFuture<List<Clan>> future;
		String headerKey, entryKey;

		switch (type) {
		case "kills":
			future = cm.getTopByKills(limit);
			headerKey = "top.header-kills";
			entryKey = "top.entry-kills";
			break;
		case "level":
			future = cm.getTopByLevel(limit);
			headerKey = "top.header-level";
			entryKey = "top.entry-level";
			break;
		default: // points
			future = cm.getTopByPoints(limit);
			headerKey = "top.header-points";
			entryKey = "top.entry-points";
			break;
		}

		final String hKey = headerKey;
		final String eKey = entryKey;

		future.thenAccept(clans -> plugin.getServer().getScheduler().runTask(plugin, () -> {
			raw(player, hKey, "{count}", String.valueOf(clans.size()));

			for (int i = 0; i < clans.size(); i++) {
				Clan clan = clans.get(i);
				String value;
				switch (type) {
				case "kills":
					value = String.valueOf(clan.getTotalKills());
					break;
				case "level":
					value = String.valueOf(clan.getLevel());
					break;
				default:
					value = formatPoints(clan.getTotalPoints());
					break;
				}
				raw(player, eKey, "{pos}", String.valueOf(i + 1), "{clan}", clan.getName(), "{value}", value, "{level}",
						String.valueOf(clan.getLevel()), "{kills}", String.valueOf(clan.getTotalKills()));
			}
			raw(player, "top.footer");
		})).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}
}