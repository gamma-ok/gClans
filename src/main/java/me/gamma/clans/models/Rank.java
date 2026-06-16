package me.gamma.clans.models;

/**
 * Jerarquía de rangos del clan. MEMBER(0) < CO_LEADER(1) < LEADER(2)
 */
public enum Rank {

	MEMBER("Member", "&9", 0), CO_LEADER("Co-Leader", "&c", 1), LEADER("Leader", "&a", 2);

	private final String displayName;
	private final String colorCode;
	private final int ladder;

	Rank(String displayName, String colorCode, int ladder) {
		this.displayName = displayName;
		this.colorCode = colorCode;
		this.ladder = ladder;
	}

	// -------------------------------------------------------
	// API
	// -------------------------------------------------------

	/** Comprueba si este rango es >= al requerido. */
	public boolean isAtLeast(Rank required) {
		return this.ladder >= required.ladder;
	}

	/** Siguiente rango en la jerarquía, o null si es LEADER. */
	public Rank next() {
		switch (this) {
		case MEMBER:
			return CO_LEADER;
		case CO_LEADER:
			return LEADER;
		default:
			return null;
		}
	}

	/** Rango anterior, o null si es MEMBER. */
	public Rank previous() {
		switch (this) {
		case CO_LEADER:
			return MEMBER;
		case LEADER:
			return CO_LEADER;
		default:
			return null;
		}
	}

	public String getColoredName() {
		return colorCode.replace("&", "§") + displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getColorCode() {
		return colorCode;
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