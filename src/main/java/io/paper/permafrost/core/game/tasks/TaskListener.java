package io.paper.permafrost.core.game.tasks;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.FlowerPot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import io.paper.permafrost.commands.ResetCommand;
import io.paper.permafrost.core.game.SpleefTeam;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.training.TrainingState;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.core.game.Arena;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("unused")
public class TaskListener implements Listener {
	private final Set<UUID> recallRescuing = new HashSet<>();
	private final Main main;
	private DataLoader dataLoader;
	private SpleefGame game;
	private boolean b = false;

	public TaskListener(Main main) {
		this.main = main;
	}

	public boolean isSnow(Material bT) {
		if (bT.equals(Material.SNOW_BLOCK))
			return true;
		return false;
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Location playerLoc = event.getPlayer().getLocation();
		event.setDropItems(false);
		if (player.getWorld().getName().equals("lobby") && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}
		if (!isSnow(event.getBlock().getType()) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
			if (event.getBlock().getType() != Material.ORANGE_CONCRETE_POWDER
					&& event.getBlock().getType() != Material.RED_CONCRETE_POWDER
					&& event.getBlock().getType() != Material.GREEN_CONCRETE_POWDER
					&& event.getBlock().getType() != Material.LIME_CONCRETE_POWDER)
				event.setCancelled(true);
		}
		if (isSnow(event.getBlock().getType())) {
			if (player.getWorld().getName().equals("ffa")) {
				if (player.getGameMode() == GameMode.SURVIVAL) {
					if (playerLoc.getZ() >= -25 && playerLoc.getZ() <= 25) {
						if (playerLoc.getX() >= -25 && playerLoc.getX() <= 25) {
							Bukkit.getScheduler().runTaskLater(main, () -> {
								event.getBlock().setType(Material.SNOW_BLOCK);
							}, 20L * 7);
						}
					}
				}
			}
		}
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(event.getPlayer());
		if (sp.isRoundStarting()) {
			event.setCancelled(true);
		}
		if (main.getDataLoader().getTrainingSession(player.getUniqueId()) != null) {
			if (main.getDataLoader().getTrainingSession(player.getUniqueId())
					.getTrainingState() == TrainingState.STARTING) {
				event.setCancelled(true);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location playerLoc = player.getLocation();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		if (sp.getInGame() || sp.getInTeamGame() && player.getWorld().getName().equals("lobby")) {
			if (sp.getGame() != null) {
				if (sp.getGame().isSplegg()) {
					sp.getPlayer().teleport(sp.getGame().getSpleggRankedArena().getPasteOrigin());
				}
				if (!sp.getGame().isSplegg()) {
					sp.getPlayer().teleport(sp.getGame().getArena().getCenter());
				}
			}
		}
		if (sp.getInGame() && player.getWorld().getName().equals("voidworld")) {
			if (sp.getGame() != null) {
				int minY = sp.getGame().getArena().getCorner1().getBlockY() - 1;
				int minX = sp.getGame().getArena().getCorner1().getBlockX() - 7;
				int maxX = sp.getGame().getArena().getCorner2().getBlockX() + 7;
				int minZ = sp.getGame().getArena().getCorner2().getBlockZ() - 7;
				int maxZ = sp.getGame().getArena().getCorner1().getBlockZ() + 7;
				if (playerLoc.getY() <= minY && playerLoc.getX() >= minX && playerLoc.getX() <= maxX
						&& playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ) {
					if (sp.getGame().getPlayers().size() < 3) {
						for (SpleefPlayer p2 : sp.getGame().getPlayers()) {
							if (!p2.equals(sp)) {
								p2.addPoint();
							}
							if (p2.getPoints() == 5) {
								b = true;
								p2.setWinner(true);
							}
						}
						if (b == false)
							if (sp.getGame().getTeamGame() == true) {
								sp.getGame().startTeamRound();
							} else {
								sp.getGame().startRound();
							}
						if (b == true) {
							if (sp.getGame().getTeamGame() == true) {
								sp.getGame().endTeamGame();
							} else {
								sp.getGame().endGame();
							}
						}
						b = false;
					}
				}

				if (player.getWorld().getName().equals("ffa")) {
					if (player.getGameMode() == GameMode.SURVIVAL) {
						if (playerLoc.getY() <= 52) {
							if (playerLoc.getZ() >= -25 && playerLoc.getZ() <= 25) {
								if (playerLoc.getX() >= -25 && playerLoc.getX() <= 25) {
									Location teleportLocation = new Location(event.getPlayer().getWorld(), 0, 65, 0);
									teleportLocation.setYaw(270f);
									event.getPlayer().teleport(teleportLocation);
								}
							}
						}
					}
				}
			}
		}
		if (sp.getInTeamGame() && player.getWorld().getName().equals("voidworld")
				&& sp.getPlayer().getGameMode() == GameMode.SURVIVAL) {
			if (sp.getGame() != null) {
				if (!sp.getGame().isSplegg()) {
					int minY = sp.getGame().getArena().getCorner1().getBlockY() - 1;
					if (sp.getSelectedPower() != null && sp.getSelectedPower().equals(ShovelPowers.DOUBLEJUMP)
							|| (sp.getSelectedPower() != null && sp.getSelectedPower().equals(ShovelPowers.ROULETTE)
									&& sp.getRandomPower().equals(ShovelPowers.DOUBLEJUMP))
							|| (sp.getSelectedPower() != null && sp.getSelectedPower().equals(ShovelPowers.ROULETTE2)
									&& sp.getRandomPower2().equals(ShovelPowers.DOUBLEJUMP))) {
						minY--;
					}
					if (main.getDataLoader().getRecallActive(sp.getPlayer().getUniqueId())) {
						minY = minY - 2;
					}
					int minX = sp.getGame().getArena().getCorner1().getBlockX() - 12;
					int maxX = sp.getGame().getArena().getCorner2().getBlockX() + 12;
					int minZ = sp.getGame().getArena().getCorner2().getBlockZ() - 12;
					int maxZ = sp.getGame().getArena().getCorner1().getBlockZ() + 12;
					if (playerLoc.getY() <= minY && playerLoc.getX() >= minX && playerLoc.getX() <= maxX
							&& playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ
							&& !recallRescuing.contains(sp.getPlayer().getUniqueId())) {
						if (main.getDataLoader().getRecallActive(sp.getPlayer().getUniqueId())) {
						    recallRescuing.add(sp.getPlayer().getUniqueId());

						    boolean recalled = triggerRecall(player);

						    Bukkit.getScheduler().runTaskLater(main, () -> {
						        recallRescuing.remove(sp.getPlayer().getUniqueId());
						    }, 3L);

						    return;
						}
						String color = sp.getTeam().getColor();
						List<UUID> playersOnline = sp.getTeam().getPlayers();
						int count = 0;
						int playersAlive = 0;
						for (UUID uuid : playersOnline) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								count++;
								playersAlive++;
							}
						}
						SpleefGame game = sp.getGame();
						for (UUID uuid : sp.getTeam().getPlayers()) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								SpleefPlayer sp2 = main.getDataLoader().getSpleefPlayer(p);
								if (sp2 != null) {
									if (sp2.isDead()) {
										count--;
										playersAlive--;
									}
								}
							}
							// if (!p.getPlayer().isOnline()) {
							// count--;
							// playersAlive--;
							// sp2.setDead(true);
							// }
						}
						if (count == 1) {
							sp.setDead(true);
							sp.setAcceptedReset(false);
							sp.getPlayer().getInventory().clear();
							count = game.getTeamSpleefTeams().size();
							for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
								if (team.isDead()) {
									count--;
								}
							}
							if (count == 1) {
								String msg = "";
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									if (!team.isDead()) {
										team.addPoint();

										if (game.isRanked() && game.getPlayers().size() <= 2) {
											UUID winner = null;
											for (UUID uuid : team.getPlayers()) {
												winner = uuid;
											}
											if (winner != null && Bukkit.getPlayer(winner) != null) {
												msg += "§e" + Bukkit.getPlayer(winner).getName()
														+ " §r§ahas won the round!";
											}
										} else if (game.isRanked() && game.getPlayers().size() > 2) {
											msg += game.getChatColor(team.getColor()) + "§l"
													+ game.capitalize(team.getColor()) + " team §r§ahas won the round!";
										} else {
											msg += game.getChatColor(team.getColor()) + "§l"
													+ game.capitalize(team.getColor()) + " team §r§ahas won the round!";
										}
									}

									if (team.getPoints() == game.getPlaytoScore()) {
										b = true;
										team.setWinner(true);
									}
								}
								for (SpleefPlayer p : game.getPlayers()) {
									p.setDead(false);
								}
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									game.setTeamSpleefTeamDead(color, false);
								}
								if (b == false)
									if (game.getTeamGame() == true) {
										game.startTeamRound();
										for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
											if (sp2.getPlayer() != null)
												sp2.getPlayer().sendMessage(msg);
										}
										for (SpleefSpectator ss : main.getDataLoader()
												.getSpectatorsForGame(sp.getGame())) {
											if (Bukkit.getPlayer(ss.getUUID()) != null)
												Bukkit.getPlayer(ss.getUUID()).sendMessage(msg);
										}
									} else {
										game.startRound();
									}
								if (b == true) {
									if (game.getTeamGame() == true) {
										game.endTeamGame();
									} else {
										game.endGame();
									}
								}
								b = false;
							} else {
								game.setTeamSpleefTeamDead(color, true);
								sp.setSpectating(true);
								Location loc = game.getArena().getBorder();
								if (loc.getBlockY() == 0) {
									sp.getPlayer().setGameMode(GameMode.SPECTATOR);
									Location newLoc = sp.getPlayer().getLocation();
									newLoc.setY(newLoc.getY());
									sp.getPlayer().teleport(newLoc);
								} else {
									sp.getPlayer().teleport(loc);
								}
								sp.getGame().faceLocation(player, sp.getGame().getArena().getCenter());
								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.getPlayer() != null) {
										sp2.getPlayer()
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");

										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()) != null) {
										Bukkit.getPlayer(ss.getUUID())
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						} else {
							if (sp.isOnline()) {
								sp.setDead(true);
								sp.setAcceptedReset(false);
								sp.getPlayer().getInventory().clear();
								playersAlive--;
								sp.setSpectating(true);
								Location loc = game.getArena().getBorder();
								if (loc.getBlockY() == 0) {
									sp.getPlayer().setGameMode(GameMode.SPECTATOR);
									Location newLoc = game.getArena().getCenter();
									newLoc.setY(newLoc.getY());
									sp.getPlayer().teleport(newLoc);
								} else {
									sp.getPlayer().teleport(loc);
								}
								sp.getGame().faceLocation(player, sp.getGame().getArena().getCenter());
								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.isOnline()) {
										sp2.getPlayer().sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()).isOnline()) {
										Bukkit.getPlayer(ss.getUUID()).sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						}

					}
				} else if (sp.getGame().isSplegg() && sp.getGame().isRanked()) {
					int minY = sp.getGame().getSpleggRankedArena().getFallLevel();
					int minX = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockX() - 100;
					int maxX = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockX() + 100;
					int minZ = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockZ() - 100;
					int maxZ = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockZ() + 100;
					if (playerLoc.getY() <= minY && playerLoc.getX() >= minX && playerLoc.getX() <= maxX
							&& playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ) {
						if (sp.isDead() || sp.isSpectating()) {
							return;
						}
						String color = sp.getTeam().getColor();
						List<UUID> playersOnline = sp.getTeam().getPlayers();
						int count = 0;
						int playersAlive = 0;
						for (UUID uuid : playersOnline) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								count++;
								playersAlive++;
							}
						}
						SpleefGame game = sp.getGame();
						for (UUID uuid : sp.getTeam().getPlayers()) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								SpleefPlayer sp2 = main.getDataLoader().getSpleefPlayer(p);
								if (sp2 != null) {
									if (sp2.isDead()) {
										count--;
										playersAlive--;
									}
								}
							}
							// if (!p.getPlayer().isOnline()) {
							// count--;
							// playersAlive--;
							// sp2.setDead(true);
							// }
						}
						if (count == 1) {
							sp.setDead(true);
							sp.setAcceptedReset(false);
							sp.getPlayer().getInventory().clear();
							count = game.getTeamSpleefTeams().size();
							for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
								if (team.isDead()) {
									count--;
								}
							}
							if (count == 1) {
								String msg = "";
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									if (!team.isDead()) {
										team.addPoint();
										UUID winner = null;
										for (UUID uuid : team.getPlayers()) {
											winner = uuid;
										}
										msg += "§e" + Bukkit.getPlayer(winner).getName() + " §r§ahas won the round!";
									}
									if (team.getPoints() == sp.getGame().getPlaytoScore()) {
										b = true;
										team.setWinner(true);
									}
								}
								for (SpleefPlayer p : game.getPlayers()) {
									p.setDead(false);
								}
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									game.setTeamSpleefTeamDead(team.getColor(), false);
								}
								if (b == false)
									if (game.getTeamGame() == true) {
										game.startTeamRound();

										for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
											if (sp2.getPlayer() != null)
												sp2.getPlayer().sendMessage(msg);
										}
										for (SpleefSpectator ss : main.getDataLoader()
												.getSpectatorsForGame(sp.getGame())) {
											if (Bukkit.getPlayer(ss.getUUID()) != null)
												Bukkit.getPlayer(ss.getUUID()).sendMessage(msg);
										}
									} else {
										game.startRound();
									}
								if (b == true) {
									if (game.getTeamGame() == true) {
										game.endTeamGame();
									} else {
										game.endGame();
									}
								}
								b = false;
							} else {
								game.setTeamSpleefTeamDead(color, true);
								sp.setSpectating(true);

								sp.getPlayer().setGameMode(GameMode.SPECTATOR);

								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.getPlayer() != null) {
										sp2.getPlayer()
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");

										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()) != null) {
										Bukkit.getPlayer(ss.getUUID())
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						} else {
							if (sp.isOnline()) {
								sp.setDead(true);
								sp.setAcceptedReset(false);
								sp.getPlayer().getInventory().clear();
								playersAlive--;
								sp.setSpectating(true);
								sp.getPlayer().setGameMode(GameMode.SPECTATOR);

								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.isOnline()) {
										sp2.getPlayer().sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()).isOnline()) {
										Bukkit.getPlayer(ss.getUUID()).sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						}

					}
				} else if (sp.getGame().isSplegg()) {
					int minY = sp.getGame().getSpleggRankedArena().getFallLevel();
					int minX = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockX() - 100;
					int maxX = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockX() + 100;
					int minZ = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockZ() - 100;
					int maxZ = sp.getGame().getSpleggRankedArena().getPasteOrigin().getBlockZ() + 100;
					if (playerLoc.getY() <= minY && playerLoc.getX() >= minX && playerLoc.getX() <= maxX
							&& playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ) {
						String color = sp.getTeam().getColor();
						List<UUID> playersOnline = sp.getTeam().getPlayers();
						int count = 0;
						int playersAlive = 0;
						for (UUID uuid : playersOnline) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								count++;
								playersAlive++;
							}
						}
						SpleefGame game = sp.getGame();
						for (UUID uuid : sp.getTeam().getPlayers()) {
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
							if (p.isOnline()) {
								SpleefPlayer sp2 = main.getDataLoader().getSpleefPlayer(p);
								if (sp2 != null) {
									if (sp2.isDead()) {
										count--;
										playersAlive--;
									}
								}
							}
							// if (!p.getPlayer().isOnline()) {
							// count--;
							// playersAlive--;
							// sp2.setDead(true);
							// }
						}
						if (count == 1) {
							sp.setDead(true);
							sp.setAcceptedReset(false);
							sp.getPlayer().getInventory().clear();
							count = game.getTeamSpleefTeams().size();
							for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
								if (team.isDead()) {
									count--;
								}
							}
							if (count == 1) {
								String msg = "";
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									if (!team.isDead()) {
										team.addPoint();
										msg += game.getChatColor(team.getColor()) + "§l"
												+ game.capitalize(team.getColor()) + " team §r§ahas won the round!";
									}
									if (team.getPoints() == sp.getGame().getPlaytoScore()) {
										b = true;
										team.setWinner(true);
									}
								}
								for (SpleefPlayer p : game.getPlayers()) {
									p.setDead(false);
								}
								for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
									game.setTeamSpleefTeamDead(color, false);
								}
								if (b == false)
									if (game.getTeamGame() == true) {

										game.startTeamRound();

										for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
											if (sp2.getPlayer() != null)
												sp2.getPlayer().sendMessage(msg);
										}
										for (SpleefSpectator ss : main.getDataLoader()
												.getSpectatorsForGame(sp.getGame())) {
											if (Bukkit.getPlayer(ss.getUUID()) != null)
												Bukkit.getPlayer(ss.getUUID()).sendMessage(msg);
										}
									} else {
										game.startRound();
									}
								if (b == true) {
									if (game.getTeamGame() == true) {
										game.endTeamGame();
									} else {
										game.endGame();
									}
								}
								b = false;
							} else {
								game.setTeamSpleefTeamDead(color, true);
								sp.setSpectating(true);

								sp.getPlayer().setGameMode(GameMode.SPECTATOR);

								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.getPlayer() != null) {
										sp2.getPlayer()
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");
										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()) != null) {
										Bukkit.getPlayer(ss.getUUID())
												.sendMessage("§r§f" + sp.getPlayer().getName() + " §r§7has fallen! "
														+ game.getChatColor(sp.getTeam().getColor()) + "§l"
														+ game.capitalize(sp.getTeam().getColor())
														+ " team §r§7is now eliminated.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						} else {
							if (sp.isOnline()) {
								sp.setDead(true);
								sp.setAcceptedReset(false);
								sp.getPlayer().getInventory().clear();
								playersAlive--;
								sp.setSpectating(true);
								sp.getPlayer().setGameMode(GameMode.SPECTATOR);
								for (PotionEffect effect : player.getActivePotionEffects()) {
									if (effect.getType() != PotionEffectType.NIGHT_VISION) {
										player.removePotionEffect(effect.getType());
									}
								}
								for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
									if (sp2.isOnline()) {
										sp2.getPlayer().sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										if (sp2.getTeam().getPlayers().contains(sp.getPlayer().getUniqueId())) {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
										} else {
											sp2.getPlayer().playSound(sp2.getPlayer().getLocation(),
													Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
										}
									}
								}
								for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(sp.getGame())) {
									if (Bukkit.getPlayer(ss.getUUID()).isOnline()) {
										Bukkit.getPlayer(ss.getUUID()).sendMessage("§r§f" + sp.getPlayer().getName()
												+ " §r§7has fallen! " + game.getChatColor(sp.getTeam().getColor())
												+ "§l" + game.capitalize(sp.getTeam().getColor())
												+ " team §r§7now has §r§7" + playersAlive + " §r§7players left.");
										Bukkit.getPlayer(ss.getUUID()).playSound(
												Bukkit.getPlayer(ss.getUUID()).getLocation(), Sound.ENTITY_VILLAGER_NO,
												1.0f, 1.0f);
									}
								}
							}
						}

					}
				}
			}
		}
		if (sp.getInParkourGame() && player.getWorld().getName().equals("voidworld")
				&& sp.getPlayer().getGameMode() == GameMode.SURVIVAL) {

		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		event.setCancelled(true);
		event.getPlayer().performCommand("spawn");
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		Player p = event.getPlayer();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
		Location teleportLocation = new Location(Bukkit.getWorld("lobby"), 8431.5, 201, 8849.5);
		teleportLocation.setYaw(0f);
		p.getPlayer().performCommand("spawn");
		p.getPlayer().getInventory().clear();
		if (sp == null) {
			if (main.getDataLoader().getTrainingSession(uuid) != null)
				main.getDataLoader().clearTraining(uuid);

			ItemStack stats = new ItemStack(Material.MACE);
			ItemMeta statsMeta = stats.getItemMeta();

			statsMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Your Stats");

			List<String> statsLore = new ArrayList<>();
			statsLore.add(ChatColor.DARK_GRAY + "────────────────────");
			statsLore.add(ChatColor.GRAY + "View your stats");
			statsLore.add(ChatColor.GRAY + "for all gamemodes.");
			statsLore.add("");
			statsLore.add(ChatColor.GREEN + "▶ Click to open stats menu");
			statsLore.add(ChatColor.DARK_GRAY + "────────────────────");

			statsMeta.setLore(statsLore);

			statsMeta.addEnchant(Enchantment.EFFICIENCY, 1, true);
			statsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

			statsMeta.setUnbreakable(true);

			stats.setItemMeta(statsMeta);
			p.getInventory().setItem(7, stats);

			ItemStack item = new ItemStack(Material.NETHER_STAR);
			ItemMeta meta = item.getItemMeta();

			meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Powers");

			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.DARK_GRAY + "────────────────────");
			lore.add(ChatColor.GRAY + "Choose your abilities");
			lore.add(ChatColor.GRAY + "before the game begins.");
			lore.add("");
			lore.add(ChatColor.GREEN + "▶ Click to open menu");
			lore.add(ChatColor.DARK_GRAY + "────────────────────");

			meta.setLore(lore);

			meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

			meta.setUnbreakable(true);

			item.setItemMeta(meta);
			p.getInventory().setItem(8, item);
		} else if (sp.getGame() == null && main.getDataLoader().getTrainingSession(uuid) == null) {
			if (main.getDataLoader().getTrainingSession(uuid) != null)
				main.getDataLoader().clearTraining(uuid);

			ItemStack stats = new ItemStack(Material.MACE);
			ItemMeta statsMeta = stats.getItemMeta();

			statsMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Your Stats");

			List<String> statsLore = new ArrayList<>();
			statsLore.add(ChatColor.DARK_GRAY + "────────────────────");
			statsLore.add(ChatColor.GRAY + "View your stats");
			statsLore.add(ChatColor.GRAY + "for all gamemodes.");
			statsLore.add("");
			statsLore.add(ChatColor.GREEN + "▶ Click to open stats menu");
			statsLore.add(ChatColor.DARK_GRAY + "────────────────────");

			statsMeta.setLore(statsLore);

			statsMeta.addEnchant(Enchantment.EFFICIENCY, 1, true);
			statsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

			statsMeta.setUnbreakable(true);

			stats.setItemMeta(statsMeta);
			p.getInventory().setItem(7, stats);

			ItemStack item = new ItemStack(Material.NETHER_STAR);
			ItemMeta meta = item.getItemMeta();

			meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Select Powers");

			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.DARK_GRAY + "────────────────────");
			lore.add(ChatColor.GRAY + "Choose your abilities");
			lore.add(ChatColor.GRAY + "before the game begins.");
			lore.add("");
			lore.add(ChatColor.GREEN + "▶ Click to open menu");
			lore.add(ChatColor.DARK_GRAY + "────────────────────");

			meta.setLore(lore);

			meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

			meta.setUnbreakable(true);

			item.setItemMeta(meta);
			p.getInventory().setItem(8, item);
		}
		if (sp != null) {
			main.getDataLoader().setRecallActive(sp.getPlayer().getUniqueId(), false);
			Bukkit.getLogger().info("1234235");
			if (sp.getInGame() || sp.getInTeamGame()) {
				Bukkit.getLogger().info("25234235345");
				if (sp.getGame().getPlayers().size() < 3) {
					Bukkit.getLogger().info("33425234523");
					if (sp != null && sp.getPlayer() != null && sp.getPlayer().isOnline()) {
						sp.setHasBeenRequested(false);
						sp.setPoints(0);
						p.getPlayer().performCommand("spawn");
						sp.setInTeamGame(false);
						sp.setSpectating(false);
						sp.setGame(null);
						sp.getPlayer().setGameMode(GameMode.SURVIVAL);
						sp.setDead(false);
					}
					if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
						main.getDataLoader().removeAllIceSpawned(sp.getUUID());
					}
					if (main.getDataLoader().getDecaySpawned(sp.getUUID()) != null) {
						main.getDataLoader().removeAllDecaySpawned(sp.getUUID());
					}
				}
				if (sp.getGame() != null) {
					if (sp.getGame().isSplegg()) {
						teleportLocation = sp.getGame().getSpleggRankedArena().getPasteOrigin().clone();
						teleportLocation.setY(teleportLocation.getY() + 5);
						p.getPlayer().teleport(teleportLocation);
						Bukkit.getScheduler().runTaskLater(main, () -> {
							p.setGameMode(GameMode.SPECTATOR);
						}, 10L);
					} else {
						if (sp.getGame().getArena().getBorder().getY() == 0) {
							teleportLocation = sp.getGame().getArena().getCenter();
							p.getPlayer().teleport(teleportLocation);
							p.setGameMode(GameMode.SPECTATOR);
						} else {
							teleportLocation = sp.getGame().getArena().getBorder();
							p.getPlayer().teleport(teleportLocation);
							sp.setDead(true);
						}
					}
				} else {
					sp.setHasBeenRequested(false);
					sp.setInGame(false);
					sp.setInTeamGame(false);

				}
			}
		}
		if (p.getGameMode() == GameMode.SPECTATOR) {
			p.setGameMode(GameMode.SURVIVAL);
		}
	}

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		main.getDuelManager().handleLeave(event.getPlayer().getUniqueId());
		UUID uuid = event.getPlayer().getUniqueId();
		Player p = event.getPlayer();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
		if (main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator())) {
			Bukkit.getConsoleSender().sendMessage("Player registered as a spectator, and they left.");
			main.getDataLoader().getSpectatorsForGame(sp.getGame()).remove(sp.getSpectator());
			sp.getSpectator().setGame(null);
			sp.getSpectator().setInGame(false);
			sp.getSpectator().setInTeamGame(false);
			sp.setGame(null);
			return;
		}
		SpleefGame game = sp.getGame();
		if (sp.getGame() != null && sp.getGame().isBanning()
				&& (sp.getGame().isInPowerPhase() || sp.getGame().isInBanningPhase())) {
			sp.getGame().cancelPregame(p.getName() + " left.");
			sp.getGame().getArea().setInGame(false);
			for (TeamSpleefTeam team : sp.getGame().getTeamSpleefTeams()) {
				for (UUID p2 : team.getPlayers()) {
					Player online = Bukkit.getPlayer(p2);
					if (online != null) {
						Location teleportLocation = new Location(Bukkit.getWorld("lobby"), 9012.5, 194, 8287.5);
						teleportLocation.setYaw(180f);
						online.teleport(teleportLocation);
						online.getInventory().clear();
						sp.setGame(null);
					}
				}
			}
		}
		Bukkit.getScheduler().runTaskLater(main, () -> {
			if (sp.getInGame() || sp.getInTeamGame()) {
				int totalPlayers = game.getPlayers().size();
				Bukkit.getLogger().info(totalPlayers + "");
				sp.setDead(true);
				if (totalPlayers > 2) {
					if (game.getTeamGame()) {
						for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
							if (team.getPlayers().contains(p.getUniqueId())) {
								int count = 0;
								int playersOffline = 0;
								for (UUID uuid2 : team.getPlayers()) {
									Player p3 = Bukkit.getPlayer(uuid2);
									if (p3 == null || !p3.isOnline()) {
										playersOffline++;
									}
									count++;
								}
								if (count == playersOffline) {
									team.setOffline(true);
								}
							}
						}
						int count = 0;
						int teamsOffline = 0;
						for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
							if (team.isOffline()) {
								teamsOffline++;
							}
							count++;
						}
						count -= 1;
						if (count == teamsOffline) {
							game.endTeamGame();
						} else {
							for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
								if (team.getPlayers().contains(p.getUniqueId())) {
									count = 0;
									int spectators = 0;
									int playersOffline = 0;
									for (UUID uuid2 : team.getPlayers()) {
										Player p3 = Bukkit.getPlayer(uuid2);
										if (p3 == null || !p3.isOnline()) {
											playersOffline++;
										} else if (main.getDataLoader().getSpleefPlayer(p3).isSpectating()) {
											spectators++;
										}
										count++;
									}
									if (count == (playersOffline + spectators)) {
										team.setDead(true);
										int teamsDead = 0;
										count = 0;
										for (TeamSpleefTeam team2 : game.getTeamSpleefTeams()) {
											if (team2.isDead()) {
												teamsDead++;
											}
											count++;
										}
										count--;
										if (count == teamsDead) {
											String msg = "";
											for (TeamSpleefTeam team3 : game.getTeamSpleefTeams()) {
												if (!team3.isDead()) {
													team3.addPoint();
													msg += game.getChatColor(team3.getColor()) + "§l"
															+ game.capitalize(team3.getColor())
															+ " team §r§ahas won the round!";
												}
												if (team3.getPoints() == sp.getGame().getPlaytoScore()) {
													b = true;
													team3.setWinner(true);
												}
											}
											for (SpleefPlayer p2 : game.getPlayers()) {
												if (p2.getPlayer() != null)
													if (p2.getPlayer().isOnline())
														p2.setDead(false);
											}
											for (TeamSpleefTeam team3 : game.getTeamSpleefTeams()) {
												game.setTeamSpleefTeamDead(team3.getColor(), false);
											}
											if (b == false)
												if (game.getTeamGame() == true) {
													game.startTeamRound();
													for (SpleefPlayer sp2 : sp.getGame().getPlayers()) {
														if (sp2.getPlayer() != null)
															sp2.getPlayer().sendMessage(msg);
													}
													for (SpleefSpectator ss : main.getDataLoader()
															.getSpectatorsForGame(sp.getGame())) {
														if (Bukkit.getPlayer(ss.getUUID()) != null)
															Bukkit.getPlayer(ss.getUUID()).getPlayer().sendMessage(msg);
													}
												} else {
													game.startRound();
												}
											if (b == true) {
												if (game.getTeamGame() == true) {
													game.endTeamGame();
												} else {
													game.endGame();
												}
											}
											b = false;
										}
									}
								}
							}
						}
					}
				} else {
					if (game.getTeamGame()) {
						game.endTeamGame();
					} else if (!game.getTeamGame()) {
						game.endGame();
					}
				}
			}
		}, 4L);
	}

	public boolean triggerRecall(Player player) {
		UUID uuid = player.getUniqueId();
		Location recallLocation = main.getDataLoader().getRecallLocations().remove(uuid);

		if (recallLocation == null) {
			return false;
		}

		main.getDataLoader().setRecallActive(uuid, false);

		player.setFallDistance(0.0f);
		player.setVelocity(new Vector(0, 0, 0));

		boolean teleported = player.teleport(recallLocation);

		if (!teleported) {
			return false;
		}

		player.removePotionEffect(PotionEffectType.SPEED);

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item_display,tag=Recall" + uuid + "]");

		return true;
	}
	
	/*
	 * @EventHandler public void onHit(ProjectileHitEvent e) { if (!(e.getEntity()
	 * instanceof Egg egg)) return;
	 * 
	 * Block hit = e.getHitBlock(); Location loc = egg.getLocation();
	 * egg.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); egg.remove();
	 * 
	 * // play sound at the impact spot loc.getWorld().playSound(loc,
	 * Sound.ENTITY_CHICKEN_EGG, 1f, 1f); // you can use any sound — examples: //
	 * Sound.BLOCK_SAND_BREAK // Sound.BLOCK_STONE_BREAK // Sound.ENTITY_EGG_THROW
	 * 
	 * if (hit == null) return;
	 * 
	 * hit.setType(Material.AIR, false); // break block without physics }
	 */

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEggHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof Egg egg))
			return;
		if (!(egg.getShooter() instanceof Player))
			return;

		// =========================
		// 1. PLAYER HIT (LEGS PASS-THROUGH)
		// =========================
		if (event.getHitEntity() instanceof Player hitPlayer) {
			if (egg.getLocation().getY() <= hitPlayer.getLocation().getY() + 1) {
				event.setCancelled(true);
			}
			return;
		}

		// =========================
		// 2. BLOCK HIT
		// =========================
		Block hit = event.getHitBlock();
		if (hit == null)
			return;

		Material type = hit.getType();

		// -------------------------
		// BARRIER → do nothing
		// -------------------------
		if (type == Material.BARRIER) {
			return;
		}

		Location center = hit.getLocation().add(0.5, 0.5, 0.5);
		World world = hit.getWorld();

		// =========================
		// 3. TNT EXPLOSION
		// =========================
		if (type == Material.TNT) {

			// remove TNT itself
			hit.setType(Material.AIR, false);

			double guaranteedRadius = 2.5; // always break -> 5 block diameter
			double maxRadius = 3.5; // possible break -> 7 block diameter total

			double guaranteedRadiusSquared = guaranteedRadius * guaranteedRadius;
			double maxRadiusSquared = maxRadius * maxRadius;

			world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
			world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

			int minX = (int) Math.floor(center.getX() - maxRadius);
			int maxX = (int) Math.floor(center.getX() + maxRadius);
			int minY = (int) Math.floor(center.getY() - maxRadius);
			int maxY = (int) Math.floor(center.getY() + maxRadius);
			int minZ = (int) Math.floor(center.getZ() - maxRadius);
			int maxZ = (int) Math.floor(center.getZ() + maxRadius);

			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {

						Block block = world.getBlockAt(x, y, z);

						// skip air + barriers
						if (block.getType() == Material.AIR || block.getType() == Material.BARRIER)
							continue;

						double dx = (x + 0.5) - center.getX();
						double dy = (y + 0.5) - center.getY();
						double dz = (z + 0.5) - center.getZ();

						double distanceSquared = (dx * dx + dy * dy + dz * dz);

						// outside full explosion radius
						if (distanceSquared > maxRadiusSquared)
							continue;

						// inner 5-diameter circle always breaks
						if (distanceSquared <= guaranteedRadiusSquared) {
							block.setType(Material.AIR, false);
							continue;
						}

						// outer ring (between 5 and 7 diameter) breaks 75% of the time
						if (Math.random() < 0.75) {
							block.setType(Material.AIR, false);
						}
					}
				}
			}

			egg.remove();
			return;
		}

		// =========================
		// 4. NORMAL BLOCK BREAK
		// =========================
		hit.setType(Material.AIR, false);

		main.getDataLoader().addBlockBroken(((Player) egg.getShooter()).getUniqueId());

		world.playSound(center, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);

		egg.remove();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onSandFall(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getTo() == Material.AIR) {
			event.setCancelled(true);
			Block block = event.getBlock();
			block.getState().update(false, false);
		}
	}

	@EventHandler

	public void eggHatch(PlayerEggThrowEvent e) {
		e.setHatching(false);
	}
}
//Bukkit.getLogger().info("");