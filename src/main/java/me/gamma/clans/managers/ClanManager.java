package me.gamma.clans.managers;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import me.gamma.clans.storage.StorageProvider;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manager central de lógica de clanes v2. Cache en memoria con
 * ConcurrentHashMap para thread-safety.
 */
public class ClanManager {

	private final Clans plugin;
	private final StorageProvider storage;

	// -------------------------------------------------------
	// Caché en memoria
	// -------------------------------------------------------
	private final Map<String, Clan> clanById = new ConcurrentHashMap<>();
	private final Map<String, String> nameIndex = new ConcurrentHashMap<>(); // nombre.lower → id
	private final Map<UUID, ClanPlayer> playerCache = new ConcurrentHashMap<>();

	private Pattern namePattern;

	// -------------------------------------------------------
	// Constructor
	// -------------------------------------------------------

	public ClanManager(Clans plugin, StorageProvider storage) {
		this.plugin = plugin;
		this.storage = storage;
		this.namePattern = Pattern.compile(plugin.getConfigManager().getNameRegex());
	}

	// -------------------------------------------------------
	// Carga inicial
	// -------------------------------------------------------

	public CompletableFuture<Void> loadAll() {
		return storage.loadAllClans().thenCompose(clans -> {
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (Clan clan : clans) {
				clanById.put(clan.getId(), clan);
				nameIndex.put(clan.getName().toLowerCase(), clan.getId());

				// Cargar aliados y miembros del clan
				CompletableFuture<Void> allyFuture = storage.loadAllies(clan.getId())
						.thenAccept(allies -> allies.forEach(clan::addAlly));
				futures.add(allyFuture);
			}
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		}).exceptionally(ex -> {
			plugin.getLogger().severe("Error cargando clanes: " + ex.getMessage());
			return null;
		});
	}

	// -------------------------------------------------------
	// Gestión de jugadores
	// -------------------------------------------------------

	public CompletableFuture<ClanPlayer> loadPlayer(Player player) {
		return storage.loadClanPlayer(player.getUniqueId()).thenApply(opt -> {
			ClanPlayer cp = opt.orElseGet(
					() -> new ClanPlayer(player.getUniqueId(), player.getName(), null, null, 0, 0, 0.0, 0, 0L));
			cp.setName(player.getName());

			// Re-vincular al modelo del clan si el clan está en caché
			if (cp.hasClan() && !clanById.containsKey(cp.getClanId())) {
				// El clan fue borrado mientras el jugador estaba offline
				cp.leaveClan();
			} else if (cp.hasClan()) {
				Clan clan = clanById.get(cp.getClanId());
				if (clan != null && cp.getRank() != null) {
					clan.addMember(player.getUniqueId(), cp.getRank());
				}
			}

			playerCache.put(player.getUniqueId(), cp);
			return cp;
		});
	}

	public void unloadPlayer(UUID uuid) {
		ClanPlayer cp = playerCache.remove(uuid);
		if (cp != null)
			storage.saveClanPlayer(cp);
	}

	// -------------------------------------------------------
	// Lecturas síncronas (desde caché)
	// -------------------------------------------------------

	public ClanPlayer getPlayer(UUID uuid) {
		return playerCache.get(uuid);
	}

	public Clan getClan(String id) {
		return clanById.get(id);
	}

	public Collection<Clan> getAllClans() {
		return Collections.unmodifiableCollection(clanById.values());
	}

	/** Búsqueda case-insensitive por nombre de clan. */
	public Clan getClanByName(String name) {
		String id = nameIndex.get(name.toLowerCase());
		return id != null ? clanById.get(id) : null;
	}

	/**
	 * Búsqueda por nombre del líder (online o offline por nombre en caché de
	 * jugadores).
	 */
	public Clan getClanByLeaderName(String leaderName) {
		for (Clan clan : clanById.values()) {
			// Buscar en playerCache primero (online)
			ClanPlayer cp = playerCache.values().stream().filter(p -> p.getUuid().equals(clan.getLeaderUuid()))
					.findFirst().orElse(null);
			String name = cp != null ? cp.getName()
					: plugin.getServer().getOfflinePlayer(clan.getLeaderUuid()).getName();
			if (name != null && name.equalsIgnoreCase(leaderName))
				return clan;
		}
		return null;
	}

