package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.commands.admin.AdminCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class ClanCommandExecutor implements CommandExecutor, TabCompleter {

	private final Clans plugin;
	private final Map<String, AbstractClanCommand> subs = new LinkedHashMap<>();
	private final HelpCommand helpCommand;
	private final AdminCommandExecutor adminExecutor;

	public ClanCommandExecutor(Clans plugin) {
		this.plugin = plugin;
		this.helpCommand = new HelpCommand(plugin);
		this.adminExecutor = new AdminCommandExecutor(plugin);

		register(helpCommand);
		register(new CreateCommand(plugin));
		register(new AcceptCommand(plugin));
		register(new LeaveCommand(plugin));
		register(new DisbandCommand(plugin));
		register(new RenameCommand(plugin));
		register(new PrefixCommand(plugin));
		register(new InviteCommand(plugin));
		register(new UninviteCommand(plugin));
		register(new KickCommand(plugin));
		register(new PromoteCommand(plugin));
		register(new DemoteCommand(plugin));
		register(new SetLeaderCommand(plugin));
		register(new AllyCommand(plugin));
		register(new BreakAllyCommand(plugin));
		register(new PvPCommand(plugin));
		register(new ChatCommand(plugin));
		register(new ChatAllyCommand(plugin));
		register(new InfoCommand(plugin));
		register(new StatsCommand(plugin));
		register(new TopCommand(plugin));
		register(new ListCommand(plugin));
	}

	private void register(AbstractClanCommand cmd) {
		subs.put(cmd.getName().toLowerCase(), cmd);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length == 0) {
			helpCommand.handle(sender, new String[0]);
			return true;
		}

		String sub = args[0].toLowerCase();
		String[] rest = Arrays.copyOfRange(args, 1, args.length);

		if ("admin".equals(sub)) {
			adminExecutor.onCommand(sender, cmd, label, rest);
			return true;
		}

		AbstractClanCommand command = subs.get(sub);
		if (command == null) {
			sender.sendMessage(plugin.getConfigManager().getMessage("general.unknown-command"));
			return true;
		}

		command.handle(sender, rest);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> result = new ArrayList<>();

		if (args.length == 1) {
			String partial = args[0].toLowerCase();
			subs.keySet().stream().filter(n -> n.startsWith(partial)).forEach(result::add);
			if ("admin".startsWith(partial))
				result.add("admin");

		} else if (args.length == 2) {
			String sub = args[0].toLowerCase();
			String partial = args[1].toLowerCase();

			if ("chat".equals(sub)) {
				if ("ally".startsWith(partial))
					result.add("ally");
			}

			if ("top".equals(sub)) {
				Arrays.asList("points", "kills", "level").stream().filter(t -> t.startsWith(partial))
						.forEach(result::add);
			}

			if ("pvp".equals(sub)) {
				Arrays.asList("on", "off").stream().filter(t -> t.startsWith(partial)).forEach(result::add);
			}

			Set<String> needsPlayer = new HashSet<>(
					Arrays.asList("invite", "uninvite", "kick", "promote", "demote", "setleader"));
			if (needsPlayer.contains(sub)) {
				plugin.getServer().getOnlinePlayers().stream()
						.filter(p -> p.getName().toLowerCase().startsWith(partial))
						.forEach(p -> result.add(p.getName()));
			}

			Set<String> needsClan = new HashSet<>(Arrays.asList("accept", "info", "stats", "ally", "breakally"));
			if (needsClan.contains(sub)) {
				plugin.getClanManager().getAllClans().stream()
						.filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
			}

		} else if (args.length == 3) {
			if ("setleader".equalsIgnoreCase(args[0])) {
				if ("confirm".startsWith(args[2].toLowerCase())) {
					result.add("confirm");
				}
			}
			if ("disband".equalsIgnoreCase(args[0])) {
				if ("confirm".startsWith(args[1].toLowerCase())) {
					result.add("confirm");
				}
			}
		}

		Collections.sort(result);
		return result;
	}
}