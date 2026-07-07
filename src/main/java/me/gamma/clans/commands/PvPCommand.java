package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import org.bukkit.entity.Player;

public class PvPCommand extends AbstractClanCommand {

	public PvPCommand(Clans plugin) {
		super(plugin, "pvp", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;

		boolean newState = !clan.isPvpEnabled();
		clan.setPvpEnabled(newState);

		plugin.getStorageProvider().updateClan(clan).exceptionally(ex -> {
			plugin.getLogger().severe(ex.getMessage());
			return null;
		});

		String msgKey = newState ? "pvp.enabled" : "pvp.disabled";
		cm.notifyClan(clan, msgKey);
	}
}