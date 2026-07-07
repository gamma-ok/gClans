package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class InviteCommand extends AbstractClanCommand {

	public InviteCommand(Clans plugin) {
		super(plugin, "invite", "gclans.use", Rank.CO_LEADER, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1, "invite"))
			return;

		Player target = plugin.getServer().getPlayer(args[0]);
		if (target == null || !target.isOnline()) {
			msg(player, "general.player-not-found", "{player}", args[0]);
			return;
		}

		if (target.getUniqueId().equals(player.getUniqueId())) {
			msg(player, "general.invalid-usage");
			return;
		}

		ClanPlayer targetCp = cm.getPlayer(target.getUniqueId());
		if (targetCp != null && targetCp.hasClan()) {
			Clan myClanCheck = cm.getClan(cp.getClanId());
			if (myClanCheck != null && myClanCheck.isMember(target.getUniqueId())) {
				msg(player, "invite.target-already-member", "{target}", target.getName());
			} else {
				msg(player, "general.target-already-in-clan", "{target}", target.getName());
			}
			return;
		}

		Clan clan = cm.getClan(cp.getClanId());
		if (clan == null)
			return;

		if (clan.isFull()) {
			msg(player, "general.invalid-usage");
			return;
		}

		if (plugin.getInvitationManager().hasInvitationFor(target.getUniqueId(), clan.getId())) {
			msg(player, "invite.already-invited", "{target}", target.getName());
			return;
		}

		plugin.getInvitationManager().invite(target.getUniqueId(), clan.getId(), clan.getName(), player.getUniqueId());

		msg(player, "invite.sent", "{target}", target.getName());

		sendClickableInvite(target, player.getName(), clan.getName());
	}

	private void sendClickableInvite(Player target, String inviterName, String clanName) {
		String infoText = cfg.getMessage("invite.received", "{player}", inviterName, "{clan}", clanName);

		String marker = ConfigManager.color("&e[click aquí]");
		int markerIdx = infoText.indexOf(marker);

		TextComponent full;

		if (markerIdx >= 0) {
			TextComponent before = new TextComponent(infoText.substring(0, markerIdx));

			TextComponent clickable = new TextComponent(marker);
			clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept " + clanName));
			clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder(ConfigManager.color("&aHaz clic para unirte al clan &e" + clanName))
							.create()));

			TextComponent after = new TextComponent(infoText.substring(markerIdx + marker.length()));

			full = new TextComponent(before);
			full.addExtra(clickable);
			full.addExtra(after);
		} else {
			full = new TextComponent(infoText);
			full.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept " + clanName));
		}

		target.spigot().sendMessage(full);
	}
}