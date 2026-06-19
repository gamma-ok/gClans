package me.gamma.clans.listeners;

import me.gamma.clans.Clans;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

	private final Clans plugin;

	public PlayerDeathListener(Clans plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player victim = event.getEntity();
		Player killer = victim.getKiller();

		if (killer == null || killer.equals(victim))
			return;

		plugin.getClanManager().processKill(killer.getUniqueId(), victim.getUniqueId());
	}
}