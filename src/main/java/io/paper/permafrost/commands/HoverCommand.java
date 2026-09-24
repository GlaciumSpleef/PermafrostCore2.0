package io.paper.permafrost.commands;

import java.util.UUID; 

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefDuel;
import io.paper.permafrost.core.game.SpleefPlayer;

public class HoverCommand implements CommandExecutor {
	
	private Main main;
	
	public HoverCommand(Main main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can do this.");
			return true;
		}

		if (cmd.getName().equalsIgnoreCase("hover")) {
			Player p = (Player) sender;
			if (args.length == 0) return false;
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
			switch (args[0]) {
				case "duelaccept":
					duelAccept(sp, args);
					return true;
				case "dueldeny":
					duelDeny(sp, args);
					return true;
				case "duelcancel":
					duelCancel(sp, args);
					return true;
			}
		}
		return false;
	}




	private void duelCancel(SpleefPlayer sp, String[] args) {
		Player p = sp.getPlayer();
		SpleefDuel duel = sp.getDuelByUUID(UUID.fromString(args[1]));
		if (duel != null) {
			for (SpleefPlayer dueled : duel.getDueledPlayers()) {
				dueled.getPlayer().sendMessage("§3§l" + sp.getName() + "§c The duel has been cancelled.");
			}
			duel.getChallenger().getPlayer().sendMessage("§cYou have cancelled the duel.");
			duel.getChallenger().getDuels().remove(duel);
		} else {
			p.sendMessage("§cThis duel request has expired.");
		}
	}

	private void duelAccept(SpleefPlayer sp, String[] args) {
		Player p = sp.getPlayer();
		Player p2 = Bukkit.getPlayer(args[1]);
		if (Bukkit.getOnlinePlayers().contains(p2)) {
			SpleefPlayer challenger = main.getDataLoader().getSpleefPlayer(p2);
			
			if (challenger.hasDueled(sp)) {
				SpleefDuel duel = challenger.getDuelByDueledPlayer(sp);
				if (sp.getGame()==null) {
					if (challenger.getGame()==null) {
						duel.acceptDuel(sp);
						
					} else {
						p.sendMessage("§cThis player is already in a game.");
					}
				} else {
					p.sendMessage("§cYou can't use this command here.");
				}
			} else {
				p.sendMessage("§cThis duel request has expired.");
			}
		} else {
			p.sendMessage("§cThe player who sent you this request isn't online.");
		}
	}

	private void duelDeny(SpleefPlayer sp, String[] args) {
		Player p = sp.getPlayer();
		Player p2 = Bukkit.getPlayer(args[1]);
		if (Bukkit.getOnlinePlayers().contains(p2)) {
			SpleefPlayer challenger = main.getDataLoader().getSpleefPlayer(p2);
			if (challenger.hasDueled(sp)) {
				SpleefDuel duel = challenger.getDuelByDueledPlayer(sp);
				sp.getPlayer().sendMessage("§cYou have denied this duel request §3§l" + challenger.getName() + "§c.");

				for (SpleefPlayer dueled : duel.getAllPlayers()) {
					if (dueled != sp) {
						dueled.getPlayer().sendMessage("§cThe player §3§l" + sp.getName() + "§c has denied your request.");
					}
				}
				challenger.getDuels().remove(duel);
			} else {
				p.sendMessage("§cThis duel request has expired.");
			}
		} else {
			p.sendMessage("§cThe player who sent you this request isn't online.");
		}
	}
}
