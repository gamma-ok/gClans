package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.managers.ConfirmationManager.Type;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class DisbandCommand extends AbstractClanCommand {

	public DisbandCommand(Clans plugin) {
		super(plugin, "disband", "gclans.use", Rank.LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!cp.hasRankAtLeast(Rank.LEADER)) {
			msg(player, "clan.disband.not-leader");
			return;
		}

		boolean isConfirm = args.length >= 1 && "confirm".equalsIgnoreCase(args[0]);

		if (isConfirm) {
			boolean done = plugin.getConfirmationManager().confirm(player.getUniqueId(), Type.DISBAND);
			if (!done)
				msg(player, "clan.disband.no-pending");
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;
		final String clanName = clan.getName();
		final String clanId = clan.getId();

		plugin.getConfirmationManager().request(player.getUniqueId(), Type.DISBAND, null, uuid -> {
			clan.getMembers().keySet().forEach(mid -> {
				Player m = plugin.getServer().getPlayer(mid);
				if (m != null && !m.getUniqueId().equals(uuid))
					m.sendMessage(cfg.getMessage("clan.disband.success", "{clan}", clanName));
			});

			cm.disbandClan(clanId).exceptionally(ex -> {
				plugin.getLogger().severe("Error disolviendo clan: " + ex.getMessage());
				return null;
			});

			Player leader = plugin.getServer().getPlayer(uuid);
			if (leader != null)
				leader.sendMessage(cfg.getMessage("clan.disband.success", "{clan}", clanName));
		});

		msg(player, "clan.disband.confirm", "{time}", String.valueOf(cfg.getConfirmTimeout()));
	}
}