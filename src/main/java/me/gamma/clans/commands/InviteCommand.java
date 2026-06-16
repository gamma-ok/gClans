package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * /clan invite <player> Envía una invitación con un componente clickeable
 * [ACEPTAR]. Requiere Co-Leader o superior.
 */
public class InviteCommand extends AbstractClanCommand {

	public InviteCommand(Clans plugin) {
		super(plugin, "invite", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null || !target.isOnline()) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}

		if (target.getUniqueId().equals(player.getUniqueId())) {
			msg(player, "general.invalid-usage");
			return;
		}

		ClanPlayer targetCp = cm.getPlayer(target.getUniqueId());
		if (targetCp != null && targetCp.hasClan()) {
			msg(player, "general.target-already-in-clan", "{target}", target.getName());
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;

		if (clan.isFull()) {
			msg(player, "general.invalid-usage");
			return;
		}

		if (plugin.getInvitationManager().hasInvitationFor(target.getUniqueId(), clan.getId())) {
			msg(player, "invite.already-invited", "{target}", target.getName());
			return;
		}

		plugin.getInvitationManager().invite(target.getUniqueId(), clan.getId(), clan.getName(), player.getUniqueId());

		// Notificar al invitador
		msg(player, "invite.sent", "{target}", target.getName());

		// Enviar mensaje clickeable al invitado
		sendClickableInvite(target, player.getName(), clan.getName());
	}

	/**
	 * Construye y envía el mensaje de invitación con botón [ACEPTAR] clickeable.
	 * Compatible con Spigot 1.8.8 usando BungeeCord ChatAPI.
	 */
	private void sendClickableInvite(Player target, String inviterName, String clanName) {
		// Parte 1: texto informativo
		String infoText = cfg.getMessage("invite.received", "{player}", inviterName, "{clan}", clanName);

		// Eliminamos el placeholder del click ya que lo hacemos con componente
		// El messages.yml tiene: "... Haz &e[click aquí] &ao usa &e/clan accept
		// {clan}&a."
		// Partimos el mensaje en dos: antes y después de [click aquí]
		String marker = ConfigManager.color("&e[click aquí]");
		int markerIdx = infoText.indexOf(marker);

		TextComponent full;

		if (markerIdx >= 0) {
			// Texto antes del botón
			TextComponent before = new TextComponent(infoText.substring(0, markerIdx));

			// Botón clickeable
			TextComponent clickable = new TextComponent(marker);
			clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept " + clanName));
			clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ConfigManager.color("&aHaz click para unirte al clan &e" + clanName))
							.create()));

			// Texto después del botón
			TextComponent after = new TextComponent(infoText.substring(markerIdx + marker.length()));

			full = new TextComponent(before);
			full.addExtra(clickable);
			full.addExtra(after);
		} else {
			// Fallback: mensaje simple sin partir
			full = new TextComponent(infoText);
			full.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept " + clanName));
		}

		target.spigot().sendMessage(full);
	}
}