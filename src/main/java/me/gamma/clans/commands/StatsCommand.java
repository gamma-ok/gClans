package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

/**
 * /clan stats <clan> Muestra estadísticas detalladas de un clan.
 */
public class StatsCommand extends AbstractClanCommand {

	public StatsCommand(Clans plugin) {
		super(plugin, "stats", "gclans.use", null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1))
			return;

		Clan clan = cm.getClanByName(args[0]);
		if (clan == null) {
			msg(player, "general.clan-not-found", "{clan}", args[0]);
			return;
		}

		double kd = clan.getTotalDeaths() == 0 ? clan.getTotalKills()
				: Math.round((double) clan.getTotalKills() / clan.getTotalDeaths() * 100.0) / 100.0;

		String ks = clan.getBestKillstreak() > 0
				? clan.getBestKillstreak() + " (" + clan.getBestKillstreakPlayer() + ")"
				: "0";

		raw(player, "stats.header", "{clan}", clan.getName());
		raw(player, "stats.kills", "{value}", String.valueOf(clan.getTotalKills()));
		raw(player, "stats.deaths", "{value}", String.valueOf(clan.getTotalDeaths()));
		raw(player, "stats.kd", "{value}", String.valueOf(kd));
		raw(player, "stats.points", "{value}", formatPoints(clan.getTotalPoints()));
		raw(player, "stats.level", "{value}", String.valueOf(clan.getLevel()));
		raw(player, "stats.killstreak", "{value}", ks, "{player}", clan.getBestKillstreakPlayer());
	}
}