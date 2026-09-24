package io.paper.permafrost.core.game.tasks;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefDuel;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefType;
import io.paper.permafrost.core.game.SpleggGame;
import io.paper.permafrost.core.game.SpleggRankedArena;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.gui.TeamSpleefMenu;
import io.paper.permafrost.training.TrainingPlot;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShardShovel;
import io.paper.permafrost.utilities.ShellRank;
import io.paper.permafrost.utilities.SpleefMode;

public class TeamSpleefListener implements Listener {
	private final Main main;
	TeamSpleefMenu menu;
	private Map<UUID, Boolean> serverClosingPlayers = new HashMap<>(); // custom check to see whether a player is
																		// closing their menu manually or not

	public TeamSpleefListener(Main main) {
		this.main = main;
		menu = null;
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		UUID playerId = player.getUniqueId();
		ItemStack clickedItem = event.getCurrentItem();
		SkullMeta skullMeta = null;
		String targetPlayerName = null;
		SpleefPlayer spp = main.getDataLoader().getSpleefPlayer(player);

		if (event.getSlot() == 38) {
			ItemStack current = event.getCurrentItem();
			if (current != null)
				if (current.getType().equals(Material.LEATHER_CHESTPLATE)) {
					if (event.getAction() == InventoryAction.PICKUP_ALL
							|| event.getAction() == InventoryAction.PICKUP_HALF
							|| event.getAction() == InventoryAction.PICKUP_ONE
							|| event.getAction() == InventoryAction.PICKUP_SOME
							|| event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
							|| event.getAction() == InventoryAction.HOTBAR_SWAP
							|| event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
							|| event.getAction() == InventoryAction.DROP_ALL_SLOT
							|| event.getAction() == InventoryAction.DROP_ONE_SLOT
							|| event.getClick() == ClickType.NUMBER_KEY
							|| event.getClick() == ClickType.DROP
							|| event.getClick() == ClickType.CONTROL_DROP) {
						event.setCancelled(true);
					}
				}
		}

		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Join FFA Queue!")) {
			event.setCancelled(true);
			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType().isAir()) {
				return;
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) {
				return;
			}
			Material type = item.getType();
			switch (type) {
			case Material.WIND_CHARGE: {
				player.performCommand("spleefqueue powerspleef");
				break;
			}
			case Material.SNOW_BLOCK: {
				player.performCommand("spleefqueue airspleef");
				break;
			}
			case Material.BARRIER: {
				player.performCommand("spleefqueue prototype");
				break;
			}
			}
			player.closeInventory();
		}

		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Join Ranked Queue!")) {
			event.setCancelled(true);
			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType().isAir()) {
				return;
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) {
				return;
			}
			Material type = item.getType();
			switch (type) {
			case Material.WIND_CHARGE: {
				player.performCommand("rankedqueue spleef powerspleef");
				break;
			}
			case Material.SNOW_BLOCK: {
				player.performCommand("rankedqueue spleef airspleef");
				break;
			}
			case Material.STONE_BUTTON: {
				player.performCommand("rankedqueue spleef minispleef");
				break;
			}
			}
			player.closeInventory();
		}

		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Team Duel Mode")) {
			event.setCancelled(true);

			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType().isAir()) {
				return;
			}

			String selectedMode = null;

			switch (item.getType()) {
			case EGG:
				selectedMode = "splegg";
				break;
			case DIAMOND_SHOVEL:
				selectedMode = "powerspleef";
				break;
			case FEATHER:
				selectedMode = "airspleef";
				break;
			case RED_CONCRETE:
				selectedMode = "minispleef";
				break;
			default:
				return;
			}

			player.setMetadata("team_duel_mode", new FixedMetadataValue(Main.getInstance(), selectedMode));

			TeamSpleefMenu teamMenu = new TeamSpleefMenu(main, player, 0, null, false);
			teamMenu.openMenu();
			return;
		}
		if (event.getView().getTitle()
				.equals("" + ChatColor.RED + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your Teams Ban!")) {
			event.setCancelled(true);

			// Prevent bottom inventory clicks
			if (event.getRawSlot() >= event.getView().getTopInventory().getSize())
				return;

			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			String plainName = null;
			ItemMeta meta = clicked.getItemMeta();
			if (meta.hasDisplayName()) {
				String displayName = meta.getDisplayName();
				plainName = ChatColor.stripColor(displayName);
				plainName = plainName.replaceAll("[^A-Za-z0-9 ]", "").trim();
			}

			if (plainName == null || plainName.isEmpty()) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			boolean validPower = false;
			switch (plainName.toUpperCase()) {
			case "SPEED":
			case "WINDCHARGE":
			case "BUILDER":
			case "ICEGRENADE":
			case "DOUBLEJUMP":
			case "RECALL":
			case "BLACKGOO":
			case "LEVITATION":
			case "SWAP":
			case "DECAYGRENADE":
			case "TNTRUN":
			case "SILENCER":
			case "SNARE":
			case "SMOKEGRENADE":
			case "VORTEXGRENADE":
			case "ROULETTE":
			case "PURGINGSHIELD":
			case "ENDERPEARL":
				validPower = true;
				break;
			default:
				break;
			}

			if (!validPower) {
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.65f, 0.8f);
				return;
			}

			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
			player.performCommand("bans 8882469198 " + plainName.toUpperCase());
			player.sendMessage(
					"§e[PowerSpleef] > §eYou selected the §a" + plainName.toUpperCase() + " §epower to ban!");
			player.closeInventory();
		}
		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Choose Your Favorite Maps!")) {
			event.setCancelled(true);

			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType().isAir()) {
				return;
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null || !meta.hasDisplayName()) {
				return;
			}

			String name = ChatColor.stripColor(meta.getDisplayName());
			if (name == null) {
				return;
			}

			name = name.replace("✦", "").trim();

			for (SpleggRankedArena arena : main.getDataLoader().getSpleggRankedArenas()) {
				if (name.equalsIgnoreCase(arena.getId())) {
					if (main.getDataLoader().hasFavoritedArena(playerId, arena)) {
						main.getDataLoader().removeFavoritedArena(playerId, arena);
					} else {
						main.getDataLoader().addFavoritedArena(playerId, arena);
					}

					player.openInventory(buildFavoriteMapsGui(player));
					return;
				}
			}
		}
		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "View Buyable Ranks!")) {
			event.setCancelled(true);
		}
		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "View Your Ranks!")) {
			event.setCancelled(true);

			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType().isAir()) {
				return;
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) {
				return;
			}

			PersistentDataContainer pdc = meta.getPersistentDataContainer();

			String type = pdc.get(new NamespacedKey(main, "rank_type"), PersistentDataType.STRING);
			String value = pdc.get(new NamespacedKey(main, "rank_value"), PersistentDataType.STRING);

			if (type == null) {
				return;
			}

			if (type.equals("custom")) {
				main.getCosmeticRankManager().applyStoredCustomRank(player.getUniqueId());
				player.sendMessage("§a✔ Using your custom rank.");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				player.closeInventory();
				return;
			}

			if (type.equals("default")) {
				main.getCosmeticRankManager().clearAllCustomRanks(player.getUniqueId());
				player.sendMessage("§a✔ Using your normal server rank.");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				player.closeInventory();
				return;
			}

			if (type.equals("shell")) {
				if (value == null) {
					player.sendMessage("§cThis shell rank item is missing its value.");
					return;
				}

				ShellRank rank = ShellRank.valueOf(value);
				Bukkit.getLogger().info("Applying shell rank: " + rank.name());

				main.getCosmeticRankManager().applyShellRank(player.getUniqueId(), rank);
				if (main.getCosmeticRankManager().hasShellRankUnlocked(playerId, rank))
					player.sendMessage("§a✔ You selected the rank §f" + rank.getDisplayName() + "§r");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				player.closeInventory();
				return;
			}

			if (type.equals("purchasable")) {
				if (value == null) {
					player.sendMessage("§cThis purchasable rank item is missing its value.");
					return;
				}

				PurchasableRank rank = PurchasableRank.valueOf(value);
				Bukkit.getLogger().info("Applying purchasable rank: " + rank.name());

				if (!main.getDataLoader().hasPurchasableRank(player.getUniqueId(), rank)) {
					player.sendMessage("§cYou do not own this rank.");
					return;
				}

				if (rank == PurchasableRank.CUSTOM) {
					String customName = main.getDataLoader().getCustomRankName(player.getUniqueId());
					if (customName == null || customName.isBlank()) {
						player.sendMessage("§cYour custom rank text has not been set yet.");
						return;
					}
				}

				main.getCosmeticRankManager().applyPurchasableRank(player.getUniqueId(), rank);
				player.sendMessage("§a✔ You selected the rank §f" + rank.getDisplayName() + "§r");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				player.closeInventory();
			}
		}
		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "View Your Shovels!")) {

			event.setCancelled(true);

			ItemStack item = event.getCurrentItem();
			if (item == null || !item.hasItemMeta())
				return;

			String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

			if (name.equals("Base Diamond Shovel")) {
				String sql = "INSERT INTO player_shovel_model_data (uuid, custom_model_data) VALUES (?, ?) "
						+ "ON DUPLICATE KEY UPDATE custom_model_data = VALUES(custom_model_data)";

				try (Connection connection = main.getConnection();
						PreparedStatement ps = connection.prepareStatement(sql)) {

					ps.setString(1, player.getUniqueId().toString());
					ps.setInt(2, 0);

					ps.executeUpdate();
					player.sendMessage("§aYou selected the §ebasic §ashovel!");

				} catch (SQLException e) {
					e.printStackTrace();
					player.sendMessage("§cThere was an error saving your shovel model data.");
				}

			} else if (name.equals("Polar Shovel")) {
				if (main.getDataLoader().hasPurchasableRank(playerId, PurchasableRank.POLAR)
						|| main.getDataLoader().hasPurchasableRank(playerId, PurchasableRank.CUSTOM)) {

					String sql = "INSERT INTO player_shovel_model_data (uuid, custom_model_data) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE custom_model_data = VALUES(custom_model_data)";

					try (Connection connection = main.getConnection();
							PreparedStatement ps = connection.prepareStatement(sql)) {

						ps.setString(1, player.getUniqueId().toString());
						ps.setInt(2, ShardShovel.fromDisplayName(name).getCustomModelData());

						ps.executeUpdate();
						player.sendMessage("§aYou selected the §e" + ShardShovel.fromDisplayName(name).getDisplayName()
								+ " §ashovel!");

					} catch (SQLException e) {
						e.printStackTrace();
						player.sendMessage("§cThere was an error saving your shovel model data.");
					}

				} else {
					player.sendMessage("§cYou do not have access to this shovel.");
				}

			} else if (getFoundShovelModelData(name) != null) {
				String column = getFoundShovelColumn(name);
				int modelData = getFoundShovelModelData(name);

				if (column == null) {
					player.sendMessage("§cInvalid shovel.");
					player.closeInventory();
					return;
				}

				if (!main.getDataLoader().hasFoundInteractableShovel(playerId, column)) {
					player.sendMessage("§cYou have not found this shovel yet.");
					player.closeInventory();
					return;
				}

				String sql = "INSERT INTO player_shovel_model_data (uuid, custom_model_data) VALUES (?, ?) "
						+ "ON DUPLICATE KEY UPDATE custom_model_data = VALUES(custom_model_data)";

				try (Connection connection = main.getConnection();
						PreparedStatement ps = connection.prepareStatement(sql)) {

					ps.setString(1, player.getUniqueId().toString());
					ps.setInt(2, modelData);

					ps.executeUpdate();
					player.sendMessage("§aYou selected the §e" + name + " §ashovel!");

				} catch (SQLException e) {
					e.printStackTrace();
					player.sendMessage("§cThere was an error saving your shovel model data.");
				}

			} else {
				ShardShovel shardShovel = ShardShovel.fromDisplayName(name);

				if (shardShovel == null) {
					player.sendMessage("§cInvalid shovel.");
					player.closeInventory();
					return;
				}

				if (main.getDataLoader().hasAccessToShardShovel(playerId, shardShovel)) {
					String sql = "INSERT INTO player_shovel_model_data (uuid, custom_model_data) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE custom_model_data = VALUES(custom_model_data)";

					try (Connection connection = main.getConnection();
							PreparedStatement ps = connection.prepareStatement(sql)) {

						ps.setString(1, player.getUniqueId().toString());
						ps.setInt(2, shardShovel.getCustomModelData());

						ps.executeUpdate();
						player.sendMessage("§aYou selected the §e" + shardShovel.getDisplayName() + " §ashovel!");

					} catch (SQLException e) {
						e.printStackTrace();
						player.sendMessage("§cThere was an error saving your shovel model data.");
					}
				} else {
					player.sendMessage("§cYou do not have access to this shovel.");
				}
			}

			player.closeInventory();
		}

		if (event.getView().getTitle().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Buy Ranks or Join Discord!")) {

			event.setCancelled(true);

			ItemStack item = event.getCurrentItem();
			if (item == null || !item.hasItemMeta())
				return;

			String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

			switch (name) {
			case "Discord":
				player.closeInventory();
				Bukkit.dispatchCommand(player, "discord");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				break;

			case "Shop":
				player.closeInventory();
				Bukkit.dispatchCommand(player, "shop");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
				break;
			}
		}

		if (event.getView().getTitle().equals(ChatColor.AQUA + "Presets Available")) {

			switch (clickedItem.getType()) {

			case NETHER_STAR -> {

				TrainingPlot plot = main.getDataLoader().getTrainingPlot("circlereach");

				if (plot == null) {
					player.sendMessage(ChatColor.RED + "This training plot is not configured correctly.");
					player.closeInventory();
					return;
				}

				File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", plot.getSchematic());

				main.getFAWEAPI().pasteSchematic(schemFile,
						main.getDataLoader().getTrainingSession(playerId).getArena().getPlotPasteLocation(), false);

				plot.setYaw(225.0f);

				main.getDataLoader().getTrainingSession(playerId).setPlot(plot);

				player.closeInventory();

				player.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + plot.getName() + " " + ChatColor.GRAY
						+ "preset selected. " + ChatColor.GREEN + "Click the green concrete to begin your training.");

				ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§a§lSTART SESSION");
				item.setItemMeta(meta);

				player.getInventory().setItem(4, item);

				break;
			}

			case WITHER_SKELETON_SKULL -> {

				TrainingPlot plot = main.getDataLoader().getTrainingPlot("diagonalruin");

				if (plot == null) {
					player.sendMessage(ChatColor.RED + "This training plot is not configured correctly.");
					player.closeInventory();
					return;
				}

				File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", plot.getSchematic());

				main.getFAWEAPI().pasteSchematic(schemFile,
						main.getDataLoader().getTrainingSession(playerId).getArena().getPlotPasteLocation(), false);

				main.getDataLoader().getTrainingSession(playerId).setPlot(plot);

				player.closeInventory();

				player.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + plot.getName() + " " + ChatColor.GRAY
						+ "preset selected. " + ChatColor.GREEN + "Click the green concrete to begin your training.");

				ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§a§lSTART SESSION");
				item.setItemMeta(meta);

				player.getInventory().setItem(4, item);

				break;
			}
			case STICK -> {
				TrainingPlot plot = main.getDataLoader().getTrainingPlot("straightreach1");

				if (plot == null) {
					player.sendMessage(ChatColor.RED + "This training plot is not configured correctly.");
					player.closeInventory();
					return;
				}

				File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", plot.getSchematic());

				main.getFAWEAPI().pasteSchematic(schemFile,
						main.getDataLoader().getTrainingSession(playerId).getArena().getPlotPasteLocation(), false);

				main.getDataLoader().getTrainingSession(playerId).setPlot(plot);
				plot.setYaw(270.0f);
				player.closeInventory();

				player.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + plot.getName() + " " + ChatColor.GRAY
						+ "preset selected. " + ChatColor.GREEN + "Click the green concrete to begin your training.");

				ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§a§lSTART SESSION");
				item.setItemMeta(meta);

				player.getInventory().setItem(4, item);

				break;
			}
			}
		}

		if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD)
			skullMeta = (SkullMeta) clickedItem.getItemMeta();
		if (skullMeta != null && skullMeta.hasOwner()) {
			if (skullMeta.getOwningPlayer().getName() != null) {
				targetPlayerName = skullMeta.getOwningPlayer().getName();
				OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
				menu = new TeamSpleefMenu(main, player, 0, (Player) targetPlayer, true);
			} else {
				menu = new TeamSpleefMenu(main, player, 0, null, true);
			}
		} else if (skullMeta == null) {
			menu = new TeamSpleefMenu(main, player, 0, null, true);
		}

		if (player.hasMetadata("OpenedMenu")) {
			int sender = 0;
			for (int i = 1; i <= main.getDataLoader().getTeamCount(playerId); i++) {
				for (UUID uuid : main.getDataLoader().getTeam(playerId, i)) {
					if (uuid == playerId)
						sender++;
				}
			}
			int requestedPlayersCount = 0;
			List<UUID> requestedPlayers = new ArrayList<>();
			for (int i = 1; i <= main.getDataLoader().getTeamCount(playerId); i++) {
				for (UUID uuid : main.getDataLoader().getTeam(playerId, i)) {
					if (main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(uuid)).getHasBeenRequested()) {
						requestedPlayersCount++;
						requestedPlayers.add(uuid);
					}
				}
			}
			// checks slots
			// 0-17
			if (event.getRawSlot() >= 0 && event.getRawSlot() <= 17) {
				if (clickedItem == null)
					return;
				if (skullMeta == null || !skullMeta.hasOwner())
					return;
				if (targetPlayerName == null)
					return;
				menu.setAddRemove(true);
				serverClosingPlayers.put(playerId, true);
				if (main.getDataLoader().getRandomized(player.getUniqueId())) {
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					menu.openRandomizedMenu();
				} else if (main.getDataLoader().getFinalized(player.getUniqueId())) {
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					menu.openFinalSelectionMenu();
				} else {
					menu.openMenu();
				}
				// checks slots
				// 27-44
			} else if (event.getRawSlot() >= 27 && event.getRawSlot() <= 44) {
				if (clickedItem == null)
					return;
				if (skullMeta == null || !skullMeta.hasOwner())
					return;
				if (targetPlayerName == null)
					return;
				menu.setAddRemove(false);
				serverClosingPlayers.put(playerId, true);
				if (main.getDataLoader().getRandomized(playerId)) {
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					menu.openRandomizedMenu();
				} else if (main.getDataLoader().getFinalized(playerId)) {
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					menu.openFinalSelectionMenu();
				} else {
					menu.openMenu();
				}
				// checks slot
				// 45
			} else if (event.getRawSlot() == 45) {
				if (!main.getDataLoader().getRandomized(playerId) && !main.getDataLoader().getFinalized(playerId)) {
					main.getDataLoader().setRandomized(playerId, true);
					serverClosingPlayers.put(playerId, true);
					menu.openRandomizedMenu();
				} else {
					return;
				}
				// checks slot
				// 48
			} else if (event.getRawSlot() == 48) {
				if (main.getDataLoader().getRandomized(player.getUniqueId())
						|| main.getDataLoader().getFinalized(player.getUniqueId()))
					return;
				menu.setAddRemove(true);
				int currentCount = main.getDataLoader().getTeamCount(playerId);
				main.getDataLoader().setTeamCount(playerId, currentCount + 1);
				if (main.getDataLoader().getTeamCount(playerId) > 16) {
					player.sendMessage("§cYou cannot send TeamSpleef with more than 16 teams.");
					player.closeInventory();
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);

				} else if (main.getDataLoader().getTeamCount(playerId) < 2) {
					player.sendMessage("§cYou cannot send TeamSpleef with less than 2 teams.");
					player.closeInventory();
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
				} else if (event.getClickedInventory().getItem(27) == null) {
					player.sendMessage("§cYou cannot send TeamSpleef with an empty team.");
					player.closeInventory();
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
				} else {
					serverClosingPlayers.put(playerId, true);
					menu.openMenu();
				}
				// checks slot
				// 49
			} else if (event.getRawSlot() == 49) {
				if (main.getDataLoader().getRandomized(player.getUniqueId())) {
					if (event.getClickedInventory().getItem(27) == null) {
						player.sendMessage("§cYou need to choose players to continue (click on their heads).");
						player.closeInventory();
						// main.getDataLoader().setTeamCount(playerId, 1);
						// main.getDataLoader().clearTeams(playerId);
						// List<Player> onlinePlayers = new
						// ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						// main.getDataLoader().setOnlinePlayers(onlinePlayers);
					} else {
						serverClosingPlayers.put(playerId, true);
						List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						List<Player> toRemove = new ArrayList<>();
						for (Player p : onlinePlayers) {
							SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
							if (spleefPlayer.getGame() != null) {
								toRemove.add(p);
							}
						}
						onlinePlayers.removeAll(toRemove);
						main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
						menu.openFinalSelectionMenu();
					}
				} else if (main.getDataLoader().getFinalized(player.getUniqueId())) {
					if (event.getClickedInventory().getItem(27) == null) {
						player.sendMessage("§cYou need to choose players to continue (click on their heads).");
						player.closeInventory();
					} else {
						//
						// START FINALIZED GAME
						//
						//
					}
				} else {
					return;
				}
				// checks slot
				// 50
			} else if (event.getRawSlot() == 50) {
				if (main.getDataLoader().getTeamCount(playerId) < 2) {
					player.sendMessage("§cYou cannot send TeamSpleef with less than 2 teams.");
					player.closeInventory();
					main.getDataLoader().setTeamCount(playerId, 1);
					main.getDataLoader().clearTeams(playerId);
					List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
					List<Player> toRemove = new ArrayList<>();
					for (Player p : onlinePlayers) {
						SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
						if (spleefPlayer.getGame() != null) {
							toRemove.add(p);
						}
					}
					onlinePlayers.removeAll(toRemove);
					main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					return;
				} else {
					if (main.getDataLoader().getRandomized(player.getUniqueId()))
						return;
					if (main.getDataLoader().getFinalized(player.getUniqueId()))
						return;
					else if (event.getClickedInventory().getItem(27) == null) {
						player.sendMessage("§cYou cannot send TeamSpleef with an empty team.");
						player.closeInventory();
						main.getDataLoader().setTeamCount(playerId, 1);
						main.getDataLoader().clearTeams(playerId);
						List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						List<Player> toRemove = new ArrayList<>();
						for (Player p : onlinePlayers) {
							SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
							if (spleefPlayer.getGame() != null) {
								toRemove.add(p);
							}
						}
						onlinePlayers.removeAll(toRemove);
						main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					} else if (sender != 1) {
						player.sendMessage("§cYou cannot send TeamSpleef without yourself included.");
						player.closeInventory();
						main.getDataLoader().setTeamCount(playerId, 1);
						main.getDataLoader().clearTeams(playerId);
						List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						List<Player> toRemove = new ArrayList<>();
						for (Player p : onlinePlayers) {
							SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
							if (spleefPlayer.getGame() != null) {
								toRemove.add(p);
							}
						}
						onlinePlayers.removeAll(toRemove);
						main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
					} else if (requestedPlayersCount >= 1) {
						String messageee = "§cYou cannot send TeamSpleef to players that have already recieved a duel request.\n§cPlayers with an existing request: ";
						for (UUID uuid : requestedPlayers) {
							messageee += Bukkit.getPlayer(uuid).getName() + " ";
						}
						player.sendMessage(messageee);
						player.closeInventory();
						main.getDataLoader().setTeamCount(playerId, 1);
						main.getDataLoader().clearTeams(playerId);
						List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						List<Player> toRemove = new ArrayList<>();
						for (Player p : onlinePlayers) {
							SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
							if (spleefPlayer.getGame() != null) {
								toRemove.add(p);
							}
						}
						onlinePlayers.removeAll(toRemove);
					} else {
						String selectedMode = "powerspleef";
						if (player.hasMetadata("team_duel_mode")) {
							selectedMode = player.getMetadata("team_duel_mode").get(0).asString();
						}

						boolean splegg = selectedMode.equalsIgnoreCase("splegg");
						SpleefMode mode = SpleefMode.POWERSPLEEF;

						if (!splegg) {
							if (selectedMode.equalsIgnoreCase("airspleef")) {
								mode = SpleefMode.AIRSPLEEF;
							} else if (selectedMode.equalsIgnoreCase("minispleef")) {
								mode = SpleefMode.MINISPLEEF;
							} else {
								mode = SpleefMode.POWERSPLEEF;
							}
						}

						SpleefGame game = new SpleefGame(main, splegg, false, splegg ? null : mode);

						for (int i = main.getDataLoader().getTeamCount(playerId); i > 0; i--) {
							for (UUID uuid : main.getDataLoader().getTeam(playerId, i)) {
								String color = "";
								switch (i) {
								case 1:
									color = "blue";
									break;
								case 2:
									color = "red";
									break;
								case 3:
									color = "green";
									break;
								case 4:
									color = "yellow";
									break;
								case 5:
									color = "orange";
									break;
								case 6:
									color = "purple";
									break;
								case 7:
									color = "pink";
									break;
								case 8:
									color = "light blue";
									break;
								case 9:
									color = "magenta";
									break;
								case 10:
									color = "brown";
									break;
								case 11:
									color = "cyan";
									break;
								case 12:
									color = "light gray";
									break;
								case 13:
									color = "gray";
									break;
								case 14:
									color = "dark green";
									break;
								case 15:
									color = "white";
									break;
								case 16:
									color = "black";
									break;
								}

								List<UUID> teamUUIDs = main.getDataLoader().getTeam(playerId, i);
								TeamSpleefTeam team = new TeamSpleefTeam(main, color, teamUUIDs);
								game.setTeamSpleefTeam(color, teamUUIDs);

								for (UUID uuid2 : teamUUIDs) {
									Player player2 = Bukkit.getPlayer(uuid2);
									if (player2 == null)
										continue;
									SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player2);
									if (sp == null)
										continue;
									sp.setTeam(team);
								}
							}
						}

						game.setTeamCount(main.getDataLoader().getTeamCount(playerId));

						Map<Integer, List<UUID>> teamMap = new HashMap<>();
						for (int i = 1; i <= main.getDataLoader().getTeamCount(playerId); i++) {
							teamMap.put(i, main.getDataLoader().getTeam(playerId, i));
						}

						for (Map.Entry<Integer, List<UUID>> entry : teamMap.entrySet()) {
							Bukkit.getLogger().info("Team " + entry.getKey() + ": " + entry.getValue());
						}

						game.setTeamGame(true);
						game.saveTeams(teamMap);
						Bukkit.getLogger().info("Saved " + game.getPlayers().size() + " players to game.");
						main.getDuelManager().setGame(game);

						List<Integer> teamSizes = new ArrayList<>();
						Set<UUID> players = new HashSet<>();

						for (int i = 1; i <= main.getDataLoader().getTeamCount(playerId); i++) {
							int teamSize = 0;
							for (UUID uuid2 : main.getDataLoader().getTeam(playerId, i)) {
								if (!uuid2.equals(playerId)) {
									players.add(uuid2);
								}
								teamSize++;
							}
							teamSizes.add(teamSize);
						}

						Bukkit.getPlayer(playerId).closeInventory();
						main.getDuelManager().sendRequest(playerId, players, teamSizes);

						main.getDataLoader().setTeamCount(playerId, 1);
						main.getDataLoader().clearTeams(playerId);

						List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
						List<Player> toRemove = new ArrayList<>();
						for (Player p : onlinePlayers) {
							SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
							if (spleefPlayer.getGame() != null) {
								toRemove.add(p);
							}
						}
						onlinePlayers.removeAll(toRemove);
						main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);

					}
				}
			}
		}

	}

	public Inventory buildFavoriteMapsGui(Player player) {
		Inventory gui = Bukkit.createInventory(null, 27,
				ChatColor.AQUA + "" + ChatColor.BOLD + "Choose Your Favorite Maps!");

		int i = 0;
		for (SpleggRankedArena arena : main.getDataLoader().getSpleggRankedArenas()) {
			ItemStack item = new ItemStack(Material.FIREWORK_STAR);
			ItemMeta meta = item.getItemMeta();

			if (meta != null) {
				boolean favorited = main.getDataLoader().hasFavoritedArena(player.getUniqueId(), arena);

				if (favorited) {
					meta.setDisplayName("§f✦ §6§l" + arena.getId());
				} else {
					meta.setDisplayName("§6§l" + arena.getId());
				}

				List<String> lore = new ArrayList<>();
				lore.add("");
				lore.add(favorited ? "§eClick to remove from favorites" : "§eClick to set as favorite");
				meta.setLore(lore);

				meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

				item.setItemMeta(meta);
			}

			gui.setItem(i, item);
			i++;
			if (i >= gui.getSize()) {
				break;
			}
		}

		return gui;
	}

	private Integer getFoundShovelModelData(String name) {
		switch (name) {
		case "Spoon":
			return 1017;
		case "Chorizo":
			return 1005;
		case "Lost Trident":
			return 1001;
		case "Ancient Tusk":
			return 1016;
		case "Colourful Rainbow":
			return 1002;
		case "King's Blade":
			return 1003;
		case "Sunflower":
			return 1015;
		case "Mystic":
			return 1013;
		case "Stop Sign":
			return 1019;
		case "Nature's Staff":
			return 1014;
		case "Valentines":
			return 1018;
		default:
			return null;
		}
	}

	private String getFoundShovelColumn(String name) {
		switch (name) {
		case "Spoon":
			return "spoon";
		case "Chorizo":
			return "chorizo";
		case "Lost Trident":
			return "lost_trident";
		case "Ancient Tusk":
			return "ancient_tusk";
		case "Colourful Rainbow":
			return "colourful_rainbow";
		case "King's Blade":
			return "kings_blade";
		case "Sunflower":
			return "sunflower";
		case "Mystic":
			return "mystic";
		case "Stop Sign":
			return "stop_sign";
		case "Nature's Staff":
			return "natures_staff";
		case "Valentines":
			return "valentines";
		default:
			return null;
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		UUID playerId = player.getUniqueId();
		if (serverClosingPlayers.containsKey(playerId)) {
			if (serverClosingPlayers.get(playerId).equals(false)) {
				main.getDataLoader().setTeamCount(playerId, 1);
				main.getDataLoader().clearTeams(playerId);
				List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
				List<Player> toRemove = new ArrayList<>();
				for (Player p : onlinePlayers) {
					SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
					if (spleefPlayer.getGame() != null) {
						toRemove.add(p);
					}
				}
				onlinePlayers.removeAll(toRemove);
				main.getDataLoader().setOnlinePlayers(playerId, onlinePlayers);
				main.getDataLoader().setRandomized(playerId, false);
				main.getDataLoader().clearRandomized(playerId);
			} else if (serverClosingPlayers.get(playerId).equals(true)) {
				serverClosingPlayers.replace(playerId, false);
			}
		}
	}
}
