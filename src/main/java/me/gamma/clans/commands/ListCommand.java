package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class ListCommand extends AbstractClanCommand {

	public ListCommand(Clans plugin) {
		super(plugin, "list", "gclans.use", (RankPermission) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		Collection<Clan> all = cm.getAllClans();

		if (all.isEmpty()) {
			msg(player, "list.empty");
			return;
		}

		List<Clan> sorted = new ArrayList<>(all);
		sorted.sort((a, b) -> b.getMemberCount() - a.getMemberCount());

		int pageSize = cfg.getListPageSize();
		int totalPages = (int) Math.ceil((double) sorted.size() / pageSize);

		int page = 1;
		if (args.length >= 1) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (NumberFormatException ignored) {
			}
		}
		page = Math.max(1, Math.min(page, totalPages));

		int from = (page - 1) * pageSize;
		int to = Math.min(from + pageSize, sorted.size());

		StringBuilder entries = new StringBuilder();
		for (int i = from; i < to; i++) {
			Clan clan = sorted.get(i);
			int online = cm.getOnlineCount(clan);
			int total = clan.getMemberCount();

			String entry = cfg.getRaw("list.entry", "{pos}", String.valueOf(i + 1), "{clan}", clan.getName(),
					"{online}", String.valueOf(online), "{total}", String.valueOf(total));

			if (i > from)
				entries.append("\n");
			entries.append(entry);
		}

		String nextPage = String.valueOf(Math.min(page + 1, totalPages));
		boolean isLast = (page >= totalPages);

		List<String> lines = cfg.getMessages().getStringList("list.lines");

		if (lines.isEmpty()) {
			send(player, "&eClan List &8(Page &f" + page + "&8/&f" + totalPages + "&8)");
			for (String entryLine : entries.toString().split("\n")) {
				send(player, entryLine);
			}
			if (!isLast)
				send(player, "&7To view other pages, use &e/clan list " + nextPage + "&7.");
			return;
		}

		for (String line : lines) {
			if (isLast && line.contains("{next}"))
				continue;

			if (line.contains("{entries}")) {
				if (entries.length() > 0) {
					for (String entryLine : entries.toString().split("\n")) {
						send(player, entryLine);
					}
				}
			} else {
				send(player, line.replace("{page}", String.valueOf(page)).replace("{max}", String.valueOf(totalPages))
						.replace("{next}", nextPage));
			}
		}
	}
}