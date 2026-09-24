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

public class ForceEndGameCommand implements CommandExecutor {
	private final Main main;

	public ForceEndGameCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("fendgame")) {
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

		if (args.length != 1) {
			player.sendMessage("§cUsage: /fendgame <player>");
			return true;
		}

		Player target = Bukkit.getPlayerExact(args[0]);
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

		for (SpleefPlayer spp : game.getPlayers()) {
			spp.setAcceptedEndgame(false);
		}

		int count = -1;
		TeamSpleefTeam winner = null;

		for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
			if (team.getPoints() > count) {
				count = team.getPoints();
				winner = team;
			}
		}

		if (winner != null) {
			winner.setWinner(true);
		}

		for (SpleefPlayer spp : game.getPlayers()) {
			Player gamePlayer = spp.getPlayer();
			if (gamePlayer != null) {
				gamePlayer.sendMessage("§4§lGame Force Ended");
				gamePlayer.sendMessage("§7An administrator has ended this match.");
			}
		}

		player.sendMessage("§aForce ended the game for §f" + target.getName() + "§a.");

		if (targetSp.getInTeamGame()) {
			game.setEndedByCommand(true);
			game.endTeamGame();
		} else if (targetSp.getInGame()) {
			game.endGame();
		} else {
			game.endGame();
		}

		return true;
	}
}
