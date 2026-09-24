package io.paper.permafrost.core.game.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleggQueueManager;
import io.paper.permafrost.managers.RankedSpleggManager;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import net.md_5.bungee.api.ChatColor;

public class FFAListener implements Listener {
	private Main main;

	public FFAListener(Main main) {
		this.main = main;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		SpleefQueueManager manager = main.getSpleefQueueManager();
		SpleefFFAGame game = manager.getAlivePlayerFFA(uuid);

		if (game == null) {
			return;
		}

		main.getDataLoader().addSpleefFFABlockBroken(uuid);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		SpleggQueueManager manager = main.getSpleggQueueManager();
		if (manager.getPlayersAliveInGame() != null) {
			if (manager.getPlayersAliveInGame().containsKey(uuid)) {
				if (manager.getPlayersAliveInGame().get(uuid)) {
					if (p.getLocation().getY() < (manager.getArena().getFallLevel() - 4) && !manager.isGameEnding()) {
						manager.setDead(uuid);
						Location strikeLoc = p.getLocation().clone();
						p.getWorld().strikeLightningEffect(strikeLoc);
						p.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1f);
						p.setGameMode(GameMode.SPECTATOR);
						if (manager.getPlayersRemaining() == 1 || manager.getPlayersRemaining() == 0) {
							manager.endFFAGame();
							for (UUID uu : manager.getPlayersAliveInGame().keySet()) {
								if (manager.getPlayersAliveInGame().get(uu)) {
									if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.CUSTOM)) {
										addPoints(main.getConnection(), uu, 20);
									} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.POLAR)) {
										addPoints(main.getConnection(), uu, 17);
									} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.GLACIER)) {
										addPoints(main.getConnection(), uu, 15);
									} else {
										addPoints(main.getConnection(), uu, 10);
									}
								}
							}
						} else {
							for (UUID uu : manager.getPlayersAliveInGame().keySet()) {
								String str = "";
								str = "§7" + Bukkit.getPlayer(uuid).getName() + " has fallen! " + "§e§l"
										+ manager.getPlayersRemaining() + " players remain. ";
								if (!uu.equals(uuid) && manager.getPlayersAliveInGame().get(uu)) {
									if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.CUSTOM)) {
										addPoints(main.getConnection(), uu, 20);
										str += "§f(+20 shells)";
									} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.POLAR)) {
										addPoints(main.getConnection(), uu, 17);
										str += "§f(+17 shells)";
									} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.GLACIER)) {
										addPoints(main.getConnection(), uu, 15);
										str += "§f(+15 shells)";
									} else {
										addPoints(main.getConnection(), uu, 10);
										str += "§f(+10 shells)";
									}
								}
								Bukkit.getPlayer(uu).sendMessage(str);

							}
						}
					}
				}
			}
		}

		SpleefQueueManager spleefManager = main.getSpleefQueueManager();
		SpleefFFAGame game = spleefManager.getAlivePlayerFFA(uuid);
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);

		if (game == null) {
			return;
		}

		if (!game.isInGame() || game.isGameEnding()) {
			return;
		}

		if (p.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		if (game.getMode() != null && game.getMode().equals(SpleefFFAMode.PROTOTYPE)) {
			if (!p.getWorld().equals(game.getProtoArena().getCenter().getWorld())) {
				return;
			}

			int minY = game.getProtoArena().getCenter().getBlockY() - 1;
			Location playerLoc = p.getLocation();

			if (playerLoc.getY() <= minY) {
				game.setDead(uuid);
				p.getWorld().playSound(playerLoc, Sound.ENTITY_VILLAGER_NO, 1.5f, 1f);
				game.addToDeathCount();
				p.setGameMode(GameMode.SPECTATOR);
				sendDeathRespawnTitle(p);
			}
		} else {
			if (!p.getWorld().equals(game.getArena().getCenter().getWorld())) {
				return;
			}

			int minY = game.getArena().getCorner1().getBlockY() - 1;

			Location playerLoc = p.getLocation();

			if (sp.getSelectedPower() != null && sp.getSelectedPower().equals(ShovelPowers.DOUBLEJUMP)) {
				minY--;
			}
			
			Bukkit.getLogger().info("[PowerSpleef Death] "
					+ p.getName()
					+ " aliveBefore=" + game.getPlayersAlive().contains(uuid)
					+ " spectatorsBefore=" + game.getPlayersSpectating().contains(uuid)
					+ " aliveCountBefore=" + game.getPlayersRemaining()
					+ " y=" + playerLoc.getY()
					+ " minY=" + minY);
			
			
			if (playerLoc.getY() <= minY) {
				game.setDead(uuid);
				p.getWorld().playSound(playerLoc, Sound.ENTITY_VILLAGER_NO, 1.5f, 1f);
				
				boolean died = game.isDead(uuid);

				Bukkit.getLogger().info("[PowerSpleef Death] "
						+ p.getName()
						+ " diedProcessed=" + died
						+ " aliveAfter=" + game.getPlayersAlive().contains(uuid)
						+ " spectatorsAfter=" + game.getPlayersSpectating().contains(uuid)
						+ " aliveCountAfter=" + game.getPlayersRemaining());

				if (!died) {
					return;
				}
				p.setGameMode(GameMode.SPECTATOR);

				int remaining = game.getPlayersRemaining();

				if (remaining <= 1) {
					for (UUID uu : new HashSet<>(game.getPlayersAlive())) {
						if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.CUSTOM)) {
							main.getDataLoader().addShards(uu, 20);
						} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.POLAR)) {
							main.getDataLoader().addShards(uu, 17);
						} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.GLACIER)) {
							main.getDataLoader().addShards(uu, 15);
						} else {
							main.getDataLoader().addShards(uu, 10);
						}
					}
					if (game.getInEvent()) {
						game.addPoints(uuid, 2);
						if (game.getPoints(uuid) >= game.getPlayToEventPoints()) {
							game.endFFAEvent();
							game.setPlayToEventPoints(0);
							game.setInEvent(false);
						}
					}
					game.setTopTwo(p.getUniqueId());
					game.endGame();
				} else {
					if (remaining == 2) {
						if (game.getInEvent()) {
							game.addPoints(uuid, 1);
							if (game.getPoints(uuid) >= game.getPlayToEventPoints()) {
								game.endFFAEvent();
								game.setPlayToEventPoints(0);
								game.setInEvent(false);
							}
						}
						game.setTopThree(p.getUniqueId());
					}

					for (UUID uu : new HashSet<>(game.getPlayersAlive())) {
						Player alive = Bukkit.getPlayer(uu);

						if (alive == null) {
							continue;
						}

						String str = "§7" + p.getName() + " has fallen! §e§l" + remaining + " players remain.";

						if (!uu.equals(uuid)) {
							if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.CUSTOM)) {
								main.getDataLoader().addShards(uu, 20);
								str += " §f(+20 shards)";
							} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.POLAR)) {
								main.getDataLoader().addShards(uu, 17);
								str += " §f(+17 shards)";
							} else if (main.getDataLoader().hasPurchasableRank(uu, PurchasableRank.GLACIER)) {
								main.getDataLoader().addShards(uu, 15);
								str += " §f(+15 shards)";
							} else {
								main.getDataLoader().addShards(uu, 10);
								str += " §f(+10 shards)";
							}
						}

						alive.sendMessage(str);
					}

					for (UUID uu : new HashSet<>(game.getPlayersSpectating())) {
						Player dead = Bukkit.getPlayer(uu);

						if (dead == null) {
							continue;
						}

						String str = "§7" + p.getName() + " has fallen! §e§l" + remaining + " players remain.";

						dead.sendMessage(str);
					}
				}
			}

		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		SpleggQueueManager manager = main.getSpleggQueueManager();
		RankedSpleggManager manager2 = main.getRankedSpleggManager();
		SpleefQueueManager manager3 = main.getSpleefQueueManager();
		if (manager.getPlayers().containsKey(uuid)) {
			manager.removePlayer(uuid);
		}
		if (manager.getPlayersAliveInGame().containsKey(uuid)) {
			manager.removePlayerFromGame(uuid);
		}
		SpleefFFAGame spleefGame = manager3.getPlayerFFA(uuid);

		if (spleefGame != null) {
			if (spleefGame.isAlive(uuid)) {
				spleefGame.setDead(uuid);
				int remaining = spleefGame.getPlayersRemaining();

				if (remaining == 2) {
					spleefGame.setTopThree(uuid);
				} else if (remaining == 1) {
					spleefGame.setTopTwo(uuid);
				}

				if (spleefGame.isInGame() && !spleefGame.isGameEnding() && remaining <= 1) {
					spleefGame.endGame();
				}
			}

			spleefGame.removePlayer(uuid);
		}
		if (main.getSpleggQueueManager().getPlayersRemaining() == 1
				|| main.getSpleggQueueManager().getPlayersRemaining() == 0) {
			main.getSpleggQueueManager().endFFAGame();
		}
		if (main.getSpleggQueueManager().getPlayersSpectating().contains(p.getUniqueId())) {
			main.getSpleggQueueManager().getPlayersSpectating().remove(p.getUniqueId());
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"hologram edit &6Splegg setline 4 &fPlayers: &e" + main.getSpleggQueueManager().getPlayerCount());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit &6Spleef setline 4 &fPlayers: &e"
				+ main.getSpleefQueueManager().getTotalPlayersInQueue());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSpleef setline 4 &fIn Queue: &e"
				+ main.getRankedSpleefManager().getTotalPlayersInQueue());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
				+ main.getRankedSpleggManager().getPlayersInQueue().size());
		if (manager2.getPlayersInQueue().contains(uuid)) {
			manager2.removePlayerFromQueue(uuid);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		SpleggQueueManager manager = main.getSpleggQueueManager();
		RankedSpleggManager manager2 = main.getRankedSpleggManager();
		main.getDataLoader().loadFavoritedArenas(p.getUniqueId());

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"hologram edit &6Splegg setline 4 &fPlayers: &e" + main.getSpleggQueueManager().getPlayerCount());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit &6Spleef setline 4 &fPlayers: &e"
				+ main.getSpleefQueueManager().getTotalPlayersInQueue());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSpleef setline 4 &fIn Queue: &e"
				+ main.getRankedSpleefManager().getTotalPlayersInQueue());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
				+ main.getRankedSpleggManager().getPlayersInQueue().size());
	}

	public void addPoints(Connection connection, UUID uuid, int amount) {
		String sql = "UPDATE splegg_ffa_points SET points = points + ? WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, amount);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createPlayerStatsIfNotExists(Connection connection, UUID uuid) {
		String uuidString = uuid.toString();

		String insertBlocks = "INSERT IGNORE INTO splegg_ffa_blocks_broken (uuid, blocks_broken) VALUES (?, 0)";
		String insertEggs = "INSERT IGNORE INTO splegg_ffa_eggs_fired (uuid, eggs_fired) VALUES (?, 0)";
		String insertWins = "INSERT IGNORE INTO splegg_ffa_wins (uuid, wins) VALUES (?, 0)";
		String insertPoints = "INSERT IGNORE INTO splegg_ffa_points (uuid, points) VALUES (?, 0)";

		try (PreparedStatement ps1 = connection.prepareStatement(insertBlocks);
				PreparedStatement ps2 = connection.prepareStatement(insertEggs);
				PreparedStatement ps3 = connection.prepareStatement(insertWins);
				PreparedStatement ps4 = connection.prepareStatement(insertPoints)) {
			ps1.setString(1, uuidString);
			ps2.setString(1, uuidString);
			ps3.setString(1, uuidString);
			ps4.setString(1, uuidString);

			ps1.executeUpdate();
			ps2.executeUpdate();
			ps3.executeUpdate();
			ps4.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void sendDeathRespawnTitle(Player player) {
		new BukkitRunnable() {
			int seconds = 3;
			SpleefQueueManager spleefManager = main.getSpleefQueueManager();
			SpleefFFAGame game = spleefManager.getPlayerFFA(player.getUniqueId());

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if (player == null || !player.isOnline()) {
					cancel();
					return;
				}

				if (seconds <= 0) {
					player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "RESPAWNED!",
							ChatColor.GRAY + "You are back in the game.", 0, 10, 5);
					player.teleport(getRespawnLocationNearCircle(game, player));
					game.setAlive(player.getUniqueId());
					player.setGameMode(GameMode.SURVIVAL);
					faceLocation(player, game.getProtoArena().getCenter());
					cancel();
					return;
				}
				player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "YOU ARE DEAD!",
						ChatColor.GRAY + "Respawning in " + ChatColor.YELLOW + seconds + ChatColor.GRAY + "...", 0, 25,
						0);

				seconds--;
			}
		}.runTaskTimer(main, 0L, 20L);
	}
	private Location getRespawnLocationNearCircle(SpleefFFAGame game, Player player) {
		List<Location> playerSpawns = new ArrayList<>(game.getProtoArena().getPlayerSpawns());

		if (playerSpawns.isEmpty()) {
			return game.getProtoArena().getCenter().clone();
		}

		Location targetCircle = getBestRespawnCircle(game, player);

		playerSpawns.sort(Comparator.comparingDouble(spawn -> distanceSquaredXZ(spawn, targetCircle)));

		int amount;

		if (isOuterEdgeCircle(targetCircle)) {
			amount = 3;
		} else {
			amount = 4;
		}

		int max = Math.min(amount, playerSpawns.size());
		List<Location> validSpawns = playerSpawns.subList(0, max);

		return validSpawns.get(ThreadLocalRandom.current().nextInt(validSpawns.size())).clone();
	}
	
	private Location getBestRespawnCircle(SpleefFFAGame game, Player player) {
		Location circle1 = game.getCircleLocation();
		Location circle2 = game.getCircleLocation2();

		if (circle1 == null) {
			circle1 = game.getProtoArena().getCenter();
		}

		if (circle2 == null) {
			return circle1;
		}

		double distToCircle1 = distanceSquaredXZ(player.getLocation(), circle1);
		double distToCircle2 = distanceSquaredXZ(player.getLocation(), circle2);

		return distToCircle2 < distToCircle1 ? circle2 : circle1;
	}

	private boolean isCenterCircle(Location loc, SpleefFFAGame game) {
		if (loc == null || game == null || game.getProtoArena() == null) {
			return false;
		}

		Location center = game.getProtoArena().getCenter();

		return loc.getWorld().equals(center.getWorld())
				&& loc.getBlockX() == center.getBlockX()
				&& loc.getBlockZ() == center.getBlockZ();
	}

	private boolean isOuterEdgeCircle(Location loc) {
		if (loc == null) {
			return false;
		}

		int x = loc.getBlockX();
		int z = loc.getBlockZ();

		return (x == 1036 && z == 0)
				|| (x == 1000 && z == 36)
				|| (x == 964 && z == 0)
				|| (x == 1000 && z == -36);
	}
	
	private List<Location> getClosestPlayerSpawns(Location circleLoc, int amount, SpleefFFAGame game) {
		List<Location> playerSpawns = new ArrayList<>(game.getProtoArena().getPlayerSpawns());

		playerSpawns.sort(Comparator.comparingDouble(spawn -> distanceSquaredXZ(spawn, circleLoc)));

		return playerSpawns.stream()
				.limit(amount)
				.map(Location::clone)
				.toList();
	}

	private double distanceSquaredXZ(Location a, Location b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();

		return dx * dx + dz * dz;
	}

	public void faceLocation(Player player, Location target) {
		Location playerLoc = player.getLocation();
		Location targetLoc = target.clone().add(0.5, 0.5, 0.5);

		double dx = targetLoc.getX() - playerLoc.getX();
		double dz = targetLoc.getZ() - playerLoc.getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

		playerLoc.setYaw(yaw);
		player.teleport(playerLoc);
	}

	public Location getSpawnLocation(Location center, double angleDegrees, double distance, float yawf, float pitchf) {
		double angleRadians = Math.toRadians(angleDegrees);
		double xOffset = distance * Math.cos(angleRadians);
		double zOffset = distance * Math.sin(angleRadians);
		Location cloneCenter = center.clone();
		cloneCenter.setYaw(yawf);
		cloneCenter.setPitch(pitchf);
		return cloneCenter.add(xOffset, 1, zOffset);
	}
}
