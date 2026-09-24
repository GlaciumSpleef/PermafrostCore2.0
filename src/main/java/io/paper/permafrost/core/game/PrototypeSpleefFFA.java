package io.paper.permafrost.core.game;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.SpleefFFAMode;
import net.md_5.bungee.api.ChatColor;

public class PrototypeSpleefFFA extends SpleefFFAGame implements Listener {

	private Map<UUID, BukkitRunnable> timers = new HashMap<>();

	private Location preLoc;

	private Location preLoc2;

	private final Map<UUID, Boolean> extraPointTimers = new HashMap<>();

	private Set<BukkitTask> tasks = new HashSet<>();

	public PrototypeSpleefFFA(Main main) {
		super(main, SpleefFFAMode.PROTOTYPE);
	}

	@Override
	public void startGame() {
		if (protoArena == null || protoArena.getCenter() == null) {
			return;
		}
		deathCount = 0;
		File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", protoArena.getSchematicPath());
		main.getFAWEAPI().pasteSchematic(schemFile, protoArena.getCenter(), false);

		List<UUID> onlinePlayers = new ArrayList<>();

		for (UUID uuid : new HashSet<>(getQueue())) {
			Player player = Bukkit.getPlayer(uuid);

			if (player == null || !player.isOnline()) {
				getQueue().remove(uuid);
				continue;
			}

			onlinePlayers.add(uuid);
		}

		Collections.shuffle(onlinePlayers);

		moveQueueToGame();
		getAlive().retainAll(onlinePlayers);

		int cnt = 1;
		for (UUID uuid : onlinePlayers) {
			Player player = Bukkit.getPlayer(uuid);
			player.setAllowFlight(false);
			if (player == null || !player.isOnline()) {
				getAlive().remove(uuid);
				continue;
			}

			switch (cnt) {
			case 1: {
				player.teleport(protoArena.getPlayerSpawns().get(0));
				break;
			}
			case 2: {
				player.teleport(protoArena.getPlayerSpawns().get(1));
				break;
			}
			case 3: {
				player.teleport(protoArena.getPlayerSpawns().get(2));
				break;
			}
			case 4: {
				player.teleport(protoArena.getPlayerSpawns().get(3));
				break;
			}
			}
			faceLocation(player, protoArena.getCenter());
			cnt++;
			if (cnt > 4) {
				cnt = 1;
			}
		}

		topThree = new ArrayList<>(3);
		Set<UUID> playersInFFA = new HashSet<>(getAlive());

		times = new HashMap<>();

		for (UUID uuid : playersInFFA) {
			times.put(uuid, 0.00);
		}

		new BukkitRunnable() {
			int seconds = 0;

			@Override
			public void run() {
				seconds++;

				if (seconds == 1) {
					broadcastGameMessage("&b&lSpleef FFA &8» &7Get ready...");
					sendGameTitle("&bSpleef FFA", "&7Match starting");
					playPlayersSound(playersInFFA, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
				}

				if (seconds == 2) {
					sendPlayersActionBar(playersInFFA, "&7You can break in &e3");
					sendPlayersTitle(playersInFFA, "&e3", "&7Prepare to fight", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.5f);
				}

				if (seconds == 3) {
					sendPlayersActionBar(playersInFFA, "&7You can break in &e2");
					sendPlayersTitle(playersInFFA, "&62", "&7Prepare to fight", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.6f);
				}

				if (seconds == 4) {
					sendPlayersActionBar(playersInFFA, "&7You can break in &c1");
					sendPlayersTitle(playersInFFA, "&c1", "&7Prepare to fight", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.7f);
				}

				if (seconds == 5) {
					sendPlayersTitle(playersInFFA, "&a&lGO!", "&7Start spleefing!", 0, 20, 10);
					sendPlayersActionBar(playersInFFA, "&a&lFight!");
					playPlayersSound(playersInFFA, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f, 1.2f);
					gameTasks();
					secondsTillGame = 0;
					startTimer(main);

					for (UUID uuid : new HashSet<>(getAlive())) {
						Player p = Bukkit.getPlayer(uuid);
						if (p == null || !p.isOnline()) {
							getAlive().remove(uuid);
							continue;
						}

						p.setGameMode(GameMode.SURVIVAL);
						PlayerInventory inventory = p.getInventory();
						inventory.clear();

						int modelData = main.getPlayerShovelModelData(p);

						ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
						ItemMeta meta = shovel.getItemMeta();

						if (meta != null) {
							meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
							meta.setUnbreakable(true);
							if (modelData > 0) {
								meta.setEnchantmentGlintOverride(false);
								meta.setCustomModelData(modelData);
							}
							shovel.setItemMeta(meta);
						}

						shovel.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						inventory.addItem(shovel);
					}

					gameStarting = false;
					cancel();
				}
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	@Override
	public void endGame() {
		if (gameEnding) {
			return;
		}

		for (BukkitTask task : tasks) {
			task.cancel();
		}

		tasks = new HashSet<>();

		for (BukkitRunnable timer : timers.values()) {
			if (timer != null) {
				timer.cancel();
			}
		}

		timers.clear();
		times.clear();

		times = new HashMap<>();
		deathCount = 0;
		circleLocation = null;
		circleLocation2 = null;
		gameEnding = true;

		final PrototypeArena endingArena = protoArena;

		shrunkLayers = 0;

		if (gameTask != null) {
			gameTask.cancel();
			gameTask = null;
		}

		Player winner = null;

		for (UUID uuid : new HashSet<>(getAlive())) {
			Player p = Bukkit.getPlayer(uuid);
			if (topThree.get(0) == uuid) {
				main.getDataLoader().addSpleefFFAWin(uuid);
				winner = p;
				p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
				if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.CUSTOM)) {
					p.sendTitle(color("&6&lVICTORY!"), color("&7+200 shards"), 10, 50, 15);
				} else if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.POLAR)) {
					p.sendTitle(color("&6&lVICTORY!"), color("&7+175 shards"), 10, 50, 15);
				} else if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.GLACIER)) {
					p.sendTitle(color("&6&lVICTORY!"), color("&7+150 shards"), 10, 50, 15);
				} else {
					p.sendTitle(color("&6&lVICTORY!"), color("&7+100 shards"), 10, 50, 15);
				}
				if (getInEvent()) {
					addPoints(uuid, 3);
				}
			}
		}

		announceTopThree();
		announceFFAWinner(winner);
		if (isInEvent) {
			if (showPoints) {
				announceAllPoints();
			} else {
				for (UUID uuid : points.keySet()) {
					Player player = Bukkit.getPlayer(uuid);

					if (player == null) {
						continue;
					}

					int playerPoints = points.getOrDefault(uuid, 0);
					player.sendMessage("§aYou currently have §e" + playerPoints + "§a points.");
				}
			}
			if (getPoints(winner.getUniqueId()) >= getPlayToEventPoints()) {
				endFFAEvent();
				setPlayToEventPoints(0);
				setInEvent(false);
			}
		}

		new BukkitRunnable() {
			int seconds = 0;

			@Override
			public void run() {
				seconds++;

				if (seconds == 5) {

					Set<UUID> participantsToReset = new HashSet<>();
					participantsToReset.addAll(getAlive());
					participantsToReset.addAll(getSpectators());

					for (UUID uuid : participantsToReset) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null && p.isOnline()) {
							resetPlayerToLobby(p, SpleefFFAMode.PROTOTYPE);
						}
					}

					getAlive().clear();
					getSpectators().clear();

					gameStarting = false;
					secondsTillGame = 0;

					stopTimer();

					if (protoArena == endingArena) {
						protoArena = null;
					}

					inGame = false;
					gameEnding = false;

					cancelCountdownTask();
					tryStartCountdown();

					cancel();
				}
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	private void resetPlayerToLobby(Player p, SpleefFFAMode modeToRequeue) {
		p.setGameMode(GameMode.SURVIVAL);
		p.getInventory().clear();

		ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
		ItemMeta leaveMeta = slimeball.getItemMeta();
		if (leaveMeta != null) {
			leaveMeta.addEnchant(Enchantment.EFFICIENCY, 5, true);
			leaveMeta.setUnbreakable(true);
			leaveMeta.setDisplayName(color("&cLeave"));
			leaveMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
			slimeball.setItemMeta(leaveMeta);
		}
		p.getInventory().setItem(8, slimeball);

		Location teleportLocation = new Location(Bukkit.getWorld("lobby"), 9012.5, 194, 8287.5);
		teleportLocation.setYaw(180f);
		p.teleport(teleportLocation);

		getQueue().add(p.getUniqueId());

		int waiting = getWaitingPlayers();
		p.sendMessage(
				color("&a&lQueue Joined &8» &7You are now in the Spleef FFA queue. &8(&e" + waiting + " waiting&8)"));
		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
	}

	private void gameTasks() {
		gameTask = new BukkitRunnable() {
			int seconds = 0;
			final Random random = new Random();

			@Override
			public void run() {
				if (!inGame || gameEnding || protoArena == null || protoArena.getCenter() == null) {
					cancel();
					return;
				}

				if (seconds % 20 == 15) {
					Set<Location> circleLocations = protoArena.getCircleSpawns();

					Location currentCircle = circleLocation == null ? protoArena.getCenter().clone().add(0, -1, 0)
							: circleLocation;

					List<Location> possibleLocations = getAdjacentCircleSpawns(currentCircle, circleLocations);

					if (possibleLocations.isEmpty()) {
						return;
					}

					preLoc = possibleLocations.get(random.nextInt(possibleLocations.size())).clone().add(0, -1, 0);

					spawnBlackOrb(main, preLoc.clone().add(0, 1.2, 0), 1.5, 3);

					if ((playersAlive.size() + playersSpectating.size()) >= 8) {
						Location currentCircle2 = circleLocation2 == null ? protoArena.getCenter().clone().add(0, -1, 0)
								: circleLocation2;

						possibleLocations = getAdjacentCircleSpawns(currentCircle2, circleLocations);

						if (preLoc != null) {
							possibleLocations.removeIf(loc -> sameCircleSpot(loc, preLoc));
						}

						if (possibleLocations.isEmpty()) {
							return;
						}

						preLoc2 = possibleLocations.get(random.nextInt(possibleLocations.size())).clone().add(0, -1, 0);

						spawnBlackOrb(main, preLoc2.clone().add(0, 1.2, 0), 1.5, 3);
					}
				}

				if (seconds % 20 == 0) {
					if (circleLocation == null) {
						circleLocation = protoArena.getCenter().clone().add(0, -1, 0);
					} else if (preLoc != null) {
						circleLocation = preLoc.clone();
						preLoc = null;
					}

					spawnCircleParticles(main, circleLocation.getBlock(), 6, 10, Particle.HAPPY_VILLAGER);
					spawnCircleParticles(main, circleLocation.getBlock(), 3, 10, Particle.PORTAL);

					if ((playersAlive.size() + playersSpectating.size()) >= 8 && preLoc2 != null) {
						circleLocation2 = preLoc2.clone();
						preLoc2 = null;

						spawnCircleParticles(main, circleLocation2.getBlock(), 6, 10, Particle.HAPPY_VILLAGER);
						spawnCircleParticles(main, circleLocation2.getBlock(), 3, 10, Particle.PORTAL);
					}
				}

				seconds++;
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	private List<Location> getAdjacentCircleSpawns(Location current, Collection<Location> allSpawns) {
		List<Location> adjacent = new ArrayList<>();

		for (Location loc : allSpawns) {
			if (sameCircleSpot(loc, current)) {
				continue;
			}

			if (isAdjacentCircleSpot(current, loc)) {
				adjacent.add(loc);
			}
		}

		return adjacent;
	}

	private boolean isAdjacentCircleSpot(Location current, Location candidate) {
		if (current == null || candidate == null) {
			return false;
		}

		int cx = current.getBlockX();
		int cz = current.getBlockZ();

		int x = candidate.getBlockX();
		int z = candidate.getBlockZ();

		if (cx == 1000 && cz == 0) {
			return isOuterCircleSpot(x, z);
		}

		if (x == 1000 && z == 0 && isOuterCircleSpot(cx, cz)) {
			return true;
		}

		return isRingNeighbor(cx, cz, x, z);
	}

	private boolean isOuterCircleSpot(int x, int z) {
		return (x == 1036 && z == 0) || (x == 1018 && z == 18) || (x == 1000 && z == 36) || (x == 982 && z == 18)
				|| (x == 964 && z == 0) || (x == 982 && z == -17) || (x == 1000 && z == -35) || (x == 1018 && z == -17);
	}

	private boolean isRingNeighbor(int cx, int cz, int x, int z) {
		return isSameXZ(cx, cz, 1036, 0) && (isSameXZ(x, z, 1018, 18) || isSameXZ(x, z, 1018, -17))
				|| isSameXZ(cx, cz, 1018, 18) && (isSameXZ(x, z, 1036, 0) || isSameXZ(x, z, 1000, 36))
				|| isSameXZ(cx, cz, 1000, 36) && (isSameXZ(x, z, 1018, 18) || isSameXZ(x, z, 982, 18))
				|| isSameXZ(cx, cz, 982, 18) && (isSameXZ(x, z, 1000, 36) || isSameXZ(x, z, 964, 0))
				|| isSameXZ(cx, cz, 964, 0) && (isSameXZ(x, z, 982, 18) || isSameXZ(x, z, 982, -17))
				|| isSameXZ(cx, cz, 982, -17) && (isSameXZ(x, z, 964, 0) || isSameXZ(x, z, 1000, -35))
				|| isSameXZ(cx, cz, 1000, -35) && (isSameXZ(x, z, 982, -17) || isSameXZ(x, z, 1018, -17))
				|| isSameXZ(cx, cz, 1018, -17) && (isSameXZ(x, z, 1000, -35) || isSameXZ(x, z, 1036, 0));
	}

	private boolean isSameXZ(int x1, int z1, int x2, int z2) {
		return x1 == x2 && z1 == z2;
	}

	private boolean sameCircleSpot(Location loc1, Location loc2) {
		if (loc1 == null || loc2 == null) {
			return false;
		}

		return loc1.getWorld().equals(loc2.getWorld()) && loc1.getBlockX() == loc2.getBlockX()
				&& loc1.getBlockZ() == loc2.getBlockZ();
	}

	public void spawnCircleParticles(Main plugin, Block centerBlock, double radius, double heightSeconds,
			Particle particle) {
		final World world = centerBlock.getWorld();
		final Location center = centerBlock.getLocation().add(0.5, 0.0, 0.5);

		final int period = 2;

		BukkitTask particleTask = new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if (tick++ >= (heightSeconds * 20)) {
					cancel();
					return;
				}
				if (gameEnding) {
					cancel();
					return;
				}

				for (int i = 0; i < 72; i++) {
					double theta = (2 * Math.PI) * i / 72;

					double x = center.getX() + Math.cos(theta) * radius;
					double z = center.getZ() + Math.sin(theta) * radius;
					Location p = new Location(world, x, center.getY() + 1, z);
					for (Player online : world.getPlayers()) {
						online.spawnParticle(particle, p, 1, 0.02, 0.02, 0.02, 0.0, null, true);
					}
				}
			}
		}.runTaskTimer(plugin, 0L, period);

		tasks.add(particleTask);
	}

	public boolean isBlockInCircle(Block block, Location center, int radius) {
		if (block == null || center == null || center.getWorld() == null) {
			return false;
		}

		if (!block.getWorld().equals(center.getWorld())) {
			return false;
		}

		int dx = block.getX() - center.getBlockX();
		int dz = block.getZ() - center.getBlockZ();

		return dx * dx + dz * dz <= radius * radius;
	}

	public void spawnBlackOrb(Main plugin, Location center, double radius, double seconds) {
		World world = center.getWorld();
		for (UUID uuid : playersAlive) {
			Player player = Bukkit.getPlayer(uuid);
			player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.6f);
		}
		for (UUID uuid : playersSpectating) {
			Player player = Bukkit.getPlayer(uuid);
			player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.6f);
		}
		if (world == null) {
			return;
		}

		new BukkitRunnable() {
			int ticks = 0;
			final int maxTicks = 5 * 20;

			@Override
			public void run() {
				if (ticks >= maxTicks) {
					cancel();
					return;
				}

				for (int i = 0; i < 80; i++) {
					double theta = Math.random() * 2 * Math.PI;
					double phi = Math.acos(2 * Math.random() - 1);

					double x = center.getX() + radius * Math.sin(phi) * Math.cos(theta);
					double y = center.getY() + radius * Math.cos(phi);
					double z = center.getZ() + radius * Math.sin(phi) * Math.sin(theta);

					Location particleLoc = new Location(world, x, y, z);

					for (Player player : world.getPlayers()) {
						player.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
								new Particle.DustOptions(Color.BLACK, 1.5F), true);
					}
				}

				for (Player player : world.getPlayers()) {
					player.spawnParticle(Particle.SMOKE, center, 8, radius * 0.3, radius * 0.3, radius * 0.3, 0.01,
							null, true);
				}

				ticks += 2;
			}
		}.runTaskTimer(plugin, 0L, 2L);
	}

	public void startPlayerTimer(JavaPlugin plugin, UUID uuid, boolean extra) {
		if (timers.containsKey(uuid)) {
			return;
		}

		times.putIfAbsent(uuid, 0.0);
		extraPointTimers.put(uuid, extra);

		BukkitRunnable task = new BukkitRunnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);

				if (player == null || !player.isOnline() || !getAlive().contains(uuid)) {
					stopPlayerTimer(uuid);
					cancel();
					return;
				}

				Block blockUnderPlayer = player.getLocation().clone().add(0, -1, 0).getBlock();

				boolean inInnerCircle = false;
				boolean inOuterCircle = false;

				if (circleLocation != null) {
					if (isBlockInCircle(blockUnderPlayer, circleLocation, 3)) {
						inInnerCircle = true;
					} else if (isBlockInCircle(blockUnderPlayer, circleLocation, 6)) {
						inOuterCircle = true;
					}
				}

				if ((playersAlive.size() + playersSpectating.size()) >= 8 && circleLocation2 != null) {
					if (isBlockInCircle(blockUnderPlayer, circleLocation2, 3)) {
						inInnerCircle = true;
					} else if (isBlockInCircle(blockUnderPlayer, circleLocation2, 6)) {
						inOuterCircle = true;
					}
				}

				if (!inInnerCircle && !inOuterCircle) {
					stopPlayerTimer(uuid);
					cancel();
					return;
				}

				extraPointTimers.put(uuid, inInnerCircle);

				double seconds = times.getOrDefault(uuid, 0.0);

				if (inInnerCircle) {
					seconds += 2.0 / 20.0;
				} else {
					seconds += 1.0 / 20.0;
				}

				times.put(uuid, seconds);

				if (times.get(uuid) >= 90) {
					SpleefFFAGame game = main.getSpleefQueueManager().getAlivePlayerFFA(uuid);

					if (game != null) {
						List<Map.Entry<UUID, Double>> sortedTimes = new ArrayList<>(times.entrySet());

						sortedTimes.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

						if (sortedTimes.size() > 0) {
							game.setTopOne(sortedTimes.get(0).getKey());
						}

						if (sortedTimes.size() > 1) {
							game.setTopTwo(sortedTimes.get(1).getKey());
						}

						if (sortedTimes.size() > 2) {
							game.setTopThree(sortedTimes.get(2).getKey());
						}
					}

					endGame();
				}
			}
		};

		timers.put(uuid, task);
		task.runTaskTimer(plugin, 0L, 1L);
	}

	public void stopPlayerTimer(UUID uuid) {
		BukkitRunnable task = timers.remove(uuid);

		if (task != null) {
			task.cancel();
		}

		extraPointTimers.remove(uuid);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();

		if (!(main.getSpleefQueueManager().isAliveInAnyFFA(event.getPlayer().getUniqueId()) && main
				.getSpleefQueueManager().getFFAMode(event.getPlayer().getUniqueId()).equals(SpleefFFAMode.PROTOTYPE))) {
			return;
		}

		if (!inGame || gameEnding || gameStarting) {
			return;
		}

		if (protoArena.getCenter() == null) {
			return;
		}

		Block blockUnderPlayer = p.getLocation().clone().add(0, -1, 0).getBlock();

		boolean inInnerCircle = false;
		boolean inOuterCircle = false;

		if (circleLocation != null) {
			if (isBlockInCircle(blockUnderPlayer, circleLocation, 3)) {
				inInnerCircle = true;
			} else if (isBlockInCircle(blockUnderPlayer, circleLocation, 6)) {
				inOuterCircle = true;
			}
		}

		if ((playersAlive.size() + playersSpectating.size()) >= 8 && circleLocation2 != null) {
			if (isBlockInCircle(blockUnderPlayer, circleLocation2, 3)) {
				inInnerCircle = true;
			} else if (isBlockInCircle(blockUnderPlayer, circleLocation2, 6)) {
				inOuterCircle = true;
			}
		}

		if (inInnerCircle) {
			extraPointTimers.put(uuid, true);

			if (!timers.containsKey(uuid)) {
				startPlayerTimer(main, uuid, true);
			}
		} else if (inOuterCircle) {
			extraPointTimers.put(uuid, false);

			if (!timers.containsKey(uuid)) {
				startPlayerTimer(main, uuid, false);
			}
		} else {
			extraPointTimers.remove(uuid);

			if (timers.containsKey(uuid)) {
				stopPlayerTimer(uuid);
			}
		}

		if (inInnerCircle) {
			if (!timers.containsKey(uuid)) {
				startPlayerTimer(main, uuid, true);
			}
		} else if (inOuterCircle) {
			if (!timers.containsKey(uuid)) {
				startPlayerTimer(main, uuid, false);
			}
		} else {
			if (timers.containsKey(uuid)) {
				stopPlayerTimer(uuid);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!(main.getSpleefQueueManager().isAliveInAnyFFA(event.getPlayer().getUniqueId()) && main
				.getSpleefQueueManager().getFFAMode(event.getPlayer().getUniqueId()).equals(SpleefFFAMode.PROTOTYPE)))
			return;

		Block block = event.getBlock();

		if (!block.getType().equals(Material.SNOW_BLOCK) && !block.getType().equals(Material.GREEN_CONCRETE_POWDER)) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				block.setType(Material.GREEN_CONCRETE_POWDER);
			}
		}.runTaskLater(main, 180L);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (block.getType() != Material.AIR)
					block.setType(Material.SNOW_BLOCK);
			}
		}.runTaskLater(main, 200L);
	}
}
