package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

		// Colores y configuración
		final String onlineColor = cfg.getString("member-list.online-color", "a");
		final String offlineColor = cfg.getString("member-list.offline-color", "7");
		final boolean showClanKills = cfg.getBoolean("member-list.show-clan-kills", true);
		final boolean tooltipEnabled = cfg.getBoolean("member-list.tooltip-enabled", true);
		final String killsFormat = cfg.getString("member-list.kills-format", "&7[&c{kills}&7]");

		// Listas para cada rango
		final List<TextComponent> leaderList = new ArrayList<>();
		final List<TextComponent> coLeaderList = new ArrayList<>();
		final List<TextComponent> captainList = new ArrayList<>();
		final List<TextComponent> memberList = new ArrayList<>();

		// Construcción de componentes con hover y kills
		for (Map.Entry<UUID, Rank> e : clan.getMembers().entrySet()) {
			UUID memberUuid = e.getKey();
			Rank rank = e.getValue();
			String rawName = getPlayerName(memberUuid);
			boolean isOnline = plugin.getServer().getPlayer(memberUuid) != null;
			String color = isOnline ? "§" + onlineColor : "§" + offlineColor;

			// Crear el componente base con el nombre
			TextComponent nameComponent = new TextComponent(color + rawName);

			// Agregar kills al lado si está activado
			if (showClanKills) {
				int clanKills = getClanKills(memberUuid);
				String formattedKills = killsFormat.replace("{kills}", String.valueOf(clanKills));
				addLegacyText(nameComponent, formattedKills);
			}

			// Agregar tooltip si está habilitado
			if (tooltipEnabled && !rawName.equals("Desconocido")) {
				ClanPlayer memberCP = cm.getPlayer(memberUuid);
				if (memberCP != null) {
					String tooltipText = buildTooltipText(memberCP, clan.getName());
					BaseComponent[] hoverText = new ComponentBuilder(tooltipText).create();
					nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
				}
			}

			// Asignar según rango
			if (rank == Rank.LEADER) {
				leaderList.add(nameComponent);
			} else if (rank == Rank.CO_LEADER) {
				coLeaderList.add(nameComponent);
			} else if (rank == Rank.CAPTAIN) {
				captainList.add(nameComponent);
			} else {
				memberList.add(nameComponent);
			}
		}

		final String prefixStr = clan.hasCustomPrefix() ? clan.getColoredPrefix() : null;
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
					sendSimpleInfo(player, clanName, online, total, dateStr, leaderList, coLeaderList, captainList,
							memberList, prefixStr, totalPoints, posStr, pvpStr);
					return;
				}

				// Enviar cada línea con soporte condicional
				for (String line : lines) {
					// Verificar condicionales {?coleaders}, {?captains}, {?members}, {?prefix}
					boolean shouldShow = true;
					String cleanLine = line;

					if (line.startsWith("{?coleaders}")) {
						shouldShow = !coLeaderList.isEmpty();
						cleanLine = line.substring("{?coleaders}".length());
					} else if (line.startsWith("{?captains}")) {
						shouldShow = !captainList.isEmpty();
						cleanLine = line.substring("{?captains}".length());
					} else if (line.startsWith("{?members}")) {
						shouldShow = !memberList.isEmpty();
						cleanLine = line.substring("{?members}".length());
					} else if (line.startsWith("{?prefix}")) {
						shouldShow = prefixStr != null;
						cleanLine = line.substring("{?prefix}".length());
					}

					if (shouldShow) {
						sendFormattedLine(player, cleanLine, clanName, online, total, dateStr, leaderList, coLeaderList,
								captainList, memberList, prefixStr, totalPoints, posStr, pvpStr);
					}
				}
			});
		}).exceptionally(ex -> {
			plugin.getLogger().severe("Error al obtener top: " + ex.getMessage());
			return null;
		});
	}

	private String buildTooltipText(ClanPlayer cp, String clanName) {
		List<String> rawLines = cfg.getMessages().getStringList("info.tooltip");
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < rawLines.size(); i++) {
			String line = rawLines.get(i).replace("{name}", cp.getName()).replace("{clan}", clanName)
					.replace("{kills}", String.valueOf(cp.getClanKills()))
					.replace("{deaths}", String.valueOf(cp.getDeaths()))
					.replace("{kdr}", String.format("%.1f", cp.getKDRatio()))
					.replace("{streak}", String.valueOf(cp.getKillstreak()))
					.replace("{best_streak}", String.valueOf(cp.getBestKillstreak()));
			sb.append(line.replace("&", "§"));
			if (i < rawLines.size() - 1) {
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	private void sendFormattedLine(Player player, String line, String clanName, int online, int total, String dateStr,
			List<TextComponent> leaders, List<TextComponent> coLeaders, List<TextComponent> captains,
			List<TextComponent> members, String prefix, double points, String pos, String pvp) {

		String processed = line.replace("{clan}", clanName).replace("{online}", String.valueOf(online))
				.replace("{total}", String.valueOf(total)).replace("{date}", dateStr)
				.replace("{prefix}", prefix != null ? prefix : cfg.getRaw("info.no-prefix"))
				.replace("{points}", formatPoints(points)).replace("{pos}", pos).replace("{pvp}", pvp);

		TextComponent fullLine = new TextComponent("");
		String remaining = processed;

		while (!remaining.isEmpty()) {
			int leaderIdx = remaining.indexOf("{leader}");
			int coleadersIdx = remaining.indexOf("{coleaders}");
			int captainsIdx = remaining.indexOf("{captains}");
			int membersIdx = remaining.indexOf("{members}");

			int idx = -1;
			String found = null;
			if (leaderIdx != -1 && (idx == -1 || leaderIdx < idx)) {
				idx = leaderIdx;
				found = "{leader}";
			}
			if (coleadersIdx != -1 && (idx == -1 || coleadersIdx < idx)) {
				idx = coleadersIdx;
				found = "{coleaders}";
			}
			if (captainsIdx != -1 && (idx == -1 || captainsIdx < idx)) {
				idx = captainsIdx;
				found = "{captains}";
			}
			if (membersIdx != -1 && (idx == -1 || membersIdx < idx)) {
				idx = membersIdx;
				found = "{members}";
			}

			if (idx == -1) {
				addLegacyText(fullLine, remaining);
				break;
			}

			if (idx > 0) {
				addLegacyText(fullLine, remaining.substring(0, idx));
			}

			int len = found.length();
			switch (found) {
			case "{leader}":
				addComponentList(fullLine, leaders, cfg.getRaw("info.no-coleaders"));
				break;
			case "{coleaders}":
				addComponentList(fullLine, coLeaders, cfg.getRaw("info.no-coleaders"));
				break;
			case "{captains}":
				addComponentList(fullLine, captains, cfg.getRaw("info.no-captains"));
				break;
			case "{members}":
				addComponentList(fullLine, members, cfg.getRaw("info.no-members"));
				break;
			}

			remaining = remaining.substring(idx + len);
		}

		player.spigot().sendMessage(fullLine);
	}

	private int findFirstPlaceholder(String text) {
		int min = Integer.MAX_VALUE;
		String[] placeholders = { "{clan}", "{online}", "{total}", "{date}", "{leader}", "{coleaders}", "{captains}",
				"{members}", "{prefix}", "{points}", "{pos}", "{pvp}" };

		for (String ph : placeholders) {
			int idx = text.indexOf(ph);
			if (idx != -1 && idx < min) {
				min = idx;
			}
		}

		return min == Integer.MAX_VALUE ? -1 : min;
	}

	private void addLegacyText(TextComponent base, String text) {
		if (text == null || text.isEmpty())
			return;
		String colored = text.replace("&", "§");
		BaseComponent[] parts = TextComponent.fromLegacyText(colored);
		for (BaseComponent part : parts) {
			base.addExtra(part);
		}
	}

	private void addComponentList(TextComponent base, List<TextComponent> components, String emptyText) {
		if (components.isEmpty()) {
			addLegacyText(base, emptyText);
			return;
		}

		for (int i = 0; i < components.size(); i++) {
			base.addExtra(components.get(i));
			if (i < components.size() - 1) {
				addLegacyText(base, "§7, ");
			}
		}
	}

	private void sendSimpleInfo(Player player, String clanName, int online, int total, String dateStr,
			List<TextComponent> leaders, List<TextComponent> coLeaders, List<TextComponent> captains,
			List<TextComponent> members, String prefix, double points, String pos, String pvp) {
		player.sendMessage("§7§m-------------------------------------------------");
		player.sendMessage("§6§l" + clanName + " §7[" + online + "§7/" + total + "§7] §f- §eCreado: §f" + dateStr);
		player.sendMessage("§eLíder: " + joinNames(leaders));
		if (!coLeaders.isEmpty())
			player.sendMessage("§eCo-Líder: " + joinNames(coLeaders));
		if (!captains.isEmpty())
			player.sendMessage("§eCapitán: " + joinNames(captains));
		if (!members.isEmpty())
			player.sendMessage("§eMiembros: " + joinNames(members));
		if (prefix != null)
			player.sendMessage("§ePrefix: " + prefix);
		player.sendMessage("§ePuntos: §f" + formatPoints(points));
		player.sendMessage("§ePosición: §f#" + pos);
		player.sendMessage("§eClan PvP: " + pvp);
		player.sendMessage("§7§m-------------------------------------------------");
	}

	private String joinNames(List<TextComponent> components) {
		if (components.isEmpty())
			return "§7(Ninguno)";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.size(); i++) {
			sb.append(components.get(i).toLegacyText());
			if (i < components.size() - 1) {
				sb.append("§7, ");
			}
		}
		return sb.toString();
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