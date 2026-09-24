package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import io.paper.permafrost.core.game.SpleggRankedArena;

public class FavoritesCommand implements CommandExecutor {
	private Main main;

	public FavoritesCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}

		Player player = (Player) sender;

		if (command.getName().equalsIgnoreCase("favorites")) {
			Inventory gui = Bukkit.createInventory(null, 27,
					ChatColor.AQUA + "" + ChatColor.BOLD + "Choose Your Favorite Maps!");
			int i = 0;
			for (SpleggRankedArena arena : main.getDataLoader().getSpleggRankedArenas()) {
				if (!main.getDataLoader().getFavoritedArenas(player.getUniqueId()).contains(arena) || !main.getDataLoader().getAllFavoritedArenas().containsKey(player.getUniqueId())) {
					ItemStack item = new ItemStack(Material.FIREWORK_STAR);
					ItemMeta meta = item.getItemMeta();
					if (meta != null) {
						meta.setDisplayName("§6§l" + arena.getId());
						List<String> lore = new ArrayList<>();
						lore.add("");
						lore.add("§eClick to set as favorite");
						meta.setLore(lore);
						// glow effect
						meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
						meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
						item.setItemMeta(meta);
					}
					gui.setItem(i, item);
				} else {
					ItemStack item = new ItemStack(Material.FIREWORK_STAR);
					ItemMeta meta = item.getItemMeta();
					if (meta != null) {
						meta.setDisplayName("§f✦ §6§l" + arena.getId());
						List<String> lore = new ArrayList<>();
						lore.add("");
						lore.add("§eClick to remove from favorites");
						meta.setLore(lore);
						// glow effect
						meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
						meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
						item.setItemMeta(meta);
					}
					gui.setItem(i, item);
				}
				i++;
			}
			player.openInventory(gui);
			return true;
		}

		return true;
	}
}