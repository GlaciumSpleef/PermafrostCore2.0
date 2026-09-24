package io.paper.permafrost.commands;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefDuel;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefType;
import io.paper.permafrost.utilities.Utils;

public class DuelCommand implements CommandExecutor {
	private Main main;

	public DuelCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	    if (!(sender instanceof Player)) {
	        sender.sendMessage("Only players can send duels!");
	        return true;
	    }
	    if (cmd.getName().equalsIgnoreCase("duel")) {
	        Player p = (Player) sender;
	        SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);

	        if (sp == null) {
	            sender.sendMessage("§cYou are not registered as a Spleef player!");
	            return false;
	        }

	        if (args.length == 0) {
	            sender.sendMessage("§aUse the command: /duel <Players>");
	            return true;
	        }

	        List<String> list = new ArrayList<>(Arrays.asList(args));
	        List<SpleefPlayer> sp2 = new ArrayList<>();

	        if ((list.size() + 1) % 2 != 0) {
	            sender.sendMessage("§cYou can only duel an even amount of players! (1,3,5,7,etc.)");
	            return false;
	        }
	        if (Utils.hasDuplicate(list)) {
	            sender.sendMessage("§cYou can only duel the same player once.");
	            return false;
	        }

	        for (String s : list) {
	            Player op = Bukkit.getPlayer(s);
	            if (op != null && Bukkit.getOnlinePlayers().contains(op)) {
	                if (!op.equals(p)) {
	                    SpleefPlayer dueled = main.getDataLoader().getSpleefPlayer(op);
	                    if (dueled != null) {
	                        sp2.add(dueled);
	                    }
	                }
	            }
	        }

	        if (sp.getPlayer() != null) {
	            sp.getPlayer().closeInventory();
	        }

	        SpleefDuel sduel = new SpleefDuel(main, sp, sp2, SpleefType.SPLEEF);
	        sp.getDuels().add(sduel);

	        new BukkitRunnable() {
	            public void run() {
	                sp.getDuels().remove(sduel);
	            }
	        }.runTaskLater(main, 20 * 60L);
	    }
	    return false;
	}
}