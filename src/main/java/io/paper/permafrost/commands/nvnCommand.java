package io.paper.permafrost.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleggFFAArena;

public class nvnCommand implements CommandExecutor {
    private Main main;

    public nvnCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!command.getName().equalsIgnoreCase("nvn")) {
            return true;
        }

        if (args.length == 0) {
        	if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
        	    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
        	} else {
        	    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        	}
            return true;
        }
        return true;
    }
}
