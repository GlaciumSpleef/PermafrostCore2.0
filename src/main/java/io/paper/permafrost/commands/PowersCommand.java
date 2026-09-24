package io.paper.permafrost.commands;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.gui.ShovelPowersManager;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.important.CoreSettings;
import net.md_5.bungee.api.chat.TextComponent;

public class PowersCommand implements CommandExecutor {
	private Main main;

	public PowersCommand(Main main) {
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
			sender.sendMessage("§cYou cannot select powers in game.");
			return true;
		}
		if (sp.getGame() != null) {
			if (sp.getGame().isBanning()) {
				if (sp.getGame().isInBanningPhase()) {
					sender.sendMessage("§cYou cannot select powers during banning selection.");
					return true;
				}
			}
		}
		if (command.getName().equalsIgnoreCase("powers") && args.length > 1) {
			if (args[0].equalsIgnoreCase("set")) {
				ShovelPowers power;
				try {
					power = ShovelPowers.valueOf(args[1].toUpperCase());
					if (main.getDataLoader().getSpleefPlayer(player).getGame() != null) {
						if (sp.getGame().isBanning()) {
							if (sp.getGame().getBan1() != null) {
								if (sp.getGame().getBan1().equals(power)) {
									player.sendMessage("§cThis power is banned!");
									return true;
								}
							}
							if (sp.getGame().getBan2() != null) {
								if (sp.getGame().getBan2().equals(power)) {
									player.sendMessage("§cThis power is banned!");
									return true;
								}
							}
						}
					}
					if (main.getDataLoader().getPowerInt(player.getUniqueId()) == 0) {
						main.getDataLoader().setPowerInt(player.getUniqueId(), 1);
					} else if (main.getDataLoader().getPowerInt(player.getUniqueId()) == 1) {
						main.getDataLoader().setPowerInt(player.getUniqueId(), 2);
					}
					String column = "power" + main.getDataLoader().getPowerInt(player.getUniqueId());
					String sql = "UPDATE player_powers SET " + column + " = ? WHERE uuid = ?";
					try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
						ps.setString(1, args[1].toUpperCase());
						ps.setString(2, player.getUniqueId().toString());
						int rows = ps.executeUpdate();
						if (rows == 0) {
							sql = "INSERT INTO player_powers (uuid, " + column + ") VALUES (?, ?)";
							try (PreparedStatement insertPs = main.getConnection().prepareStatement(sql)) {
								insertPs.setString(1, player.getUniqueId().toString());
								insertPs.setString(2, args[1].toUpperCase());
								insertPs.executeUpdate();
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}

				} catch (IllegalArgumentException ex) {
					sender.sendMessage("Invalid power type: " + args[1]);
					return true;
				}

				CoreSettings.getInstance().setShovelPower(power);
			}
		}
		if (command.getName().equalsIgnoreCase("powers") && args.length < 1) {
			main.getDataLoader().setPowerInt(player.getUniqueId(), 0);
			ShovelPowersManager menu = new ShovelPowersManager(main);
			menu.openPowersMenu(player);
		}
		return true;
	}
}
