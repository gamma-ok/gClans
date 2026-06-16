package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class ClanCommandExecutor implements CommandExecutor, TabCompleter {

	private final Clans plugin;
	private final Map<String, AbstractClanCommand> subs = new LinkedHashMap<>();
	private final HelpCommand helpCommand;

	public ClanCommandExecutor(Clans plugin) {
		this.plugin = plugin;
		this.helpCommand = new HelpCommand(plugin);

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
		String[] rem = Arrays.copyOfRange(args, 1, args.length);

		AbstractClanCommand command = subs.get(sub);
		if (command == null) {
			sender.sendMessage(plugin.getConfigManager().getMessage("general.unknown-command"));
			return true;
		}
		command.handle(sender, rem);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> result = new ArrayList<>();
		if (args.length == 1) {
			String partial = args[0].toLowerCase();
			subs.keySet().stream().filter(n -> n.startsWith(partial)).forEach(result::add);
		} else if (args.length == 2) {
			String sub = args[0].toLowerCase();
			Set<String> needsPlayer = new HashSet<>(
					Arrays.asList("invite", "uninvite", "kick", "promote", "demote", "setleader"));
			Set<String> needsClan = new HashSet<>(Arrays.asList("accept", "info", "stats", "ally", "breakally"));

			String partial = args[1].toLowerCase();
			if (needsPlayer.contains(sub)) {
				plugin.getServer().getOnlinePlayers().stream()
						.filter(p -> p.getName().toLowerCase().startsWith(partial))
						.forEach(p -> result.add(p.getName()));
			}
			if (needsClan.contains(sub)) {
				plugin.getClanManager().getAllClans().stream()
						.filter(c -> c.getName().toLowerCase().startsWith(partial))
						.forEach(c -> result.add(c.getName()));
			}
			if ("top".equals(sub)) {
				Arrays.asList("points", "kills", "level").stream().filter(t -> t.startsWith(partial))
						.forEach(result::add);
			}
			if ("pvp".equals(sub)) {
				Arrays.asList("on", "off").stream().filter(t -> t.startsWith(partial)).forEach(result::add);
			}
		}
		Collections.sort(result);
		return result;
	}
}