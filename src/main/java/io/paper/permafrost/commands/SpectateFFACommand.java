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
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import net.md_5.bungee.api.ChatColor;

public class SpectateFFACommand implements CommandExecutor {
	private Main main;

	public SpectateFFACommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else {
			Player player = (Player) sender;
			if (command.getName().equalsIgnoreCase("spectateffa")) {
				if (main.getSpleggQueueManager().isInGame()) {
					main.getSpleggQueueManager().addSpectator(player.getUniqueId());
					player.teleport(main.getSpleggQueueManager().getArena().getPasteOrigin());
					player.getInventory().clear();
					player.sendMessage("§7You are spectating an FFA match! Leave by typing the command /spawn");
					player.setGameMode(GameMode.SPECTATOR);
					return true;
				} else {
					player.sendMessage("§cThere are no current ongoing FFA matches.");
					return true;
				}
			} 
			return true;
		}
	}
}