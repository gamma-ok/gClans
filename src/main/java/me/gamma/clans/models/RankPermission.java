package me.gamma.clans.models;

public enum RankPermission {

    CHAT,
    INVITE,
    KICK,
    ALLY,
    BREAKALLY,
    PVP,
    PROMOTE,
    RENAME,
    PREFIX,
    DISBAND,
    SETLEADER,
    ALL;

	public static RankPermission fromString(String s) {
		if (s == null)
			return null;
		try {
			return valueOf(s.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}