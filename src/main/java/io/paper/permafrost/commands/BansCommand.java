package io.paper.permafrost.commands;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.gui.ShovelPowersManager;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.important.CoreSettings;

public class BansCommand implements CommandExecutor {
	private Main main;

	public BansCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		Player player = (Player) sender;
		SpleefPlayer sp = main.getDataLoader().getPlayer(player.getUniqueId());
		if (sp.getInGame() || sp.getInTeamGame() || (main.getSpleefQueueManager().isAliveInAnyFFA(player.getUniqueId())
				&& main.getSpleefQueueManager().getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
			sender.sendMessage("§cYou cannot select bans in game.");
			return true;
		}
		if (sp.getGame() != null) {
			if (sp.getGame().isBanning()) {
				if (sp.getGame().isInPowerPhase() || !sp.getGame().isInBanningPhase()) {
					sender.sendMessage("§cYou cannot select bans during powers selection.");
					return true;
				}
			}
		}
		if (command.getName().equalsIgnoreCase("bans") && args.length > 1) {
			if (args[0].equalsIgnoreCase("8882469198")) {
				ShovelPowers power = ShovelPowers.valueOf(args[1].toUpperCase());
				sp.getGame().setBan(power, player.getUniqueId());
			}
		}
		if (command.getName().equalsIgnoreCase("bans") && args.length == 1) {
			if (args[0].equalsIgnoreCase("8882469198")) {
				ShovelPowersManager menu = new ShovelPowersManager(main);
				menu.openBansMenu(player);
			}
		}
		return true;
	}
}
