package me.gamma.clans.listeners;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ClanChatListener implements Listener {

	private final Clans plugin;

	public ClanChatListener(Clans plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		ClanPlayer cp = plugin.getClanManager().getPlayer(player.getUniqueId());

		if (cp == null || !cp.hasClan())
			return;

		if (cp.isClanChatActive()) {
			event.setCancelled(true);
			Clan clan = plugin.getClanManager().getClan(cp.getClanId());
			if (clan == null)
				return;

			String msg = build(plugin.getConfigManager().getClanChatFormat(), clan.getName(), player.getName(),
					event.getMessage());
			Set<Player> recipients = getOnlineMembers(clan);
			broadcast(recipients, msg);
			spy(msg, recipients, "[ClanChat]");

		} else if (cp.isAllyChatActive()) {
			event.setCancelled(true);
			Clan clan = plugin.getClanManager().getClan(cp.getClanId());
			if (clan == null)
				return;

			String msg = build(plugin.getConfigManager().getAllyChatFormat(), clan.getName(), player.getName(),
					event.getMessage());

			Set<Player> recipients = getOnlineMembers(clan);
			clan.getAllies().forEach(allyId -> {
				Clan ally = plugin.getClanManager().getClan(allyId);
				if (ally != null)
					recipients.addAll(getOnlineMembers(ally));
			});
			broadcast(recipients, msg);
			spy(msg, recipients, "[AllyChat]");
		}
	}

	private Set<Player> getOnlineMembers(Clan clan) {
		Set<Player> set = new HashSet<>();
		clan.getMembers().keySet().forEach(uuid -> {
			Player p = plugin.getServer().getPlayer(uuid);
			if (p != null && p.isOnline())
				set.add(p);
		});
		return set;
	}

	private String build(String format, String clan, String player, String message) {
		return ConfigManager
				.color(format.replace("{clan}", clan).replace("{player}", player).replace("{message}", message));
	}

	private void broadcast(Set<Player> recipients, String msg) {
		recipients.forEach(p -> p.sendMessage(msg));
	}

	private void spy(String msg, Set<Player> already, String tag) {
		if (!plugin.getConfigManager().isSpyEnabled())
			return;
		plugin.getServer().getOnlinePlayers().forEach(p -> {
			if (!already.contains(p) && p.hasPermission("gclans.admin")) {
				String spyMsg = ConfigManager.color(plugin.getConfigManager().getSpyChatFormat().replace("{clan}", tag)
						.replace("{player}", "").replace("{message}", msg));
				p.sendMessage(spyMsg);
			}
		});
	}
}