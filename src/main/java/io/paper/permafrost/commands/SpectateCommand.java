package io.paper.permafrost.commands;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.Arena;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.core.game.SpleggArena;
import io.paper.permafrost.core.game.SpleggRankedArena;
import io.paper.permafrost.utilities.SpleefMode;

public class SpectateCommand implements CommandExecutor {
	private Main main;

	public SpectateCommand(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		} else {
			Player player = (Player) sender;
			if (args.length == 0) {
				sender.sendMessage("§aUse the command: /spectate <Player>");
				return true;
			}
			SpleefPlayer sp = null;
			if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
				player.sendMessage("§cThis command cannot be run in ffa queue.");
				return true;
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

			if (main.getDataLoader().getSpleefPlayer(player) != null) {
				if (main.getDataLoader().getSpleefPlayer(player).getInGame()
						|| main.getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
					player.sendMessage("§cThis command cannot be run in game.");
					return true;
				}
			}
			if (command.getName().equalsIgnoreCase("spectate")) {
				List<String> list = new ArrayList<>(Arrays.asList(args));
				for (String s : list) {
					Player op = Bukkit.getPlayer(s);
					if (op != null && Bukkit.getOnlinePlayers().contains(op))
						if (!op.equals(player))
							sp = main.getDataLoader().getSpleefPlayer(op);
				}
				if(sp.getPlayer().equals(player)) {
					player.sendMessage("§cYou cannot spectate yourself.");
					return true;
				}
				SpleefPlayer spec = main.getDataLoader().getSpleefPlayer(player);
				if(spec.getGame() != null && spec.getSpectator() != null
						&& main.getDataLoader().getSpectatorsForGame(spec.getGame()) != null
						&& main.getDataLoader().getSpectatorsForGame(spec.getGame()).contains(spec.getSpectator())) {
					if (spec != null) {
						spec.getPlayer().performCommand("spawn");
						new BukkitRunnable() {
				            public void run() {
				            	SpleefPlayer sp = null;
				            	List<String> list = new ArrayList<>(Arrays.asList(args));
								for (String s : list) {
									Player op = Bukkit.getPlayer(s);
									if (op != null && Bukkit.getOnlinePlayers().contains(op)) {
										if (!op.equals(player)) {
											sp = main.getDataLoader().getSpleefPlayer(op);
										}
									}
								}
				            	if (!sp.getGame().isSplegg()) {
									Arena arena = sp.getGame().getArena();
									if (arena.getBorder().getBlockX() == 0 && arena.getBorder().getBlockY() == 0
											&& arena.getBorder().getBlockZ() == 0) {
										Location teleport = arena.getCenter().clone();
										teleport.setY(arena.getCenter().getY() + 5);
										player.teleport(teleport);

										Bukkit.getScheduler().runTaskLater(main, () -> {
											player.setGameMode(GameMode.SPECTATOR);
											Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
										}, 1L);
									} else {
										Location teleport = arena.getBorder();
										player.getPlayer().teleport(teleport);
										Bukkit.getScheduler().runTaskLater(main, () -> {
											player.setGameMode(GameMode.ADVENTURE);
											Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
										}, 1L);
									}
									sp.getGame().faceLocation(player, arena.getCenter());
									player.getPlayer().getInventory().clear();
									SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
									vise.setGame(sp.getGame());
									SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
									main.getDataLoader().addSpectator(sp.getGame(), spectator);
									vise.setSpectator(spectator);
									if (sp.getInGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInGame(true);
									} else if (sp.getInTeamGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInTeamGame(true);
									}
									spectator.setGame(sp.getGame());
								} else if (sp.getGame().isSplegg() && sp.getGame().isRanked()) {
									SpleggRankedArena arena = sp.getGame().getSpleggRankedArena();
									Location teleport = arena.getPasteOrigin();
									player.getPlayer().teleport(teleport);
									player.getPlayer().getInventory().clear();
									SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
									vise.setGame(sp.getGame());
									SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
									main.getDataLoader().addSpectator(sp.getGame(), spectator);
									vise.setSpectator(spectator);
									if (sp.getInGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInGame(true);
									} else if (sp.getInTeamGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInTeamGame(true);
									}
									spectator.setGame(sp.getGame());
									Bukkit.getScheduler().runTaskLater(main, () -> {
										player.setGameMode(GameMode.SPECTATOR);
										Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
									}, 1L);
								} else if (sp.getGame().isSplegg()) {
									SpleggRankedArena arena = sp.getGame().getSpleggRankedArena();
									Location teleport = arena.getPasteOrigin();
									player.getPlayer().teleport(teleport);
									player.getPlayer().getInventory().clear();
									SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
									vise.setGame(sp.getGame());
									SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
									main.getDataLoader().addSpectator(sp.getGame(), spectator);
									vise.setSpectator(spectator);
									if (sp.getInGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInGame(true);
									} else if (sp.getInTeamGame()) {
										spectator.setGame(sp.getGame());
										spectator.setInTeamGame(true);
									}
									spectator.setGame(sp.getGame());
									Bukkit.getScheduler().runTaskLater(main, () -> {
										player.setGameMode(GameMode.SPECTATOR);
										Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
									}, 1L);
								}
				            }
				        }.runTaskLater(main, 5L);
				        return true;
					}
				}
				if (!sp.getGame().isSplegg()) {
					Arena arena = sp.getGame().getArena();
					if (arena.getBorder().getBlockX() == 0 && arena.getBorder().getBlockY() == 0
							&& arena.getBorder().getBlockZ() == 0) {
						Location teleport = arena.getCenter().clone();
						teleport.setY(arena.getCenter().getY() + 5);
						player.teleport(teleport);

						Bukkit.getScheduler().runTaskLater(main, () -> {
							player.setGameMode(GameMode.SPECTATOR);
							Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
						}, 1L);
					} else {
						Location teleport = arena.getBorder();
						player.getPlayer().teleport(teleport);
						Bukkit.getScheduler().runTaskLater(main, () -> {
							player.setGameMode(GameMode.ADVENTURE);
							Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
						}, 1L);
					}
					sp.getGame().faceLocation(player, arena.getCenter());
					player.getPlayer().getInventory().clear();
					SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
					vise.setGame(sp.getGame());
					SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
					main.getDataLoader().addSpectator(sp.getGame(), spectator);
					vise.setSpectator(spectator);
					if (sp.getInGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInGame(true);
					} else if (sp.getInTeamGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInTeamGame(true);
					}
					spectator.setGame(sp.getGame());
					return true;
				} else if (sp.getGame().isSplegg() && sp.getGame().isRanked()) {
					SpleggRankedArena arena = sp.getGame().getSpleggRankedArena();
					Location teleport = arena.getPasteOrigin();
					player.getPlayer().teleport(teleport);
					player.getPlayer().getInventory().clear();
					SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
					vise.setGame(sp.getGame());
					SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
					main.getDataLoader().addSpectator(sp.getGame(), spectator);
					vise.setSpectator(spectator);
					if (sp.getInGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInGame(true);
					} else if (sp.getInTeamGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInTeamGame(true);
					}
					spectator.setGame(sp.getGame());
					Bukkit.getScheduler().runTaskLater(main, () -> {
						player.setGameMode(GameMode.SPECTATOR);
						Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
					}, 1L);
					return true;
				} else if (sp.getGame().isSplegg()) {
					SpleggRankedArena arena = sp.getGame().getSpleggRankedArena();
					Location teleport = arena.getPasteOrigin();
					player.getPlayer().teleport(teleport);
					player.getPlayer().getInventory().clear();
					SpleefPlayer vise = main.getDataLoader().getSpleefPlayer(player);
					vise.setGame(sp.getGame());
					SpleefSpectator spectator = new SpleefSpectator(main, vise.getUUID());
					main.getDataLoader().addSpectator(sp.getGame(), spectator);
					vise.setSpectator(spectator);
					if (sp.getInGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInGame(true);
					} else if (sp.getInTeamGame()) {
						spectator.setGame(sp.getGame());
						spectator.setInTeamGame(true);
					}
					spectator.setGame(sp.getGame());
					Bukkit.getScheduler().runTaskLater(main, () -> {
						player.setGameMode(GameMode.SPECTATOR);
						Bukkit.getLogger().info("Gamemode after set: " + player.getGameMode());
					}, 1L);
					return true;
				}
			}
		}
		return false;
	}
}
