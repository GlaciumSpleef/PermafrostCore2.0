package io.paper.permafrost.commands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.core.game.tasks.CountdownTask;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class TeamListCommand implements CommandExecutor {
	private Main main;

	public TeamListCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		if (command.getName().equalsIgnoreCase("teamlist")) {
			Player player = (Player) sender;
			SpleefPlayer sp = main.getDataLoader().getPlayer(player.getUniqueId());
			SpleefGame game = sp.getGame();
			String color = "";
			int i = 0;
			if (!sp.getInTeamGame()) {
				sender.sendMessage("Only players in team games can use this command.");
				return true;
			}
			String msg = "";
			for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
				color = team.getColor();
				msg += game.getChatColor(color) + "§l" + game.capitalize(color) + " §lTeam: §r";
				i = team.getPlayers().size();
				for (UUID uuid : team.getPlayers()) {
					Player p = Bukkit.getPlayer(uuid);
					i--;
					if (i == 0) {
						msg += "§r§f" + p.getName() + "\n";
					} else {
						msg += "§r§f" + p.getName() + ", ";
					}
				}
			}
			sender.sendMessage(msg);
			return true;
		}
		return true;
	}
}
