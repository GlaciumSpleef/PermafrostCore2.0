package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.parkour.ParkourGame;
import io.paper.permafrost.utilities.SpleefMode;

public class ForceStartCommand implements CommandExecutor {

	private final Main main;

	public ForceStartCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!command.getName().equalsIgnoreCase("fstartgame")) {
			return true;
		}

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (!player.hasPermission("permafrostcore.fstartgame")) {
			player.sendMessage("§cYou do not have permission to use this command.");
			return true;
		}

		if (args.length < 3) {
			player.sendMessage( // this is for banning
					"§cUsage: /fstartgame <powerspleef|airspleef|minispleef|splegg|parkour> <true/false> <player1> <player2> [more players...]");
			return true;
		}

		String modeArg = args[0].toLowerCase();
		boolean banning = args[1].equals("true");
		boolean splegg = false;
		boolean parkour = false;
		SpleefMode mode;

		switch (modeArg) {
		case "splegg":
			splegg = true;
			mode = null;
			break;
		case "parkour":
			parkour = true;
			mode = null;
			break;
		case "powerspleef":
			mode = SpleefMode.POWERSPLEEF;
			break;
		case "airspleef":
			mode = SpleefMode.AIRSPLEEF;
			break;
		case "minispleef":
			mode = SpleefMode.MINISPLEEF;
			break;
		case "testspleef":
			mode = SpleefMode.TESTSPLEEF;
			break;
		default:
			player.sendMessage("§cInvalid gamemode.");
			player.sendMessage(
					"§cUsage: /fstartgame <powerspleef|airspleef|minispleef|splegg|parkour> <true/false> <player1> <player2> [more players...]");
			return true;
		}

		int playerCount = args.length - 2;

		if (banning == true && mode != SpleefMode.POWERSPLEEF) {
			player.sendMessage("§cYou set banning to true but didn't send Powerspleef, continuing anyways.");
		}

		if (playerCount < 2) {
			player.sendMessage("§cYou need at least 2 players.");
			return true;
		}

		if (playerCount % 2 != 0) {
			player.sendMessage("§cSend a command with an even amount of players.");
			return true;
		}

		List<Player> targets = new ArrayList<>();
		List<SpleefPlayer> spleefPlayers = new ArrayList<>();

		for (int i = 2; i < args.length; i++) {
			Player target = Bukkit.getPlayerExact(args[i]);

			if (target == null) {
				player.sendMessage("§cPlayer §f" + args[i] + " §cis not online.");
				return true;
			}

			if (targets.contains(target)) {
				player.sendMessage("§cYou cannot include the same player twice.");
				return true;
			}

			SpleefPlayer sp = main.getDataLoader().getPlayer(target.getUniqueId());
			if (sp == null) {
				player.sendMessage("§c" + target.getName() + " is not a registered Spleef player.");
				return true;
			}

			if (sp.getGame() != null && sp.getSpectator() != null
					&& Main.getInstance().getDataLoader().getSpectatorsForGame(sp.getGame()) != null
					&& Main.getInstance().getDataLoader().getSpectatorsForGame(sp.getGame())
							.contains(sp.getSpectator())) {
				player.sendMessage("§c" + target.getName() + " is spectating a game, removing them now.");
				SpleefGame game = sp.getGame();

				if (game != null) {
					main.getDataLoader().getSpectatorsForGame(game).remove(sp.getSpectator());
				}

				sp.getSpectator().setGame(null);
				sp.getSpectator().setInGame(false);
				sp.getSpectator().setInTeamGame(false);

				sp.setSpectator(null);
				sp.setGame(null);
			}

			if (sp.getInGame() || sp.getInTeamGame()) {
				player.sendMessage("§c" + target.getName() + " is already in a game.");
				return true;
			}

			if (main.getSpleggQueueManager().getPlayers().containsKey(target.getUniqueId())) {
				player.sendMessage("§cPlayer " + sp.getName() + " is already in a queue.");
				return true;
			}
			if (main.getRankedSpleggManager().getPlayersInQueue().contains(target.getUniqueId())) {
				player.sendMessage("§cPlayer " + sp.getName() + " is already in a queue.");
				return true;
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF)
					.contains(target.getUniqueId())) {
				player.sendMessage("§cPlayer " + sp.getName() + " is already in a queue.");
				return true;
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(target.getUniqueId())) {
				player.sendMessage("§cPlayer " + sp.getName() + " is already in a queue.");
				return true;
			}
			if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(target.getUniqueId())) {
				player.sendMessage("§cPlayer " + sp.getName() + " is already in a queue.");
				return true;
			}

			targets.add(target);
			spleefPlayers.add(sp);
		}

		List<UUID> team1 = new ArrayList<>();
		List<UUID> team2 = new ArrayList<>();

		if (!parkour) {
			if (playerCount == 2) {
				team1.add(Bukkit.getPlayerExact(args[2]).getUniqueId());
				team2.add(Bukkit.getPlayerExact(args[3]).getUniqueId());
			} else {
				int teamSize = spleefPlayers.size() / 2;

				for (int i = 0; i < spleefPlayers.size(); i++) {
					UUID uuid = spleefPlayers.get(i).getPlayer().getUniqueId();

					if (i < teamSize) {
						team1.add(uuid);
					} else {
						team2.add(uuid);
					}
				}
			}

			boolean instantStart = true;

			SpleefGame game = new SpleefGame(main, splegg, instantStart, mode);

			Map<Integer, List<UUID>> teamMap = new HashMap<>();
			teamMap.put(1, team1);
			teamMap.put(2, team2);

			TeamSpleefTeam blue = new TeamSpleefTeam(main, "blue", team1);
			TeamSpleefTeam red = new TeamSpleefTeam(main, "red", team2);

			game.setTeamSpleefTeam("blue", team1);
			game.setTeamSpleefTeam("red", team2);

			for (UUID uuid : team1) {
				SpleefPlayer sp = main.getDataLoader().getPlayer(uuid);
				if (sp != null) {
					sp.setTeam(blue);
				}
			}

			for (UUID uuid : team2) {
				SpleefPlayer sp = main.getDataLoader().getPlayer(uuid);
				if (sp != null) {
					sp.setTeam(red);
				}
			}

			game.setTeamCount(2);
			game.setTeamGame(true);
			game.saveTeams(teamMap);

			main.getDuelManager().setGame(game);

			game.setBanning(banning);

			game.startTeamGame();

			String modeName = "Powerspleef";
			if (splegg) {
				modeName = "Splegg";
			} else if (mode == SpleefMode.AIRSPLEEF) {
				modeName = "Airspleef";
			} else if (mode == SpleefMode.MINISPLEEF) {
				modeName = "Minispleef";
			} else if (mode == SpleefMode.TESTSPLEEF) {
				modeName = "Testspleef";
			}

			for (Player target : targets) {
				target.sendMessage("§aA forced §f" + modeName + " §agame has started.");
			}

			player.sendMessage("§aForce started a §f" + modeName + " §agame with §f" + playerCount + " §aplayers.");
			return true;
		} else if (parkour) {
			ParkourGame game = new ParkourGame(main, true, Bukkit.getPlayerExact(args[2]).getUniqueId(),
					Bukkit.getPlayerExact(args[3]).getUniqueId());
			game.startMatch();
			main.getDuelManager().setGame(game);
			if (Bukkit.getPlayer(Bukkit.getPlayerExact(args[2]).getUniqueId()) != null) {
				Bukkit.getPlayer(Bukkit.getPlayerExact(args[2]).getUniqueId())
						.sendMessage(ChatColor.AQUA + "Your ranked §fparkour §bmatch is starting against "
								+ Bukkit.getPlayer(Bukkit.getPlayerExact(args[3]).getUniqueId()).getName() + "!");
			}
			if (Bukkit.getPlayer(Bukkit.getPlayerExact(args[3]).getUniqueId()) != null) {
				Bukkit.getPlayer(Bukkit.getPlayerExact(args[3]).getUniqueId())
						.sendMessage(ChatColor.AQUA + "Your ranked §fparkour §bmatch is starting against "
								+ Bukkit.getPlayer(Bukkit.getPlayerExact(args[2]).getUniqueId()).getName() + "!");
			}
			return true;
		}
		return true;
	}
}