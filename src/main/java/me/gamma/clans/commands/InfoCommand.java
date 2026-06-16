package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * /clan info <clan|player> Muestra información detallada. Busca por nombre de
 * clan o por nombre de jugador.
 *
 * Formato: Elite [4/8] - [14/06/2026] Leader: pepe123 Co-Leader: juan159
 * Members: diloe[42], lucas[18] Points: 1,230 Position: #3 Clan PvP: ON/OFF
 */
public class InfoCommand extends AbstractClanCommand {

	public InfoCommand(Clans plugin) {
		super(plugin, "info", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		String input = args[0];
		Clan clan = resolveClan(input);

		if (clan == null) {
			msg(player, "general.clan-not-found", "{clan}", input);
			return;
		}

		displayInfo(player, clan);
	}

	/**
	 * Resuelve el clan buscando primero por nombre de clan, luego por nombre de
	 * jugador (busca el clan de ese jugador).
	 */
	private Clan resolveClan(String input) {
		// 1. Intentar como nombre de clan
		Clan byName = cm.getClanByName(input);
		if (byName != null)
			return byName;

		// 2. Intentar como nombre de jugador → obtener su clan
		return cm.getClanOfPlayer(resolveUuid(input));
	}

	private UUID resolveUuid(String name) {
		Player online = plugin.getServer().getPlayer(name);
		if (online != null)
			return online.getUniqueId();
		// OfflinePlayer fallback
		return plugin.getServer().getOfflinePlayer(name).getUniqueId();
	}

	private void displayInfo(Player player, Clan clan) {
		// Construir listas por rango
		List<String> leaders = new ArrayList<>();
		List<String> coLeaders = new ArrayList<>();
		List<String> members = new ArrayList<>();

		clan.getMembers().forEach((uuid, rank) -> {
			String name = getPlayerName(uuid);
			int kills = getPlayerKills(uuid);
			String entry = name + "[" + kills + "]";

			if (rank == Rank.LEADER)
				leaders.add(name);
			else if (rank == Rank.CO_LEADER)
				coLeaders.add(name);
			else
				members.add(entry);
		});

		int online = cm.getOnlineCount(clan);
		int total = clan.getMemberCount();

		// Obtener posición en top de puntos (consulta asíncrona)
		int limit = plugin.getConfigManager().getTopCount();
		cm.getTopByPoints(limit).thenAccept(top -> {
			int position = -1;
			for (int i = 0; i < top.size(); i++) {
				if (top.get(i).getId().equals(clan.getId())) {
					position = i + 1;
					break;
				}
			}
			final int pos = position;

			plugin.getServer().getScheduler().runTask(plugin, () -> {
				// Header
				raw(player, "info.header", "{clan}", clan.getName(), "{online}", String.valueOf(online), "{total}",
						String.valueOf(total), "{date}", clan.getCreatedAtFormatted());

				// Leader
				raw(player, "info.leader", "{player}", leaders.isEmpty() ? "?" : leaders.get(0));

				// Co-Leaders (si los hay)
				if (!coLeaders.isEmpty()) {
					raw(player, "info.coleader", "{player}", String.join(", ", coLeaders));
				}

				// Members
				if (!members.isEmpty()) {
					raw(player, "info.members", "{list}", String.join(", ", members));
				}

				// Points
				raw(player, "info.points", "{value}", formatPoints(clan.getTotalPoints()));

				// Position
				String posStr = pos > 0 ? "#" + pos : "N/A";
				raw(player, "info.position", "{pos}", posStr);

				// PvP status
				String pvpStatus = clan.isPvpEnabled() ? "§aON" : "§cOFF";
				raw(player, "info.pvp-status", "{status}", pvpStatus);
			});
		}).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}

	private String getPlayerName(UUID uuid) {
		Player p = plugin.getServer().getPlayer(uuid);
		if (p != null)
			return p.getName();
		org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(uuid);
		return op.getName() != null ? op.getName() : "Desconocido";
	}

	private int getPlayerKills(UUID uuid) {
		ClanPlayer cp = plugin.getClanManager().getPlayer(uuid);
		return cp != null ? cp.getKills() : 0;
	}
}