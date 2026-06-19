package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class StatsCommand extends AbstractClanCommand {

	public StatsCommand(Clans plugin) {
		super(plugin, "stats", "gclans.use", (RankPermission) null, false);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		Clan clan;

		if (args.length == 0) {
			if (!cp.hasClan()) {
				msg(player, "general.not-in-clan");
				return;
			}
			clan = cm.getClan(cp.getClanId());
		} else {
			clan = cm.getClanByName(args[0]);
		}

		if (clan == null) {
			msg(player, "general.clan-not-found", "{clan}", args.length > 0 ? args[0] : "");
			return;
		}

		double kd = clan.getTotalDeaths() == 0 ? clan.getTotalKills()
				: Math.round((double) clan.getTotalKills() / clan.getTotalDeaths() * 100.0) / 100.0;

		String ksValue = String.valueOf(clan.getBestKillstreak());
		String ksPlayer = clan.getBestKillstreak() > 0 ? clan.getBestKillstreakPlayer() : "N/A";

		List<String> lines = cfg.getMessages().getStringList("stats.lines");
		for (String line : lines) {
			send(player, line.replace("{clan}", clan.getName()).replace("{kills}", String.valueOf(clan.getTotalKills()))
					.replace("{deaths}", String.valueOf(clan.getTotalDeaths())).replace("{kd}", String.valueOf(kd))
					.replace("{points}", formatPoints(clan.getTotalPoints()))
					.replace("{level}", String.valueOf(clan.getLevel())).replace("{ks_value}", ksValue)
					.replace("{ks_player}", ksPlayer));
		}
	}
}