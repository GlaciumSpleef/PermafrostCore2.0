package io.paper.permafrost.core.game.tasks;

import java.util.UUID;

import org.bukkit.Bukkit;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.core.game.SpleggGame;
import io.paper.permafrost.core.game.TeamSpleefTeam;

public class CountdownTask {
	private Main main;
	private SpleefGame game;
	private SpleggGame eggGame;
	private int time, i;
	private int task;

	public CountdownTask(Main main, SpleefGame game) {
		this.main = main;
		this.game = game;
		this.time = 4;
		this.i = 0;
		this.game.setGameState(GameState.STARTING);
		task();
	}

	public CountdownTask(Main main, SpleggGame eggGame) {
		this.main = main;
		this.eggGame = eggGame;
		this.time = 4;
		this.i = 0;
		this.game.setGameState(GameState.STARTING);
		task();
	}

	private void task() {
		task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
			public void run() {
				if (game != null) {
					if (!game.getGameState().equals(GameState.STARTING)) {
						Bukkit.getScheduler().cancelTask(task);
						return;
					}

					float d = 0.85F;
					String title = "";

					switch (time) {
					case 4:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "3...";
						break;
					case 3:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "2...";
						break;
					case 2:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "1...";
						break;
					case 1:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "GO!!!";
						d = 1F;
						i = 1;
						break;
					}
					// this will not work for /duel
					for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
						for (UUID uuid : team.getPlayers()) {
							Player p = Bukkit.getPlayer(uuid);
							if (p != null) {
								if (p.isOnline()) {
									if (p != null && i == 0) {
										p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
										p.sendTitle("", title, 5, 20, 5);
									} else {
										p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, d);
										p.sendTitle("", title, 5, 20, 5);
									}
								}
							}
						}
					}
					for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(game)) {
						if (ss != null) {
							if (Bukkit.getPlayer(ss.getUUID()).isOnline()) {
								Player p = Bukkit.getPlayer(ss.getUUID());
								if (p != null && i == 0) {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								} else {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								}
							}
						}
					}
					time--;

					if (time <= 0) {
						game.setGameState(GameState.GAME);
						for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
							for (UUID uuid : team.getPlayers()) {
								Player p = Bukkit.getPlayer(uuid);
								SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
								if (sp != null) {
									if (p.isOnline()) {
										sp.setRoundStarting(false);
										sp.setResetStarting(false);
										if (p != null) {
											p.setAllowFlight(false);
											p.setFlying(false);
										}
									}
								}
							}
						}
						if (game.getStartingGame() == true) {
							for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
								for (UUID uuid : team.getPlayers()) {
									Player p = Bukkit.getPlayer(uuid);
									SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
									if (sp != null) {
										
									}
								}
							}
							game.setStartingGame(false);
						}
						Bukkit.getScheduler().cancelTask(task);
					}
				} else if (eggGame != null) {
					if (!eggGame.getGameState().equals(GameState.STARTING)) {
						Bukkit.getScheduler().cancelTask(task);
						return;
					}

					float d = 0.85F;
					String title = "";

					switch (time) {
					case 4:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "3...";
						break;
					case 3:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "2...";
						break;
					case 2:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "1...";
						break;
					case 1:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "GO!!!";
						d = 1F;
						i = 1;
						break;
					}
					// this will not work for /duel
					for (TeamSpleefTeam team : eggGame.getTeamSpleefTeams()) {
						for (UUID uuid : team.getPlayers()) {
							Player p = Bukkit.getPlayer(uuid);
							if (p != null) {
								if (p.isOnline()) {
									if (p != null && i == 0) {
										p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
										p.sendTitle("", title, 5, 20, 5);
									} else {
										p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, d);
										p.sendTitle("", title, 5, 20, 5);
									}
								}
							}
						}
					}
					for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForSpleggGame(eggGame)) {
						if (ss != null) {
							if (Bukkit.getPlayer(ss.getUUID()).isOnline()) {
								Player p = Bukkit.getPlayer(ss.getUUID());
								if (p != null && i == 0) {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								} else {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								}
							}
						}
					}
					time--;

					if (time <= 0) {
						eggGame.setGameState(GameState.GAME);
						for (TeamSpleefTeam team : eggGame.getTeamSpleefTeams()) {
							for (UUID uuid : team.getPlayers()) {
								Player p = Bukkit.getPlayer(uuid);
								SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
								if (sp != null) {
									if (p.isOnline()) {
										sp.setRoundStarting(false);
										sp.setResetStarting(false);
										if (p != null) {
											p.setAllowFlight(false);
											p.setFlying(false);
										}
									}
								}
							}
						}
						if (eggGame.getStartingGame() == true) {
							for (TeamSpleefTeam team : eggGame.getTeamSpleefTeams()) {
								for (UUID uuid : team.getPlayers()) {
									Player p = Bukkit.getPlayer(uuid);
									SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
									if (sp != null && !sp.getGame().isSplegg()) {
										
									}
								}
							}
							eggGame.setStartingGame(false);
						}
						Bukkit.getScheduler().cancelTask(task);
					}
				}
			}
		}, 2L, 20L);
	}
}
