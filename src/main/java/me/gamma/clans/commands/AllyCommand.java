package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

/**
 * /clan ally <clan|leader> Solicita o acepta alianza. Busca por nombre de clan
 * O nombre del líder. Flujo bidireccional: ClanA envía → ClanB acepta con el
 * mismo comando.
 */
public class AllyCommand extends AbstractClanCommand {

	public AllyCommand(Clans plugin) {
		super(plugin, "ally", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		Clan myClan = cm.getClan(cp.getClanId());
		if (myClan == null)
			return;

		// Búsqueda dual: por nombre de clan o por nombre de líder
		Clan target = cm.findClanByNameOrLeader(args[0]);
		if (target == null) {
			msg(player, "ally.not-found");
			return;
		}
		if (target.getId().equals(myClan.getId())) {
			msg(player, "ally.cannot-self");
			return;
		}
		if (myClan.isAlliedWith(target.getId())) {
			msg(player, "ally.already-allies", "{clan}", target.getName());
			return;
		}

		int maxAllies = cfg.getMaxAllies();
		if (maxAllies > 0 && myClan.getAllies().size() >= maxAllies) {
			msg(player, "ally.max-reached", "{max}", String.valueOf(maxAllies));
			return;
		}

		// Si el otro clan ya envió solicitud → aceptar y formalizar
		if (target.hasPendingRequestTo(myClan.getId())) {
			cm.formAlliance(myClan.getId(), target.getId())
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
						cm.notifyClan(myClan, "ally.formed", "{clan1}", myClan.getName(), "{clan2}", target.getName());
						cm.notifyClan(target, "ally.formed", "{clan1}", myClan.getName(), "{clan2}", target.getName());
					})).exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});
		} else {
			// Enviar solicitud
			myClan.addPendingRequest(target.getId());
			msg(player, "ally.request-sent", "{clan}", target.getName());

			// Notificar a co-líderes y líderes del clan objetivo
			target.getMembers().forEach((uuid, rank) -> {
				if (rank.isAtLeast(Rank.CO_LEADER)) {
					Player m = plugin.getServer().getPlayer(uuid);
					if (m != null)
						m.sendMessage(cfg.getMessage("ally.request-received", "{clan}", myClan.getName()));
				}
			});
		}
	}
}