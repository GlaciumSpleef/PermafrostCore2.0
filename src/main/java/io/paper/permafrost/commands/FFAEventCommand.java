package io.paper.permafrost.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.SpleefMode;

public class FFAEventCommand implements CommandExecutor {

	private final Main main;

	public FFAEventCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!command.getName().equalsIgnoreCase("ffaevent")) {
			return true;
		}

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (!player.hasPermission("permafrostcore.ffaevent")) {
			player.sendMessage("§cYou do not have permission to use this command.");
			return true;
		}

		if (args.length < 3) {
			player.sendMessage( // this is for banning
					"§cUsage: /ffaevent <powerspleef|airspleef|prototype> <points> <true|false>");
			return true;
		}
		
		String modeArg = args[0].toLowerCase();
		int points;
		try {
			points = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			player.sendMessage("§cPoints must be a valid integer.");
			return true;
		}
		SpleefFFAMode mode = null;
		boolean showPoints = args[2].equals("true");
		
		switch (modeArg) {
		case "powerspleef":
			mode = SpleefFFAMode.POWERSPLEEF;
			break;
		case "airspleef":
			mode = SpleefFFAMode.AIRSPLEEF;
			break;
		case "prototype":
			mode = SpleefFFAMode.PROTOTYPE;
			break;
		default:
			player.sendMessage("§cInvalid gamemode.");
			player.sendMessage(
					"§cUsage: /ffaevent <powerspleef|airspleef|prototype> <points> <true|false>");
			return true;
		}
		SpleefFFAGame game = main.getSpleefQueueManager().getGame(mode);
		game.setInEvent(true);
		game.setPlayToEventPoints(points);
		game.setShowPoints(showPoints);
		Bukkit.broadcastMessage("§aA §e" + modeArg + " FFA Event §ahas started! This event is playing to §e" + points + "§a!");
		
		return true;
	}
}
