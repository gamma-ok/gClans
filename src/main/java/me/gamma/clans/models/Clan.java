package me.gamma.clans.models;

import java.text.SimpleDateFormat;
import java.util.*;

public class Clan {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	private final String id;
	private final long createdAt;
	private String name;
	private String prefix;
	private String color;
	private UUID leaderUuid;
	private final Map<UUID, Rank> members = new LinkedHashMap<>();
	private final Set<String> allies = new HashSet<>();
	private final Set<String> pendingAllyRequests = new HashSet<>();
	private int totalKills;
	private int totalDeaths;
	private double totalPoints;
	private int level;
	private int xp;
	private int slots;
	private int bestKillstreak;
	private String bestKillstreakPlayer;
	private boolean pvpEnabled;

	public Clan(String id, String name, String prefix, String color, UUID founderUuid, int defaultSlots) {
		this.id = id;
		this.name = name;
		this.prefix = "";
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

	public void onMemberKill(String killerName, int killerStreak, double killPoints, int xpPerKill) {
		totalKills++;
		totalPoints += killPoints;
		xp += xpPerKill;
		if (killerStreak > bestKillstreak) {
			bestKillstreak = killerStreak;
			bestKillstreakPlayer = killerName;
		}
	}

	public void onMemberDeath(double deathPoints) {
		totalDeaths++;
		totalPoints -= deathPoints;
	}

	public int xpForNextLevel(String formula) {
		String resolved = formula.replace("%level%", String.valueOf(level));
		try {
			return (int) evalSimpleFormula(resolved);
		} catch (Exception e) {
			return level * 10;
		}
	}

	public boolean tryLevelUp(String formula, int maxLevel, int slotsForLevel, int maxSlots) {
		if (level >= maxLevel)
			return false;
		int needed = xpForNextLevel(formula);
		if (xp >= needed) {
			xp -= needed;
			level++;
			if (slotsForLevel > 0) {
				int newSlots = slots + slotsForLevel;
				slots = (maxSlots > 0) ? Math.min(newSlots, maxSlots) : newSlots;
			}
			return true;
		}
		return false;
	}

	public String getCreatedAtFormatted() {
		return DATE_FORMAT.format(new Date(createdAt));
	}

	public String getColoredPrefix() {
		if (prefix.contains("§"))
			return prefix;
		return "§" + color + prefix;
	}

	public boolean hasCustomPrefix() {
		if (prefix == null || prefix.isEmpty())
			return false;

		String stripped = prefix.replaceAll("§[0-9a-fA-FrRkKlLmMnNoO]", "").trim();
		return !stripped.isEmpty();
	}

	public String getColoredName() {
		return "§" + color + name;
	}

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

	public void setTotalKillsAdmin(int v) {
		this.totalKills = v;
	}

	public int getTotalDeaths() {
		return totalDeaths;
	}

	public void setTotalDeaths(int v) {
		this.totalDeaths = v;
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