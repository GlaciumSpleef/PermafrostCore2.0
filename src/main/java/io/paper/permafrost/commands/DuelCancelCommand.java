package io.paper.permafrost.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.core.game.DuelManager;

public class DuelCancelCommand implements CommandExecutor {

    private final DuelManager duelManager;

    public DuelCancelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        duelManager.cancelRequest(player.getUniqueId(), "Cancelled by sender.");
        return true;
    }
}
