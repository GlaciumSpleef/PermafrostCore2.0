package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class PowersListCommand implements CommandExecutor {
	private Main main;

	public PowersListCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("§cThis command can only be run by a player.");
			return true;
		}

		Player player = (Player) sender;
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);

		if (sp == null) {
			player.sendMessage("§cCould not find your Spleef data.");
			return true;
		}

		SpleefGame game = null;

		if (sp.getInGame() || sp.getInTeamGame()) {
			game = sp.getGame();
		}

		if (game == null) {
			SpleefSpectator spectator = sp.getSpectator();

			if (spectator != null && (spectator.isInGame() || spectator.isInTeamGame())) {
				game = spectator.getGame();
			}
		}

		SpleefFFAGame freeForAllGame = null;

		if ((main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId()) || main.getSpleefQueueManager().getInFFA(player.getUniqueId()))
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF)) {
			freeForAllGame = main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF);
		}

		if (game == null && freeForAllGame == null) {
			player.sendMessage("§cThis command can only be run while playing or spectating a game.");
			return true;
		}

		if (game != null)
			sendPowersList(player, game);

		if (freeForAllGame != null)
			sendPowersList(player, freeForAllGame);

		return true;
	}

	private void sendPowersList(Player viewer, SpleefGame game) {
		viewer.sendMessage("§b§lPowers List");

		List<SpleefPlayer> players = getPlayersInGame(game);

		if (players.isEmpty()) {
			viewer.sendMessage("§cNo players were found in this game.");
			return;
		}

		for (SpleefPlayer sp : players) {
			Player target = sp.getPlayer();
			String name = target != null ? target.getName() : target.getName();

			if (name == null) {
				name = "Unknown";
			}

			SpleefPlayer targetSp = null;

			if (target != null) {
				targetSp = main.getDataLoader().getSpleefPlayer(target);
			}

			if (targetSp == null) {
				viewer.sendMessage("§e" + name + " §8» §cUnknown");
				continue;
			}

			String selected = targetSp.getSelectedPower() != null ? targetSp.getFormattedSelectedPower() : "NONE";

			String deselected = targetSp.getDeselectedPower() != null ? targetSp.getFormattedDeselectedPower() : "NONE";

			viewer.sendMessage("§e" + name + " §8» §a" + selected + " §7/ §c" + deselected);
		}
		if(game.isBanning()) {
			viewer.sendMessage("§9Team 1's Ban:" + " §8» §a" + game.getBan1());
			viewer.sendMessage("§cTeam 2's Ban:" + " §8» §a" + game.getBan2());
		}
	}
	
	private void sendPowersList(Player viewer, SpleefFFAGame game) {
		viewer.sendMessage("§b§lPowers List");

		Set<UUID> players = game.getPlayersAlive();
		
		for(UUID uuid : game.getSpectators()) {
			players.add(uuid);
		}

		if (players.isEmpty()) {
			viewer.sendMessage("§cNo players were found in this game.");
			return;
		}

		for (UUID uuid : players) {
			Player target = Bukkit.getPlayer(uuid);
			String name = target != null ? target.getName() : target.getName();

			if (name == null) {
				name = "Unknown";
			}

			SpleefPlayer targetSp = null;

			if (target != null) {
				targetSp = main.getDataLoader().getSpleefPlayer(target);
			}

			if (targetSp == null) {
				viewer.sendMessage("§e" + name + " §8» §cUnknown");
				continue;
			}

			String selected = targetSp.getSelectedPower() != null ? targetSp.getFormattedSelectedPower() : "NONE";

			String deselected = targetSp.getDeselectedPower() != null ? targetSp.getFormattedDeselectedPower() : "NONE";

			viewer.sendMessage("§e" + name + " §8» §a" + selected + " §7/ §c" + deselected);
		}
	}

	private List<SpleefPlayer> getPlayersInGame(SpleefGame game) {
		List<SpleefPlayer> players = new ArrayList<>();

		if (game.getPlayers() != null) {
			players.addAll(game.getPlayers());
		}

		return players;
	}
}