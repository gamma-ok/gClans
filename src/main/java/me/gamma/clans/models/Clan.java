package me.gamma.clans.models;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Modelo central de un Clan. Incluye sistema de niveles, XP, puntos flotantes,
 * toggle PvP y fecha de creación.
 */
public class Clan {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

	// -------------------------------------------------------
	// Identidad (inmutable)
	// -------------------------------------------------------
	private final String id;
	private final long createdAt; // timestamp de creación

	// -------------------------------------------------------
	// Datos mutables
	// -------------------------------------------------------
	private String name;
	private String prefix; // prefijo coloreado (con §)
	private String color; // código de color sin '&' (ej: "e")
	private UUID leaderUuid;

	// -------------------------------------------------------
	// Miembros y aliados
	// -------------------------------------------------------
	private final Map<UUID, Rank> members = new LinkedHashMap<>();
	private final Set<String> allies = new HashSet<>();
	private final Set<String> pendingAllyRequests = new HashSet<>();

	// -------------------------------------------------------
	// Estadísticas del clan (agregado de miembros)
	// -------------------------------------------------------
	private int totalKills;
	private int totalDeaths;
	private double totalPoints; // puede ser negativo

	// -------------------------------------------------------
	// Sistema de nivel
	// -------------------------------------------------------
	private int level;
	private int xp;
	private int slots; // máximo de miembros permitidos

	// -------------------------------------------------------
	// Mejor killstreak (jugador + valor)
	// -------------------------------------------------------
	private int bestKillstreak;
	private String bestKillstreakPlayer; // nombre del jugador

	// -------------------------------------------------------
	// Toggle PvP entre miembros
	// -------------------------------------------------------
	private boolean pvpEnabled; // false por defecto: miembros NO se dañan

	// -------------------------------------------------------
	// Constructor (nuevo clan)
	// -------------------------------------------------------
	public Clan(String id, String name, String prefix, String color, UUID founderUuid, int defaultSlots) {
		this.id = id;
		this.name = name;
		this.prefix = prefix;
		this.color = color;
		this.leaderUuid = founderUuid;
		this.createdAt = System.currentTimeMillis();
		this.level = 1;
		this.xp = 0;
		this.slots = defaultSlots;
		this.totalKills = 0;
		this.totalDeaths = 0;
		this.totalPoints = 0.0;
		this.bestKillstreak = 0;
		this.bestKillstreakPlayer = "";
		this.pvpEnabled = false;
		this.members.put(founderUuid, Rank.LEADER);
	}

	/**
	 * Constructor de reconstrucción desde BD. Los miembros y aliados se añaden por
	 * separado.
	 */
	public Clan(String id, String name, String prefix, String color, UUID leaderUuid, long createdAt, int level, int xp,
			int slots, int totalKills, int totalDeaths, double totalPoints, int bestKillstreak,
			String bestKillstreakPlayer, boolean pvpEnabled) {
		this.id = id;
		this.name = name;
		this.prefix = prefix;
		this.color = color;
		this.leaderUuid = leaderUuid;
		this.createdAt = createdAt;
		this.level = level;
		this.xp = xp;
		this.slots = slots;
		this.totalKills = totalKills;
		this.totalDeaths = totalDeaths;
		this.totalPoints = totalPoints;
		this.bestKillstreak = bestKillstreak;
		this.bestKillstreakPlayer = bestKillstreakPlayer != null ? bestKillstreakPlayer : "";
		this.pvpEnabled = pvpEnabled;
	}

	// -------------------------------------------------------
	// Gestión de miembros
	// -------------------------------------------------------

	public void addMember(UUID uuid, Rank rank) {
		members.put(uuid, rank);
	}

	public void removeMember(UUID uuid) {
		members.remove(uuid);
	}

	public boolean isMember(UUID uuid) {
		return members.containsKey(uuid);
	}

	public Rank getMemberRank(UUID uuid) {
		return members.get(uuid);
	}

	public int getMemberCount() {
		return members.size();
	}

	public void setMemberRank(UUID uuid, Rank rank) {
		if (!members.containsKey(uuid))
			throw new IllegalArgumentException("Not a member");
		members.put(uuid, rank);
	}

	/**
	 * Transfiere el liderazgo de forma atómica. El líder actual baja a CO_LEADER;
	 * el nuevo sube a LEADER.
	 */
	public void transferLeadership(UUID newLeader) {
		if (!members.containsKey(newLeader))
			throw new IllegalArgumentException("Not a member");
		members.put(this.leaderUuid, Rank.CO_LEADER);
		members.put(newLeader, Rank.LEADER);
		this.leaderUuid = newLeader;
	}

	public boolean isFull() {
		return members.size() >= slots;
	}

	// -------------------------------------------------------
	// Alianzas
	// -------------------------------------------------------

	public void addAlly(String clanId) {
		allies.add(clanId);
		pendingAllyRequests.remove(clanId);
	}

	public void removeAlly(String clanId) {
		allies.remove(clanId);
	}

	public boolean isAlliedWith(String clanId) {
		return allies.contains(clanId);
	}

