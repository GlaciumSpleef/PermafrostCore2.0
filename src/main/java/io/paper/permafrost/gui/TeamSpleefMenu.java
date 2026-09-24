package io.paper.permafrost.gui;

import java.util.ArrayList;
import java.util.Arrays;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import net.md_5.bungee.api.ChatColor;

/**
 * REMEMBER to save the teams after you're done with the visualization of the
 * menu Save teams in a hashmap ARRAY OF ARRAYS OF PLAYER OBJECTS
 */
import java.lang.reflect.Field;

public class TeamSpleefMenu {
	private Main main;
	private static final int PAGE_SIZE = 18;
	private static final ItemStack NEXT_PAGE = createNavigationItem(Material.ARROW, "§aNext Page");
	private static final ItemStack PREV_PAGE = createNavigationItem(Material.ARROW, "§cPrevious Page");
	private final Map<UUID, Integer> teamCounts = new HashMap<>();
	// List<Player> team = new ArrayList<>();
	private Player player;
	private int page;
	private Player playerMoved;
	private boolean addRemove; // this variable tells the code whether or not the player head clicked on is
								// meant to be added to a team or removed from one
	private static final Map<UUID, Integer> playerPages = new HashMap<>();
	Map<Integer, List<Player>> teams = new HashMap<>();

	public TeamSpleefMenu(Main main, Player player, int page, Player playerMoved, boolean addRemove) {
		this.main = main;
		this.player = player;
		this.page = page;
		this.playerMoved = playerMoved;
		this.addRemove = addRemove;
	}

	public Player getPlayerMoved() {
		return playerMoved;
	}

