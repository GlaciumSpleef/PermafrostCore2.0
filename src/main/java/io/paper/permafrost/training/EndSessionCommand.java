package io.paper.permafrost.training;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class EndSessionCommand implements CommandExecutor {
	private Main main;

	public EndSessionCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		} else {
			Player player = (Player) sender;
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
			if (sp.getInGame() || sp.getInTeamGame()) {
				if (!main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator())) {
					sender.sendMessage("This command cannot be run in game.");
					return true;
				}
			}
			if (command.getName().equalsIgnoreCase("endsession")) {
				if (main.getDataLoader().getTrainingSession(player.getUniqueId()) != null) {
					if (main.getDataLoader().getTrainingSession(player.getUniqueId()).getLeader() == player
							.getUniqueId()) {
						main.getDataLoader().setBlocksToBreak(player.getUniqueId(), 0);
						main.getDataLoader().getTrainingSession(player.getUniqueId()).stopTimer(true);
						main.getDataLoader().getTrainingSession(player.getUniqueId()).getArena().setInGame(false);
						main.getDataLoader().getTrainingSession(player.getUniqueId()).endSession();
					} else {
						sender.sendMessage("You are not the leader of this session. Ask " + Bukkit
								.getPlayer(main.getDataLoader().getTrainingSession(player.getUniqueId()).getLeader())
								.getName() + " to end the session.");
					}

				} else {
					sender.sendMessage("This command can only be run in a training session.");
				}
			}
		}
		return true;
	}
}
