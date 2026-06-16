package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.models.ClanPlayer;
import org.bukkit.entity.Player;

public class ChatAllyCommand extends AbstractClanCommand {
	public ChatAllyCommand(Clans plugin) {
		super(plugin, "chatally", "gclans.use", null, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		boolean next = !cp.isAllyChatActive();
		cp.setAllyChatActive(next);
		msg(player, next ? "chat.ally.on" : "chat.ally.off");
	}
}