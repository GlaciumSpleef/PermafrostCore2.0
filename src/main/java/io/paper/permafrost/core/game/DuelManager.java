package io.paper.permafrost.core.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.tasks.DuelTask;
import io.paper.permafrost.parkour.ParkourGame;
import io.paper.permafrost.utilities.SpleefMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DuelManager {
	private final Map<UUID, DuelTask> pendingRequests = new HashMap<>();
	private Main main;
	private SpleefGame game;
	private SpleggGame spleggGame;
	private ParkourGame parkourGame;

	public DuelManager(Main main) {
		this.main = main;
	}

	public Main getInstance() {
		return main;
	}

	public boolean hasPending(UUID player) {
		return pendingRequests.containsKey(player)
				|| pendingRequests.values().stream().anyMatch(req -> req.isRecipient(player));
	}

	public void setGame(SpleefGame game) {
		this.game = game;
	}

	public void setGame(ParkourGame game) {
		this.parkourGame = game;
	}

	public void sendRequest(UUID sender, Set<UUID> players, List<Integer> teamSizes) {
		Set<UUID> filteredRecipients = players.stream().filter(uuid -> !uuid.equals(sender))
				.collect(Collectors.toSet());

		Player senderPlayer = Bukkit.getPlayer(sender);

		if (filteredRecipients.isEmpty()) {
			if (senderPlayer != null) {
				senderPlayer.sendMessage(ChatColor.RED + "You must send a duel request to at least one other player.");
			}
			return;
		}

		if (hasPending(sender) || filteredRecipients.stream().anyMatch(this::hasPending)) {
			if (senderPlayer != null) {
				senderPlayer.sendMessage(ChatColor.RED + "A duel request is already pending.");
			}
			Bukkit.getLogger().info("Duel blocked by pending request check.");
			return;
		}

		// Block if sender is in Splegg FFA queue
		if (isInSpleggFFA(sender)) {
			if (senderPlayer != null) {
				senderPlayer.sendMessage(ChatColor.RED + "You cannot send a duel request while in Splegg FFA.");
			}
			return;
		}

		// Block if any recipient is in Splegg FFA queue
		for (UUID recipient : filteredRecipients) {
			if (isInSpleggFFA(recipient)) {
				Player target = Bukkit.getPlayer(recipient);
				if (senderPlayer != null) {
					String targetName = target != null ? target.getName() : "That player";
					senderPlayer.sendMessage(ChatColor.RED + targetName + " is in Splegg FFA.");
				}
				return;
			}
		}

		DuelTask request = new DuelTask(main, sender, filteredRecipients, teamSizes,
				() -> cancelRequest(sender, "Request timed out."));

		pendingRequests.put(sender, request);

		SpleefPlayer senderSp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(sender));
		if (senderSp != null) {
			senderSp.setHasBeenRequested(true);
		}

		for (UUID recipient : filteredRecipients) {
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(recipient));
			if (sp != null) {
				sp.setHasBeenRequested(true);
			}

			Player target = Bukkit.getPlayer(recipient);
			if (target != null) {
				sendInviteMessage(target, sender, request);
			}
		}

		if (senderPlayer != null) {
			sendSenderMessage(senderPlayer, request);
		}
	}

	public void respond(UUID player, boolean accepted) {
		if (accepted) {
			if (main.getRankedSpleggManager().getPlayersInQueue().contains(player)) {
				Bukkit.getPlayer(player).sendMessage("§cYou accepted a duel in ranked queue! Leaving queue now.");
				main.getRankedSpleggManager().removePlayerFromQueue(player);
			}

			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player)) {
				Bukkit.getPlayer(player).sendMessage("§cYou accepted a duel in ranked queue! Leaving queue now.");
				main.getRankedSpleefManager().removePlayerFromQueue(player, SpleefMode.POWERSPLEEF);
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player)) {
				Bukkit.getPlayer(player).sendMessage("§cYou accepted a duel in ranked queue! Leaving queue now.");
				main.getRankedSpleefManager().removePlayerFromQueue(player, SpleefMode.AIRSPLEEF);
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player)) {
				Bukkit.getPlayer(player).sendMessage("§cYou accepted a duel in ranked queue! Leaving queue now.");
				main.getRankedSpleefManager().removePlayerFromQueue(player, SpleefMode.MINISPLEEF);
			}
		}
		DuelTask req = getByRecipient(player);
		if (req == null)
			return;

		Player responder = Bukkit.getPlayer(player);

		// DENY should instantly cancel
		if (!accepted) {
			String name = responder != null ? responder.getName() : "A player";
			cancelRequest(req.getSender(), "Duel denied by " + name + ".");
			return;
		}

		req.respond(player, true);
		updatePlayers(req);

		if (req.isComplete() && req.allAccepted()) {
			startDuel(req);
		}
	}

	public void cancelRequest(UUID sender, String reason) {
		DuelTask req = pendingRequests.remove(sender);
		if (req == null)
			return;

		req.cancel();

		// Clear sender flag
		SpleefPlayer senderSp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(sender));
		if (senderSp != null) {
			senderSp.setHasBeenRequested(false);
		}

		Player senderPlayer = Bukkit.getPlayer(sender);
		if (senderPlayer != null) {
			senderPlayer.sendMessage(ChatColor.RED + reason);
		}

		for (UUID recipient : req.getRecipients()) {
			Player p = Bukkit.getPlayer(recipient);
			if (p != null) {
				p.sendMessage(ChatColor.RED + "Duel request cancelled: " + reason);
			}

			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(recipient));
			if (sp != null) {
				sp.setHasBeenRequested(false);
			}
		}
	}

	public void handleLeave(UUID player) {
		DuelTask req = pendingRequests.get(player);
		if (req != null) {
			cancelRequest(player, "Player left.");
			return;
		}

		DuelTask asRecipient = getByRecipient(player);
		if (asRecipient != null) {
			cancelRequest(asRecipient.getSender(), "Player left.");
		}
	}

	private DuelTask getByRecipient(UUID player) {
		return pendingRequests.values().stream().filter(req -> req.isRecipient(player)).findFirst().orElse(null);
	}

	private void updatePlayers(DuelTask req) {
		List<UUID> pending = req.getPending();
		String pendingNames = formatPlayers(pending);

		for (UUID recipient : req.getRecipients()) {
			Player player = Bukkit.getPlayer(recipient);
			if (player == null)
				continue;

			Boolean response = req.getResponses().get(recipient);

			TextComponent statusUpdate = Component.text("Left to accept: " + pendingNames, NamedTextColor.GOLD);
			player.sendMessage(statusUpdate);

			if (response == null) {
				TextComponent accept = Component.text("[ACCEPT]", NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
						.clickEvent(ClickEvent.runCommand("/duelaccept"))
						.hoverEvent(HoverEvent.showText(Component.text("Click to accept")));

				TextComponent deny = Component.text(" [DENY]", NamedTextColor.RED).decorate(TextDecoration.BOLD)
						.clickEvent(ClickEvent.runCommand("/dueldeny"))
						.hoverEvent(HoverEvent.showText(Component.text("Click to deny")));

				player.sendMessage(accept.append(deny));
			} else {
				String resultText = response ? "You have accepted this duel." : "You have denied this duel.";
				NamedTextColor color = response ? NamedTextColor.GREEN : NamedTextColor.RED;
				player.sendMessage(Component.text(resultText, color));
			}
		}

		Player senderPlayer = Bukkit.getPlayer(req.getSender());
		if (senderPlayer != null) {
			senderPlayer.sendMessage(Component.empty());

			TextComponent statusUpdate = Component.text("Left to accept: " + pendingNames, NamedTextColor.GOLD);
			senderPlayer.sendMessage(statusUpdate);

			TextComponent cancel = Component.text("[CANCEL]", NamedTextColor.RED).decorate(TextDecoration.BOLD)
					.clickEvent(ClickEvent.runCommand("/duelcancel"))
					.hoverEvent(HoverEvent.showText(Component.text("Click to cancel the duel request")));

			senderPlayer.sendMessage(cancel);
		}
	}

	private void startDuel(DuelTask req) {
		// remove pending request + clear request flags first
		DuelTask removed = pendingRequests.remove(req.getSender());
		if (removed != null) {
			removed.cancel();
		}

		SpleefPlayer senderSp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(req.getSender()));
		if (senderSp != null) {
			senderSp.setHasBeenRequested(false);
		}

		Player senderPlayer = Bukkit.getPlayer(req.getSender());
		if (senderPlayer != null) {
			senderPlayer.sendMessage(ChatColor.GREEN + "All players accepted! Game starting...");
		}

		for (UUID recipient : req.getRecipients()) {
			Player p = Bukkit.getPlayer(recipient);
			if (p != null) {
				p.sendMessage(ChatColor.GREEN + "All players accepted! Game starting...");
			}

			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(recipient));
			if (sp != null) {
				sp.setHasBeenRequested(false);
			}
		}
		if (parkourGame == null) {
			game.startTeamGame();
		} else {
			parkourGame.startMatch();
		}
	}

	private void sendInviteMessage(Player target, UUID sender, DuelTask request) {
		TextComponent accept = Component.text("[ACCEPT]", NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand("/duelaccept"))
				.hoverEvent(HoverEvent.showText(Component.text("Click to accept")));

		TextComponent deny = Component.text(" [DENY]", NamedTextColor.RED).decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand("/dueldeny"))
				.hoverEvent(HoverEvent.showText(Component.text("Click to deny")));

		String teamText = formatTeamSizes(request.getTeamSizes());
		Player senderPlayer = Bukkit.getPlayer(sender);
		String senderName = senderPlayer != null ? senderPlayer.getName() : "Someone";

		TextComponent msg = Component.text(senderName + " has sent you a duel request for a " + teamText
				+ "! Left to accept: " + formatPlayers(request.getPending()), NamedTextColor.GOLD);

		target.sendMessage(msg);
		target.sendMessage(accept.append(deny));
	}

	private void sendSenderMessage(Player sender, DuelTask request) {
		TextComponent cancel = Component.text("[CANCEL]", NamedTextColor.RED).decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand("/duelcancel"))
				.hoverEvent(HoverEvent.showText(Component.text("Click to cancel the duel request")));

		String teamText = formatTeamSizes(request.getTeamSizes());
		TextComponent msg = Component.text("You have sent a duel request for a " + teamText + "! Left to accept: "
				+ formatPlayers(request.getPending()), NamedTextColor.GOLD);

		sender.sendMessage(msg);
		sender.sendMessage(cancel);
	}

	private boolean isInSpleggFFA(UUID uuid) {
		Boolean inFFA = main.getSpleggQueueManager().getPlayers().containsKey(uuid);
		return inFFA != null && inFFA;
	}

	private String formatPlayers(List<UUID> players) {
		if (players.isEmpty()) {
			return "(none)";
		}

		List<String> names = new ArrayList<>();
		for (UUID uuid : players) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null) {
				names.add("?");
			} else {
				names.add(p.getName());
			}
		}
		return String.join(", ", names);
	}

	private String formatTeamSizes(List<Integer> teamSizes) {
		return teamSizes.stream().map(String::valueOf).collect(Collectors.joining("v"));
	}
}
