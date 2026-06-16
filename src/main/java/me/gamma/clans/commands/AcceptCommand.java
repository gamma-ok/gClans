package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

/**
 * /clan accept <clan> Acepta la invitación al clan especificado. También es el
 * comando ejecutado al hacer click en el mensaje de invitación.
 */
public class AcceptCommand extends AbstractClanCommand {

	public AcceptCommand(Clans plugin) {
		super(plugin, "accept", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		if (cp.hasClan()) {
			msg(player, "general.already-in-clan");
			return;
		}

		String input = args[0];

		// Buscar clan (por nombre, case-insensitive)
		Clan clan = cm.getClanByName(input);
		if (clan == null) {
			msg(player, "general.clan-not-found", "{clan}", input);
			return;
		}

		// Verificar invitación activa
		if (!plugin.getInvitationManager().hasInvitationFor(player.getUniqueId(), clan.getId())) {
			msg(player, "accept.not-invited");
			return;
		}

		// Verificar slots
		if (clan.isFull()) {
			msg(player, "general.invalid-usage");
			return;
		}

		plugin.getInvitationManager().cancel(player.getUniqueId());
		final String clanName = clan.getName();

		cm.addMember(clan.getId(), player.getUniqueId())
				.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
					msg(player, "accept.success-self", "{clan}", clanName);

					// Notificar a miembros online
					clan.getMembers().keySet().forEach(mid -> {
						if (!mid.equals(player.getUniqueId())) {
							Player m = plugin.getServer().getPlayer(mid);
							if (m != null)
								m.sendMessage(cfg.getMessage("accept.success-broadcast", "{player}", player.getName()));
						}
					});
				})).exceptionally(ex -> {
					plugin.getLogger().severe(ex.getMessage());
					return null;
				});
	}
}