	public void setPlayerMoved(Player playerMoved) {
		this.playerMoved = playerMoved;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public boolean getAddRemove(boolean addRemove) {
		return addRemove;
	}

	public void setAddRemove(boolean addRemove) {
		this.addRemove = addRemove;
	}

	public Map<Integer, List<Player>> getTeams() {
		return teams;
	}

	public List<Player> getTeam(int teamNumber) {
		return teams.getOrDefault(teamNumber, new ArrayList<>());
	}

	public void setTeam(int teamNumber, List<Player> players) {
		teams.put(teamNumber, players);
	}

	// public void addPlayerToTeam(int teamNumber, Player player) {
	// teams.computeIfAbsent(teamNumber, k -> new ArrayList<>()).add(player);
	// }

	// public void removePlayerFromTeam(int teamNumber, Player player) {
	// List<Player> team = teams.get(teamNumber);
	// if (team != null) {
	// team.remove(player);
	// if (team.isEmpty()) {
	// teams.remove(teamNumber);
	// }
	// }
	// }

	// public void removeDisplayHeads(List<Player> players) {
	// onlinePlayers.remove(players);
	// }

	public int getPlayerTeam(Player player) {
		for (Map.Entry<Integer, List<Player>> entry : teams.entrySet()) {
			if (entry.getValue().contains(player)) {
				return entry.getKey();
			}
		}
		return -1;
	}

	@SuppressWarnings("deprecation")
	public void openMenu() {
		Inventory inventory = Bukkit.createInventory(player, 9 * 6,
				ChatColor.AQUA + "Team " + main.getDataLoader().getTeamCount(player.getUniqueId()) + " Menu");
		int i = 0;

		ItemStack[] items = new ItemStack[18];
		for (int r = 0; r < 18; r++) {
			if (r <= 8) {
				ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = pane.getItemMeta();

				if (meta != null) {
					meta.setDisplayName(" "); // single space to hide the default name
					pane.setItemMeta(meta);
				}

				items[r] = pane;
			} else if (r == 9) {
				// items[r] = items[r] = new ItemStack(Material.PURPLE_CONCRETE);
				ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = pane.getItemMeta();

				if (meta != null) {
					meta.setDisplayName(" "); // single space to hide the default name
					pane.setItemMeta(meta);
				}

				items[r] = pane;
			} else if (r == 12) {
				ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "NEW TEAM");
				meta.setLore(
						Arrays.asList(ChatColor.GRAY + "Click this item to", ChatColor.GRAY + "create a new team!"));
				item.setItemMeta(meta);
				items[r] = item;
			} else if (r == 14) {
				ItemStack item = new ItemStack(Material.RED_CONCRETE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "SEND DUEL");
				meta.setLore(Arrays.asList(ChatColor.GRAY + "Click this item to",
						ChatColor.GRAY + "send a duel request", ChatColor.GRAY + "to the selected players!"));
				item.setItemMeta(meta);
				items[r] = item;
			} else if (r == 16) {
				ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = pane.getItemMeta();
				if (meta != null) {
					meta.setDisplayName(" ");
					pane.setItemMeta(meta);
				}
				items[r] = pane;
			} else if (r == 17) {
				ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = pane.getItemMeta();
				if (meta != null) {
					meta.setDisplayName(" ");
					pane.setItemMeta(meta);
				}
				items[r] = pane;
			} else {
				ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = pane.getItemMeta();

				if (meta != null) {
					meta.setDisplayName(" "); // single space to hide the default name
					pane.setItemMeta(meta);
				}

				items[r] = pane;
			}
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).size() > 18 && r == 8) {
					// (to go to a new row of players)
					items[r] = new ItemStack(Material.ARROW);
					// player.setMetadata("OpenedMenuPage2", new
					// FixedMetadataValue(Main.getInstance(), "TeamSpleefMenu"));
				}
			}
		}

		if (playerMoved != null && addRemove == true) {
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null)
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved))
					main.getDataLoader().getOnlinePlayers(player.getUniqueId()).remove(playerMoved);
			main.getDataLoader().addPlayerToTeam(player.getUniqueId(),
					main.getDataLoader().getTeamCount(player.getUniqueId()), playerMoved.getUniqueId());
			// team.add(playerMoved);
			// addPlayerToTeam(teamNumber, playerMoved);
		} else if (playerMoved != null && addRemove == false) {
			// team.remove(playerMoved);
			main.getDataLoader().removePlayerFromTeam(player.getUniqueId(),
					main.getDataLoader().getTeamCount(player.getUniqueId()), playerMoved.getUniqueId());
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) == null) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			} else if (!main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved)) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			}
		}
		if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
			for (Player p : main.getDataLoader().getOnlinePlayers(player.getUniqueId())) {
				inventory.setItem(i, getPlayerHead(p.getName()));
				i++;
			}
		}
		i = 27;
		for (UUID uuid : main.getDataLoader().getTeam(player.getUniqueId(),
				main.getDataLoader().getTeamCount(player.getUniqueId()))) {
			inventory.setItem(i, getPlayerHead(Bukkit.getPlayer(uuid).getName()));
			i++;
		}
		for (int index = 0; index < 54; index++) {
			if (index > 17 && index <= 26) {
				inventory.setItem(index, items[index - 18]);
			} else if (index > 44) {
				inventory.setItem(index, items[index - 36]);
			}
		}
		int totalPages = (int) Math.ceil((double) Bukkit.getOnlinePlayers().size() / PAGE_SIZE);
		if (page < 0)
			page = 0;
		if (page >= totalPages)
			page = totalPages - 1;
		if (page > 0)
			inventory.setItem(18, PREV_PAGE);
		if (page < totalPages - 1)
			inventory.setItem(26, NEXT_PAGE);
		playerPages.put(player.getUniqueId(), page);

		// sort teams
		// https://minecraft-heads.com/custom-heads/head/10195-green-arrow-right

		player.openInventory(inventory);
		player.setMetadata("OpenedMenu", new FixedMetadataValue(Main.getInstance(), "Team 1 Menu"));
	}

	private static ItemStack createPlayerHead(Player target) {
		ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwningPlayer(target);
		meta.setDisplayName("§e" + target.getName());
		skull.setItemMeta(meta);
		return skull;
	}

	private static ItemStack createNavigationItem(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}

	public void handleMenuClick(InventoryClickEvent event) {
		if (event.getView().getTitle().equals("Team 1 Menu")) {
			event.setCancelled(true);
			Player player = (Player) event.getWhoClicked();
			UUID playerId = player.getUniqueId();

			if (!playerPages.containsKey(playerId))
				return;
			int currentPage = playerPages.get(playerId);

			// Handle navigation
			if (event.getCurrentItem() != null) {
				if (event.getCurrentItem().isSimilar(NEXT_PAGE)) {
					TeamSpleefMenu menu = new TeamSpleefMenu(main, player, currentPage + 1, null, false);
					menu.openMenu();
				} else if (event.getCurrentItem().isSimilar(PREV_PAGE)) {
					TeamSpleefMenu menu = new TeamSpleefMenu(main, player, currentPage - 1, null, false);
					menu.openMenu();
				}
			}
		}
	}

	public void openRandomizedMenu() {
		main.getDataLoader().setRandomized(player.getUniqueId(), true);
		Inventory inventory = Bukkit.createInventory(player, 9 * 6,
				ChatColor.AQUA + "Choose Players to Send Team Spleef Request:");
		int i = 0;

		ItemStack[] items = new ItemStack[18];
		for (int r = 0; r < 18; r++) {
			if (r <= 8) {
				items[r] = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			} else if (r == 13) {
				// (to add a new team)
				items[r] = items[r] = new ItemStack(Material.GREEN_CONCRETE);
			} else {
				items[r] = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			}
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).size() > 18 && r == 8) {
					// (to go to a new row of players)
					items[r] = new ItemStack(Material.ARROW);
					// player.setMetadata("OpenedMenuPage2", new
					// FixedMetadataValue(Main.getInstance(), "TeamSpleefMenu"));
				}
			}
		}

		if (playerMoved != null && addRemove == true) {
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null)
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved))
					main.getDataLoader().getOnlinePlayers(player.getUniqueId()).remove(playerMoved);
			main.getDataLoader().addPlayerToRandomized(player.getUniqueId(), playerMoved);
			// team.add(playerMoved);
			// addPlayerToTeam(teamNumber, playerMoved);
		} else if (playerMoved != null && addRemove == false) {
			// team.remove(playerMoved);
			main.getDataLoader().removePlayerFromRandomized(player.getUniqueId(), playerMoved);
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) == null) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			} else if (!main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved)) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			}
		}

		if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
			for (Player p : main.getDataLoader().getOnlinePlayers(player.getUniqueId())) {
				inventory.setItem(i, getPlayerHead(p.getName()));
				i++;
			}
		}
		i = 27;
		for (Player p : main.getDataLoader().getRandomizedPlayers(player.getUniqueId())) {
			inventory.setItem(i, getPlayerHead(p.getName()));
			i++;
		}
		for (int index = 0; index < 54; index++) {
			if (index > 17 && index <= 26) {
				inventory.setItem(index, items[index - 18]);
			} else if (index > 44) {
				inventory.setItem(index, items[index - 36]);
			}
		}
		int totalPages = (int) Math.ceil((double) Bukkit.getOnlinePlayers().size() / PAGE_SIZE);
		if (page < 0)
			page = 0;
		if (page >= totalPages)
			page = totalPages - 1;
		if (page > 0)
			inventory.setItem(18, PREV_PAGE);
		if (page < totalPages - 1)
			inventory.setItem(26, NEXT_PAGE);
		playerPages.put(player.getUniqueId(), page);

		// sort teams
		// https://minecraft-heads.com/custom-heads/head/10195-green-arrow-right

		player.openInventory(inventory);
	}

	public void openFinalSelectionMenu() {
		main.getDataLoader().setRandomized(player.getUniqueId(), true);
		Inventory inventory = Bukkit.createInventory(player, 9 * 6, ChatColor.AQUA + "Choose Amount of Players:");
		int i = 0;

		ItemStack[] items = new ItemStack[18];
		for (int r = 0; r < 18; r++) {
			if (r <= 8) {
				items[r] = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			} else if (r == 13) {
				// (to add a new team)
				items[r] = new ItemStack(Material.GREEN_CONCRETE);
			} else {
				items[r] = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			}
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).size() > 18 && r == 8) {
					// (to go to a new row of players)
					items[r] = new ItemStack(Material.ARROW);
					// player.setMetadata("OpenedMenuPage2", new
					// FixedMetadataValue(Main.getInstance(), "TeamSpleefMenu"));
				}
			}
		}

		if (playerMoved != null && addRemove == true) {
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null)
				if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved))
					main.getDataLoader().getOnlinePlayers(player.getUniqueId()).remove(playerMoved);
			main.getDataLoader().addPlayerToFinalized(player.getUniqueId(), playerMoved);
			// team.add(playerMoved);
			// addPlayerToTeam(teamNumber, playerMoved);
		} else if (playerMoved != null && addRemove == false) {
			// team.remove(playerMoved);
			main.getDataLoader().removePlayerFromFinalized(player.getUniqueId(), playerMoved);
			if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) == null) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			} else if (!main.getDataLoader().getOnlinePlayers(player.getUniqueId()).contains(playerMoved)) {
				main.getDataLoader().getOnlinePlayers(player.getUniqueId()).add(playerMoved);
			}
		}

		if (main.getDataLoader().getOnlinePlayers(player.getUniqueId()) != null) {
			for (Player p : main.getDataLoader().getOnlinePlayers(player.getUniqueId())) {
				inventory.setItem(i, getPlayerHead(p.getName()));
				i++;
			}
		}
		i = 27;
		for (Player p : main.getDataLoader().getFinalizedPlayers(player.getUniqueId())) {
			inventory.setItem(i, getPlayerHead(p.getName()));
			i++;
		}
		for (int index = 0; index < 54; index++) {
			if (index > 17 && index <= 26) {
				inventory.setItem(index, items[index - 18]);
			} else if (index > 44) {
				inventory.setItem(index, items[index - 36]);
			}
		}
		int totalPages = (int) Math.ceil((double) Bukkit.getOnlinePlayers().size() / PAGE_SIZE);
		if (page < 0)
			page = 0;
		if (page >= totalPages)
			page = totalPages - 1;
		if (page > 0)
			inventory.setItem(18, PREV_PAGE);
		if (page < totalPages - 1)
			inventory.setItem(26, NEXT_PAGE);
		playerPages.put(player.getUniqueId(), page);

		// sort teams
		// https://minecraft-heads.com/custom-heads/head/10195-green-arrow-right

		player.openInventory(inventory);
	}

	public static ItemStack getPlayerHead(String playerName) {
		ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();

		if (meta != null) {
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
			meta.setOwningPlayer(player);
			skull.setItemMeta(meta);
		}
		return skull;
	}

	public static ItemStack getCustomHead(String textureBase64) {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

		GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
		profile.getProperties().put("textures", new Property("textures", textureBase64));

		try {
			Field profileField = skullMeta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(skullMeta, profile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		head.setItemMeta(skullMeta);
		return head;
	}
}
