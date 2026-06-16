package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /clan list [page]
 *
 * Formato: Clan List (Page 1/5) 1. Elite [8/30 online] ... To view other pages,
 * use /clan list <page>.
 */
public class ListCommand extends AbstractClanCommand {

	public ListCommand(Clans plugin) {
		super(plugin, "list", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		Collection<Clan> all = cm.getAllClans();

		if (all.isEmpty()) {
			msg(player, "list.empty");
			return;
		}

		// Ordenar por total de miembros desc
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

		// Header: "Clan List (Page 1/5)"
		send(player, "§eClan List §8(Page §f" + page + "§8/§f" + totalPages + "§8)");

		for (int i = from; i < to; i++) {
			Clan clan = sorted.get(i);
			int online = cm.getOnlineCount(clan);
			int total = clan.getMemberCount();

			raw(player, "list.entry", "{pos}", String.valueOf(i + 1), "{clan}", clan.getName(), "{online}",
					String.valueOf(online), "{total}", String.valueOf(total));
		}

		// Footer
		if (page < totalPages) {
			raw(player, "list.footer", "{next}", String.valueOf(page + 1));
		}
	}
}