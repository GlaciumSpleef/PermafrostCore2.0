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


public class DonationsHoloCommand implements CommandExecutor{
	private Main main;

    public DonationsHoloCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("donationsholo")) {
        	
        	Inventory gui = Bukkit.createInventory(null, 27, ChatColor.AQUA + "" + ChatColor.BOLD + "Buy Ranks or Join Discord!");
        	
        	ItemStack discord = new ItemStack(Material.BOOK);
        	
        	ItemMeta dMeta = discord.getItemMeta();

        	if (dMeta != null) {
        	    dMeta.setDisplayName("§b§lDiscord");
        	    List<String> lore = new ArrayList<>();
        	    lore.add("§7Join our community");
        	    lore.add("§7and stay updated!");
        	    lore.add("");
        	    lore.add("§eClick to get the link");
        	    dMeta.setLore(lore);
        	    // glow effect
        	    dMeta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        	    dMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        	    discord.setItemMeta(dMeta);
        	}
        	
        	ItemStack shop = new ItemStack(Material.SUNFLOWER);
        	ItemMeta sMeta = shop.getItemMeta();

        	if (sMeta != null) {
        	    sMeta.setDisplayName("§6§lShop");

        	    List<String> lore = new ArrayList<>();
        	    lore.add("§7Support the server");
        	    lore.add("§7and unlock perks");
        	    lore.add("");
        	    lore.add("§8Includes future cosmetics");
        	    lore.add("");
        	    lore.add("§eClick to open");

        	    sMeta.setLore(lore);

        	    // glow effect
        	    sMeta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        	    sMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        	    shop.setItemMeta(sMeta);
        	}
        	
        	gui.setItem(11, shop);
        	gui.setItem(15, discord);
        	player.openInventory(gui);
            return true;
        }
        
        return true;
    }
}