package io.paper.permafrost.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleggFFAArena;
import io.paper.permafrost.utilities.SpleefMode;

public class VoteCommand implements CommandExecutor {
    private Main main;

    public VoteCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!command.getName().equalsIgnoreCase("vote")) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /vote <arena>");
            return true;
        }
        
        if(main.getDataLoader().getSpleefPlayer(player) != null) {
			if(main.getDataLoader().getSpleefPlayer(player).getInGame() || main.getDataLoader().getSpleefPlayer(player).getInTeamGame() || main.getSpleggQueueManager().getPlayersAliveInGame().containsKey(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in game.");
				return true;
			}
		}
		
		if(main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}
		
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}

        String arenaName = args[0];
        SpleggFFAArena selected = null;

        for (SpleggFFAArena arena : main.getDataLoader().getSpleggFFAArenas()) {
            if (arena.getId().equalsIgnoreCase(arenaName)) {
                selected = arena;
                break;
            }
        }

        if (selected == null) {
            player.sendMessage(ChatColor.RED + "That arena is not in the current vote.");
            return true;
        }

        main.getSpleggQueueManager().handleVote(player.getUniqueId(), selected.getId());
        player.sendMessage(ChatColor.GREEN + "You voted for arena " + selected.getId() + ".");

        return true;
    }
}
