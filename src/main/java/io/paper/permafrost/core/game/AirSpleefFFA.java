package io.paper.permafrost.core.game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import github.scarsz.discordsrv.DiscordSRV;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.SpleefFFAMode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class AirSpleefFFA extends SpleefFFAGame {

	public AirSpleefFFA(Main main) {
		super(main, SpleefFFAMode.AIRSPLEEF);
	}

	@Override
	public void startGame() {
		if (arena == null || arena.getCenter() == null) {
			return;
		}

		Location center = arena.getCenter().clone();

		corner1 = arena.getCorner1().clone();
		corner2 = arena.getCorner2().clone();

		radius = 10;
		shrunkLayers = 0;

		int startingPlayerCount = getQueue().size();

		if (startingPlayerCount > 4) {
			int extraRadius = ((int) Math.floor((startingPlayerCount - 4) / 2.0)) * 2;
			double delta = (Math.floor((startingPlayerCount - 4) / 2.0)) * 2;

			radius = 10 + extraRadius;

			Bukkit.getLogger().info("count >4 extraRadius=" + extraRadius + " radius=" + radius);

			corner1.setX(corner1.getX() - delta);
			corner1.setZ(corner1.getZ() + delta);
			corner2.setX(corner2.getX() + delta);
			corner2.setZ(corner2.getZ() - delta);
		}

		main.getFAWEAPI().createFlatSnowCircle(center, radius, Material.SNOW_BLOCK);

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

		int cnt = 0;
		int teamId = 1;

		for (UUID uuid : onlinePlayers) {
			Player player = Bukkit.getPlayer(uuid);
			player.setAllowFlight(false);

			if (player == null || !player.isOnline()) {
				getAlive().remove(uuid);
				cnt++;
				teamId++;
				continue;
			}

			float yaw = cnt == 0 ? 90f : 270f;

			Location playerSpawn = getSpawnLocation(arena.getCenter(),
					-180 + (360.0 / Math.max(1, onlinePlayers.size())) * teamId, radius - 1, yaw, 0f);

			playerSpawn.setX(Math.floor(playerSpawn.getX()) + 0.5);
			playerSpawn.setZ(Math.floor(playerSpawn.getZ()) + 0.5);

			player.teleport(playerSpawn);
			faceLocation(player, arena.getCenter());

			cnt++;
			teamId++;
		}

		topThree = new ArrayList<>(3);
		Set<UUID> playersInFFA = new HashSet<>(getAlive());

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

	public void endGame() {
		if (gameEnding) {
			return;
		}

		gameEnding = true;

		final Arena endingArena = arena;
		final int endingRadius = radius;

		shrunkLayers = 0;

		if (gameTask != null) {
			gameTask.cancel();
			gameTask = null;
		}

		cancelPendingShrinkTasks();
		cancelPendingInnerDecayTasks();

		Player winner = null;

		for (UUID uuid : new HashSet<>(getAlive())) {
			main.getDataLoader().addSpleefFFAWin(uuid);

			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				winner = p;
				setTopOne(p.getUniqueId());
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
			}
			if (getInEvent()) {
				addPoints(uuid, 3);
			}
		}

		announceTopThree();
		announceFFAWinner(winner);
		if(isInEvent) {
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
			if(getPoints(winner.getUniqueId()) >= getPlayToEventPoints()) {
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
					if (endingArena != null && endingArena.getCenter() != null) {
						main.getFAWEAPI().createFlatSnowCircle(endingArena.getCenter().clone(), endingRadius,
								Material.AIR);
						endingArena.setInGame(false);
					}

					Set<UUID> participantsToReset = new HashSet<>();
					participantsToReset.addAll(getAlive());
					participantsToReset.addAll(getSpectators());

					for (UUID uuid : participantsToReset) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null && p.isOnline()) {
							resetPlayerToLobby(p, SpleefFFAMode.AIRSPLEEF);
						}
					}

					getAlive().clear();
					getSpectators().clear();

					gameStarting = false;
					secondsTillGame = 0;

					stopTimer();

					if (arena == endingArena) {
						arena = null;
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

			@Override
			public void run() {
				if (!inGame || gameEnding || arena == null || arena.getCenter() == null) {
					cancel();
					return;
				}

				if (seconds % 10 == 7) {
					sendCircleResetCountdown(3);
				}
				if (seconds % 10 == 8) {
					sendCircleResetCountdown(2);
				}
				if (seconds % 10 == 9) {
					sendCircleResetCountdown(1);
				}

				if (seconds % 7 == 0 && seconds <= 120) {
					makeIslandSpawn();
				}

				if (seconds % 12 == 0 && seconds > 120) {
					makeIslandSpawn();
				}

				if (seconds <= 120 && seconds % 10 == 0 && seconds != 0) {
					spawnRegenParticlesAroundCircle(arena.getCenter(), 5);
					cancelPendingInnerDecayTasks();
					main.getFAWEAPI().createFlatSnowCircle(arena.getCenter().clone(), 5, Material.SNOW_BLOCK);
				}

				if (seconds > 120 && seconds % 10 == 0 && seconds != 0) {
					spawnRegenParticlesAroundCircle(arena.getCenter(), 4);
					cancelPendingInnerDecayTasks();
					main.getFAWEAPI().createFlatSnowCircle(arena.getCenter().clone(), 4, Material.SNOW_BLOCK);
				}

				decayRandomBlockInCircle(5);

				int outerDecayRate = 3;

				for (int i = 0; i < outerDecayRate; i++) {
					decayRandomBlockNotInCircle(radius, 5);
				}

				int eliminatedPlayers = getSpectators().size();
				if (eliminatedPlayers % 2 == 0) {
					int layersToShrink = eliminatedPlayers / 2;
					int layersNeeded = layersToShrink - shrunkLayers;

					if (layersNeeded > 0) {
						shrinkCircularArenaAnimated(layersNeeded);
						shrunkLayers += layersNeeded;
					}
				}

				seconds++;
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	private void sendCircleResetCountdown(int number) {
		for (UUID uuid : new HashSet<>(getAlive())) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) {
				continue;
			}

			player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
					net.md_5.bungee.api.chat.TextComponent
							.fromLegacyText(ChatColor.AQUA + "" + ChatColor.BOLD + number));

			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
		}
	}

	public void spawnIceBreakParticlesAroundPlayers() {
		for (UUID uuid : new HashSet<>(getAlive())) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null) {
				continue;
			}

			Location loc = p.getLocation();

			loc.getWorld().spawnParticle(Particle.BLOCK, loc, 5, 0.25, 0.15, 0.25, 0.02,
					Material.BLUE_ICE.createBlockData());

			p.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
		}
	}

	private void spawnRegenParticlesAroundCircle(Location center, int radius) {
		World world = center.getWorld();
		if (world == null) {
			return;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		int outer = radius * radius;
		int inner = Math.max(0, radius - 1) * Math.max(0, radius - 1);

		for (int x = centerX - radius; x <= centerX + radius; x++) {
			for (int z = centerZ - radius; z <= centerZ + radius; z++) {
				int dx = x - centerX;
				int dz = z - centerZ;
				int distSquared = dx * dx + dz * dz;

				if (distSquared <= outer && distSquared >= inner) {
					Location particleLoc = new Location(world, x + 0.5, centerY + 1.1, z + 0.5);

					world.spawnParticle(Particle.BLOCK, particleLoc, 5, 0.25, 0.15, 0.25, 0.02,
							Material.BLUE_ICE.createBlockData());
				}
			}
		}
	}

	private void decayRandomBlockInCircle(int radius) {
		if (arena == null || arena.getCenter() == null) {
			return;
		}

		Location center = arena.getCenter();
		World world = center.getWorld();
		if (world == null) {
			return;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		int radiusSquared = radius * radius;

		for (int attempts = 0; attempts < 60; attempts++) {
			int x = ThreadLocalRandom.current().nextInt(centerX - radius, centerX + radius + 1);
			int z = ThreadLocalRandom.current().nextInt(centerZ - radius, centerZ + radius + 1);

			int dx = x - centerX;
			int dz = z - centerZ;

			if (dx * dx + dz * dz > radiusSquared) {
				continue;
			}

			Block block = world.getBlockAt(x, centerY, z);

			if (block.getType() != Material.SNOW_BLOCK) {
				continue;
			}

			block.setType(Material.ORANGE_CONCRETE_POWDER);

			BukkitTask redTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (block.getType() == Material.ORANGE_CONCRETE_POWDER) {
						block.setType(Material.RED_CONCRETE_POWDER);
					}
				}
			}.runTaskLater(main, 20L);

			innerDecayTasks.add(redTask);

			BukkitTask airTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (block.getType() == Material.RED_CONCRETE_POWDER
							|| block.getType() == Material.ORANGE_CONCRETE_POWDER
							|| block.getType() == Material.SNOW_BLOCK) {
						block.setType(Material.AIR);
					}
				}
			}.runTaskLater(main, 40L);

			innerDecayTasks.add(airTask);
			return;
		}
	}

	private void decayRandomBlockNotInCircle(int outerRadius, int radiusToIgnore) {
		if (arena == null || arena.getCenter() == null) {
			return;
		}
		outerRadius++;

		Location center = arena.getCenter();
		World world = center.getWorld();

		if (world == null) {
			return;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		int outerSquared = outerRadius * outerRadius;
		int ignoreSquared = radiusToIgnore * radiusToIgnore;

		List<Block> possibleBlocks = new ArrayList<>();

		for (int x = centerX - outerRadius; x <= centerX + outerRadius; x++) {
			for (int z = centerZ - outerRadius; z <= centerZ + outerRadius; z++) {
				int dx = x - centerX;
				int dz = z - centerZ;
				int distSquared = dx * dx + dz * dz;

				if (distSquared > outerSquared || distSquared <= ignoreSquared) {
					continue;
				}

				Block block = world.getBlockAt(x, centerY, z);

				if (block.getType() == Material.SNOW_BLOCK) {
					possibleBlocks.add(block);
				}
			}
		}

		if (possibleBlocks.isEmpty()) {
			return;
		}

		possibleBlocks.sort((a, b) -> {
			int adx = a.getX() - centerX;
			int adz = a.getZ() - centerZ;
			int bdx = b.getX() - centerX;
			int bdz = b.getZ() - centerZ;

			int aDist = adx * adx + adz * adz;
			int bDist = bdx * bdx + bdz * bdz;

			return Integer.compare(bDist, aDist);
		});

		int topRange = Math.min(12, possibleBlocks.size());
		Block block = possibleBlocks.get(ThreadLocalRandom.current().nextInt(topRange));

		block.setType(Material.ORANGE_CONCRETE_POWDER);

		BukkitTask redTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!inGame || gameEnding || arena == null) {
					return;
				}

				if (block.getType() == Material.ORANGE_CONCRETE_POWDER) {
					block.setType(Material.RED_CONCRETE_POWDER);
				}
			}
		}.runTaskLater(main, 20L);

		activeShrinkTasks.add(redTask);

		BukkitTask airTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!inGame || gameEnding || arena == null) {
					return;
				}

				if (block.getType() == Material.RED_CONCRETE_POWDER
						|| block.getType() == Material.ORANGE_CONCRETE_POWDER
						|| block.getType() == Material.SNOW_BLOCK) {
					block.setType(Material.AIR);
				}
			}
		}.runTaskLater(main, 40L);

		activeShrinkTasks.add(airTask);
	}

	public boolean isOutsideRadius(Location center, Location check, int radius) {
		int dx = check.getBlockX() - center.getBlockX();
		int dz = check.getBlockZ() - center.getBlockZ();

		return dx * dx + dz * dz > radius * radius;
	}

	private void makeIslandSpawn() {
		if (arena == null || arena.getCenter() == null) {
			return;
		}

		Location arenaCenter = arena.getCenter();
		World world = arenaCenter.getWorld();

		if (world == null) {
			return;
		}

		int arenaRadius = radius - shrunkLayers;
		int innerCircleRadius = 5;

		for (UUID uuid : new HashSet<>(getAlive())) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) {
				continue;
			}

			for (int i = 0; i < 30; i++) {
				Location loc = getRandomBlockInCircle(player.getLocation(), 4);

				if (loc == null) {
					continue;
				}

				loc.setY(arenaCenter.getBlockY());

				if (isOutsideRadius(arenaCenter, loc, arenaRadius)) {
					continue;
				}

				if (isInsideRadius(arenaCenter, loc, innerCircleRadius)) {
					continue;
				}

				if (!canSpawn3x3Island(loc, arenaRadius)) {
					continue;
				}

				startIslandWarningThenSpawn(loc, arenaRadius);
				break;
			}
		}
	}

	private boolean isInsideRadius(Location center, Location check, int radius) {
		int dx = check.getBlockX() - center.getBlockX();
		int dz = check.getBlockZ() - center.getBlockZ();

		return dx * dx + dz * dz <= radius * radius;
	}

	private void startIslandWarningThenSpawn(Location center, int arenaRadius) {
		Location islandCenter = center.clone();

		BukkitTask warningTask = new BukkitRunnable() {
			int ticksPassed = 0;

			@Override
			public void run() {
				if (!inGame || gameEnding || arena == null || arena.getCenter() == null) {
					cancel();
					return;
				}

				if (ticksPassed >= 40) {
					cancel();
					return;
				}

				spawnIslandNotifyParticles(islandCenter);

				ticksPassed += 5;
			}
		}.runTaskTimer(main, 0L, 5L);

		activeShrinkTasks.add(warningTask);

		BukkitTask spawnTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!inGame || gameEnding || arena == null || arena.getCenter() == null) {
					return;
				}

				spawnSnowIsland3x3(islandCenter);
			}
		}.runTaskLater(main, 40L);

		activeShrinkTasks.add(spawnTask);
	}

	private void spawnIslandNotifyParticles(Location center) {
		World world = center.getWorld();
		if (world == null) {
			return;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		for (int x = centerX - 1; x <= centerX + 1; x++) {
			for (int z = centerZ - 1; z <= centerZ + 1; z++) {
				Location particleLoc = new Location(world, x + 0.5, centerY + 1.1, z + 0.5);

				world.spawnParticle(Particle.BLOCK, particleLoc, 10, 0.25, 0.15, 0.25, 0.02,
						Material.BLUE_ICE.createBlockData());
			}
		}
	}

	private boolean canSpawn3x3Island(Location center, int arenaRadius) {
		if (arena == null || arena.getCenter() == null) {
			return false;
		}

		World world = center.getWorld();
		if (world == null) {
			return false;
		}

		Location arenaCenter = arena.getCenter();

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		for (int x = centerX - 1; x <= centerX + 1; x++) {
			for (int z = centerZ - 1; z <= centerZ + 1; z++) {
				Location check = new Location(world, x, centerY, z);

				if (isOutsideRadius(arenaCenter, check, arenaRadius)) {
					return false;
				}
				if (world.getBlockAt(x, centerY, z).getType() != Material.AIR) {
					return false;
				}
			}
		}

		return true;
	}

	private void spawnSnowIsland3x3(Location center) {
		World world = center.getWorld();
		if (world == null) {
			return;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		for (int x = centerX - 1; x <= centerX + 1; x++) {
			for (int z = centerZ - 1; z <= centerZ + 1; z++) {
				world.getBlockAt(x, centerY, z).setType(Material.SNOW_BLOCK);
			}
		}
	}

	public Location getRandomBlockInCircle(Location center, int radius) {
		World world = center.getWorld();
		if (world == null) {
			return null;
		}

		int centerX = center.getBlockX();
		int centerY = center.getBlockY();
		int centerZ = center.getBlockZ();

		ThreadLocalRandom rand = ThreadLocalRandom.current();
		int radiusSquared = radius * radius;

		for (int attempts = 0; attempts < 100; attempts++) {
			int x = rand.nextInt(centerX - radius, centerX + radius + 1);
			int z = rand.nextInt(centerZ - radius, centerZ + radius + 1);

			int dx = x - centerX;
			int dz = z - centerZ;

			if (dx * dx + dz * dz <= radiusSquared) {
				return new Location(world, x, centerY, z);
			}
		}

		return null;
	}

	private void cancelPendingShrinkTasks() {
		for (BukkitTask task : activeShrinkTasks) {
			if (task != null) {
				task.cancel();
			}
		}
		activeShrinkTasks.clear();
	}

	private void cancelPendingInnerDecayTasks() {
		for (BukkitTask task : innerDecayTasks) {
			if (task != null) {
				task.cancel();
			}
		}
		innerDecayTasks.clear();
	}

	private void shrinkCircularArenaAnimated(int layers) {
		if (arena == null || arena.getCenter() == null) {
			return;
		}

		Location center = arena.getCenter();
		World world = center.getWorld();

		if (world == null) {
			return;
		}

		int y = arena.getCorner1().getBlockY();

		int startRadius = radius - shrunkLayers;

		for (int layer = 0; layer < layers; layer++) {
			int radiusToRemove = startRadius - layer;

			if (radiusToRemove <= 2) {
				return;
			}

			List<Location> outerLayer = getCircleOuterLayer(center, y, radiusToRemove);

			long delay = layer * 25L;

			BukkitTask orangeTask = new BukkitRunnable() {
				@Override
				public void run() {
					for (Location loc : outerLayer) {
						Block block = loc.getBlock();

						if (block.getType() != Material.AIR) {
							block.setType(Material.ORANGE_CONCRETE_POWDER);
						}
					}
				}
			}.runTaskLater(main, delay);

			BukkitTask redTask = new BukkitRunnable() {
				@Override
				public void run() {
					for (Location loc : outerLayer) {
						Block block = loc.getBlock();

						if (block.getType() == Material.ORANGE_CONCRETE_POWDER) {
							block.setType(Material.RED_CONCRETE_POWDER);
						}
					}
				}
			}.runTaskLater(main, delay + 10L);

			BukkitTask airTask = new BukkitRunnable() {
				@Override
				public void run() {
					for (Location loc : outerLayer) {
						loc.getBlock().setType(Material.AIR);
					}
				}
			}.runTaskLater(main, delay + 20L);

			activeShrinkTasks.add(orangeTask);
			activeShrinkTasks.add(redTask);
			activeShrinkTasks.add(airTask);
		}

		long cleanupDelay = layers * 25L + 25L;

		BukkitTask cleanupTask = new BukkitRunnable() {
			@Override
			public void run() {
				int finalRadius = startRadius - layers;

				if (finalRadius < 2) {
					finalRadius = 2;
				}
				removeBlocksOutsideCurrentRadius(center, y, finalRadius);
			}
		}.runTaskLater(main, cleanupDelay);

		activeShrinkTasks.add(cleanupTask);
	}

	private void removeBlocksOutsideCurrentRadius(Location center, int y, int currentRadius) {
		World world = center.getWorld();

		if (world == null) {
			return;
		}

		int cx = center.getBlockX();
		int cz = center.getBlockZ();

		int currentRadiusSquared = currentRadius * currentRadius;
		int maxRadiusSquared = radius * radius;

		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				int dx = x - cx;
				int dz = z - cz;
				int distSquared = dx * dx + dz * dz;

				if (distSquared <= currentRadiusSquared) {
					continue;
				}

				if (distSquared > maxRadiusSquared) {
					continue;
				}

				Block block = world.getBlockAt(x, y, z);

				if (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.ORANGE_CONCRETE_POWDER
						|| block.getType() == Material.RED_CONCRETE_POWDER) {
					block.setType(Material.AIR);
				}
			}
		}
	}

	private List<Location> getCircleOuterLayer(Location center, int y, int radius) {
		List<Location> blocks = new ArrayList<>();

		World world = center.getWorld();
		int cx = center.getBlockX();
		int cz = center.getBlockZ();

		int innerRadius = radius - 1;

		int outerSquared = radius * radius;
		int innerSquared = innerRadius * innerRadius;

		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				int dx = x - cx;
				int dz = z - cz;
				int distSquared = dx * dx + dz * dz;

				if (distSquared <= outerSquared && distSquared > innerSquared) {
					blocks.add(new Location(world, x, y, z));
				}
			}
		}

		return blocks;
	}

}