	public void addPendingRequest(String clanId) {
		pendingAllyRequests.add(clanId);
	}

	public boolean hasPendingRequestTo(String id) {
		return pendingAllyRequests.contains(id);
	}

	// -------------------------------------------------------
	// Estadísticas
	// -------------------------------------------------------

	/** Registra una kill: actualiza totales, XP, puntos y mejor killstreak. */
	public void onMemberKill(String killerName, int killerStreak, double killPoints, int xpPerKill) {
		totalKills++;
		totalPoints += killPoints;
		xp += xpPerKill;
		if (killerStreak > bestKillstreak) {
			bestKillstreak = killerStreak;
			bestKillstreakPlayer = killerName;
		}
	}

	/** Registra una muerte: descuenta puntos (el XP/nivel NO bajan). */
	public void onMemberDeath(double deathPoints) {
		totalDeaths++;
		totalPoints -= deathPoints;
	}

	// -------------------------------------------------------
	// Nivel
	// -------------------------------------------------------

	/**
	 * Calcula la XP necesaria para subir al siguiente nivel usando la fórmula de
	 * config. Reemplaza %level% con el nivel actual. La fórmula se evalúa aquí de
	 * forma simple (multiplicación y suma).
	 */
	public int xpForNextLevel(String formula) {
		String resolved = formula.replace("%level%", String.valueOf(level));
		try {
			return (int) evalSimpleFormula(resolved);
		} catch (Exception e) {
			return level * 10; // fallback seguro
		}
	}

	/**
	 * Comprueba si el clan puede subir de nivel y lo hace si corresponde.
	 *
	 * @param formula       Fórmula de XP de config.
	 * @param maxLevel      Nivel máximo de config.
	 * @param slotsPerLevel Slots que se añaden por nivel.
	 * @return true si subió de nivel.
	 */
	public boolean tryLevelUp(String formula, int maxLevel, int slotsPerLevel) {
		if (level >= maxLevel)
			return false;
		int needed = xpForNextLevel(formula);
		if (xp >= needed) {
			xp -= needed;
			level++;
			slots += slotsPerLevel;
			return true;
		}
		return false;
	}

	// -------------------------------------------------------
	// Formateo
	// -------------------------------------------------------

	public String getCreatedAtFormatted() {
		return DATE_FORMAT.format(new Date(createdAt));
	}

	/**
	 * Retorna el prefijo con el color del clan antepuesto. Ej: si color="e" y
	 * prefix="[ELT]" → "§e[ELT]"
	 */
	public String getColoredPrefix() {
		return "§" + color + prefix;
	}

	/**
	 * Retorna el nombre del clan con su color. Ej: "§eElite"
	 */
	public String getColoredName() {
		return "§" + color + name;
	}

	// -------------------------------------------------------
	// Utilidades internas
	// -------------------------------------------------------

	/**
	 * Evaluador minimalista de fórmulas de la forma "N * M", "N + M", "N - M".
	 * Suficiente para las fórmulas de nivel configurables.
	 */
	private double evalSimpleFormula(String formula) {
		formula = formula.trim();
		if (formula.contains("*")) {
			String[] p = formula.split("\\*", 2);
			return Double.parseDouble(p[0].trim()) * Double.parseDouble(p[1].trim());
		}
		if (formula.contains("+")) {
			String[] p = formula.split("\\+", 2);
			return Double.parseDouble(p[0].trim()) + Double.parseDouble(p[1].trim());
		}
		if (formula.contains("-")) {
			String[] p = formula.split("-", 2);
			return Double.parseDouble(p[0].trim()) - Double.parseDouble(p[1].trim());
		}
		return Double.parseDouble(formula);
	}

	// -------------------------------------------------------
	// Getters / Setters
	// -------------------------------------------------------

	public String getId() {
		return id;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public String getName() {
		return name;
	}

	public void setName(String n) {
		this.name = n;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String p) {
		this.prefix = p;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String c) {
		this.color = c;
	}

	public UUID getLeaderUuid() {
		return leaderUuid;
	}

	public Map<UUID, Rank> getMembers() {
		return Collections.unmodifiableMap(members);
	}

	public Set<String> getAllies() {
		return Collections.unmodifiableSet(allies);
	}

	public Set<String> getPendingAllyRequests() {
		return Collections.unmodifiableSet(pendingAllyRequests);
	}

	public int getTotalKills() {
		return totalKills;
	}

	public int getTotalDeaths() {
		return totalDeaths;
	}

	public double getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(double v) {
		this.totalPoints = v;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int l) {
		this.level = l;
	}

	public int getXp() {
		return xp;
	}

	public void setXp(int x) {
		this.xp = x;
	}

	public int getSlots() {
		return slots;
	}

	public void setSlots(int s) {
		this.slots = s;
	}

	public int getBestKillstreak() {
		return bestKillstreak;
	}

	public String getBestKillstreakPlayer() {
		return bestKillstreakPlayer;
	}

	public boolean isPvpEnabled() {
		return pvpEnabled;
	}

	public void setPvpEnabled(boolean b) {
		this.pvpEnabled = b;
	}
}