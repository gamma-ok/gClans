package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.managers.ClanManager;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base para todos los subcomandos de /clan. Centraliza: verificación de
 * jugador, permiso Bukkit, clan y rango mínimo.
 */
public abstract class AbstractClanCommand {

	protected final Clans plugin;
	protected final ConfigManager cfg;
	protected final ClanManager cm;

	private final String name;
	private final String permission;
	private final Rank minRank;
	private final boolean needsClan;

	protected AbstractClanCommand(Clans plugin, String name, String permission, Rank minRank, boolean needsClan) {
		this.plugin = plugin;
		this.cfg = plugin.getConfigManager();
		this.cm = plugin.getClanManager();
		this.name = name;
		this.permission = permission;
		this.minRank = minRank;
		this.needsClan = needsClan;
	}

	// -------------------------------------------------------
	// Entry point desde el router
	// -------------------------------------------------------

	public boolean handle(CommandSender sender, String[] args) {
		// Solo jugadores
		if (!(sender instanceof Player)) {
			sender.sendMessage(cfg.getMessage("general.player-only"));
			return true;
		}
		Player player = (Player) sender;

		// Permiso Bukkit
		if (permission != null && !player.hasPermission(permission)) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		// ClanPlayer siempre debe estar en caché si el jugador está online
		ClanPlayer cp = cm.getPlayer(player.getUniqueId());
		if (cp == null) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		// Requiere clan
		if (needsClan && !cp.hasClan()) {
			player.sendMessage(cfg.getMessage("general.not-in-clan"));
			return true;
		}

		// Rango mínimo
		if (minRank != null && needsClan && !cp.hasRankAtLeast(minRank)) {
			player.sendMessage(cfg.getMessage("general.no-permission"));
			return true;
		}

		execute(player, cp, args);
		return true;
	}

	protected abstract void execute(Player player, ClanPlayer cp, String[] args);

	// -------------------------------------------------------
	// Helpers para subclases
	// -------------------------------------------------------

	/** Envía mensaje con prefijo y reemplazos. */
	protected void msg(Player p, String path, String... replacements) {
		p.sendMessage(cfg.getMessage(path, replacements));
	}

	/** Envía mensaje SIN prefijo (para líneas de info, listas, etc.). */
	protected void raw(Player p, String path, String... replacements) {
		p.sendMessage(cfg.getRaw(path, replacements));
	}

	/** Envía un string ya coloreado directamente. */
	protected void send(Player p, String text) {
		p.sendMessage(ConfigManager.color(text));
	}

	/** Retorna false y envía "uso incorrecto" si no hay suficientes args. */
	protected boolean requireArgs(Player p, String[] args, int min) {
		if (args.length < min) {
			msg(p, "general.invalid-usage");
			return false;
		}
		return true;
	}

	/** Formatea puntos con 1 decimal si tiene decimales, entero si no. */
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