package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.managers.ClanManager;
import me.gamma.clans.managers.RankManager;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import me.gamma.clans.models.RankPermission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class AbstractClanCommand {

	protected final Clans plugin;
	protected final ConfigManager cfg;
	protected final ClanManager cm;
	protected final RankManager rankManager;
	private final String name;
	private final String permission;
	private final RankPermission requiredRankPerm;
	private final Rank minRank;
	private final boolean needsClan;

	protected AbstractClanCommand(Clans plugin, String name, String permission, RankPermission requiredRankPerm,
			boolean needsClan) {
		this.plugin = plugin;
		this.cfg = plugin.getConfigManager();
		this.cm = plugin.getClanManager();
		this.rankManager = plugin.getRankManager();
		this.name = name;
		this.permission = permission;
		this.requiredRankPerm = requiredRankPerm;
		this.minRank = null;
		this.needsClan = needsClan;
	}

	protected AbstractClanCommand(Clans plugin, String name, String permission, Rank minRank, boolean needsClan) {
		this.plugin = plugin;
		this.cfg = plugin.getConfigManager();
		this.cm = plugin.getClanManager();
		this.rankManager = plugin.getRankManager();
		this.name = name;
		this.permission = permission;
		this.requiredRankPerm = null;
		this.minRank = minRank;
		this.needsClan = needsClan;
	}

	public boolean handle(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(cfg.getMessage("general.player-only"));
			return true;
		}
		Player player = (Player) sender;

		if (permission != null && !player.hasPermission(permission)) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		ClanPlayer cp = cm.getPlayer(player.getUniqueId());
		if (cp == null) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		if (needsClan && !cp.hasClan()) {
			player.sendMessage(cfg.getMessage("general.not-in-clan"));
			return true;
		}

		if (requiredRankPerm != null && needsClan) {
			if (cp.getRank() == null || !rankManager.hasPermission(cp.getRank(), requiredRankPerm)) {
				player.sendMessage(cfg.getMessage("general.no-permission"));
				return true;
			}
		}

		if (minRank != null && needsClan && !cp.hasRankAtLeast(minRank)) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		execute(player, cp, args);
		return true;
	}

	protected abstract void execute(Player player, ClanPlayer cp, String[] args);

	protected void msg(Player p, String path, String... replacements) {
		p.sendMessage(cfg.getMessage(path, replacements));
	}

	protected void raw(Player p, String path, String... replacements) {
		p.sendMessage(cfg.getRaw(path, replacements));
	}

	protected void send(Player p, String text) {
		p.sendMessage(ConfigManager.color(text));
	}

	protected boolean requireArgs(Player p, String[] args, int min) {
		if (args.length < min) {
			msg(p, "general.invalid-usage");
			return false;
		}
		return true;
	}

	protected boolean requireArgs(Player p, String[] args, int min, String usagePath) {
		if (args.length < min) {
			msg(p, "usage." + usagePath);
			return false;
		}
		return true;
	}

	protected String formatPoints(double pts) {
		if (pts == Math.floor(pts) && !Double.isInfinite(pts)) {
			return String.valueOf((long) pts);
		}
		return String.format("%.1f", pts);
	}

	public String getName() {
		return name;
	}
}