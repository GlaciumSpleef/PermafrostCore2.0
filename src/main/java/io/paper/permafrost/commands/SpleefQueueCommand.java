package io.paper.permafrost.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.SpleefMode;
import net.md_5.bungee.api.ChatColor;

public class SpleefQueueCommand implements CommandExecutor {
	private Main main;

	public SpleefQueueCommand(Main main) {
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
					player.sendMessage("§cThis command cannot be run in game.");
					return true;
				}
			}

			if (main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}

			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF)
					.contains(player.getUniqueId())) {
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

			if (command.getName().equalsIgnoreCase("spleefqueue")) {
				if (!main.getQueuesOpen()) {
					player.sendMessage("§cQueues are closed!");
					return true;
				}
				if(main.getSpleefQueueManager().isInAnyFFA(player.getUniqueId())) {
					player.sendMessage("§cYou are already in FFA queue!");
					return true;
				}
				if (args.length == 0) {
					Inventory gui = Bukkit.createInventory(null, 27,
							ChatColor.AQUA + "" + ChatColor.BOLD + "Join FFA Queue!");

					ItemStack windcharge = new ItemStack(Material.WIND_CHARGE);
					ItemMeta windMeta = windcharge.getItemMeta();
					if (windMeta != null) {
						windMeta.setDisplayName("§b§lPowerSpleef FFA");
						List<String> lore = new ArrayList<>();
						lore.add("§7Play PowerSpleef & Farm Shards!");
						lore.add("");
						lore.add("§fIn Queue: §e" + main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF)
								.getTotalPlayerCount());
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
						snowMeta.setDisplayName("§e§lAirSpleef FFA");
						List<String> lore = new ArrayList<>();
						lore.add("§7Classic FFA Spleef");
						lore.add("");
						lore.add("§fIn Queue: §e"
								+ main.getSpleefQueueManager().getGame(SpleefFFAMode.AIRSPLEEF).getTotalPlayerCount());
						lore.add("§eClick to join the queue");
						snowMeta.setLore(lore);
						// glow effect
						snowMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
						snowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
						snow.setItemMeta(snowMeta);
					}
					gui.setItem(13, snow);

					ItemStack barrier = new ItemStack(Material.BARRIER);
					ItemMeta barrierMeta = barrier.getItemMeta();
					if (barrierMeta != null) {
						barrierMeta.setDisplayName("§6§lPrototype FFA");
						List<String> lore = new ArrayList<>();
						lore.add("§7Different Points System = Double the Fun!");
						lore.add("§cIT IS HIGHLY RECOMMENDED TO HAVE PARTICLES ON");
						lore.add("");
						lore.add("§fIn Queue: §e"
								+ main.getSpleefQueueManager().getGame(SpleefFFAMode.PROTOTYPE).getTotalPlayerCount());
						lore.add("§eClick to join the queue");
						barrierMeta.setLore(lore);
						// glow effect
						barrierMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
						barrierMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
						barrier.setItemMeta(barrierMeta);
					}
					gui.setItem(15, barrier);
					player.openInventory(gui);
					return true;
				} else if (args.length == 1) {
					switch (args[0]) {
					case "powerspleef": {
						String[] powers = getPlayerPowers(player.getUniqueId());

						if (powers[0] == null || powers[1] == null) {
							player.sendMessage("§cYou need to select two powers before joining PowerSpleef FFA.");
							player.sendMessage("§7Use §e/powers §7to choose your powers.");
							return true;
						}
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
						SpleefQueueManager spleefManager = main.getSpleefQueueManager();

						spleefManager.addPlayer(player.getUniqueId(), SpleefFFAMode.POWERSPLEEF);

						int waiting = spleefManager.getGame(SpleefFFAMode.POWERSPLEEF).getPlayersInQueueCount();
						int playing = spleefManager.getGame(SpleefFFAMode.POWERSPLEEF).getPlayersAliveCount()
								+ spleefManager.getGame(SpleefFFAMode.POWERSPLEEF).getPlayersSpectatingCount();

						player.sendMessage(ChatColor.GREEN + "You have joined the PowerSpleef FFA queue!\nThere are currently "
								+ ChatColor.YELLOW + playing + ChatColor.GREEN
								+ " players playing and " + ChatColor.YELLOW + waiting
								+ ChatColor.GREEN + " players waiting.");

						Bukkit.dispatchCommand(
								Bukkit.getConsoleSender(),
								"hologram edit &6Spleef setline 4 &fPlayers: &e" + spleefManager.getTotalPlayersInQueue()
						);
						break;
					}
					case "airspleef": {
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
						SpleefQueueManager spleefManager = main.getSpleefQueueManager();

						spleefManager.addPlayer(player.getUniqueId(), SpleefFFAMode.AIRSPLEEF);

						int waiting = spleefManager.getGame(SpleefFFAMode.AIRSPLEEF).getPlayersInQueueCount();
						int playing = spleefManager.getGame(SpleefFFAMode.AIRSPLEEF).getPlayersAliveCount()
								+ spleefManager.getGame(SpleefFFAMode.AIRSPLEEF).getPlayersSpectatingCount();

						player.sendMessage(ChatColor.GREEN + "You have joined the AirSpleef FFA queue!\nThere are currently "
								+ ChatColor.YELLOW + playing + ChatColor.GREEN
								+ " players playing and " + ChatColor.YELLOW + waiting
								+ ChatColor.GREEN + " players waiting.");

						Bukkit.dispatchCommand(
								Bukkit.getConsoleSender(),
								"hologram edit &6Spleef setline 4 &fPlayers: &e" + spleefManager.getTotalPlayersInQueue()
						);
						break;
					}
					case "prototype": {
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
						SpleefQueueManager spleefManager = main.getSpleefQueueManager();

						spleefManager.addPlayer(player.getUniqueId(), SpleefFFAMode.PROTOTYPE);

						int waiting = spleefManager.getGame(SpleefFFAMode.PROTOTYPE).getPlayersInQueueCount();
						int playing = spleefManager.getGame(SpleefFFAMode.PROTOTYPE).getPlayersAliveCount()
								+ spleefManager.getGame(SpleefFFAMode.PROTOTYPE).getPlayersSpectatingCount();

						player.sendMessage(ChatColor.GREEN + "You have joined the Prototype FFA queue!\nThere are currently "
								+ ChatColor.YELLOW + playing + ChatColor.GREEN
								+ " players playing and " + ChatColor.YELLOW + waiting
								+ ChatColor.GREEN + " players waiting.");

						Bukkit.dispatchCommand(
								Bukkit.getConsoleSender(),
								"hologram edit &6Spleef setline 4 &fPlayers: &e" + spleefManager.getTotalPlayersInQueue()
						);
						break;
					}
					}
				}
			}
			return true;
		}
	}
	
	public String[] getPlayerPowers(UUID uuid) {
		String[] powers = new String[2];
		String sql = "SELECT power1, power2 FROM player_powers WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					powers[0] = rs.getString("power1");
					powers[1] = rs.getString("power2");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return powers;
	}
}
