package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.RankPermission;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

public class ChatCommand extends AbstractClanCommand {

	public ChatCommand(Clans plugin) {
		super(plugin, "chat", "gclans.use", (RankPermission) null, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (args.length >= 1 && args[0].equalsIgnoreCase("ally")) {
			handleAllyChat(player, cp);
			return;
		}

		boolean next = !cp.isClanChatActive();
		cp.setClanChatActive(next);
		msg(player, next ? "chat.clan.toggled-on" : "chat.clan.toggled-off");
	}

	private void handleAllyChat(Player player, ClanPlayer cp) {
		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;

		if (clan.getAllies().isEmpty()) {
			msg(player, "chat.ally.no-allies");
			return;
		}

		boolean next = !cp.isAllyChatActive();
		cp.setAllyChatActive(next);
		msg(player, next ? "chat.ally.toggled-on" : "chat.ally.toggled-off");
	}
}