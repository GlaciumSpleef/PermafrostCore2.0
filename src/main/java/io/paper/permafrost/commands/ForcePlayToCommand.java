package io.paper.permafrost.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;

public class ForcePlayToCommand implements CommandExecutor {
	private final Main main;

	public ForcePlayToCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("fplayto")) {
			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		Player player = (Player) sender;

		if (!player.hasPermission("permafrostcore.fendgame")) {
			player.sendMessage("§cYou do not have permission to use this command.");
			return true;
		}

		if (args.length != 2) {
			player.sendMessage("§cUsage: /fplayto <player> <score>");
			return true;
		}

		Player target = Bukkit.getPlayerExact(args[0]);
		int score = Integer.parseInt(args[1]);
		if (target == null) {
			player.sendMessage("§cThat player is not online.");
			return true;
		}

		SpleefPlayer targetSp = main.getDataLoader().getPlayer(target.getUniqueId());
		if (targetSp == null) {
			player.sendMessage("§cThat player is not a registered Spleef player.");
			return true;
		}

		SpleefGame game = targetSp.getGame();
		if (game == null) {
			player.sendMessage("§cThat player is not currently in a game.");
			return true;
		}
		
		game.setPlaytoScore(score);
		
		for (SpleefPlayer spp : game.getPlayers()) {
			Player gamePlayer = spp.getPlayer();
			if (gamePlayer != null) {
				gamePlayer.sendMessage("§eGame score set to " + score + " by an Admin.");
			}
		}
		return true;
	}
}
