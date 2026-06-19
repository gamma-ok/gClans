package me.gamma.clans.models;

public enum Rank {

	MEMBER("Member", "&9", 0), CAPTAIN("Captain", "&b", 1), CO_LEADER("Co-Leader", "&c", 2), LEADER("Leader", "&6", 3);

	private final String defaultDisplayName;
	private final String defaultColorCode;
	private final int ladder;

	Rank(String defaultDisplayName, String defaultColorCode, int ladder) {
		this.defaultDisplayName = defaultDisplayName;
		this.defaultColorCode = defaultColorCode;
		this.ladder = ladder;
	}

	public boolean isAtLeast(Rank required) {
		return this.ladder >= required.ladder;
	}

	public Rank next() {
		switch (this) {
		case MEMBER:
			return CAPTAIN;
		case CAPTAIN:
			return CO_LEADER;
		case CO_LEADER:
			return LEADER;
		default:
			return null;
		}
	}

	public Rank previous() {
		switch (this) {
		case CAPTAIN:
			return MEMBER;
		case CO_LEADER:
			return CAPTAIN;
		case LEADER:
			return CO_LEADER;
		default:
			return null;
		}
	}

	public String getColoredName() {
		return defaultColorCode.replace("&", "§") + defaultDisplayName;
	}

	public String getDisplayName() {
		return defaultDisplayName;
	}

	public String getColorCode() {
		return defaultColorCode;
	}

	public int getLadder() {
		return ladder;
	}

	public static Rank fromString(String s) {
		if (s == null)
			return null;
		for (Rank r : values()) {
			if (r.name().equalsIgnoreCase(s))
				return r;
		}
		return null;
	}
}