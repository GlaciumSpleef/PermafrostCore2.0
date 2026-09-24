package io.paper.permafrost.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.SpleefMode;

public class FlyCommand implements CommandExecutor {

	private final Main main;

	public FlyCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cOnly players can use this command.");
			return true;
		}

		UUID uuid = player.getUniqueId();

		boolean hasRank = main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)
				|| main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)
				|| main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM);

		if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ffa queue.");
			return true;
		}
		
		if(player.getWorld().getName().equals("voidworld")) {
			player.sendMessage("§cYou can't fly here silly!");
			return true;
		}

		if (main.getDataLoader().getSpleefPlayer(player) != null) {
			if (main.getDataLoader().getSpleefPlayer(player).getInGame()
					|| main.getDataLoader().getSpleefPlayer(player).getInTeamGame()
					|| main.getSpleefQueueManager().isInAnyFFA(uuid)
					|| (main.getSpleggQueueManager().getPlayers() != null
							&& main.getSpleggQueueManager().getPlayers().containsKey(uuid))) {
				player.sendMessage("§cThis command cannot be run in game.");
				return true;
			}
		}
		if (main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			main.getRankedSpleggManager().removePlayerFromQueue(player.getUniqueId());
		}

		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.POWERSPLEEF);
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.AIRSPLEEF);
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			main.getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.MINISPLEEF);
		}

		if (!hasRank) {
			player.sendMessage("§cYou need the §fGlacier§c, §bPolar§c, or §dCustom§c rank to use /fly.");
			return true;
		}

		boolean enableFlight = !player.getAllowFlight();

		player.setAllowFlight(enableFlight);

		if (enableFlight) {
			player.sendMessage("§aFlight enabled.");
		} else {
			player.setFlying(false);
			player.sendMessage("§cFlight disabled.");
		}

		return true;
	}
}
