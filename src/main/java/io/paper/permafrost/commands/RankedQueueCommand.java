package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.SpleefMode;
import net.md_5.bungee.api.ChatColor;

public class RankedQueueCommand implements CommandExecutor {
	private Main main;

	public RankedQueueCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else {
			Player player = (Player) sender;
			if (main.getDataLoader().getSpleefPlayer(player) != null) {
				if (main.getDataLoader().getSpleefPlayer(player).getInGame()
						|| main.getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
					player.sendMessage("&cThis command cannot be run in game.");
					return true;
				}
			}
			if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
				player.sendMessage("&cThis command cannot be run in ffa queue.");
				return true;
			}
			if(main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}
			
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}
			if (command.getName().equalsIgnoreCase("rankedqueue")) {
				if(!main.getQueuesOpen()) {
					player.sendMessage("§cQueues are closed!");
					return true;
				}
				if (args.length == 1) {
					if (args[0].equals("splegg")) {
						player.getInventory().clear();
						ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
						ItemMeta m = slimeball.getItemMeta();
						m.addEnchant(Enchantment.EFFICIENCY, 10, true);
						m.setUnbreakable(true);
						m.setDisplayName("§cLeave");
						slimeball.setItemMeta(m);
						slimeball.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						if (!player.getInventory().contains(slimeball)) {
							player.getInventory().setItem(8, slimeball);
						}

						main.getRankedSpleggManager().addPlayerToQueue(player.getUniqueId());
						if (main.getRankedSpleggManager().getGames() != null) {
							int playerCount = 0;
							for (SpleefGame game : main.getRankedSpleggManager().getGames()) {
								for (SpleefPlayer sp : game.getPlayers()) {
									playerCount++;
								}
							}
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the splegg ranked queue!\nThere are currently "
									+ ChatColor.YELLOW + playerCount + ChatColor.GREEN + " players playing and "
									+ ChatColor.YELLOW + main.getRankedSpleggManager().getPlayersInQueue().size()
									+ " players waiting.");
							main.getRankedSpleggManager().addPlayerToQueue(player.getUniqueId());
						} else {
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the splegg ranked queue!\nThere are currently "
									+ main.getRankedSpleggManager().getPlayersInQueue().size() + " players waiting.");
							main.getRankedSpleggManager().addPlayerToQueue(player.getUniqueId());
						}
						return true;

					}
				} else if (args.length == 2) {
					if (args[0].equals("spleef") && args[1].equals("menu")) {
						Inventory gui = Bukkit.createInventory(null, 27,
								ChatColor.AQUA + "" + ChatColor.BOLD + "Join Ranked Queue!");

						ItemStack windcharge = new ItemStack(Material.WIND_CHARGE);
						ItemMeta windMeta = windcharge.getItemMeta();
						if (windMeta != null) {
							windMeta.setDisplayName("§b§lPowerSpleef");
							List<String> lore = new ArrayList<>();
							lore.add("§7Use powers to play!");
							lore.add("");
							lore.add("§fIn Queue: §e" + main.getRankedSpleefManager().getQueueSize(SpleefMode.POWERSPLEEF));
							lore.add("§eClick to join the queue");
							windMeta.setLore(lore);
							// glow effect
							windMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
							windMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
							windcharge.setItemMeta(windMeta);
						}
						gui.setItem(11, windcharge);

						ItemStack snow = new ItemStack(Material.SNOW_BLOCK);
						ItemMeta snowMeta = snow.getItemMeta();
						if (snowMeta != null) {
							snowMeta.setDisplayName("§e§lAirSpleef");
							List<String> lore = new ArrayList<>();
							lore.add("§7Classic, normal spleef");
							lore.add("");
							lore.add("§fIn Queue: §e" + main.getRankedSpleefManager().getQueueSize(SpleefMode.AIRSPLEEF));
							lore.add("§eClick to join the queue");
							snowMeta.setLore(lore);
							// glow effect
							snowMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
							snowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
							snow.setItemMeta(snowMeta);
						}
						gui.setItem(13, snow);

						ItemStack button = new ItemStack(Material.STONE_BUTTON);
						ItemMeta buttonMeta = button.getItemMeta();
						if (buttonMeta != null) {
							buttonMeta.setDisplayName("§6§lMiniSpleef");
							List<String> lore = new ArrayList<>();
							lore.add("§7Spleef, but tinier!");
							lore.add("");
							lore.add("§fIn Queue: §e" + main.getRankedSpleefManager().getQueueSize(SpleefMode.MINISPLEEF));
							lore.add("§eClick to join the queue");
							buttonMeta.setLore(lore);
							// glow effect
							buttonMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
							buttonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
							button.setItemMeta(buttonMeta);
						}
						gui.setItem(15, button);
						player.openInventory(gui);
						return true;
					} else if (args[0].equals("spleef") && args[1].equals("powerspleef")) {
						player.getInventory().clear();
						ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
						ItemMeta m = slimeball.getItemMeta();
						m.addEnchant(Enchantment.EFFICIENCY, 10, true);
						m.setUnbreakable(true);
						m.setDisplayName("§cLeave");
						slimeball.setItemMeta(m);
						slimeball.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						if (!player.getInventory().contains(slimeball)) {
							player.getInventory().setItem(8, slimeball);
						}
						if (main.getRankedSpleefManager().getGames() != null) {
							int playerCount = 0;
							for (SpleefGame game : main.getRankedSpleefManager().getGames()) {
								for (SpleefPlayer sp : game.getPlayers()) {
									playerCount++;
								}
							}
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the PowerSpleef ranked queue!\nThere are currently "
									+ ChatColor.YELLOW + playerCount + ChatColor.GREEN + " players playing and "
									+ ChatColor.YELLOW + main.getRankedSpleefManager().getTotalPlayersInQueue()
									+ " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.POWERSPLEEF);
						} else {
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the PowerSpleef ranked queue!\nThere are currently "
									+ main.getRankedSpleefManager().getTotalPlayersInQueue() + " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.POWERSPLEEF);
						}
					} else if (args[0].equals("spleef") && args[1].equals("airspleef")) {
						player.getInventory().clear();
						ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
						ItemMeta m = slimeball.getItemMeta();
						m.addEnchant(Enchantment.EFFICIENCY, 10, true);
						m.setUnbreakable(true);
						m.setDisplayName("§cLeave");
						slimeball.setItemMeta(m);
						slimeball.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						if (!player.getInventory().contains(slimeball)) {
							player.getInventory().setItem(8, slimeball);
						}
						if (main.getRankedSpleefManager().getGames() != null) {
							int playerCount = 0;
							for (SpleefGame game : main.getRankedSpleefManager().getGames()) {
								for (SpleefPlayer sp : game.getPlayers()) {
									playerCount++;
								}
							}
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the AirSpleef ranked queue!\nThere are currently "
									+ ChatColor.YELLOW + playerCount + ChatColor.GREEN + " players playing and "
									+ ChatColor.YELLOW + main.getRankedSpleefManager().getTotalPlayersInQueue()
									+ " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.AIRSPLEEF);
						} else {
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the AirSpleef ranked queue!\nThere are currently "
									+ main.getRankedSpleefManager().getTotalPlayersInQueue() + " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.AIRSPLEEF);
						}
					} else if (args[0].equals("spleef") && args[1].equals("minispleef")) {
						player.getInventory().clear();
						ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
						ItemMeta m = slimeball.getItemMeta();
						m.addEnchant(Enchantment.EFFICIENCY, 10, true);
						m.setUnbreakable(true);
						m.setDisplayName("§cLeave");
						slimeball.setItemMeta(m);
						slimeball.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						if (!player.getInventory().contains(slimeball)) {
							player.getInventory().setItem(8, slimeball);
						}
						if (main.getRankedSpleefManager().getGames() != null) {
							int playerCount = 0;
							for (SpleefGame game : main.getRankedSpleefManager().getGames()) {
								for (SpleefPlayer sp : game.getPlayers()) {
									playerCount++;
								}
							}
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the MiniSpleef ranked queue!\nThere are currently "
									+ ChatColor.YELLOW + playerCount + ChatColor.GREEN + " players playing and "
									+ ChatColor.YELLOW + main.getRankedSpleefManager().getTotalPlayersInQueue()
									+ " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.MINISPLEEF);
						} else {
							player.sendMessage(ChatColor.GREEN
									+ "You have joined the MiniSpleef ranked queue!\nThere are currently "
									+ main.getRankedSpleefManager().getTotalPlayersInQueue() + " players waiting.");
							main.getRankedSpleefManager().addPlayerToQueue(player.getUniqueId(),
									SpleefMode.MINISPLEEF);
						}
					}
				}

			}
			return true;
		}
	}
}
