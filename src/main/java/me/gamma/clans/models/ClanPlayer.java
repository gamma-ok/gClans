package me.gamma.clans.models;

import java.util.UUID;

/**
 * Representa a un jugador dentro del sistema de clanes. Todos los datos en
 * memoria; persistencia delegada a StorageProvider.
 */
public class ClanPlayer {

	// -------------------------------------------------------
	// Identidad
	// -------------------------------------------------------
	private final UUID uuid;
	private String name;

	// -------------------------------------------------------
	// Estado de clan
	// -------------------------------------------------------
	private String clanId; // null = sin clan
	private Rank rank; // null = sin clan

	// -------------------------------------------------------
	// Chat toggles
	// -------------------------------------------------------
	private boolean clanChatActive;
	private boolean allyChatActive;

	// -------------------------------------------------------
	// Estadísticas personales
	// -------------------------------------------------------
	private int kills;
	private int deaths;
	private double points; // puede ser negativo
	private int killstreak; // racha activa
	private int bestKillstreak; // mejor racha histórica

	// -------------------------------------------------------
	// Cooldown
	// -------------------------------------------------------
	/** Timestamp (ms) hasta el que el jugador no puede crear un clan. */
	private long createCooldownUntil;

	// -------------------------------------------------------
	// Constructor (carga desde BD)
	// -------------------------------------------------------
	public ClanPlayer(UUID uuid, String name, String clanId, Rank rank, int kills, int deaths, double points,
			int bestKillstreak, long createCooldownUntil) {
		this.uuid = uuid;
		this.name = name;
		this.clanId = clanId;
		this.rank = rank;
		this.kills = kills;
		this.deaths = deaths;
		this.points = points;
		this.killstreak = 0; // la racha activa no persiste entre sesiones
		this.bestKillstreak = bestKillstreak;
		this.createCooldownUntil = createCooldownUntil;
	}

	// -------------------------------------------------------
	// API de consulta
	// -------------------------------------------------------

	public boolean hasClan() {
		return clanId != null;
	}

	public boolean hasRankAtLeast(Rank required) {
		return rank != null && rank.isAtLeast(required);
	}

	public boolean isOnCooldown() {
		return System.currentTimeMillis() < createCooldownUntil;
	}

	public long getCooldownRemainingSeconds() {
		return Math.max(0, (createCooldownUntil - System.currentTimeMillis()) / 1000);
	}

	public double getKDRatio() {
		return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
	}

	// -------------------------------------------------------
	// API de mutación
	// -------------------------------------------------------

	public void joinClan(String clanId, Rank rank) {
		this.clanId = clanId;
		this.rank = rank;
	}

	public void leaveClan() {
		this.clanId = null;
		this.rank = null;
		this.clanChatActive = false;
		this.allyChatActive = false;
		this.killstreak = 0;
	}

	/** Registra una kill: incrementa racha, actualiza mejor racha. */
	public void registerKill() {
		kills++;
		killstreak++;
		if (killstreak > bestKillstreak) {
			bestKillstreak = killstreak;
		}
	}

	/** Registra una muerte: resetea racha activa. */
	public void registerDeath() {
		deaths++;
		killstreak = 0;
	}

	public void setClanChatActive(boolean active) {
		this.clanChatActive = active;
		if (active)
			this.allyChatActive = false;
	}

	public void setAllyChatActive(boolean active) {
		this.allyChatActive = active;
		if (active)
			this.clanChatActive = false;
	}

	// -------------------------------------------------------
	// Getters / Setters
	// -------------------------------------------------------

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClanId() {
		return clanId;
	}

	public Rank getRank() {
		return rank;
	}

	public void setRank(Rank rank) {
		this.rank = rank;
	}

	public boolean isClanChatActive() {
		return clanChatActive;
	}

	public boolean isAllyChatActive() {
		return allyChatActive;
	}

	public int getKills() {
		return kills;
	}

	public int getDeaths() {
		return deaths;
	}

	public double getPoints() {
		return points;
	}

	public void addPoints(double v) {
		this.points += v;
	}

	public void subtractPoints(double v) {
		this.points -= v;
	}

	public int getKillstreak() {
		return killstreak;
	}

	public int getBestKillstreak() {
		return bestKillstreak;
	}

	public long getCreateCooldownUntil() {
		return createCooldownUntil;
	}

	public void setCreateCooldownUntil(long ms) {
		this.createCooldownUntil = ms;
	}
}