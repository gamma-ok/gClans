package me.gamma.clans.listeners;

import me.gamma.clans.Clans;
import me.gamma.clans.managers.ClanManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Protección de daño entre miembros del clan y aliados.
 *
 * Reglas: 1. Miembros del mismo clan → bloqueado SIEMPRE, a menos que /clan pvp
 * esté ON. NO hay bypass por OP ni por ningún permiso. 2. Aliados → SIEMPRE
 * bloqueado, independientemente del /clan pvp. (Las alianzas tienen protección
 * permanente y separada.)
 */
public class FriendlyFireListener implements Listener {

	private final Clans plugin;
	private final ClanManager cm;

	public FriendlyFireListener(Clans plugin) {
		this.plugin = plugin;
		this.cm = plugin.getClanManager();
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player victim = (Player) event.getEntity();
		Player attacker = resolveAttacker(event.getDamager());
		if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId()))
			return;

		// -------------------------------------------------------
		// Protección de aliados — SIEMPRE activa, sin excepción
		// -------------------------------------------------------
		if (plugin.getConfigManager().isProtectAllies() && cm.areAllied(attacker.getUniqueId(), victim.getUniqueId())) {
			event.setCancelled(true);
			attacker.sendMessage(plugin.getConfigManager().getMessage("pvp.blocked-ally"));
			return;
		}

		// -------------------------------------------------------
		// Protección de clan — depende del toggle /clan pvp
		// NO tiene bypass de OP, admins incluidos
		// -------------------------------------------------------
		if (plugin.getConfigManager().isProtectMembers()
				&& cm.areInSameClan(attacker.getUniqueId(), victim.getUniqueId())) {

			ClanPlayer attackerCp = cm.getPlayer(attacker.getUniqueId());
			if (attackerCp != null && attackerCp.hasClan()) {
				Clan clan = cm.getClan(attackerCp.getClanId());
				// Si el PvP del clan está ON → permitir daño
				if (clan != null && clan.isPvpEnabled())
					return;
			}

			// PvP desactivado → bloquear
			event.setCancelled(true);
			attacker.sendMessage(plugin.getConfigManager().getMessage("pvp.blocked-member"));
		}
	}

	private Player resolveAttacker(Entity damager) {
		if (damager instanceof Player)
			return (Player) damager;
		if (damager instanceof Projectile) {
			ProjectileSource src = ((Projectile) damager).getShooter();
			if (src instanceof Player)
				return (Player) src;
		}
		return null;
	}
}