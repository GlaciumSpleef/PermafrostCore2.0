package io.paper.permafrost.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import io.paper.permafrost.Main;

@SuppressWarnings("unused")
public class SpleefCommand implements CommandExecutor {
	private Main main;
	public boolean spleefCommandRunning;
	public SpleefCommand(Main main) {
		this.main = main;
	}
	public boolean getSpleefCommandRunning() {
		return spleefCommandRunning;
	}
	public void setSpleefCommandRunning(boolean run) {
		spleefCommandRunning = run;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else {
			Player player = (Player) sender;
			if (command.getName().equalsIgnoreCase("spleef")) {
				Location teleportLocation = new Location(Bukkit.getWorld("ffa"), 0, 65, 0);
				teleportLocation.setYaw(270f);
				player.getPlayer().teleport(teleportLocation);
				ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
				ItemStack wind = new ItemStack(Material.WIND_CHARGE, 3);
				ItemMeta meta = shovel.getItemMeta();
				PlayerInventory inventory = player.getPlayer().getInventory();
				meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
				meta.setUnbreakable(true);
				shovel.setItemMeta(meta);
				shovel.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				if(inventory.contains(Material.WIND_CHARGE)){
					inventory.clear();
					inventory.setItem(40, wind);
				} else {
					inventory.setItem(40, wind);
				}
				if(!inventory.contains(shovel)) {
					inventory.addItem(shovel);
					}
				if(!player.getGameMode().equals(GameMode.SURVIVAL)) {
					player.setGameMode(GameMode.SURVIVAL);
				}
				setSpleefCommandRunning(true);
				return true;
			}
		}
		setSpleefCommandRunning(false);
		return false;
	}
	
}
