package me.gamma.clans.listeners;

import me.gamma.clans.Clans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

	private final Clans plugin;

	public PlayerConnectionListener(Clans plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoin(PlayerJoinEvent event) {
		plugin.getClanManager().loadPlayer(event.getPlayer()).exceptionally(ex -> {
			plugin.getLogger().severe("Error cargando jugador " + event.getPlayer().getName() + ": " + ex.getMessage());
			return null;
		});
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		plugin.getConfirmationManager().cancel(event.getPlayer().getUniqueId());
		plugin.getInvitationManager().cancel(event.getPlayer().getUniqueId());
		plugin.getClanManager().unloadPlayer(event.getPlayer().getUniqueId());
	}
}