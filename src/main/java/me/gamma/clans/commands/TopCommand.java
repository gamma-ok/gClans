package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TopCommand extends AbstractClanCommand {

	public TopCommand(Clans plugin) {
		super(plugin, "top", "gclans.use", (RankPermission) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		String type = args.length >= 1 ? args[0].toLowerCase() : "points";

		int limit = cfg.getTopCount();
		CompletableFuture<List<Clan>> future;
		String linesKey, entryKey;

		switch (type) {
		case "kills":
			future = cm.getTopByKills(limit);
			linesKey = "top.lines-kills";
			entryKey = "top.entry-kills";
			break;
		case "level":
			future = cm.getTopByLevel(limit);
			linesKey = "top.lines-level";
			entryKey = "top.entry-level";
			break;
		default: // points
			future = cm.getTopByPoints(limit);
			linesKey = "top.lines-points";
			entryKey = "top.entry-points";
			break;
		}

		final String lk = linesKey;
		final String ek = entryKey;
		final String tp = type;

		future.thenAccept(clans -> plugin.getServer().getScheduler().runTask(plugin, () -> {
			StringBuilder entries = new StringBuilder();
			for (int i = 0; i < clans.size(); i++) {
				Clan clan = clans.get(i);
				String value;
				switch (tp) {
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
				String entry = cfg.getRaw(ek, "{pos}", String.valueOf(i + 1), "{clan}", clan.getName(), "{value}",
						value, "{level}", String.valueOf(clan.getLevel()), "{kills}",
						String.valueOf(clan.getTotalKills()));

				if (i > 0)
					entries.append("\n");
				entries.append(entry);
			}

			List<String> lines = cfg.getMessages().getStringList(lk);

			if (lines.isEmpty()) {
				send(player, "&8&m----&r &6Top Clanes &8&m----");
				for (String entryLine : entries.toString().split("\n")) {
					send(player, entryLine);
				}
				send(player, "&8&m-------------------------------");
				return;
			}

			for (String line : lines) {
				if (line.contains("{entries}")) {
					if (entries.length() > 0) {
						for (String entryLine : entries.toString().split("\n")) {
							send(player, entryLine);
						}
					}
				} else {
					send(player, line.replace("{count}", String.valueOf(clans.size())));
				}
			}
		})).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}
}