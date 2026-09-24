package io.paper.permafrost.core.game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import github.scarsz.discordsrv.DiscordSRV;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.SpleefFFAMode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public abstract class SpleefFFAGame {
	protected final Main main;
	protected final SpleefFFAMode mode;
	protected boolean isInEvent;
	protected Map<UUID, Integer> points = new HashMap<>();
	protected int playToPoints;
	protected boolean showPoints;

	protected final Set<UUID> playersInQueue = new HashSet<>();
	protected final Set<UUID> playersAlive = new HashSet<>();
	protected final Set<UUID> playersSpectating = new HashSet<>();

	protected ArrayList<UUID> topThree = new ArrayList<>(3);

	protected Arena arena;
	protected PrototypeArena protoArena;

	protected Location circleLocation;
	protected Location circleLocation2;

	protected int deathCount;

	protected Map<UUID, Double> times = new HashMap<>();

	protected boolean inGame;
	protected boolean gameStarting;
	protected boolean gameEnding;

	protected BukkitRunnable countdownTask;
	protected BukkitTask gameTask;
	protected BukkitRunnable timerTask;

	protected int secondsTillGame;
	protected int secondsElapsed;

	protected int radius;
	protected int shrunkLayers;

	protected Location corner1;
	protected Location corner2;

	protected final List<BukkitTask> activeShrinkTasks = new ArrayList<>();
	protected final List<BukkitTask> innerDecayTasks = new ArrayList<>();

	public SpleefFFAGame(Main main, SpleefFFAMode mode) {
		this.main = main;
		this.mode = mode;
		this.isInEvent = false;
	}

	public abstract void startGame();

	public abstract void endGame();

	public int getTotalPlayerCount() {
		return (playersInQueue.size() + playersAlive.size() + playersSpectating.size());
	}

	public Location getCircleLocation() {
		return circleLocation;
	}

	public Location getCircleLocation2() {
		return circleLocation2;
	}

	public int getDeathCount() {
		return deathCount;
	}

	public void addToDeathCount() {
		deathCount++;
	}

	public void setInEvent(boolean isInEvent) {
		this.isInEvent = isInEvent;
	}

	public boolean getInEvent() {
		return isInEvent;
	}

	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}

	public boolean getIsShowingPoints() {
		return showPoints;
	}

	public Map<UUID, Integer> getPoints() {
		return points;
	}

	public int getPoints(UUID uuid) {
		return points.getOrDefault(uuid, 0);
	}

	public void setPoints(UUID uuid, int points) {
		this.points.put(uuid, points);
	}

	public void addPoints(UUID uuid, int points) {
		this.points.put(uuid, getPoints(uuid) + points);
	}

	public void removePoints(UUID uuid) {
		this.points.remove(uuid);
	}

	public void clearPoints() {
		this.points.clear();
	}

	public void setPlayToEventPoints(int playToPoints) {
		this.playToPoints = playToPoints;
	}

	public int getPlayToEventPoints() {
		return playToPoints;
	}

	public Map<UUID, Double> getTimes() {
		return times;
	}

	public double getTime(UUID uuid) {
		return times.get(uuid);
	}

	public boolean isQueued(UUID uuid) {
		return playersInQueue.contains(uuid);
	}

	public int getPlayersInQueueCount() {
		return playersInQueue.size();
	}

	public int getPlayersAliveCount() {
		return playersAlive.size();
	}

	public int getPlayersSpectatingCount() {
		return playersSpectating.size();
	}

	public int getPlayersRemaining() {
		return playersAlive.size();
	}

	public Set<UUID> getPlayersAlive() {
		return playersAlive;
	}

	public Set<UUID> getPlayersInQueue() {
		return playersInQueue;
	}

	public Set<UUID> getPlayersSpectating() {
		return playersSpectating;
	}

	public Arena getArena() {
		return arena;
	}

	public PrototypeArena getProtoArena() {
		return protoArena;
	}

	public boolean isInGame() {
		return inGame;
	}

	public boolean isGameEnding() {
		return gameEnding;
	}

	public void removePlayer(UUID uuid) {
		playersInQueue.remove(uuid);
		playersAlive.remove(uuid);
		playersSpectating.remove(uuid);
	}

	public void addPlayer(UUID uuid) {
		playersInQueue.add(uuid);
		tryStartCountdown();
	}

	public void setDead(UUID uuid) {
		if (playersAlive.remove(uuid)) {
			playersSpectating.add(uuid);
		}
	}
	
	public boolean isDead(UUID uuid) {
		return playersSpectating.contains(uuid) || !playersAlive.contains(uuid);
	}

	public void setAlive(UUID uuid) {
		if (playersSpectating.remove(uuid)) {
			playersAlive.add(uuid);
		}
	}

	public SpleefFFAMode getMode() {
		return mode;
	}

	public int getRadius() {
		return radius;
	}

	public boolean isInThisFFA(UUID uuid) {
		return playersInQueue.contains(uuid) || playersAlive.contains(uuid) || playersSpectating.contains(uuid);
	}

	public Set<UUID> getAlive() {
		return playersAlive;
	}

	public Set<UUID> getSpectators() {
		return playersSpectating;
	}

	public Set<UUID> getQueue() {
		return playersInQueue;
	}

	public boolean isAlive(UUID uuid) {
		return playersAlive.contains(uuid);
	}

	public boolean isSpectating(UUID uuid) {
		return playersSpectating.contains(uuid);
	}

	public int getWaitingPlayers() {
		return playersInQueue.size();
	}

	public int getAlivePlayers() {
		return playersAlive.size();
	}

	protected void moveQueueToGame() {
		playersAlive.clear();
		playersAlive.addAll(playersInQueue);
		playersInQueue.clear();
		playersSpectating.clear();
	}

	protected void tryStartCountdown() {
		if (inGame || gameEnding || gameStarting) {
			return;
		}

		if (playersInQueue.size() < 2) {
			return;
		}

		startCountdown();
	}

	protected void startCountdown() {
		cancelCountdownTask();

		gameStarting = true;

		final int totalSeconds = getPregameCountdownSeconds();
		secondsTillGame = totalSeconds;

		broadcastQueueMessage("&a&lMatch Found!");
		broadcastQueueMessage("&7Game starts in &e" + totalSeconds + " seconds&7.");

		playQueueSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);

		countdownTask = new BukkitRunnable() {
			int seconds = totalSeconds;

			@Override
			public void run() {
				if (playersInQueue.size() < 2) {
					broadcastQueueMessage("&c&lSpleef FFA &8» &cNot enough players. Countdown cancelled.");
					playQueueSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);

					inGame = false;
					gameStarting = false;
					secondsTillGame = 0;
					countdownTask = null;

					cancel();
					return;
				}

				secondsTillGame = seconds;

				if (seconds <= 5 && seconds >= 1) {
					sendCountdownState(seconds);
				}

				if (seconds == 0) {
					if (mode.equals(SpleefFFAMode.PROTOTYPE)) {
						PrototypeArena selectedArena = null;

						for (PrototypeArena arena : main.getDataLoader().getPrototypeFFAArenas()) {
							selectedArena = arena;
						}

						if (selectedArena == null) {
							broadcastQueueMessage("&c&lSpleef FFA &8» &cNo available arena found.");
							playQueueSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);

							inGame = false;
							gameStarting = false;
							secondsTillGame = 0;
							countdownTask = null;

							cancel();
							return;
						}

						protoArena = selectedArena;
						playQueueSound(Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.4f);

						inGame = true;

						startGame();

						countdownTask = null;
						cancel();
						return;
					} else {
						Arena selectedArena = findAvailableArena();

						if (selectedArena == null) {
							broadcastQueueMessage("&c&lSpleef FFA &8» &cNo available arena found.");
							playQueueSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);

							inGame = false;
							gameStarting = false;
							secondsTillGame = 0;
							countdownTask = null;

							cancel();
							return;
						}

						arena = selectedArena;
						arena.setInGame(true);
						playQueueSound(Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.4f);

						inGame = true;

						startGame();

						countdownTask = null;
						cancel();
						return;
					}
				}

				seconds--;
			}
		};

		countdownTask.runTaskTimer(main, 0L, 20L);
	}

	protected int getPregameCountdownSeconds() {
		return 15;
	}

	protected void cancelCountdownTask() {
		if (countdownTask != null) {
			countdownTask.cancel();
			countdownTask = null;
		}
	}

	protected Arena findAvailableArena() {
		List<Arena> available = new ArrayList<>();
		for (Arena arena : main.getDataLoader().getArenas()) {
			if (arena.isInGame()) {
				continue;
			}

			if (mode.equals(SpleefFFAMode.POWERSPLEEF)) {
				if (arena.getMaxPlayers() < (playersInQueue.size() + 20)) {
					continue;
				}
			} else {

				if (arena.getMaxPlayers() < playersInQueue.size()) {
					continue;
				}
			}

			available.add(arena);
		}

		if (available.isEmpty()) {
			return null;
		}

		return available.get(ThreadLocalRandom.current().nextInt(available.size()));
	}

	protected void broadcastQueueMessage(String message) {
		for (UUID uuid : new HashSet<>(playersInQueue)) {
			Player p = Bukkit.getPlayer(uuid);

			if (p != null && p.isOnline()) {
				p.sendMessage(color(message));
			}
		}
	}

	protected void playQueueSound(Sound sound, float volume, float pitch) {
		for (UUID uuid : new HashSet<>(playersInQueue)) {
			Player p = Bukkit.getPlayer(uuid);

			if (p != null && p.isOnline()) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
	}

	protected void sendQueueActionBar(String message) {
		for (UUID uuid : new HashSet<>(playersInQueue)) {
			Player p = Bukkit.getPlayer(uuid);

			if (p != null && p.isOnline()) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
			}
		}
	}

	protected void sendQueueTitle(String title, String subtitle) {
		for (UUID uuid : new HashSet<>(playersInQueue)) {
			Player p = Bukkit.getPlayer(uuid);

			if (p != null && p.isOnline()) {
				p.sendTitle(color(title), color(subtitle), 10, 40, 10);
			}
		}
	}

	protected void sendCountdownState(int number) {
		String colorNumber;
		float pitch;

		switch (number) {
		case 5:
			colorNumber = "&a&l5";
			pitch = 1.0f;
			break;
		case 4:
			colorNumber = "&2&l4";
			pitch = 1.1f;
			break;
		case 3:
			colorNumber = "&e&l3";
			pitch = 1.2f;
			break;
		case 2:
			colorNumber = "&6&l2";
			pitch = 1.4f;
			break;
		default:
			colorNumber = "&c&l1";
			pitch = 1.6f;
			break;
		}

		sendQueueActionBar("&7Game starting in " + colorNumber);
		sendQueueTitle(colorNumber, "&7Get ready");
		playQueueSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
	}

	public void announceFFAWinner(Player winner) {
		if (winner == null) {
			return;
		}

		String border = color("&8&m----------------------------------------");
		Bukkit.broadcastMessage(border);
		Bukkit.broadcastMessage(color("&6&lSpleef FFA"));
		Bukkit.broadcastMessage(color("&eWinner: &f" + winner.getName()));
		Bukkit.broadcastMessage(color("&ePlayers: &f" + getTotalPlayerCount()));
		Bukkit.broadcastMessage(border);

		Set<UUID> participants = new HashSet<>();
		participants.addAll(getAlive());
		participants.addAll(getSpectators());

		for (UUID uuid : participants) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
			}
		}

		String discordMessage = "----------------------------------------\n" + "**SPLEEF FFA**\n" + "Winner: "
				+ winner.getName() + "\n" + "Players: " + getTotalPlayerCount() + "\n"
				+ "----------------------------------------";

		sendDiscordMessage(discordMessage);
	}

	public void sendDiscordMessage(String message) {
		DiscordSRV.getPlugin().getMainTextChannel().sendMessage(message).queue();
	}

	public void broadcastGameMessage(String message) {
		for (UUID uuid : new HashSet<>(getAlive())) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendMessage(color(message));
			}
		}
	}

	public void playPlayersSound(Set<UUID> uuids, Sound sound, float volume, float pitch) {
		for (UUID uuid : new HashSet<>(uuids)) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
	}

	public void sendPlayersActionBar(Set<UUID> uuids, String message) {
		for (UUID uuid : new HashSet<>(uuids)) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
			}
		}
	}

	public void sendGameTitle(String title, String subtitle) {
		for (UUID uuid : new HashSet<>(getAlive())) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendTitle(color(title), color(subtitle), 10, 40, 10);
			}
		}
	}

	public void sendPlayersTitle(Set<UUID> uuids, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		for (UUID uuid : new HashSet<>(uuids)) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
			}
		}
	}

	public UUID getTopOne() {
		while (topThree.size() <= 1) {
			topThree.add(null);
		}
		if (topThree.get(0) == null)
			return null;
		return topThree.get(0);
	}

	public UUID getTopTwo() {
		while (topThree.size() <= 2) {
			topThree.add(null);
		}
		if (topThree.get(1) == null)
			return null;
		return topThree.get(1);
	}

	public UUID getTopThree() {
		while (topThree.size() <= 3) {
			topThree.add(null);
		}
		if (topThree.get(2) == null)
			return null;
		return topThree.get(2);
	}

	protected void placePlayer(int index, UUID uuid) {
		while (topThree.size() <= index) {
			topThree.add(null);
		}

		topThree.set(index, uuid);
	}

	public void setTopOne(UUID uuid) {
		placePlayer(0, uuid);
	}

	public void setTopTwo(UUID uuid) {
		placePlayer(1, uuid);
	}

	public void setTopThree(UUID uuid) {
		placePlayer(2, uuid);
	}

	@SuppressWarnings("deprecation")
	protected void announceTopThree() {
		Bukkit.broadcastMessage(color("&b&lSpleef FFA &8» &fFinal Results:"));

		if (topThree.size() > 0 && topThree.get(0) != null) {
			if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.CUSTOM)) {
				Bukkit.broadcastMessage(color("&e&l1st &8» &f" + getName(topThree.get(0))) + " §7(+200 shards)");
				main.getDataLoader().addShards(topThree.get(0), 200);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.POLAR)) {
				Bukkit.broadcastMessage(color("&e&l1st &8» &f" + getName(topThree.get(0))) + " §7(+175 shards)");
				main.getDataLoader().addShards(topThree.get(0), 175);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(0), PurchasableRank.GLACIER)) {
				Bukkit.broadcastMessage(color("&e&l1st &8» &f" + getName(topThree.get(0))) + " §7(+150 shards)");
				main.getDataLoader().addShards(topThree.get(0), 150);
			} else {
				Bukkit.broadcastMessage(color("&e&l1st &8» &f" + getName(topThree.get(0))) + " §7(+100 shards)");
				main.getDataLoader().addShards(topThree.get(0), 100);
			}
		}

		if (topThree.size() > 1 && topThree.get(1) != null) {
			if (main.getDataLoader().hasPurchasableRank(topThree.get(1), PurchasableRank.CUSTOM)) {
				Bukkit.broadcastMessage(color("&6&l2nd &8» &f" + getName(topThree.get(1))) + " §7(+100 shards)");
				main.getDataLoader().addShards(topThree.get(1), 100);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(1), PurchasableRank.POLAR)) {
				Bukkit.broadcastMessage(color("&6&l2nd &8» &f" + getName(topThree.get(1))) + " §7(+87 shards)");
				main.getDataLoader().addShards(topThree.get(1), 87);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(1), PurchasableRank.GLACIER)) {
				Bukkit.broadcastMessage(color("&6&l2nd &8» &f" + getName(topThree.get(1))) + " §7(+75 shards)");
				main.getDataLoader().addShards(topThree.get(1), 75);
			} else {
				Bukkit.broadcastMessage(color("&6&l2nd &8» &f" + getName(topThree.get(1))) + " §7(+50 shards)");
				main.getDataLoader().addShards(topThree.get(1), 50);
			}
			if(getInEvent()) {
				addPoints(topThree.get(1), 2);
			}
		}

		if (topThree.size() > 2 && topThree.get(2) != null) {
			if (main.getDataLoader().hasPurchasableRank(topThree.get(2), PurchasableRank.CUSTOM)) {
				Bukkit.broadcastMessage(color("&c&l3rd &8» &f" + getName(topThree.get(2))) + " §7(+50 shards)");
				main.getDataLoader().addShards(topThree.get(2), 50);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(2), PurchasableRank.POLAR)) {
				Bukkit.broadcastMessage(color("&c&l3rd &8» &f" + getName(topThree.get(2))) + " §7(+44 shards)");
				main.getDataLoader().addShards(topThree.get(2), 44);
			} else if (main.getDataLoader().hasPurchasableRank(topThree.get(2), PurchasableRank.GLACIER)) {
				Bukkit.broadcastMessage(color("&c&l3rd &8» &f" + getName(topThree.get(2))) + " §7(+37 shards)");
				main.getDataLoader().addShards(topThree.get(2), 37);
			} else {
				Bukkit.broadcastMessage(color("&c&l3rd &8» &f" + getName(topThree.get(2))) + " §7(+25 shards)");
				main.getDataLoader().addShards(topThree.get(2), 25);
			}
			if(getInEvent()) {
				addPoints(topThree.get(2), 1);
			}
		}
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

	public void startTimer(JavaPlugin plugin) {
		if (timerTask != null) {
			timerTask.cancel();
		}

		secondsElapsed = 0;

		timerTask = new BukkitRunnable() {
			@Override
			public void run() {

				double interval = 4;

				if (secondsElapsed % interval == 0 && secondsElapsed != 0) {
					if (mode != null && mode == SpleefFFAMode.POWERSPLEEF) {
						Location center = arena.getCenter();

						int islandRadius = 3;
						int safeRadius = radius - islandRadius;

						double angle = Math.random() * 2 * Math.PI;
						double distance = Math.sqrt(Math.random()) * safeRadius;

						int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
						int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);

						Location randomBlock = new Location(center.getWorld(), x, arena.getCorner1().getBlockY(), z);

						startParticles(randomBlock, islandRadius, 1);

						new BukkitRunnable() {
							@Override
							public void run() {
								createCylinder(randomBlock, Material.SNOW_BLOCK, islandRadius, 1);
							}
						}.runTaskLater(plugin, 40L);
					}
				}
				secondsElapsed++;
			}
		};

		timerTask.runTaskTimer(plugin, 0L, 20L);
	}

	public int getTimeOngoing() {
		return secondsElapsed;
	}

	public int getSecondsUntilStart() {
		return secondsTillGame;
	}

	public void createCylinder(Location center, Material material, int radius, int height) {
		World world = center.getWorld();
		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();

		for (int y = 0; y < height; y++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + z * z <= radius * radius) {
						Location loc = new Location(world, cx + x, cy + y, cz + z);
						if (world.getBlockAt(loc).getType().equals(Material.AIR))
							world.getBlockAt(loc).setType(material);
					}
				}
			}
		}

	}

	public void startParticles(Location centerf, int radius, int height) {
		final World world = centerf.getWorld();
		final Location center = centerf.add(0.5, 0.0, 0.5);

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if (ticks >= 40) {
					cancel();
					return;
				}

				for (int i = 0; i < 72; i++) {
					double theta = (2 * Math.PI) * i / 72;

					double x = center.getX() + Math.cos(theta) * radius;
					double z = center.getZ() + Math.sin(theta) * radius;

					Location p = new Location(world, x, center.getY() + 1, z);

					BlockData iceBlock = Material.ICE.createBlockData();
					world.spawnParticle(Particle.BLOCK, p, 1, 0.02, 0.02, 0.02, 0.0, iceBlock);

				}

				ticks += 5;
			}
		}.runTaskTimer(main, 0L, 5L);
	}

	public void stopTimer() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	public void endFFAEvent() {
		if (points == null || points.isEmpty()) {
			Bukkit.broadcastMessage("§c[FFA Event] No points were recorded.");
			return;
		}

		List<Map.Entry<UUID, Integer>> topPlayers = points.entrySet().stream()
				.sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()).limit(3).toList();

		Bukkit.broadcastMessage("§6§lFFA Event Results:");

		int place = 1;

		for (Map.Entry<UUID, Integer> entry : topPlayers) {
			UUID uuid = entry.getKey();
			int playerPoints = entry.getValue();

			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
			String name = offlinePlayer.getName();

			if (name == null) {
				name = "Unknown";
			}

			String placeText;

			if (place == 1) {
				placeText = "§e1st";
			} else if (place == 2) {
				placeText = "§72nd";
			} else {
				placeText = "§63rd";
			}

			Bukkit.broadcastMessage(placeText + " §f- §a" + name + " §7with §e" + playerPoints + " §7points");

			place++;
		}
		showPoints = false;
		points.clear();
	}

	public void announceAllPoints() {
		Bukkit.broadcastMessage("§6§lFFA Points: §f" + points.entrySet().stream()
				.sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
				.map(e -> "§e" + (Bukkit.getOfflinePlayer(e.getKey()).getName() != null
						? Bukkit.getOfflinePlayer(e.getKey()).getName()
						: "Unknown") + "§7: §a" + e.getValue())
				.collect(java.util.stream.Collectors.joining("§7, §f")));
	}

	protected String getName(UUID uuid) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		return player.getName() != null ? player.getName() : "Unknown";
	}

	protected String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
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

	public void addWin(Connection connection, UUID uuid, int amount) {
		String sql = "UPDATE splegg_ffa_wins SET wins = wins + ? WHERE uuid = ?";

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
}
