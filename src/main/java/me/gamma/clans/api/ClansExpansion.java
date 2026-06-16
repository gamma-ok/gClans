package me.gamma.clans.api;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Expansión de PlaceholderAPI para gClans v2.
 *
 * ============================================================ PLACEHOLDERS DE
 * JUGADOR: ============================================================
 * %gclan_has_clan% → true / false %gclan_name% → §eElite (con color del clan;
 * "Sin clan" si no tiene) %gclan_name_raw% → Elite (sin color) %gclan_prefix% →
 * §e[ELT] (con color; vacío si no tiene) %gclan_tag% → §e[ELT]§fElite (prefijo
 * + nombre; vacío si no tiene) %gclan_rank% → Co-Leader %gclan_rank_colored% →
 * §cCo-Leader %gclan_kills% → 42 %gclan_deaths% → 10 %gclan_kd% → 4.20
 * %gclan_points% → 37.5 %gclan_killstreak% → 5 (racha activa)
 * %gclan_best_killstreak% → 12 (mejor racha) %gclan_clan_kills% → 450
 * %gclan_clan_deaths% → 120 %gclan_clan_points% → 1230 %gclan_clan_level% → 15
 * %gclan_clan_members% → 18 %gclan_clan_slots% → 30
 *
 * ============================================================ PLACEHOLDERS DE
 * TOP (hologramas — del _1 al _10):
 * ============================================================
 * %gclan_top_name_1% → nombre del clan #1 (por puntos)
 * %gclan_top_name_colored_1% → §enombre del clan #1 %gclan_top_kills_1% → kills
 * del clan #1 %gclan_top_points_1% → puntos del clan #1 %gclan_top_level_1% →
 * nivel del clan #1 %gclan_top_members_1% → miembros del clan #1
 * %gclan_top_prefix_1% → prefijo del clan #1 %gclan_top_leader_1% → nombre del
 * líder del clan #1
 *
 * ============================================================ EJEMPLO CHAT:
 * &8[&a%pvplevels_level%&8] %gclan_prefix%&7%player_name%&7: &f{message}
 *
 * EJEMPLO HOLOGRAMA: &6&lTOP CLANES &e#1 %gclan_top_name_colored_1% &7-
 * &a%gclan_top_points_1% pts &e#2 %gclan_top_name_colored_2% &7-
 * &a%gclan_top_points_2% pts
 * ============================================================
 */
public class ClansExpansion extends PlaceholderExpansion {

	private static final int MAX_TOP = 10;
	private static final long CACHE_TTL_MS = 60_000L; // 60 segundos

	private final Clans plugin;

	// Caché del top (por puntos, que es el default de hologramas)
	private List<Clan> topCache = new ArrayList<>();
	private long topCacheTime = 0L;

	public ClansExpansion(Clans plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "gclan";
	}

