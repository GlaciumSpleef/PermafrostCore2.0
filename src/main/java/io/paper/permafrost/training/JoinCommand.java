package io.paper.permafrost.training;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class JoinCommand implements CommandExecutor {
	private final TrainingService training;


	public JoinCommand(TrainingService training) {
		this.training = training;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /join <player>");
            return true;
        }

        // Already in a session
        if (training.getSession(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a training session. Type /leave first.");
            return true;
        }

        Player leader = Bukkit.getPlayerExact(args[0]);
        if (leader == null) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return true;
        }

        TrainingSession session = training.getSession(leader.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.RED + leader.getName() + " does not have an active training session.");
            return true;
        }

        // Must be invited
        if (!training.isInvited(session, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not invited to that training session.");
            return true;
        }

        training.join(session, player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Joined " + ChatColor.AQUA + leader.getName()
                + ChatColor.GREEN + "’s training session.");

        Player leaderOnline = Bukkit.getPlayer(session.getLeader());
        if (leaderOnline != null) {
            leaderOnline.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.GREEN + " joined your session.");
        }

        return true;
    }
}
