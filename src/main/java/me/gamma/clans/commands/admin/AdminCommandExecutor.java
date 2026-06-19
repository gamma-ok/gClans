package me.gamma.clans.commands.admin;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.managers.ClanManager;
import me.gamma.clans.managers.ConfirmationManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class AdminCommandExecutor implements CommandExecutor, TabCompleter {

	private final Clans plugin;
	private final ConfigManager cfg;
	private final ClanManager cm;
	private final AdminHelpCommand helpCmd;

	private final Map<UUID, PendingAdminAction> pendingActions = new HashMap<>();

	private static class PendingAdminAction {
		enum Type {
			DELETE, SET_LEADER
		}

		final Type type;
		final Object payload;
		final long expiresAt;
		int taskId = -1;

		PendingAdminAction(Type type, Object payload, long expiresAt) {
			this.type = type;
			this.payload = payload;
			this.expiresAt = expiresAt;
		}
	}

	public AdminCommandExecutor(Clans plugin) {
		this.plugin = plugin;
		this.cfg = plugin.getConfigManager();
		this.cm = plugin.getClanManager();
		this.helpCmd = new AdminHelpCommand(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ConfigManager.color("&cEste comando solo puede ser usado por jugadores."));
			return true;
		}

		Player player = (Player) sender;

		if (!player.hasPermission("gclans.admin")) {
			player.sendMessage(cfg.getMessage("admin.no-permission"));
			return true;
		}

		if (args.length == 0) {
			helpCmd.execute(player, new String[0]);
			return true;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
		case "help":
			handleHelp(player, args);
			break;
		case "delete":
			handleDelete(player, args);
			break;
		case "rename":
			handleRename(player, args);
			break;
		case "prefix":
			handlePrefix(player, args);
			break;
		case "setleader":
			handleSetLeader(player, args);
			break;
		case "add":
			handleAdd(player, args);
			break;
		case "kick":
			handleKick(player, args);
			break;
		case "setrank":
			handleSetRank(player, args);
			break;
		case "cooldown":
			handleCooldown(player, args);
			break;
		case "points":
			handleStat(player, args, "points");
			break;
		case "kills":
			handleStat(player, args, "kills");
			break;
		case "level":
			handleStat(player, args, "level");
			break;
		case "xp":
			handleStat(player, args, "xp");
			break;
		case "reset":
			handleReset(player, args);
			break;
		case "slots":
			handleSlots(player, args);
			break;
		case "ally":
			handleAlly(player, args);
			break;
		case "reload":
			handleReload(player);
			break;
		default:
			player.sendMessage(cfg.getMessage("admin.unknown"));
		}
		return true;
	}

	private void handleHelp(Player player, String[] args) {
		String[] pageArgs = args.length >= 2 ? new String[] { args[1] } : new String[0];
		helpCmd.execute(player, pageArgs);
	}

	private void handleDelete(Player player, String[] args) {
		if (args.length < 2) {
			usage(player, "/clan admin delete <clan> [confirm]");
			return;
		}

		String clanName = args[1];
		boolean isConfirm = args.length >= 3 && args[2].equalsIgnoreCase("confirm");

		if (isConfirm) {
			PendingAdminAction pending = pendingActions.get(player.getUniqueId());
			if (pending == null || pending.type != PendingAdminAction.Type.DELETE
					|| System.currentTimeMillis() > pending.expiresAt) {
				pendingActions.remove(player.getUniqueId());
				player.sendMessage(cfg.getMessage("admin.delete.no-pending"));
				return;
			}

			String clanId = (String) pending.payload;
			cancelPending(player.getUniqueId());

			Clan clan = cm.getClan(clanId);
			String name = clan != null ? clan.getName() : clanName;

			if (clan != null) {
				clan.getMembers().keySet().forEach(uuid -> {
					Player m = plugin.getServer().getPlayer(uuid);
					if (m != null && !m.getUniqueId().equals(player.getUniqueId())) {
						m.sendMessage(cfg.getMessage("clan.disband.success", "{clan}", name));
					}
				});
			}

			cm.disbandClan(clanId).exceptionally(ex -> {
				plugin.getLogger().severe("[Admin] Error eliminando clan: " + ex.getMessage());
				return null;
			});

			player.sendMessage(cfg.getMessage("admin.delete.success", "{clan}", name));
			return;
		}

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.delete.not-found", "{clan}", clanName));
			return;
		}

		int timeout = cfg.getConfirmTimeout();
		registerPending(player.getUniqueId(), PendingAdminAction.Type.DELETE, clan.getId(), timeout);

		player.sendMessage(
				cfg.getMessage("admin.delete.confirm", "{clan}", clan.getName(), "{time}", String.valueOf(timeout)));
	}

	private void handleRename(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin rename <clan> <nuevo-nombre>");
			return;
		}

		String oldName = args[1];
		String newName = args[2];

		Clan clan = cm.getClanByName(oldName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.rename.not-found", "{clan}", oldName));
			return;
		}

		String error = cm.validateName(newName);
		if (error != null) {
			String key = error.contains("taken") ? "admin.rename.taken"
					: error.contains("invalid") ? "admin.rename.invalid" : "admin.rename.not-found";
			player.sendMessage(cfg.getMessage(key, "{clan}", newName));
			return;
		}

		cm.renameClan(clan.getId(), newName).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
				() -> player.sendMessage(cfg.getMessage("admin.rename.success", "{old}", oldName, "{new}", newName))))
				.exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private void handlePrefix(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin prefix <clan> <prefix>");
			return;
		}

		String clanName = args[1];
		String newPrefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.prefix.not-found", "{clan}", clanName));
			return;
		}

		String error = cm.validatePrefix(newPrefix);
		if (error != null) {
			player.sendMessage(cfg.getMessage("admin.prefix.invalid"));
			return;
		}

		String colored = ConfigManager.color(newPrefix);
		cm.setClanPrefix(clan.getId(), colored)
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
						() -> player.sendMessage(
								cfg.getMessage("admin.prefix.success", "{clan}", clanName, "{prefix}", colored))))
				.exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private void handleSetLeader(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin setleader <clan> <jugador> [confirm]");
			return;
		}

		String clanName = args[1];
		String targetName = args[2];
		boolean isConfirm = args.length >= 4 && args[3].equalsIgnoreCase("confirm");

		if (isConfirm) {
			PendingAdminAction pending = pendingActions.get(player.getUniqueId());
			if (pending == null || pending.type != PendingAdminAction.Type.SET_LEADER
					|| System.currentTimeMillis() > pending.expiresAt) {
				pendingActions.remove(player.getUniqueId());
				player.sendMessage(cfg.getMessage("admin.setleader.no-pending"));
				return;
			}

			String[] payload = (String[]) pending.payload;
			String clanId = payload[0];
			String newLeaderStr = payload[1];
			String newLeaderName = payload[2];
			cancelPending(player.getUniqueId());

			cm.transferLeadership(clanId, UUID.fromString(newLeaderStr))
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(
							cfg.getMessage("admin.setleader.success", "{clan}", clanName, "{player}", newLeaderName))))
					.exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});
			return;
		}

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.setleader.not-found", "{clan}", clanName));
			return;
		}

		Player target = plugin.getServer().getPlayer(targetName);
		if (target == null) {
			player.sendMessage(cfg.getMessage("general.player-not-found", "{player}", targetName));
			return;
		}

		if (!clan.isMember(target.getUniqueId())) {
			player.sendMessage(cfg.getMessage("admin.setleader.no-member", "{player}", targetName, "{clan}", clanName));
			return;
		}

		int timeout = cfg.getConfirmTimeout();
		registerPending(player.getUniqueId(), PendingAdminAction.Type.SET_LEADER,
				new String[] { clan.getId(), target.getUniqueId().toString(), target.getName() }, timeout);

		player.sendMessage(cfg.getMessage("admin.setleader.confirm", "{clan}", clanName, "{player}", targetName,
				"{time}", String.valueOf(timeout)));
	}

	private void handleAdd(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin add <jugador> <clan>");
			return;
		}

		String targetName = args[1];
		String clanName = args[2];

		Player target = plugin.getServer().getPlayer(targetName);
		if (target == null) {
			player.sendMessage(cfg.getMessage("admin.add.not-found-player", "{player}", targetName));
			return;
		}

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.add.not-found-clan", "{clan}", clanName));
			return;
		}

		ClanPlayer targetCp = cm.getPlayer(target.getUniqueId());
		if (targetCp != null && targetCp.hasClan()) {
			player.sendMessage(cfg.getMessage("admin.add.already-in-clan", "{player}", targetName));
			return;
		}

		if (clan.isFull()) {
			player.sendMessage(cfg.getMessage("admin.add.clan-full", "{clan}", clanName));
			return;
		}

		cm.addMember(clan.getId(), target.getUniqueId())
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
						() -> player.sendMessage(
								cfg.getMessage("admin.add.success", "{player}", targetName, "{clan}", clanName))))
				.exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private void handleKick(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin kick <jugador> <clan>");
			return;
		}

		String targetName = args[1];
		String clanName = args[2];

		Player target = plugin.getServer().getPlayer(targetName);
		UUID targetUuid = null;

		if (target != null) {
			targetUuid = target.getUniqueId();
		} else {
			org.bukkit.OfflinePlayer offline = plugin.getServer().getOfflinePlayer(targetName);
			if (offline.hasPlayedBefore())
				targetUuid = offline.getUniqueId();
		}

		if (targetUuid == null) {
			player.sendMessage(cfg.getMessage("admin.kick.not-found-player", "{player}", targetName));
			return;
		}

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.kick.not-found-clan", "{clan}", clanName));
			return;
		}

		if (!clan.isMember(targetUuid)) {
			player.sendMessage(cfg.getMessage("admin.kick.not-member", "{player}", targetName, "{clan}", clanName));
			return;
		}

		final UUID finalUuid = targetUuid;
		cm.removeMember(finalUuid)
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
						() -> player.sendMessage(
								cfg.getMessage("admin.kick.success", "{player}", targetName, "{clan}", clanName))))
				.exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private void handleSetRank(Player player, String[] args) {
		if (args.length < 3) {
			usage(player, "/clan admin setrank <jugador> <rango>");
			return;
		}

		String targetName = args[1];
		String rankStr = args[2].toUpperCase();

		switch (rankStr) {
		case "COLEADER":
			rankStr = "CO_LEADER";
			break;
		case "CO-LEADER":
			rankStr = "CO_LEADER";
			break;
		}

		Rank newRank = Rank.fromString(rankStr);
		if (newRank == null) {
			player.sendMessage(cfg.getMessage("admin.setrank.invalid-rank"));
			return;
		}

		Player target = plugin.getServer().getPlayer(targetName);
		if (target == null) {
			player.sendMessage(cfg.getMessage("admin.setrank.not-found-player", "{player}", targetName));
			return;
		}

		ClanPlayer targetCp = cm.getPlayer(target.getUniqueId());
		if (targetCp == null || !targetCp.hasClan()) {
			player.sendMessage(cfg.getMessage("admin.setrank.not-in-clan", "{player}", targetName));
			return;
		}

		Clan targetClan = cm.getClan(targetCp.getClanId());
		if (targetClan != null && targetCp.getRank() == Rank.LEADER && newRank != Rank.LEADER
				&& targetClan.getMemberCount() == 1) {
			player.sendMessage(cfg.getMessage("admin.setrank.sole-leader", "{player}", targetName));
			return;
		}

		cm.setMemberRank(targetCp.getClanId(), target.getUniqueId(), newRank).thenRun(() -> plugin.getServer()
				.getScheduler().runTask(plugin, () -> player.sendMessage(cfg.getMessage("admin.setrank.success",
						"{player}", targetName, "{rank}", plugin.getRankManager().getDisplayName(newRank)))))
				.exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}

	private void handleCooldown(Player player, String[] args) {
		if (args.length < 3 || !args[1].equalsIgnoreCase("clear")) {
			usage(player, "/clan admin cooldown clear <jugador>");
			return;
		}

		String targetName = args[2];
		Player target = plugin.getServer().getPlayer(targetName);
		if (target == null) {
			player.sendMessage(cfg.getMessage("admin.cooldown.not-found", "{player}", targetName));
			return;
		}

		ClanPlayer targetCp = cm.getPlayer(target.getUniqueId());
		if (targetCp == null || !targetCp.isOnCooldown()) {
			player.sendMessage(cfg.getMessage("admin.cooldown.no-cooldown", "{player}", targetName));
			return;
		}

		targetCp.setCreateCooldownUntil(0L);
		plugin.getStorageProvider().saveClanPlayer(targetCp);
		player.sendMessage(cfg.getMessage("admin.cooldown.cleared", "{player}", targetName));
	}

	private void handleStat(Player player, String[] args, String stat) {
		if (args.length < 3) {
			usage(player, "/clan admin " + stat + " add|remove|set <clan> <cantidad>");
			return;
		}

		String action = args[1].toLowerCase();
		String clanName = args[2];

		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin." + stat + ".not-found", "{clan}", clanName));
			return;
		}

		switch (stat) {
		case "points":
			handlePoints(player, clan, action, args);
			break;
		case "kills":
			handleKills(player, clan, action, args);
			break;
		case "level":
			handleLevel(player, clan, action, args);
			break;
		case "xp":
			handleXp(player, clan, action, args);
			break;
		}
	}

	private void handlePoints(Player player, Clan clan, String action, String[] args) {
		switch (action) {
		case "add":
		case "remove":
		case "set":
			if (args.length < 4) {
				usage(player, "/clan admin points " + action + " <clan> <cantidad>");
				return;
			}
			double amount;
			try {
				amount = Double.parseDouble(args[3]);
			} catch (NumberFormatException e) {
				player.sendMessage(cfg.getMessage("admin.points.invalid"));
				return;
			}

			if (action.equals("add"))
				clan.setTotalPoints(clan.getTotalPoints() + amount);
			else if (action.equals("remove"))
				clan.setTotalPoints(clan.getTotalPoints() - amount);
			else
				clan.setTotalPoints(amount);

			persist(clan);
			String msgKey = action.equals("add") ? "admin.points.added"
					: action.equals("remove") ? "admin.points.removed" : "admin.points.set";
			player.sendMessage(cfg.getMessage(msgKey, "{clan}", clan.getName(), "{amount}", formatNum(amount),
					"{total}", formatNum(clan.getTotalPoints())));
			break;
		default:
			usage(player, "/clan admin points add|remove|set <clan> <cantidad>");
		}
	}

	private void handleKills(Player player, Clan clan, String action, String[] args) {
		switch (action) {
		case "add":
		case "remove":
		case "set":
			if (args.length < 4) {
				usage(player, "/clan admin kills " + action + " <clan> <cantidad>");
				return;
			}
			int amount;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				player.sendMessage(cfg.getMessage("admin.kills.invalid"));
				return;
			}

			int current = clan.getTotalKills();
			if (action.equals("add"))
				clan.setTotalKillsAdmin(current + amount);
			else if (action.equals("remove"))
				clan.setTotalKillsAdmin(Math.max(0, current - amount));
			else
				clan.setTotalKillsAdmin(Math.max(0, amount));

			persist(clan);
			String msgKey = action.equals("add") ? "admin.kills.added"
					: action.equals("remove") ? "admin.kills.removed" : "admin.kills.set";
			player.sendMessage(cfg.getMessage(msgKey, "{clan}", clan.getName(), "{amount}", String.valueOf(amount),
					"{total}", String.valueOf(clan.getTotalKills())));
			break;
		default:
			usage(player, "/clan admin kills add|remove|set <clan> <cantidad>");
		}
	}

	private void handleLevel(Player player, Clan clan, String action, String[] args) {
		switch (action) {
		case "set":
			if (args.length < 4) {
				usage(player, "/clan admin level set <clan> <nivel>");
				return;
			}
			int maxLevel = cfg.getMaxLevel();
			int level;
			try {
				level = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				player.sendMessage(cfg.getMessage("admin.level.invalid", "{max}", String.valueOf(maxLevel)));
				return;
			}

			if (level < 1 || level > maxLevel) {
				player.sendMessage(cfg.getMessage("admin.level.invalid", "{max}", String.valueOf(maxLevel)));
				return;
			}
			clan.setLevel(level);
			persist(clan);
			player.sendMessage(
					cfg.getMessage("admin.level.set", "{clan}", clan.getName(), "{amount}", String.valueOf(level)));
			break;
		default:
			usage(player, "/clan admin level set <clan> <nivel>");
		}
	}

	private void handleXp(Player player, Clan clan, String action, String[] args) {
		switch (action) {
		case "add":
		case "set":
			if (args.length < 4) {
				usage(player, "/clan admin xp " + action + " <clan> <cantidad>");
				return;
			}
			int amount;
			try {
				amount = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				player.sendMessage(cfg.getMessage("admin.xp.invalid"));
				return;
			}

			if (action.equals("add"))
				clan.setXp(clan.getXp() + amount);
			else
				clan.setXp(Math.max(0, amount));

			persist(clan);
			String msgKey = action.equals("add") ? "admin.xp.added" : "admin.xp.set";
			player.sendMessage(cfg.getMessage(msgKey, "{clan}", clan.getName(), "{amount}", String.valueOf(amount),
					"{total}", String.valueOf(clan.getXp())));
			break;
		default:
			usage(player, "/clan admin xp add|set <clan> <cantidad>");
		}
	}

	private void handleReset(Player player, String[] args) {
		if (args.length < 2) {
			usage(player, "/clan admin reset <clan>");
			return;
		}

		String clanName = args[1];
		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.reset.not-found", "{clan}", clanName));
			return;
		}

		clan.setTotalPoints(0);
		clan.setTotalKillsAdmin(0);
		clan.setLevel(1);
		clan.setXp(0);

		persist(clan);
		player.sendMessage(cfg.getMessage("admin.reset.success", "{clan}", clanName));
	}

	private void handleSlots(Player player, String[] args) {
		if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
			usage(player, "/clan admin slots set <clan> <cantidad>");
			return;
		}

		String clanName = args[2];
		Clan clan = cm.getClanByName(clanName);
		if (clan == null) {
			player.sendMessage(cfg.getMessage("admin.slots.not-found", "{clan}", clanName));
			return;
		}

		int amount;
		try {
			amount = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			player.sendMessage(cfg.getMessage("admin.slots.invalid"));
			return;
		}

		if (amount < 1) {
			player.sendMessage(cfg.getMessage("admin.slots.invalid"));
			return;
		}

		clan.setSlots(amount);
		persist(clan);
		player.sendMessage(cfg.getMessage("admin.slots.set", "{clan}", clanName, "{amount}", String.valueOf(amount)));
	}

	private void handleAlly(Player player, String[] args) {
		if (args.length < 4) {
			usage(player, "/clan admin ally add|remove <clan1> <clan2>");
			return;
		}

		String action = args[1].toLowerCase();
		String name1 = args[2];
		String name2 = args[3];

		Clan clan1 = cm.getClanByName(name1);
		Clan clan2 = cm.getClanByName(name2);

		if (clan1 == null) {
			player.sendMessage(cfg.getMessage("admin.ally.not-found-1", "{clan}", name1));
			return;
		}
		if (clan2 == null) {
			player.sendMessage(cfg.getMessage("admin.ally.not-found-2", "{clan}", name2));
			return;
		}
		if (clan1.getId().equals(clan2.getId())) {
			player.sendMessage(cfg.getMessage("admin.ally.same-clan"));
			return;
		}

		switch (action) {
		case "add":
			if (clan1.isAlliedWith(clan2.getId())) {
				player.sendMessage(cfg.getMessage("admin.ally.already-allies", "{clan1}", name1, "{clan2}", name2));
				return;
			}
			cm.formAlliance(clan1.getId(), clan2.getId())
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin,
							() -> player.sendMessage(
									cfg.getMessage("admin.ally.added", "{clan1}", name1, "{clan2}", name2))))
					.exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});
			break;

		case "remove":
			if (!clan1.isAlliedWith(clan2.getId())) {
				player.sendMessage(cfg.getMessage("admin.ally.not-allies", "{clan1}", name1, "{clan2}", name2));
				return;
			}
			final Clan finalClan1 = clan1;
			final Clan finalClan2 = clan2;
			cm.breakAlliance(clan1.getId(), clan2.getId())
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
						// Desactivar chat de aliados si los clanes ya no tienen aliados
						disableAllyChatIfNeeded(finalClan1);
						disableAllyChatIfNeeded(finalClan2);
						player.sendMessage(cfg.getMessage("admin.ally.removed", "{clan1}", name1, "{clan2}", name2));
					})).exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});
			break;

		default:
			usage(player, "/clan admin ally add|remove <clan1> <clan2>");
		}
	}

	private void handleReload(Player player) {
		try {
			cfg.reloadAll();
			plugin.getRankManager().reload();
			player.sendMessage(cfg.getMessage("admin.reload.success"));
		} catch (Exception e) {
			plugin.getLogger().severe("[Admin] Error recargando config: " + e.getMessage());
			player.sendMessage(cfg.getMessage("admin.reload.error"));
		}
	}

	private void registerPending(UUID uuid, PendingAdminAction.Type type, Object payload, int timeoutSeconds) {
		cancelPending(uuid);
		long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
		PendingAdminAction action = new PendingAdminAction(type, payload, expiresAt);

		int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			pendingActions.remove(uuid);
			Player p = plugin.getServer().getPlayer(uuid);
			if (p != null && p.isOnline()) {
				String actionName = type == PendingAdminAction.Type.DELETE ? "delete" : "setleader";
				p.sendMessage(cfg.getMessage("confirmation.expired", "{action}", "admin " + actionName, "{time}",
						String.valueOf(timeoutSeconds)));
			}
		}, timeoutSeconds * 20L).getTaskId();

		action.taskId = taskId;
		pendingActions.put(uuid, action);
	}

	private void cancelPending(UUID uuid) {
		PendingAdminAction existing = pendingActions.remove(uuid);
		if (existing != null && existing.taskId != -1) {
			plugin.getServer().getScheduler().cancelTask(existing.taskId);
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (!sender.hasPermission("gclans.admin"))
			return Collections.emptyList();

		List<String> result = new ArrayList<>();
		String partial = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

		if (args.length == 1) {
			Arrays.asList("help", "delete", "rename", "prefix", "setleader", "add", "kick", "setrank", "cooldown",
					"points", "kills", "level", "xp", "slots", "ally", "reset", "reload").stream()
					.filter(s -> s.startsWith(partial)).forEach(result::add);

		} else if (args.length == 2) {
			switch (args[0].toLowerCase()) {
			case "points":
			case "kills":
				Arrays.asList("add", "remove", "set").stream().filter(s -> s.startsWith(partial)).forEach(result::add);
				break;
			case "level":
				if ("set".startsWith(partial))
					result.add("set");
				break;
			case "xp":
				Arrays.asList("add", "set").stream().filter(s -> s.startsWith(partial)).forEach(result::add);
				break;
			case "reset":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "slots":
				if ("set".startsWith(partial))
					result.add("set");
				break;
			case "ally":
				Arrays.asList("add", "remove").stream().filter(s -> s.startsWith(partial)).forEach(result::add);
				break;
			case "cooldown":
				if ("clear".startsWith(partial))
					result.add("clear");
				break;
			case "delete":
			case "rename":
			case "prefix":
			case "setleader":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "add":
			case "kick":
			case "setrank":
				plugin.getServer().getOnlinePlayers().stream()
						.filter(p -> p.getName().toLowerCase().startsWith(partial))
						.forEach(p -> result.add(p.getName()));
				break;
			}
		} else if (args.length == 3) {
			switch (args[0].toLowerCase()) {
			case "add":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "kick":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "setrank":
				Arrays.asList("member", "captain", "coleader", "leader").stream().filter(s -> s.startsWith(partial))
						.forEach(result::add);
				break;
			case "ally":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "points":
			case "kills":
			case "level":
			case "xp":
			case "slots":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			case "cooldown":
				plugin.getServer().getOnlinePlayers().stream()
						.filter(p -> p.getName().toLowerCase().startsWith(partial))
						.forEach(p -> result.add(p.getName()));
				break;
			case "delete":
				if ("confirm".startsWith(partial))
					result.add("confirm");
				break;
			}
		} else if (args.length == 4) {
			switch (args[0].toLowerCase()) {
			case "setleader":
				if ("confirm".startsWith(partial))
					result.add("confirm");
				break;
			case "ally":
				cm.getAllClans().stream().filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
				break;
			}
		} else if (args.length == 5 && args[0].equalsIgnoreCase("setleader")) {
			if ("confirm".startsWith(partial))
				result.add("confirm");
		}

		Collections.sort(result);
		return result;
	}

	private void disableAllyChatIfNeeded(Clan clan) {
		if (!clan.getAllies().isEmpty())
			return;
		clan.getMembers().keySet().forEach(uuid -> {
			me.gamma.clans.models.ClanPlayer cp = cm.getPlayer(uuid);
			if (cp != null && cp.isAllyChatActive()) {
				cp.setAllyChatActive(false);
				Player m = plugin.getServer().getPlayer(uuid);
				if (m != null)
					m.sendMessage(cfg.getMessage("chat.ally.auto-disabled"));
			}
		});
	}

	private void persist(Clan clan) {
		plugin.getStorageProvider().updateClan(clan).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}

	private void usage(Player player, String usage) {
		player.sendMessage(ConfigManager.color("&cUso: &e" + usage));
	}

	private String formatNum(double num) {
		if (num == Math.floor(num) && !Double.isInfinite(num))
			return String.valueOf((long) num);
		return String.format("%.1f", num);
	}
}