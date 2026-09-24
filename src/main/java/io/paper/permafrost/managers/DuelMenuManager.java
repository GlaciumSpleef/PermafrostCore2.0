package io.paper.permafrost.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.parkour.ParkourGame;
import io.paper.permafrost.utilities.SpleefMode;

public class DuelMenuManager implements Listener {

	private final Main main;
	private final Map<UUID, PendingDuel> pendingDuels = new HashMap<>();

	public DuelMenuManager(Main main) {
		this.main = main;
	}

	public void setPendingDuel(UUID sender, UUID target, boolean instantStart) {
		pendingDuels.put(sender, new PendingDuel(target, instantStart));
	}

	public void removePendingDuel(UUID sender) {
		pendingDuels.remove(sender);
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		if (!event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Duel Mode")) {
			return;
		}

		event.setCancelled(true);

		ItemStack item = event.getCurrentItem();
		if (item == null || item.getType().isAir()) {
			return;
		}

		PendingDuel pending = pendingDuels.get(player.getUniqueId());
		if (pending == null) {
			player.closeInventory();
			player.sendMessage("§cThat duel request expired.");
			return;
		}

		Player dueled = Bukkit.getPlayer(pending.target());
		if (dueled == null) {
			player.closeInventory();
			player.sendMessage("§cThat player is no longer online.");
			pendingDuels.remove(player.getUniqueId());
			return;
		}

		boolean splegg = false;
		SpleefMode mode = SpleefMode.POWERSPLEEF;

		switch (item.getType()) {
		case EGG -> {
			splegg = true;
			mode = null;
		}
		case DIAMOND_SHOVEL -> mode = SpleefMode.POWERSPLEEF;
		case FEATHER -> mode = SpleefMode.AIRSPLEEF;
		case RED_CONCRETE -> mode = SpleefMode.MINISPLEEF;
		default -> {
			return;
		}
		}

		player.closeInventory();
		pendingDuels.remove(player.getUniqueId());

		sendDuel(player, dueled, pending.instantStart(), splegg, mode);
	}

	private void sendDuel(Player player, Player dueled, boolean instantStart, boolean splegg, SpleefMode mode) {
		UUID playerId = player.getUniqueId();
		UUID dueledId = dueled.getUniqueId();

		SpleefPlayer senderp = main.getDataLoader().getSpleefPlayer(player);
		SpleefPlayer spDueled = main.getDataLoader().getSpleefPlayer(dueled);

		if (senderp == null || spDueled == null) {
			player.sendMessage("§cCould not create duel.");
			return;
		}

		if (senderp.getInGame() || senderp.getInTeamGame()) {
			player.sendMessage("§cThis command cannot be run in game.");
			return;
		}

		if (spDueled.getInGame() || spDueled.getInTeamGame()) {
			player.sendMessage("§cThat player is already in a game.");
			return;
		}

		Map<Integer, List<UUID>> teamMap = new HashMap<>();
		SpleefGame game = new SpleefGame(main, splegg, instantStart, mode);

		List<UUID> teamUUIDs = new ArrayList<>();
		teamUUIDs.add(playerId);
		teamMap.put(1, teamUUIDs);

		TeamSpleefTeam team = new TeamSpleefTeam(main, "blue", teamUUIDs);
		game.setTeamSpleefTeam("blue", teamUUIDs);
		senderp.setTeam(team);

		List<UUID> teamUUIDs2 = new ArrayList<>();
		teamUUIDs2.add(dueledId);
		teamMap.put(2, teamUUIDs2);

		TeamSpleefTeam team2 = new TeamSpleefTeam(main, "red", teamUUIDs2);
		game.setTeamSpleefTeam("red", teamUUIDs2);
		spDueled.setTeam(team2);

		game.setTeamCount(2);
		game.setTeamGame(true);
		game.saveTeams(teamMap);

		Bukkit.getLogger().info("Saved " + game.getPlayers().size() + " players to game.");

		main.getDuelManager().setGame(game);

		Set<UUID> players = new HashSet<>();
		players.add(playerId);
		players.add(dueledId);

		if (instantStart) {
			if (splegg) {
				game.setSplegg(true);
				game.startTeamGame();
				main.getRankedSpleggManager().addGame(game);

				if (Bukkit.getPlayer(dueledId) != null) {
					Bukkit.getPlayer(dueledId)
							.sendMessage(ChatColor.AQUA + "Your ranked §eSPLEGG §bmatch is starting against "
									+ Bukkit.getPlayer(playerId).getName() + "!");
				}
				if (Bukkit.getPlayer(playerId) != null) {
					Bukkit.getPlayer(playerId)
							.sendMessage(ChatColor.AQUA + "Your ranked §eSPLEGG §bmatch is starting against "
									+ Bukkit.getPlayer(dueledId).getName() + "!");
				}
				return;
			} else {
				game.startTeamGame();
				main.getRankedSpleefManager().addGame(game);

				String modeName = "Powerspleef";
				if (mode == SpleefMode.AIRSPLEEF) {
					modeName = "Airspleef";
				} else if (mode == SpleefMode.MINISPLEEF) {
					modeName = "Minispleef";
				}

				if (Bukkit.getPlayer(dueledId) != null) {
					Bukkit.getPlayer(dueledId).sendMessage(ChatColor.AQUA + "Your ranked §f" + modeName
							+ " §bmatch is starting against " + Bukkit.getPlayer(playerId).getName() + "!");
				}
				if (Bukkit.getPlayer(playerId) != null) {
					Bukkit.getPlayer(playerId).sendMessage(ChatColor.AQUA + "Your ranked §f" + modeName
							+ " §bmatch is starting against " + Bukkit.getPlayer(dueledId).getName() + "!");
				}
				return;
			}
		}

		List<Integer> teamSizes = new ArrayList<>();
		teamSizes.add(1);
		teamSizes.add(1);

		main.getDuelManager().sendRequest(playerId, players, teamSizes);

		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
		List<Player> toRemove = new ArrayList<>();

		for (Player p2 : onlinePlayers) {
			SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p2);
			if (spleefPlayer != null && spleefPlayer.getGame() != null) {
				toRemove.add(p2);
			}
		}

		onlinePlayers.removeAll(toRemove);
		main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);

		String modeName = splegg ? "Splegg"
				: mode == SpleefMode.AIRSPLEEF ? "Airspleef"
						: mode == SpleefMode.MINISPLEEF ? "Minispleef" : "Powerspleef";

		player.sendMessage("§aSent a §f" + modeName + " §aduel request to §f" + dueled.getName() + "§a.");
		dueled.sendMessage("§f" + player.getName() + " §ahas challenged you to a §f" + modeName + " §aduel.");
	}

	private record PendingDuel(UUID target, boolean instantStart) {
	}
}
