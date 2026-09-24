package io.paper.permafrost.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.core.game.DuelManager;
import io.paper.permafrost.utilities.SpleefMode;

public class DuelAcceptCommand implements CommandExecutor {

    private final DuelManager duelManager;

    public DuelAcceptCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        
        if (duelManager.getInstance().getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ffa queue.");
			return true;
		}

		if (duelManager.getInstance().getDataLoader().getSpleefPlayer(player) != null) {
			if (duelManager.getInstance().getDataLoader().getSpleefPlayer(player).getInGame()
					|| duelManager.getInstance().getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
				player.sendMessage("§cThis command cannot be run in game.");
				return true;
			}
		}
		if (duelManager.getInstance().getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			duelManager.getInstance().getRankedSpleggManager().removePlayerFromQueue(player.getUniqueId());
		}

		if (duelManager.getInstance().getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			duelManager.getInstance().getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.POWERSPLEEF);
		}
		if (duelManager.getInstance().getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			duelManager.getInstance().getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.AIRSPLEEF);
		}
		if (duelManager.getInstance().getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cYou ran this command in ranked queue! Leaving queue now.");
			duelManager.getInstance().getRankedSpleefManager().removePlayerFromQueue(player.getUniqueId(), SpleefMode.MINISPLEEF);
		}

        duelManager.respond(player.getUniqueId(), true);
        return true;
    }
}
