package io.paper.permafrost.training;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class LeaveCommand implements CommandExecutor {
	private final TrainingService training;
	private Main main;
	
	public LeaveCommand(Main main, TrainingService training) {
		this.training = training;
		this.main = main;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        TrainingSession session = training.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not in a training session.");
            return true;
        }

        boolean wasLeader = training.isLeader(session, player.getUniqueId());
        UUID leaderId = session.getLeader();

        training.leave(player.getUniqueId());

        player.sendMessage(ChatColor.YELLOW + "You left the training session.");

        // Notify leader if online and player wasn't the leader
        if (!wasLeader) {
            Player leaderOnline = Bukkit.getPlayer(leaderId);
            if (leaderOnline != null) {
                leaderOnline.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " left your session.");
            }
        } else {
            // If leader left: notify remaining members (if your service ends session, members may already be removed)
        	for (UUID uuid : new HashSet<>(session.getPlayers())) {
        	    if (uuid.equals(player.getUniqueId())) continue;

        	    Player member = Bukkit.getPlayer(uuid);
        	    if (member != null) {
        	        member.sendMessage(ChatColor.RED + "✖ " + ChatColor.WHITE
        	                + "The session leader left. The training session has ended.");
        	        member.performCommand("spawn");
        	    }

        	    main.getDataLoader().removePlayerFromTraining(uuid);
        	}
        }

        return true;
    }
}
