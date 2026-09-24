package io.paper.permafrost.training;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class InviteCommand implements CommandExecutor {
	private final TrainingService training;


	public InviteCommand(TrainingService training) {
		this.training = training;
	}

	 @Override
	    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	        if (!(sender instanceof Player leader)) return true;

	        if (args.length != 1) {
	            leader.sendMessage(ChatColor.RED + "Usage: /invite <player>");
	            return true;
	        }

	        Player target = Bukkit.getPlayerExact(args[0]);
	        if (target == null) {
	            leader.sendMessage(ChatColor.RED + "That player is not online.");
	            return true;
	        }
	        if (target.getUniqueId().equals(leader.getUniqueId())) {
	            leader.sendMessage(ChatColor.RED + "You can’t invite yourself.");
	            return true;
	        }

	        // Ensure leader has a session (create if not)
	        TrainingSession session = training.getSession(leader.getUniqueId());
	        if(session == null) leader.sendMessage(ChatColor.RED + "You are not in a valid session.");

	        // Must be leader to invite (if your design requires it)
	        if (!training.isLeader(session, leader.getUniqueId())) {
	            leader.sendMessage(ChatColor.RED + "Only the session leader can invite players.");
	            return true;
	        }

	        // Target cannot already be in a session
	        if (training.getSession(target.getUniqueId()) != null) {
	            leader.sendMessage(ChatColor.RED + target.getName() + " is already in a training session.");
	            return true;
	        }

	        // Already invited?
	        if (training.isInvited(session, target.getUniqueId())) {
	            leader.sendMessage(ChatColor.YELLOW + target.getName() + " is already invited.");
	            return true;
	        }

	        training.invite(session, target.getUniqueId());

	        leader.sendMessage(ChatColor.GREEN + "Invited " + ChatColor.AQUA + target.getName()
	                + ChatColor.GREEN + " to your training session.");

	        target.sendMessage(ChatColor.AQUA + leader.getName() + ChatColor.GRAY
	                + " invited you to a training session.");
	        target.sendMessage(ChatColor.GRAY + "Type " + ChatColor.YELLOW + "/join " + leader.getName()
	                + ChatColor.GRAY + " to join, or ignore to decline.");

	        return true;
	    }
}
