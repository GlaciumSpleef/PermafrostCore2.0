package io.paper.permafrost.commands;

import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.training.PlotConfigManager;

public class AdminCommand implements CommandExecutor {

	private final PlotConfigManager plotConfig;
	private static final String PERMISSION = "permafrost.admin";

	public AdminCommand(PlotConfigManager plotConfig, Main main) {
		this.plotConfig = plotConfig;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Players only.");
			return true;
		}

		if (!player.hasPermission(PERMISSION)) {
			player.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if (args.length < 3 || !args[0].equalsIgnoreCase("plot")) {
			sendUsage(player);
			return true;
		}

		String action = args[1].toLowerCase();
		String plotName = args[2];

		if (action.equals("create")) {
			if (args.length < 4) {
				player.sendMessage(ChatColor.RED + "Usage: /admin plot create <name> <schematic>");
				return true;
			}

			String schematic = args[3];
			int maxPlayers = parsePlayersFromSchematic(schematic);

			if (maxPlayers == -1) {
				player.sendMessage(ChatColor.RED + "Invalid schematic naming format.");
				return true;
			}

			plotConfig.createPlot(plotName, schematic, maxPlayers);

			confirm(player, "Plot '" + plotName + "' created.");
			return true;
		}

		if (action.equals("edit")) {
			if (!plotConfig.plotExists(plotName)) {
				player.sendMessage(ChatColor.RED + "Plot does not exist.");
				return true;
			}

			if (args.length < 5) {
				sendEditUsage(player);
				return true;
			}

			String setting = args[3].toLowerCase();

			switch (setting) {
			
			case "blockstobreak" -> {

			    int blocks;

			    try {
			        blocks = Integer.parseInt(args[4]);
			    } catch (NumberFormatException e) {
			        player.sendMessage(ChatColor.RED + "blocksToBreak must be a number.");
			        return true;
			    }

			    if (blocks <= 0) {
			        player.sendMessage(ChatColor.RED + "blocksToBreak must be greater than 0.");
			        return true;
			    }

			    plotConfig.setBlocksToBreak(plotName, blocks);
			    confirm(player, "Blocks to break set to " + blocks + ".");
			}
			
			case "timer" -> {
				boolean value = Boolean.parseBoolean(args[4]);
				plotConfig.setTimer(plotName, value);
				confirm(player, "Timer updated.");
			}

			case "offset" -> {
				boolean value = Boolean.parseBoolean(args[4]);
				plotConfig.setOffset(plotName, value);
				confirm(player, "Offset updated.");
			}

			case "spawns" -> {
			    // /admin plot edit <name> spawns <amount> spawn1 x y z spawn2 x y z ...
			    if (args.length < 5) {
			        player.sendMessage(ChatColor.RED
			                + "Usage: /admin plot edit <name> spawns <amount> spawn1 <x> <y> <z> [spawn2 <x> <y> <z> ...]");
			        return true;
			    }

			    int amount;
			    try {
			        amount = Integer.parseInt(args[4]);
			    } catch (NumberFormatException e) {
			        player.sendMessage(ChatColor.RED + "Amount of spawns must be a number.");
			        return true;
			    }

			    if (amount <= 0) {
			        player.sendMessage(ChatColor.RED + "Amount of spawns must be greater than 0.");
			        return true;
			    }

			    // Expected tokens after amount:
			    // For each spawn: 1 label + 3 numbers = 4 tokens
			    // Total args required = 5 (base) + amount*4
			    int required = 5 + (amount * 4);
			    if (args.length < required) {
			        player.sendMessage(ChatColor.RED + "Not enough arguments for " + amount + " spawns.");
			        player.sendMessage(ChatColor.RED
			                + "Usage: /admin plot edit <name> spawns <amount> spawn1 <x> <y> <z> [spawn2 <x> <y> <z> ...]");
			        return true;
			    }

			    int cursor = 5; // start reading after <amount>

			    for (int i = 1; i <= amount; i++) {
			        String expectedLabel = "spawn" + i;
			        label = args[cursor].toLowerCase();

			        if (!label.equals(expectedLabel)) {
			            player.sendMessage(ChatColor.RED + "Expected '" + expectedLabel + "' but got '" + args[cursor] + "'.");
			            return true;
			        }

			        double x, y, z;
			        try {
			            x = parseDoubleLoose(args[cursor + 1]);
			            y = parseDoubleLoose(args[cursor + 2]);
			            z = parseDoubleLoose(args[cursor + 3]);
			        } catch (NumberFormatException e) {
			            player.sendMessage(ChatColor.RED + "Invalid number format for " + expectedLabel + " offsets.");
			            player.sendMessage(ChatColor.GRAY + "Example: spawn" + i + " 0.5 0 0.5");
			            return true;
			        }

			        // If your PlotConfigManager expects "index", decide whether it is 0-based or 1-based.
			        // Your previous command took <index> directly; I’ll assume it was 1-based for admins.
			        plotConfig.setSpawn(plotName, i, x, y, z);

			        cursor += 4; // move to next spawn chunk
			    }

			    confirm(player, amount + " spawn(s) updated.");
			}

			default -> sendEditUsage(player);
			}

			return true;
		}

		sendUsage(player);
		return true;
	}
	
	private double parseDoubleLoose(String raw) throws NumberFormatException {
	    String cleaned = raw.trim();

	    // Allow "0.5," style inputs
	    if (cleaned.endsWith(",")) {
	        cleaned = cleaned.substring(0, cleaned.length() - 1);
	    }

	    return Double.parseDouble(cleaned);
	}

	private int parsePlayersFromSchematic(String schematic) {
		String s = schematic.toLowerCase();
		if (s.contains("solo"))
			return 1;
		if (s.contains("duel"))
			return 2;
		if (s.contains("multi"))
			return 4;
		return -1;
	}

	private void confirm(Player player, String message) {
		player.sendMessage(ChatColor.GREEN + message);
		player.sendMessage(ChatColor.GRAY + "Changes saved. Restart server to apply.");
	}

	private void sendUsage(Player player) {
		player.sendMessage(ChatColor.RED + "/admin plot create <name> <schematic>");
		player.sendMessage(ChatColor.RED + "/admin plot edit <name> <timer|offset|spawns>");
	}

	private void sendEditUsage(Player player) {
	    player.sendMessage(ChatColor.RED + "/admin plot edit <name> timer <true|false>");
	    player.sendMessage(ChatColor.RED + "/admin plot edit <name> offset <true|false>");
	    player.sendMessage(ChatColor.RED + "/admin plot edit <name> blockstobreak <number>");
	    player.sendMessage(ChatColor.RED
	            + "/admin plot edit <name> spawns <amount> spawn1 <x> <y> <z> [spawn2 <x> <y> <z> ...]");
	}
}
