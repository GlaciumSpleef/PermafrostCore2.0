package io.paper.permafrost.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;


public class ShopCommand implements CommandExecutor {
    private Main main;

    public ShopCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("shop")) {
        	openShop(player);
            return true;
        }
        
        return true;
    }
    
    private void openShop(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.AQUA + "" + ChatColor.BOLD + "View Buyable Ranks!");

        gui.setItem(11, createItem(
                Material.ICE,
                "&f[" + gradient("Glacier", "#B8F4FF", "#1E8FC2") + "&f]",
                "&7Support the server",
                "&7Unlock prefix + future cosmetics",
                "&f$ &64.99 &fUSD",
                "&8These cosmetics are subject",
                "&8to change upon release"
        ));
        
        gui.setItem(13, createItem(
                Material.SNOWBALL,
                "&f[" + gradient("Polar", "#72FAF4", "#03C69B") + "&f]",
                "&7Support the server",
                "&7Unlock prefix + future cosmetics",
                "&f$ &619.99 &fUSD",
                "&8These cosmetics are subject",
                "&8to change upon release"
        ));

        gui.setItem(15, createItem(
                Material.GOLD_INGOT,
                "&f[" + gradient("Custom", "#FFD158", "#C69000") + "&f]",
                "&7Top supporter rank",
                "&7All cosmetics included",
                "&f$ &649.99 &fUSD",
                "&8Includes a customizable name"
        ));

        player.openInventory(gui);
    }
    
    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(color(line));
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    public static String gradient(String text, String startHex, String endHex) {
        Color start = Color.decode(startHex);
        Color end = Color.decode(endHex);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            double ratio = (double) i / (text.length() - 1);

            int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            result.append(toHex(r, g, b)).append(text.charAt(i));
        }

        return result.toString();
    }

    private static String toHex(int r, int g, int b) {
        return String.format("§x§%1$X§%2$X§%3$X§%4$X§%5$X§%6$X",
                (r >> 4) & 0xF, r & 0xF,
                (g >> 4) & 0xF, g & 0xF,
                (b >> 4) & 0xF, b & 0xF);
    }
    
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
