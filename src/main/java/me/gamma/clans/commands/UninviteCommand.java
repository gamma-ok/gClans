package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class UninviteCommand extends AbstractClanCommand {

	public UninviteCommand(Clans plugin) {
		super(plugin, "uninvite", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1, "uninvite"))
			return;

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;

		if (!plugin.getInvitationManager().hasInvitationFor(target.getUniqueId(), clan.getId())) {
			msg(player, "uninvite.not-invited", "{target}", target.getName());
			return;
		}

		plugin.getInvitationManager().cancelAndNotify(target.getUniqueId(), player.getName());

		msg(player, "uninvite.success", "{target}", target.getName());
	}
}