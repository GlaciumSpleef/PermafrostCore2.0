package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.Arena;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.utilities.SpleefMode;
import net.md_5.bungee.api.ChatColor;

public class SpleggQueueCommand implements CommandExecutor {
	private Main main;

	public SpleggQueueCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else {
			Player player = (Player) sender;
			
			if(main.getDataLoader().getSpleefPlayer(player) != null) {
				if(main.getDataLoader().getSpleefPlayer(player).getInGame() || main.getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
					player.sendMessage("§cThis command cannot be run in game.");
					return true;
				}
			}
			
			if(main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ranked queue.");
				return true;
			}
			
			SpleefQueueManager spleefManager = main.getSpleefQueueManager();
			SpleefFFAGame spleefGame = spleefManager.getPlayerFFA(player.getUniqueId());

			if (spleefGame != null) {

				if (spleefGame.isAlive(player.getUniqueId())) {
					spleefGame.setDead(player.getUniqueId());

					int remaining = spleefGame.getPlayersRemaining();

					if (remaining == 2) {
						spleefGame.setTopThree(player.getUniqueId());
					} else if (remaining == 1) {
						spleefGame.setTopTwo(player.getUniqueId());
					}

					if (spleefGame.isInGame() && !spleefGame.isGameEnding() && remaining <= 1) {
						spleefGame.endGame(); // or endFFAGame() if that is what you named it
					}
				}

				spleefGame.removePlayer(player.getUniqueId());

				Bukkit.dispatchCommand(
						Bukkit.getConsoleSender(),
						"hologram edit &6Spleef setline 4 &fPlayers: &e" + spleefManager.getTotalPlayersInQueue()
				);
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

			if (command.getName().equalsIgnoreCase("spleggqueue")) {
				if(!main.getQueuesOpen()) {
					player.sendMessage("§cQueues are closed!");
					return true;
				}
				Location loc = new Location(Bukkit.getWorld("spleggffa"), 44,66.5,33);
				loc.setYaw(90f);
				player.teleport(loc);
				player.setBodyYaw(90f);
				player.getInventory().clear();
				ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
				ItemMeta m = slimeball.getItemMeta();
				m.addEnchant(Enchantment.EFFICIENCY, 5, true);
				m.setUnbreakable(true);
				m.setDisplayName("§cLeave");
				slimeball.setItemMeta(m);
				slimeball.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				if (!player.getInventory().contains(slimeball)) {
					player.getInventory().setItem(8, slimeball);
				}
				main.getSpleggQueueManager().addPlayer(player.getUniqueId(), false);
				player.sendMessage(ChatColor.GREEN + "You have joined the splegg FFA queue!\nThere are currently "
						+ ChatColor.YELLOW + main.getSpleggQueueManager().getPlayersInFFA() + ChatColor.GREEN
						+ " players playing and " + ChatColor.YELLOW + (main.getSpleggQueueManager().getPlayerCount()
								- main.getSpleggQueueManager().getPlayersInFFA())
						+ " players waiting.");
				Bukkit.dispatchCommand(
					    Bukkit.getConsoleSender(),
					    "hologram edit &6Splegg setline 4 &fPlayers: &e" + main.getSpleggQueueManager().getPlayerCount()
					);
			}
			return true;
		}
	}
}