	/**
	 * Busca clan por nombre de clan O por nombre de líder. Usado en /clan ally y
	 * /clan info.
	 */
	public Clan findClanByNameOrLeader(String input) {
		Clan byName = getClanByName(input);
		return byName != null ? byName : getClanByLeaderName(input);
	}

	public Clan getClanOfPlayer(UUID uuid) {
		ClanPlayer cp = playerCache.get(uuid);
		if (cp == null || !cp.hasClan())
			return null;
		return clanById.get(cp.getClanId());
	}

	// -------------------------------------------------------
	// Validaciones
	// -------------------------------------------------------

	/** @return null si es válido, clave de mensaje si es inválido. */
	public String validateName(String name) {
		if (name.length() < plugin.getConfigManager().getNameMin())
			return "clan.create.name-too-short";
		if (name.length() > plugin.getConfigManager().getNameMax())
			return "clan.create.name-too-long";
		if (!namePattern.matcher(name).matches())
			return "clan.create.name-invalid";
		for (String bl : plugin.getConfigManager().getNameBlacklist()) {
			if (name.equalsIgnoreCase(bl))
				return "clan.create.name-blacklisted";
		}
		if (getClanByName(name) != null)
			return "clan.create.name-taken";
		return null;
	}

	/** @return null si es válido, clave de mensaje si es inválido. */
	public String validatePrefix(String prefix) {
		// Medir longitud sin códigos de color
		String stripped = prefix.replaceAll("&[0-9a-fA-FrRkKlLmMnNoO]", "");
		if (stripped.length() < plugin.getConfigManager().getPrefixMin())
			return "clan.prefix.too-short";
		if (stripped.length() > plugin.getConfigManager().getPrefixMax())
			return "clan.prefix.too-long";
		for (String blocked : plugin.getConfigManager().getPrefixBlockedCodes()) {
			if (prefix.toLowerCase().contains(blocked.toLowerCase()))
				return "clan.prefix.invalid";
		}
		return null;
	}

	// -------------------------------------------------------
	// Operaciones de clan
	// -------------------------------------------------------

	public CompletableFuture<Clan> createClan(Player founder, String name, String prefix) {
		String id = UUID.randomUUID().toString();
		String color = plugin.getConfigManager().getDefaultColor();
		int slots = plugin.getConfigManager().getDefaultSlots();
		Clan clan = new Clan(id, name, prefix, color, founder.getUniqueId(), slots);

		clanById.put(id, clan);
		nameIndex.put(name.toLowerCase(), id);

		ClanPlayer cp = playerCache.get(founder.getUniqueId());
		if (cp != null)
			cp.joinClan(id, Rank.LEADER);

		return storage.saveClan(clan).thenCompose(v -> storage.saveClanPlayer(cp)).thenApply(v -> clan);
	}

	public CompletableFuture<Void> disbandClan(String clanId) {
		Clan clan = clanById.remove(clanId);
		if (clan == null)
			return CompletableFuture.completedFuture(null);

		nameIndex.remove(clan.getName().toLowerCase());

		long cooldownSeconds = plugin.getConfigManager().getCreateCooldown();
		long cooldownUntil = System.currentTimeMillis() + cooldownSeconds * 1000L;

		for (UUID memberUuid : clan.getMembers().keySet()) {
			ClanPlayer cp = playerCache.get(memberUuid);
			if (cp != null) {
				cp.leaveClan();
				if (cooldownSeconds > 0)
					cp.setCreateCooldownUntil(cooldownUntil);
			}
		}

		return storage.deleteClan(clanId);
	}

	public CompletableFuture<Void> addMember(String clanId, UUID playerUuid) {
		Clan clan = clanById.get(clanId);
		ClanPlayer cp = playerCache.get(playerUuid);
		if (clan == null || cp == null)
			return CompletableFuture.completedFuture(null);

		clan.addMember(playerUuid, Rank.MEMBER);
		cp.joinClan(clanId, Rank.MEMBER);
		return storage.saveClanPlayer(cp);
	}