	@Override
	public String getAuthor() {
		return "gClans Dev";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public boolean persist() {
		return true;
	}

	// -------------------------------------------------------
	// Resolución principal
	// -------------------------------------------------------

	@Override
	public String onRequest(OfflinePlayer player, String identifier) {
		// Top placeholders (no requieren jugador)
		if (identifier.startsWith("top_")) {
			return resolveTop(identifier);
		}

		if (player == null)
			return resolveDefault(identifier);

		ClanPlayer cp = plugin.getClanManager().getPlayer(player.getUniqueId());
		if (cp == null)
			return resolveDefault(identifier);

		return resolvePlayer(cp, identifier);
	}

	// -------------------------------------------------------
	// Placeholders de jugador
	// -------------------------------------------------------

	private String resolvePlayer(ClanPlayer cp, String id) {
		switch (id) {
		case "has_clan":
			return String.valueOf(cp.hasClan());
		case "kills":
			return String.valueOf(cp.getKills());
		case "deaths":
			return String.valueOf(cp.getDeaths());
		case "kd":
			return String.format("%.2f", cp.getKDRatio());
		case "points":
			return formatPts(cp.getPoints());
		case "killstreak":
			return String.valueOf(cp.getKillstreak());
		case "best_killstreak":
			return String.valueOf(cp.getBestKillstreak());
		case "rank":
			return cp.hasClan() && cp.getRank() != null ? cp.getRank().getDisplayName()
					: plugin.getConfigManager().getNoClanText();
		case "rank_colored":
			return cp.hasClan() && cp.getRank() != null ? cp.getRank().getColoredName() : "";
		}

		if (!cp.hasClan())
			return resolveDefault(id);

		Clan clan = plugin.getClanManager().getClan(cp.getClanId());
		if (clan == null)
			return resolveDefault(id);

		switch (id) {
		// Nombre con color del clan (NO hereda el color anterior en el chat)
		case "name":
			return clan.getColoredName(); // ej: §eElite
		case "name_raw":
			return clan.getName(); // sin color
		case "prefix":
			return clan.getColoredPrefix(); // ej: §e[ELT]
		case "tag":
			return clan.getColoredPrefix() + "§f" + clan.getName();
		case "clan_kills":
			return String.valueOf(clan.getTotalKills());
		case "clan_deaths":
			return String.valueOf(clan.getTotalDeaths());
		case "clan_points":
			return formatPts(clan.getTotalPoints());
		case "clan_level":
			return String.valueOf(clan.getLevel());
		case "clan_members":
			return String.valueOf(clan.getMemberCount());
		case "clan_slots":
			return String.valueOf(clan.getSlots());
		default:
			return null;
		}
	}

	private String resolveDefault(String id) {
		String noClan = plugin.getConfigManager().getNoClanText();
		switch (id) {
		case "has_clan":
		case "kills":
		case "deaths":
		case "killstreak":
		case "best_killstreak":
		case "clan_kills":
		case "clan_deaths":
		case "clan_level":
		case "clan_members":
		case "clan_slots":
			return "0";
		case "kd":
		case "points":
		case "clan_points":
			return "0.0";
		case "name":
		case "rank":
			return noClan;
		case "rank_colored":
		case "prefix":
		case "tag":
		case "name_raw":
			return "";
		default:
			return null;
		}
	}

	// -------------------------------------------------------
	// Placeholders de Top
	// -------------------------------------------------------

	private String resolveTop(String identifier) {
		// Formato: top_<campo>_<posición> ej: top_name_1, top_kills_3
		String[] parts = identifier.split("_", 3); // ["top", "name", "1"]
		if (parts.length != 3)
			return null;

		String field = parts[1]; // campo
		int pos;
		try {
			pos = Integer.parseInt(parts[2]);
		} catch (NumberFormatException e) {
			return null;
		}

		if (pos < 1 || pos > MAX_TOP)
			return "N/A";

		List<Clan> top = getCachedTop();
		int index = pos - 1;
		if (index >= top.size()) {
			return field.equals("kills") || field.equals("points") || field.equals("level") || field.equals("members")
					? "0"
					: "N/A";
		}

		Clan clan = top.get(index);
		switch (field) {
		case "name":
			return clan.getName();
		case "name_colored":
			return clan.getColoredName();
		case "prefix":
			return clan.getColoredPrefix();
		case "kills":
			return String.valueOf(clan.getTotalKills());
		case "points":
			return formatPts(clan.getTotalPoints());
		case "level":
			return String.valueOf(clan.getLevel());
		case "members":
			return String.valueOf(clan.getMemberCount());
		case "leader": {
			org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(clan.getLeaderUuid());
			return op.getName() != null ? op.getName() : "N/A";
		}
		default:
			return null;
		}
	}

	// -------------------------------------------------------
	// Caché del top (asíncrono, no bloquea el hilo principal)
	// -------------------------------------------------------

	private synchronized List<Clan> getCachedTop() {
		long now = System.currentTimeMillis();
		if (now - topCacheTime > CACHE_TTL_MS) {
			topCacheTime = now; // marcar antes para evitar multiple requests
			plugin.getClanManager().getTopByPoints(MAX_TOP).thenAccept(list -> {
				synchronized (ClansExpansion.this) {
					topCache = list;
				}
			}).exceptionally(ex -> {
				plugin.getLogger().warning("Error refrescando caché top PAPI: " + ex.getMessage());
				topCacheTime = 0L; // reintentar en el próximo ciclo
				return null;
			});
		}
		return topCache;
	}

	/** Fuerza invalidar el caché (llamar después de un disbandClan). */
	public synchronized void invalidateTopCache() {
		topCacheTime = 0L;
	}

	// -------------------------------------------------------
	// Utilidades
	// -------------------------------------------------------

	private String formatPts(double pts) {
		if (pts == Math.floor(pts) && !Double.isInfinite(pts))
			return String.valueOf((long) pts);
		return String.format("%.1f", pts);
	}
}