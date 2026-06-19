package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InfoCommand extends AbstractClanCommand {

	public InfoCommand(Clans plugin) {
		super(plugin, "info", "gclans.use", (Rank) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		Clan clan;

		if (args.length == 0) {
			if (!cp.hasClan()) {
				msg(player, "general.not-in-clan");
				return;
			}
			clan = cm.getClan(cp.getClanId());
		} else {
			clan = resolveClan(args[0]);
		}

		if (clan == null) {
			msg(player, "general.clan-not-found", "{clan}", args.length > 0 ? args[0] : "");
			return;
		}

		displayInfo(player, clan);
	}

	private Clan resolveClan(String input) {
		Clan byName = cm.getClanByName(input);
		if (byName != null)
			return byName;
		return cm.getClanOfPlayer(resolveUuid(input));
	}

	private UUID resolveUuid(String name) {
		Player online = plugin.getServer().getPlayer(name);
		if (online != null)
			return online.getUniqueId();
		return plugin.getServer().getOfflinePlayer(name).getUniqueId();
	}

	private void displayInfo(Player player, Clan clan) {
		final int online = cm.getOnlineCount(clan);
		final int total = clan.getMemberCount();

		final String[] leaderHolder = { "?" };
		final List<String> coLeaderList = new ArrayList<>();
		final List<String> captainList = new ArrayList<>();
		final List<String> memberList = new ArrayList<>();

		for (Map.Entry<UUID, Rank> e : clan.getMembers().entrySet()) {
			UUID memberUuid = e.getKey();
			Rank rank = e.getValue();
			String name = getPlayerName(memberUuid);
			int clanKills = getClanKills(memberUuid);

			if (rank == Rank.LEADER) {
				leaderHolder[0] = name;
			} else if (rank == Rank.CO_LEADER) {
				coLeaderList.add(name);
			} else if (rank == Rank.CAPTAIN) {
				captainList.add(name);
			} else {
				memberList.add(name + "[" + clanKills + "]");
			}
		}

		final String leaderStr = leaderHolder[0];
		final String coLeadersStr = coLeaderList.isEmpty() ? cfg.getRaw("info.no-coleaders")
				: String.join(", ", coLeaderList);
		final String captainsStr = captainList.isEmpty() ? cfg.getRaw("info.no-captains")
				: String.join(", ", captainList);
		final String membersStr = memberList.isEmpty() ? cfg.getRaw("info.no-members") : String.join(", ", memberList);

		final String prefixStr = clan.hasCustomPrefix() ? clan.getColoredPrefix() : cfg.getRaw("info.no-prefix");

		final String pvpStr = clan.isPvpEnabled() ? cfg.getRaw("info.pvp-on") : cfg.getRaw("info.pvp-off");
		final String dateStr = clan.getCreatedAtFormatted();
		final String clanName = clan.getName();
		final double totalPoints = clan.getTotalPoints();

		cm.getTopByPoints(cfg.getTopCount()).thenAccept(top -> {
			int position = -1;
			for (int i = 0; i < top.size(); i++) {
				if (top.get(i).getId().equals(clan.getId())) {
					position = i + 1;
					break;
				}
			}
			final String posStr = position > 0 ? String.valueOf(position) : "N/A";

			plugin.getServer().getScheduler().runTask(plugin, () -> {
				List<String> lines = cfg.getMessages().getStringList("info.lines");

				if (lines.isEmpty()) {
					send(player, "&e" + clanName + " &8[&a" + online + "&8/&f" + total + "&8] - [" + dateStr + "]");
					send(player, "&6Leader&8: &f" + leaderStr);
					if (!coLeadersStr.equals(cfg.getRaw("info.no-coleaders")))
						send(player, "&cCo-Leader&8: &f" + coLeadersStr);
					if (!captainsStr.equals(cfg.getRaw("info.no-captains")))
						send(player, "&bCapitán&8: &f" + captainsStr);
					send(player, "&9Miembros&8: &f" + membersStr);
					send(player, "&7Prefix&8: " + prefixStr);
					send(player, "&7Puntos&8: &f" + formatPoints(totalPoints));
					send(player, "&7Posición&8: &f" + posStr);
					send(player, "&7Clan PvP&8: " + pvpStr);
					return;
				}

				for (String line : lines) {
					send(player,
							line.replace("{clan}", clanName).replace("{online}", String.valueOf(online))
									.replace("{total}", String.valueOf(total)).replace("{date}", dateStr)
									.replace("{leader}", leaderStr).replace("{coleaders}", coLeadersStr)
									.replace("{captains}", captainsStr).replace("{members}", membersStr)
									.replace("{prefix}", prefixStr).replace("{points}", formatPoints(totalPoints))
									.replace("{pos}", posStr).replace("{pvp}", pvpStr));
				}
			});
		}).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});
	}

	private String getPlayerName(UUID uuid) {
		Player p = plugin.getServer().getPlayer(uuid);
		if (p != null)
			return p.getName();
		String name = plugin.getServer().getOfflinePlayer(uuid).getName();
		return name != null ? name : "Desconocido";
	}

	private int getClanKills(UUID uuid) {
		ClanPlayer cp = cm.getPlayer(uuid);
		return cp != null ? cp.getClanKills() : 0;
	}
}