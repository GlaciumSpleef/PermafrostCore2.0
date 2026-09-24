package io.paper.permafrost.core.game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class PowerSpleefFFA extends SpleefFFAGame {

	public PowerSpleefFFA(Main main) {
		super(main, SpleefFFAMode.POWERSPLEEF);
	}

	@Override
	public void startGame() {
		if (arena == null || arena.getCenter() == null) {
			return;
		}

		Location center = arena.getCenter().clone();

		corner1 = arena.getCorner1().clone();
		corner2 = arena.getCorner2().clone();

		radius = 12;
		shrunkLayers = 0;

		int startingPlayerCount = main.getSpleefQueueManager()
				.getGame(SpleefFFAMode.POWERSPLEEF)
				.getQueue().size();

		if (startingPlayerCount > 4) {
			int extraRadius = ((int) Math.floor((startingPlayerCount - 4) / 2.0)) * 2;
			double delta = (Math.floor((startingPlayerCount - 4) / 2.0)) * 2;

			radius = 12 + extraRadius;

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

			main.getDataLoader().getSpleefPlayer(player).setRoundStarting(true);
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
		
		for (UUID uuid : playersInFFA) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null)
				continue;

			String[] powers = getPlayerPowers(uuid);
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);
			
			ShovelPowers p1 = powers[0] == null ? null : ShovelPowers.valueOf(powers[0]);
			ShovelPowers p2 = powers[1] == null ? null : ShovelPowers.valueOf(powers[1]);

			main.getDataLoader().setCachedPowers(uuid, p1, p2);

			sp.setAvailable(true);

			if (powers[0] != null) {
				sp.setSelectedPower(ShovelPowers.valueOf(powers[0].toUpperCase()));
			}
			if (powers[1] != null) {
				sp.setDeselectedPower(ShovelPowers.valueOf(powers[1].toUpperCase()));
			}
		}

		new BukkitRunnable() {
			int seconds = 0;

			@Override
			public void run() {
				seconds++;

				if (seconds == 1) {
					broadcastGameMessage("&b&lPowerSpleef FFA &8» &7Get ready...");
					sendGameTitle("&bPowerSpleef FFA", "&7Match starting");
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
					for (UUID uuid : playersInFFA) {
						Player player = Bukkit.getPlayer(uuid);
						SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
						setPlayerGravity(sp.getPlayer(), 0.08);
						if (sp.getSelectedPower().equals(ShovelPowers.ROULETTE)
								|| sp.getDeselectedPower().equals(ShovelPowers.ROULETTE)) {
							if ((sp.getSelectedPower().equals(ShovelPowers.ROULETTE)
									&& sp.getDeselectedPower().equals(ShovelPowers.ROULETTE2))
									|| (sp.getSelectedPower().equals(ShovelPowers.ROULETTE2)
											&& sp.getDeselectedPower().equals(ShovelPowers.ROULETTE))) {
								ShovelPowers[] powers = ShovelPowers.values();

								ShovelPowers power = powers[(int) (Math.random() * powers.length)];
								ShovelPowers power2 = powers[(int) (Math.random() * powers.length)];

								while (power.equals(ShovelPowers.ROULETTE2) || power.equals(ShovelPowers.NONE)
										|| power.equals(ShovelPowers.ROULETTE)) {
									power = powers[(int) (Math.random() * powers.length)];
								}

								while (power2.equals(ShovelPowers.ROULETTE2) || power2.equals(ShovelPowers.NONE)
										|| power2.equals(ShovelPowers.ROULETTE) || power2.equals(power)) {
									power2 = powers[(int) (Math.random() * powers.length)];
								}

								sp.setRandomPower(power);
								sp.setRandomPower2(power2);
								sp.getPlayer()
										.sendMessage("§eYour §cROULETTE §epower has randomly chosen the §c" + power + " §epower!");
								sp.getPlayer()
								.sendMessage("§eYour §cROULETTE §epower has randomly chosen the §c" + power2 + " §epower!");
							} else {
								ShovelPowers[] powers = ShovelPowers.values();

								ShovelPowers power = powers[(int) (Math.random() * powers.length)];

								if (sp.getSelectedPower().equals(ShovelPowers.ROULETTE)) {
									while (power.equals(sp.getDeselectedPower()) || power.equals(ShovelPowers.NONE)
											|| power.equals(ShovelPowers.ROULETTE) || power.equals(ShovelPowers.ROULETTE2)) {
										power = powers[(int) (Math.random() * powers.length)];
									}
								}

								if (sp.getDeselectedPower().equals(ShovelPowers.ROULETTE)) {
									while (power.equals(sp.getSelectedPower()) || power.equals(ShovelPowers.NONE)
											|| power.equals(ShovelPowers.ROULETTE) || power.equals(ShovelPowers.ROULETTE2)) {
										power = powers[(int) (Math.random() * powers.length)];
									}
								}

								sp.setRandomPower(power);
								sp.getPlayer()
										.sendMessage("§eYour §cROULETTE §epower has randomly chosen the §c" + power + " §epower!");
							}
						}
						if (main.getDataLoader().getSnowSpawned(sp.getUUID()) != null) {
							for (Location snow : main.getDataLoader().getSnowSpawned(sp.getUUID())) {
								snow.getBlock().setType(Material.AIR);
							}
							main.getDataLoader().removeAllSnowSpawned(sp.getUUID());
						}
						if (main.getDataLoader().getSnowSpawnedByIce(sp.getUUID()) != null) {
							for (Location snow : main.getDataLoader().getSnowSpawnedByIce(sp.getUUID())) {
								snow.getBlock().setType(Material.AIR);
							}
							main.getDataLoader().removeAllSnowSpawnedByIce(sp.getUUID());
						}
						main.getAmmoManger().setAmmo(sp, ShovelPowers.BLACKGOO, 3);
						main.getAmmoManger().setAmmo(sp, ShovelPowers.LEVITATION, 3);
						for (PotionEffect effect : sp.getPlayer().getActivePotionEffects()) {
							if (effect.getType() != PotionEffectType.NIGHT_VISION)
								sp.getPlayer().removePotionEffect(effect.getType());
						}
						if (main.getDataLoader().getAllSmokeSpawned().containsKey(sp.getUUID())) {
							main.getDataLoader().removeAllSmokeSpawned(sp.getUUID());
						}
						if (main.getDataLoader().getAllVortexSpawned().containsKey(sp.getUUID())) {
							main.getDataLoader().removeAllVortexSpawned(sp.getUUID());
						}
						Attribute reachAttr = resolveBlockReachAttr();
						AttributeInstance inst = player.getAttribute(reachAttr);
						inst.setBaseValue(4.5);
						sp.setSilenced(false);
						sp.setRoundStarting(false);
					}
					sendPlayersTitle(playersInFFA, "&a&lGO!", "&7Start spleefing!", 0, 20, 10);
					sendPlayersActionBar(playersInFFA, "&a&lFight!");
					playPlayersSound(playersInFFA, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f, 1.2f);
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
							resetPlayerToLobby(p, SpleefFFAMode.POWERSPLEEF);
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
	
	public Attribute getGravityAttribute() {
		try {
			return Attribute.valueOf("GENERIC_GRAVITY"); // Paper 1.21
		} catch (IllegalArgumentException ignored) {
		}

		try {
			return Attribute.valueOf("GRAVITY"); // newer API naming
		} catch (IllegalArgumentException ignored) {
		}

		return null;
	}

	public void setPlayerGravity(Player player, double value) {
		if (player == null || !player.isOnline()) {
			return;
		}

		Attribute gravityAttribute = getGravityAttribute();

		if (gravityAttribute == null) {
			return;
		}

		AttributeInstance gravity = player.getAttribute(gravityAttribute);

		if (gravity == null) {
			return;
		}

		gravity.setBaseValue(value);
	}
	
	private Attribute resolveBlockReachAttr() {
		try {
			return Attribute.valueOf("BLOCK_INTERACTION_RANGE");
		} catch (IllegalArgumentException ignored) {
			/* old name */ }
		return Attribute.valueOf("PLAYER_BLOCK_INTERACTION_RANGE");
	}
	
	public String[] getPlayerPowers(UUID uuid) {
		String[] powers = new String[2];
		String sql = "SELECT power1, power2 FROM player_powers WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					powers[0] = rs.getString("power1");
					powers[1] = rs.getString("power2");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return powers;
	}
}
