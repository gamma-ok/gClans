package me.gamma.clans.commands;

import me.gamma.clans.Clans;
import me.gamma.clans.config.ConfigManager;
import me.gamma.clans.models.Clan;
import me.gamma.clans.models.ClanPlayer;
import me.gamma.clans.models.Rank;
import me.gamma.clans.models.RankPermission;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class AllyCommand extends AbstractClanCommand {

	public AllyCommand(Clans plugin) {
		super(plugin, "ally", "gclans.use", RankPermission.ALLY, true);
	}

	@Override
	protected void execute(Player player, ClanPlayer cp, String[] args) {
		if (!requireArgs(player, args, 1, "ally"))
			return;

		Clan myClan = cm.getClan(cp.getClanId());
		if (myClan == null)
			return;

		Clan target = cm.findClanByNameOrLeader(args[0]);
		if (target == null) {
			msg(player, "ally.not-found");
			return;
		}
		if (target.getId().equals(myClan.getId())) {
			msg(player, "ally.cannot-self");
			return;
		}
		if (myClan.isAlliedWith(target.getId())) {
			msg(player, "ally.already-allies", "{clan}", target.getName());
			return;
		}

		int maxAllies = cfg.getMaxAllies();
		if (maxAllies > 0 && myClan.getAllies().size() >= maxAllies) {
			msg(player, "ally.max-reached", "{max}", String.valueOf(maxAllies));
			return;
		}

		if (target.hasPendingRequestTo(myClan.getId())) {
			cm.formAlliance(myClan.getId(), target.getId())
					.thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
						cm.notifyClan(myClan, "ally.formed", "{clan1}", myClan.getName(), "{clan2}", target.getName());
						cm.notifyClan(target, "ally.formed", "{clan1}", myClan.getName(), "{clan2}", target.getName());
					})).exceptionally(ex -> {
						plugin.getLogger().severe(ex.getMessage());
						return null;
					});

		} else {
			myClan.addPendingRequest(target.getId());
			msg(player, "ally.request-sent", "{clan}", target.getName());

			final String senderClanName = myClan.getName();
			target.getMembers().forEach((uuid, rank) -> {
				if (rank.isAtLeast(Rank.CO_LEADER)) {
					Player m = plugin.getServer().getPlayer(uuid);
					if (m != null) {
						sendClickableAllyRequest(m, senderClanName);
					}
				}
			});
		}
	}

	private void sendClickableAllyRequest(Player recipient, String senderClanName) {
		String rawMsg = cfg.getMessage("ally.request-received", "{clan}", senderClanName);

		String marker = ConfigManager.color("&e[click aquí]");
		int markerIdx = rawMsg.indexOf(marker);

		TextComponent full;

		if (markerIdx >= 0) {
			TextComponent before = new TextComponent(rawMsg.substring(0, markerIdx));
			TextComponent clickable = new TextComponent(marker);
			clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan ally " + senderClanName));
			TextComponent after = new TextComponent(rawMsg.substring(markerIdx + marker.length()));

			full = new TextComponent(before);
			full.addExtra(clickable);
			full.addExtra(after);
		} else {
			full = new TextComponent(rawMsg);
			full.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan ally " + senderClanName));

		}

		recipient.spigot().sendMessage(full);
	}
}