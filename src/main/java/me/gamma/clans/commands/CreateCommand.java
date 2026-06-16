package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

public class CreateCommand extends AbstractClanCommand {

	public CreateCommand(Clans plugin) {
		super(plugin, "create", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		// No puede estar en un clan
		if (cp.hasClan()) {
			msg(player, "general.already-in-clan");
			return;
		}

		// Cooldown anti-recreación
		if (cp.isOnCooldown()) {
			msg(player, "cooldown.create", "{time}", formatCooldown(cp.getCooldownRemainingSeconds()));
			return;
		}

		String name = args[0];

		// Validar nombre
		String nameError = cm.validateName(name);
		if (nameError != null) {
			msg(player, nameError, "{min}", String.valueOf(cfg.getNameMin()), "{max}", String.valueOf(cfg.getNameMax()),
					"{clan}", name);
			return;
		}

		// Prefijo: segundo arg o nombre truncado
		String prefix = args.length >= 2 ? args[1] : name.substring(0, Math.min(name.length(), cfg.getPrefixMax()));

		// Validar prefijo
		String prefixError = cm.validatePrefix(prefix);
		if (prefixError != null) {
			msg(player, prefixError, "{min}", String.valueOf(cfg.getPrefixMin()), "{max}",
					String.valueOf(cfg.getPrefixMax()));
			return;
		}

		cm.createClan(player, name, prefix).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
				() -> msg(player, "clan.create.success", "{clan}", name))).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private String formatCooldown(long seconds) {
		if (seconds < 60)
			return seconds + "s";
		if (seconds < 3600)
			return (seconds / 60) + "m " + (seconds % 60) + "s";
		long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
		return h + "h " + m + "m " + s + "s";
	}
}