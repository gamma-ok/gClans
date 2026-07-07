package me.gamma.clans.models;

import java.util.UUID;

public class ClanPlayer {

	private final UUID uuid;
	private String name;
	private String clanId;
	private Rank rank;
	private boolean clanChatActive;
	private boolean allyChatActive;
	private int kills;
	private int deaths;
	private double points;
	private int killstreak;
	private int bestKillstreak;
	private int clanKills;
	private long createCooldownUntil;

	public ClanPlayer(UUID uuid, String name, String clanId, Rank rank, int kills, int deaths, double points,
			int bestKillstreak, long createCooldownUntil, int clanKills) {
		this.uuid = uuid;
		this.name = name;
		this.clanId = clanId;
		this.rank = rank;
		this.kills = kills;
		this.deaths = deaths;
		this.points = points;
		this.killstreak = 0;
		this.bestKillstreak = bestKillstreak;
		this.createCooldownUntil = createCooldownUntil;
		this.clanKills = clanKills;
	}

	public ClanPlayer(UUID uuid, String name, String clanId, Rank rank, int kills, int deaths, double points,
			int bestKillstreak, long createCooldownUntil) {
		this(uuid, name, clanId, rank, kills, deaths, points, bestKillstreak, createCooldownUntil, 0);
	}

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

	public void joinClan(String clanId, Rank rank) {
		this.clanId = clanId;
		this.rank = rank;
		this.clanKills = 0;
	}

	public void leaveClan() {
		this.clanId = null;
		this.rank = null;
		this.clanChatActive = false;
		this.allyChatActive = false;
		this.killstreak = 0;
		this.clanKills = 0;
	}

	public void registerKill() {
		kills++;
		clanKills++;
		killstreak++;
		if (killstreak > bestKillstreak)
			bestKillstreak = killstreak;
	}

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

	public int getClanKills() {
		return clanKills;
	}

	public void setClanKills(int v) {
		this.clanKills = v;
	}

	public long getCreateCooldownUntil() {
		return createCooldownUntil;
	}

	public void setCreateCooldownUntil(long ms) {
		this.createCooldownUntil = ms;
	}
}