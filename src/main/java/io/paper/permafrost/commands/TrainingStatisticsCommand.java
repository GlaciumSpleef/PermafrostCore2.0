package io.paper.permafrost.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.training.PlotConfigManager;
import io.paper.permafrost.training.TrainingMenu.TrainingStatisticsMenu;
import io.paper.permafrost.Main;

public class TrainingStatisticsCommand implements CommandExecutor {

	private final Main main;

	public TrainingStatisticsCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Players only.");
			return true;
		}

		TrainingStatisticsMenu.open(player, main);
		return true;
	}
}
