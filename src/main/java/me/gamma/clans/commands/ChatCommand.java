package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

public class ChatCommand extends AbstractClanCommand {
	public ChatCommand(Clans plugin) {
		super(plugin, "chat", "gclans.use", null, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		boolean next = !cp.isClanChatActive();
		cp.setClanChatActive(next);
		msg(player, next ? "chat.clan.on" : "chat.clan.off");
	}
}