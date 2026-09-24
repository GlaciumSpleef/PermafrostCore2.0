package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.gui.TeamSpleefMenu;

public class PingCommand implements CommandExecutor {
	private Main main;

	public PingCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("§cThis command can only be run by a player.");
			return true;
		}
		Player player = (Player) sender;
		boolean all = args.length == 1 && args[0].equalsIgnoreCase("all");
		if (!all) {
			int ping = player.getPing();
			player.sendMessage("§aYour ping: §f" + ping + "ms");
			return true;
		}
		player.sendMessage("§e§lPlayer Pings:");
		Map<Player, Integer> pings = new HashMap<>();

		for (Player online : Bukkit.getOnlinePlayers()) {
			pings.put(online, online.getPing());
		}

		List<Map.Entry<Player, Integer>> sorted = new ArrayList<>(pings.entrySet());

		sorted.sort(Map.Entry.comparingByValue());
		String color;
		for (Map.Entry<Player, Integer> entry : sorted) {
			Player p = entry.getKey();
			int ping = entry.getValue();

			if (ping < 50)
				color = "§a";
			else if (ping < 100)
				color = "§e";
			else
				color = "§c";

			player.sendMessage("§7- §f" + p.getName() + " §7» " + color + ping + "ms");
		}
		return true;
	}

}