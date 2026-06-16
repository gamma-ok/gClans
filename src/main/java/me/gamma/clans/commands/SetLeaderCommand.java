package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.managers.ConfirmationManager.Type;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class SetLeaderCommand extends AbstractClanCommand {

	public SetLeaderCommand(Clans plugin) {
		super(plugin, "setleader", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		// /clan setleader <player> confirm
		boolean isConfirm = args.length >= 2 && "confirm".equalsIgnoreCase(args[1]);
		if (isConfirm) {
			boolean done = plugin.getConfirmationManager().confirm(player.getUniqueId(), Type.SET_LEADER);
			if (!done)
				msg(player, "rank.setleader.no-pending");
			return;
		}

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}
		if (target.getUniqueId().equals(player.getUniqueId())) {
			msg(player, "rank.setleader.cannot-self");
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null || !clan.isMember(target.getUniqueId())) {
			msg(player, "rank.setleader.not-member", "{target}", target.getName());
			return;
		}

		final String clanName = clan.getName();
		final String clanId = clan.getId();
		final String oldName = player.getName();
		final String newName = target.getName();

		plugin.getConfirmationManager().request(player.getUniqueId(), Type.SET_LEADER, target.getUniqueId(), uuid -> {
			cm.transferLeadership(clanId, target.getUniqueId())
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
						Player old = plugin.getServer().getPlayer(uuid);
						if (old != null)
							old.sendMessage(cfg.getMessage("rank.setleader.success-old", "{target}", newName));
						if (target.isOnline())
							target.sendMessage(cfg.getMessage("rank.setleader.success-new", "{clan}", clanName));
						clan.getMembers().keySet().forEach(mid -> {
							Player m = plugin.getServer().getPlayer(mid);
							if (m != null)
								m.sendMessage(cfg.getMessage("rank.setleader.success-broadcast", "{player}", oldName,
										"{target}", newName));
						});
					})).exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});
		});

		msg(player, "rank.setleader.confirm", "{target}", newName, "{time}", String.valueOf(cfg.getConfirmTimeout()));
	}
}