package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.DuelManager;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.parkour.ParkourGame;
import io.paper.permafrost.utilities.SpleefMode;

public class DuelCommand2 implements CommandExecutor {

	private final Main main;

	public DuelCommand2(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.RED + "Only players can use this command.");
			return true;
		}

		if (args.length < 1) {
			player.sendMessage(ChatColor.RED + "Usage: /duel <player>");
			return true;
		}

		if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())
				|| main.getSpleefQueueManager().getInFFA(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ffa queue.");
			return true;
		}

		if (!main.getQueuesOpen() && !player.hasPermission("permafrostcore.fstartgame")) {
			player.sendMessage("§cQueues are closed!");
			return true;
		}

		if (main.getDataLoader().getSpleefPlayer(player) != null) {
			if (main.getDataLoader().getSpleefPlayer(player).getInGame()
					|| main.getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
				player.sendMessage("§cThis command cannot be run in game.");
				return true;
			}
		}

		Player dueled = Bukkit.getPlayer(args[0]);
		if (dueled == null) {
			player.sendMessage(ChatColor.RED + "Player not found.");
			return true;
		}

		if (dueled.getUniqueId().equals(player.getUniqueId())) {
			player.sendMessage("§cYou cannot duel yourself.");
			return true;
		}

		SpleefPlayer spDueled = main.getDataLoader().getSpleefPlayer(dueled);
		if (spDueled == null) {
			player.sendMessage("§cThat player is not registered.");
			return true;
		}

		if (spDueled.getInGame() || spDueled.getInTeamGame()) {
			player.sendMessage("§cThat player is already in a game.");
			return true;
		}

		boolean instantStart = args.length >= 2 && args[1].equalsIgnoreCase("true");
		boolean skipMenu = args.length >= 4 && args[3].equalsIgnoreCase("true");

		// Ranked queue / direct mode path:
		// /duel <player> true <mode> true
		if (args.length >= 4 && skipMenu) {
			boolean splegg = false;
			SpleefMode mode = SpleefMode.POWERSPLEEF;

			String modeArg = args[2].toLowerCase();

			switch (modeArg) {
			case "splegg":
				splegg = true;
				mode = null;
				break;
			case "powerspleef":
				mode = SpleefMode.POWERSPLEEF;
				break;
			case "airspleef":
				mode = SpleefMode.AIRSPLEEF;
				break;
			case "minispleef":
				mode = SpleefMode.MINISPLEEF;
				break;
			default:
				player.sendMessage("§cInvalid mode. Use powerspleef, airspleef, minispleef, or splegg.");
				return true;
			}
			sendDuel(player, dueled, instantStart, splegg, mode);
			return true;
		}

		// Normal path: leave ranked queues, then open menu
		if (main.isPlayerInAnyQueue(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			main.removePlayerFromAllRankedQueues(player.getUniqueId());
		}

		openGamemodeMenu(player, dueled, instantStart);
		return true;
	}

	private void openGamemodeMenu(Player player, Player dueled, boolean instantStart) {
		Inventory gui = Bukkit.createInventory(null, 27, ChatColor.AQUA + "" + ChatColor.BOLD + "Select Duel Mode");

		gui.setItem(11, createItem(Material.EGG, "§e§lSplegg", Arrays.asList("§7Click to duel in", "§eSplegg")));

		gui.setItem(12, createItem(Material.DIAMOND_SHOVEL, "§b§lPowerspleef",
				Arrays.asList("§7Click to duel in", "§bPowerspleef")));

		gui.setItem(14,
				createItem(Material.FEATHER, "§f§lAirspleef", Arrays.asList("§7Click to duel in", "§fAirspleef")));

		gui.setItem(15, createItem(Material.RED_CONCRETE, "§c§lMinispleef",
				Arrays.asList("§7Click to duel in", "§cMinispleef")));

		main.getDuelMenuManager().setPendingDuel(player.getUniqueId(), dueled.getUniqueId(), instantStart);
		player.openInventory(gui);
	}

	private ItemStack createItem(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		return item;
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

		player.closeInventory();

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

	}
}
