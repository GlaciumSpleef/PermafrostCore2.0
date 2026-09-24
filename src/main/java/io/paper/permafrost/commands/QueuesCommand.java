package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.utilities.SpleefMode;

public class QueuesCommand implements CommandExecutor {

	private final Main main;

	public QueuesCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!command.getName().equalsIgnoreCase("queues")) {
			return true;
		}

		if (!(sender instanceof Player sender2)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (!sender2.hasPermission("permafrostcore.queues")) {
			sender2.sendMessage("§cYou do not have permission to use this command.");
			return true;
		}
		
		if(args.length < 1) {
			sender2.sendMessage("§cUsage: /queues <open|close>");
			return true;
		}
		
		boolean close = args[0].equals("close");
		boolean open = args[0].equals("open");
		
		if(!close && !open) {
			sender2.sendMessage("§cUsage: /queues <open|close>");
			return true;
		} else if(close) {
			main.setQueuesOpen(false);
			Bukkit.broadcastMessage("§cQueues are now closed!");
			return true;
		} else if (open) {
			main.setQueuesOpen(true);
			Bukkit.broadcastMessage("§aQueues are now opened!");
			return true;
		}
		return true;
	}
}