	public CompletableFuture<Void> removeMember(UUID playerUuid) {
		ClanPlayer cp = playerCache.get(playerUuid);
		if (cp == null || !cp.hasClan())
			return CompletableFuture.completedFuture(null);

		Clan clan = clanById.get(cp.getClanId());
		if (clan != null)
			clan.removeMember(playerUuid);

		long cooldownSeconds = plugin.getConfigManager().getCreateCooldown();
		if (cooldownSeconds > 0) {
			cp.setCreateCooldownUntil(System.currentTimeMillis() + cooldownSeconds * 1000L);
		}

		cp.leaveClan();
		return storage.removeMember(playerUuid).thenCompose(v -> storage.saveClanPlayer(cp));
	}

	public CompletableFuture<Void> setMemberRank(String clanId, UUID uuid, Rank rank) {
		Clan clan = clanById.get(clanId);
		ClanPlayer cp = playerCache.get(uuid);
		if (clan != null)
			clan.setMemberRank(uuid, rank);
		if (cp != null)
			cp.setRank(rank);
		return storage.updateMemberRank(clanId, uuid, rank);
	}

	public CompletableFuture<Void> transferLeadership(String clanId, UUID newLeader) {
		Clan clan = clanById.get(clanId);
		if (clan == null)
			return CompletableFuture.completedFuture(null);

		UUID oldLeader = clan.getLeaderUuid();
		clan.transferLeadership(newLeader);

		ClanPlayer oldCp = playerCache.get(oldLeader);
		ClanPlayer newCp = playerCache.get(newLeader);
		if (oldCp != null)
			oldCp.setRank(Rank.CO_LEADER);
		if (newCp != null)
			newCp.setRank(Rank.LEADER);

		return CompletableFuture.allOf(storage.updateMemberRank(clanId, oldLeader, Rank.CO_LEADER),
				storage.updateMemberRank(clanId, newLeader, Rank.LEADER), storage.updateClan(clan));
	}

	public CompletableFuture<Void> renameClan(String clanId, String newName) {
		Clan clan = clanById.get(clanId);
		if (clan == null)
			return CompletableFuture.completedFuture(null);
		nameIndex.remove(clan.getName().toLowerCase());
		clan.setName(newName);
		nameIndex.put(newName.toLowerCase(), clanId);
		return storage.updateClan(clan);
	}

	public CompletableFuture<Void> setClanPrefix(String clanId, String prefix) {
		Clan clan = clanById.get(clanId);
		if (clan == null)
			return CompletableFuture.completedFuture(null);
		clan.setPrefix(prefix);
		return storage.updateClan(clan);
	}

	public CompletableFuture<Void> formAlliance(String id1, String id2) {
		Clan c1 = clanById.get(id1), c2 = clanById.get(id2);
		if (c1 != null)
			c1.addAlly(id2);
		if (c2 != null)
			c2.addAlly(id1);
		return storage.addAlliance(id1, id2);
	}

	public CompletableFuture<Void> breakAlliance(String id1, String id2) {
		Clan c1 = clanById.get(id1), c2 = clanById.get(id2);
		if (c1 != null)
			c1.removeAlly(id2);
		if (c2 != null)
			c2.removeAlly(id1);
		return storage.removeAlliance(id1, id2);
	}

	// -------------------------------------------------------
	// Sistema de puntos y nivel (llamado desde listeners)
	// -------------------------------------------------------

