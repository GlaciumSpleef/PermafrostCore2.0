package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;

public class StaffChatCommand implements CommandExecutor {
	private final Main main;

	public StaffChatCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		Player player = (Player) sender;

		if (!player.hasPermission("permafrostcore.staffchat")) {
			player.sendMessage("§cYou do not have permission to use this command.");
			return true;
		}

		if (args.length < 1) {
			player.sendMessage("§cUsage: /sc <message>");
			return true;
		}

		String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
		
		List<Player> staff = new ArrayList<>();
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.hasPermission("permafrostcore.staffchat")) {
				staff.add(p);
			}
		}
		
		for(Player p : staff) {
			p.sendMessage("§6[Staff] §6" + player.getName() + "§6 > §6" + message);
		}
		
		return true;
	}
}