	/**
	 * Procesa una kill: actualiza puntos, XP, nivel y killstreak del clan.
	 * 
	 * @param killerUuid UUID del jugador que hizo la kill.
	 * @param victimUuid UUID del jugador que murió.
	 */
	public void processKill(UUID killerUuid, UUID victimUuid) {
		ClanPlayer killer = playerCache.get(killerUuid);
		ClanPlayer victim = playerCache.get(victimUuid);

		double killPts = plugin.getConfigManager().getKillPoints();
		double deathPts = plugin.getConfigManager().getDeathPoints();
		int xpPerKill = plugin.getConfigManager().getXpPerKill();

		// --- Actualizar killer ---
		if (killer != null) {
			killer.registerKill();
			killer.addPoints(killPts);

			if (killer.hasClan()) {
				Clan killerClan = clanById.get(killer.getClanId());
				if (killerClan != null) {
					killerClan.onMemberKill(killer.getName(), killer.getKillstreak(), killPts, xpPerKill);
					// Comprobar subida de nivel
					String formula = plugin.getConfigManager().getXpFormula();
					int maxLevel = plugin.getConfigManager().getMaxLevel();
					int slotsPerLvl = plugin.getConfigManager().getSlotsPerLevel();

					if (killerClan.tryLevelUp(formula, maxLevel, slotsPerLvl)) {
						// Notificar al clan del subida de nivel
						notifyClan(killerClan, "level.up", "{level}", String.valueOf(killerClan.getLevel()));
						// Persistir (ya se guarda en el auto-save periódico,
						// pero forzamos para el nivel)
						storage.updateClan(killerClan);
					} else {
						storage.updateClan(killerClan);
					}
				}
				storage.saveClanPlayer(killer);
			}
		}

		// --- Actualizar víctima ---
		if (victim != null) {
			victim.registerDeath();
			victim.subtractPoints(deathPts);

			if (victim.hasClan()) {
				Clan victimClan = clanById.get(victim.getClanId());
				if (victimClan != null) {
					victimClan.onMemberDeath(deathPts);
					storage.updateClan(victimClan);
				}
				storage.saveClanPlayer(victim);
			}
		}
	}

	// -------------------------------------------------------
	// Consultas de relación
	// -------------------------------------------------------

	public boolean areInSameClan(UUID a, UUID b) {
		ClanPlayer ca = playerCache.get(a), cb = playerCache.get(b);
		if (ca == null || cb == null || !ca.hasClan() || !cb.hasClan())
			return false;
		return ca.getClanId().equals(cb.getClanId());
	}

	public boolean areAllied(UUID a, UUID b) {
		ClanPlayer ca = playerCache.get(a), cb = playerCache.get(b);
		if (ca == null || cb == null || !ca.hasClan() || !cb.hasClan())
			return false;
		Clan clanA = clanById.get(ca.getClanId());
		return clanA != null && clanA.isAlliedWith(cb.getClanId());
	}

	// -------------------------------------------------------
	// Top
	// -------------------------------------------------------

	public CompletableFuture<List<Clan>> getTopByPoints(int limit) {
		return storage.getTopByPoints(limit);
	}

	public CompletableFuture<List<Clan>> getTopByKills(int limit) {
		return storage.getTopByKills(limit);
	}

	public CompletableFuture<List<Clan>> getTopByLevel(int limit) {
		return storage.getTopByLevel(limit);
	}

	// -------------------------------------------------------
	// Utilidades
	// -------------------------------------------------------

	/** Envía un mensaje a todos los miembros online de un clan. */
	public void notifyClan(Clan clan, String msgPath, String... replacements) {
		clan.getMembers().keySet().forEach(uuid -> {
			Player p = plugin.getServer().getPlayer(uuid);
			if (p != null && p.isOnline()) {
				p.sendMessage(plugin.getConfigManager().getMessage(msgPath, replacements));
			}
		});
	}

	/** Número de miembros online de un clan. */
	public int getOnlineCount(Clan clan) {
		int count = 0;
		for (UUID uuid : clan.getMembers().keySet()) {
			if (plugin.getServer().getPlayer(uuid) != null)
				count++;
		}
		return count;
	}

	/** Nombre del líder (desde caché o OfflinePlayer). */
	public String getLeaderName(Clan clan) {
		ClanPlayer cp = playerCache.get(clan.getLeaderUuid());
		if (cp != null)
			return cp.getName();
		org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(clan.getLeaderUuid());
		return op.getName() != null ? op.getName() : "Desconocido";
	}
}