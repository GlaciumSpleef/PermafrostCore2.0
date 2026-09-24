package io.paper.permafrost;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.paper.permafrost.core.game.Arena;
import io.paper.permafrost.core.game.BanningArea;
import io.paper.permafrost.core.game.PrototypeArena;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.core.game.SpleefTeam;
import io.paper.permafrost.core.game.SpleggArena;
import io.paper.permafrost.core.game.SpleggFFAArena;
import io.paper.permafrost.core.game.SpleggGame;
import io.paper.permafrost.core.game.SpleggRankedArena;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.parkour.ParkourMap;
import io.paper.permafrost.training.TrainingArena;
import io.paper.permafrost.training.TrainingPlot;
import io.paper.permafrost.training.TrainingSession;
import io.paper.permafrost.training.TrainingType;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.Ranks;
import io.paper.permafrost.utilities.ShardShovel;
import io.paper.permafrost.utilities.ShellRank;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.Utils;

public class DataLoader implements Listener {
	private Set<Arena> arenas = new HashSet<Arena>();
	private final Map<UUID, ShovelPowers[]> playerPowersCache = new HashMap<>();
	private Set<BanningArea> banningAreas = new HashSet<BanningArea>();
	private Set<SpleggArena> spleggArenas = new HashSet<SpleggArena>();
	private Set<SpleggRankedArena> spleggRankedArenas = new HashSet<SpleggRankedArena>();
	private Set<SpleggFFAArena> spleggFFAArenas = new HashSet<SpleggFFAArena>();
	private Set<PrototypeArena> prototypeFFAArenas = new HashSet<PrototypeArena>();
	private Set<ParkourMap> parkourMaps = new HashSet<ParkourMap>();
	private Set<TrainingArena> trainingArenas = new HashSet<TrainingArena>();
	private final Map<String, TrainingPlot> trainingPlots = new HashMap<>();
	private Set<UUID> tntRunAffectedPlayers = new HashSet<>();
	private Main main;
	private boolean queuesClosed;
	private boolean arenasLoaded = false;
	private HashMap<UUID, SpleefPlayer> players = new HashMap<>();
	private HashMap<UUID, List<Player>> onlinePlayers = new HashMap<>();
	// private HashMap<Integer, List<Player>> playerTeams = new HashMap<>();
	private HashMap<UUID, Map<Integer, List<UUID>>> playerTeams = new HashMap<>();
	private final Map<UUID, Integer> teamCounts = new HashMap<>();
	private final Map<UUID, Boolean> isRandomized = new HashMap<>();
	private HashMap<UUID, List<Player>> randomizedPlayers = new HashMap<>();
	private HashMap<UUID, List<Player>> finalizedPlayers = new HashMap<>();
	private final Map<UUID, Boolean> isFinalized = new HashMap<>();
	private HashMap<UUID, TeamSpleefTeam> teams = new HashMap<>();
	private List<UUID> duelers = new ArrayList<>();
	private HashMap<UUID, Integer> playToInts = new HashMap<>();
	private HashMap<UUID, Integer> powerInts = new HashMap<>();
	private final HashMap<UUID, List<Location>> iceSpawned = new HashMap<>();
	private final HashMap<UUID, List<Location>> snowSpawnedByIce = new HashMap<>();
	private HashMap<SpleefGame, List<SpleefSpectator>> spectators = new HashMap<>();
	private final HashMap<UUID, List<Location>> decaySpawned = new HashMap<>();
	private final HashMap<UUID, List<Location>> vortexSpawned = new HashMap<>();
	private final HashMap<UUID, List<Location>> smokeSpawned = new HashMap<>();
	private final HashMap<UUID, List<Location>> tntRunDecaySpawned = new HashMap<>();
	private final HashMap<UUID, List<Location>> snowSpawned = new HashMap<>();
	// key = affected player's UUID
	// value = vortex owner's UUID
	private final Map<UUID, UUID> gravityAffected = new HashMap<>();
	private final Map<UUID, UUID> blockedWindCharges = new HashMap<>();
	private final Map<UUID, Boolean> toggledSplegg = new HashMap<>();
	private final Map<UUID, Boolean> toggledPowers = new HashMap<>();
	private HashMap<UUID, TrainingSession> playersInTraining = new HashMap<>();
	private final Map<UUID, Integer> blocksToBreakInPlot = new HashMap<>();
	private HashMap<UUID, Boolean> snareUsed = new HashMap<>();
	private HashMap<UUID, Boolean> recallActive = new HashMap<>();
	private HashMap<UUID, Integer> spleggFFAStats = new HashMap<>();
	private final String SPLEEF_ELO_TABLE = "ranked_spleef_elo";
	private final String SPLEGG_ELO_TABLE = "ranked_splegg_elo";
	private HashMap<UUID, List<SpleggRankedArena>> favoritedArenas = new HashMap<>();
	private final Map<UUID, Location> recallLocations = new HashMap<>();
	private Set<UUID> protectedPlayers = new HashSet<>();

	public DataLoader(Main main) {
		this.main = main;
	}

	public void loadArenas() {
		(new BukkitRunnable() {
			public void run() {
				int arenasint = 0;
				if (Main.arenas.getConfig().contains("arenas")) {
					Set<String> arenas2 = Main.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
					for (String b : arenas2) {
						try {
							Location corner1 = Utils
									.getLoc(Main.arenas.getConfig().getString("arenas." + b + ".corner1"), false);
							Location corner2 = Utils
									.getLoc(Main.arenas.getConfig().getString("arenas." + b + ".corner2"), false);
							Location center = Utils.getLoc(Main.arenas.getConfig().getString("arenas." + b + ".center"),
									false);
							Location border = Utils.getLoc(Main.arenas.getConfig().getString("arenas." + b + ".border"),
									false);
							int maxPlayers = Main.arenas.getConfig().getInt("arenas." + b + ".maxplayers");
							Arena a = new Arena(b, corner1, corner2, center, border, maxPlayers);
							arenas.add(a);
							arenasint++;
						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(arenasint) + " Arenas Loaded!");
				}
				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public void loadBanningAreas() {
		(new BukkitRunnable() {
			public void run() {
				int arenasint = 0;
				if (Main.banningAreas.getConfig().contains("arenas")) {
					Set<String> arenas2 = Main.banningAreas.getConfig().getConfigurationSection("arenas")
							.getKeys(false);
					for (String b : arenas2) {
						try {
							Location spawn1 = Utils
									.getLoc(Main.banningAreas.getConfig().getString("arenas." + b + ".spawn1"), false);
							Location spawn2 = Utils
									.getLoc(Main.banningAreas.getConfig().getString("arenas." + b + ".spawn2"), false);
							Location banner1 = Utils
									.getLoc(Main.banningAreas.getConfig().getString("arenas." + b + ".banner1"), false);
							Location banner2 = Utils
									.getLoc(Main.banningAreas.getConfig().getString("arenas." + b + ".banner2"), false);
							BanningArea a = new BanningArea(b, spawn1, spawn2, banner1, banner2);
							banningAreas.add(a);
							arenasint++;
						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(arenasint) + " Banning Areas Loaded!");
				}
				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public void loadSpleggArenas() {
		(new BukkitRunnable() {
			public void run() {
				int arenasint = 0;
				if (Main.spleggArenas.getConfig().contains("arenas")) {
					Set<String> arenas2 = Main.spleggArenas.getConfig().getConfigurationSection("arenas")
							.getKeys(false);
					for (String b : arenas2) {
						try {
							String id = b;
							World world = Bukkit
									.getWorld(Main.spleggArenas.getConfig().getString("arenas." + b + ".world"));
							String schematicPath = Main.spleggArenas.getConfig()
									.getString("arenas." + b + ".schematic");
							Location pasteOrigin = Utils.getLoc(
									Main.spleggArenas.getConfig().getString("arenas." + b + ".pasteOrigin"), false);
							int fallLevel = Main.spleggArenas.getConfig().getInt("arenas." + b + ".fallLevel");
							List<Map<?, ?>> teamsList = Main.spleggArenas.getConfig()
									.getMapList("arenas." + b + ".teams");
							int teamAmount = (teamsList == null) ? 0 : teamsList.size();

							Set<SpleggArena.SpawnPoint> playerSpawns = new HashSet<>();

							if (teamsList != null && world != null && pasteOrigin != null) {
								final int ox = pasteOrigin.getBlockX();
								final int oy = pasteOrigin.getBlockY();
								final int oz = pasteOrigin.getBlockZ();

								for (Map<?, ?> team : teamsList) {
									String teamName = String.valueOf(team.get("name"));
									String teamColor = String.valueOf(team.get("color"));
									boolean single = toBool(team.get("single-block"));

									Object spawnsObj = team.get("spawns");
									if (spawnsObj instanceof List<?> spawnsList) {
										for (Object entry : spawnsList) {
											if (entry instanceof Map<?, ?> sm) {
												int sx = toInt(sm.get("x"));
												int sy = toInt(sm.get("y"));
												int sz = toInt(sm.get("z"));

												Location abs = new Location(world, ox + sx, oy + sy, oz + sz);

												SpleggArena.SpawnPoint spot = new SpleggArena.SpawnPoint(abs, teamColor,
														teamName, single);

												playerSpawns.add(spot);
											}
										}
									}
								}
							}

							SpleggArena a = new SpleggArena(b, world, schematicPath, pasteOrigin, teamAmount,
									playerSpawns, fallLevel);
							spleggArenas.add(a);
							arenasint++;
						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(arenasint) + " Arenas Loaded!");
				}
				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public void loadRankedSpleggArenas() {
		(new BukkitRunnable() {
			public void run() {
				int arenasint = 0;

				if (Main.spleggRankedArenas.getConfig().contains("arenas")) {
					Set<String> arenas2 = Main.spleggRankedArenas.getConfig().getConfigurationSection("arenas")
							.getKeys(false);

					for (String b : arenas2) {
						try {
							String id = b;
							World world = Bukkit
									.getWorld(Main.spleggRankedArenas.getConfig().getString("arenas." + b + ".world"));
							String schematicPath = Main.spleggRankedArenas.getConfig()
									.getString("arenas." + b + ".schematic");
							Location pasteOrigin = Utils.getLoc(
									Main.spleggRankedArenas.getConfig().getString("arenas." + b + ".pasteOrigin"),
									false);
							int fallLevel = Main.spleggRankedArenas.getConfig().getInt("arenas." + b + ".fallLevel");

							List<Map<?, ?>> spawnsList = Main.spleggRankedArenas.getConfig()
									.getMapList("arenas." + b + ".spawns");

							Set<Location> playerSpawns = new HashSet<>();

							if (spawnsList != null) {
								for (Map<?, ?> spawnGroup : spawnsList) {
									int number = toInt(spawnGroup.get("number"));

									for (int i = 1; i <= number; i++) {
										String locString = String.valueOf(spawnGroup.get("location" + i));

										if (locString != null && !locString.equals("null")) {
											Location loc = Utils.getLoc(locString, false);
											if (loc != null) {
												playerSpawns.add(loc);
											}
										}
									}
								}
							}

							SpleggRankedArena a = new SpleggRankedArena(id, world, schematicPath, pasteOrigin,
									playerSpawns, fallLevel);
							spleggRankedArenas.add(a);
							arenasint++;

						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(arenasint) + " Arenas Loaded!");
				}

				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public void loadTrainingArenas() {
		(new BukkitRunnable() {
			public void run() {
				int arenasint = 0;
				if (Main.trainingArenas.getConfig().contains("arenas")) {
					Set<String> arenas = Main.trainingArenas.getConfig().getConfigurationSection("arenas")
							.getKeys(false);
					for (String b : arenas) {
						try {
							String id = b;
							World world = Bukkit
									.getWorld(Main.trainingArenas.getConfig().getString("arenas." + b + ".world"));
							Location playerSpawn = Utils.getLoc(
									Main.trainingArenas.getConfig().getString("arenas." + b + ".playerSpawn"), false);
							Location plotPasteLocation = Utils.getLoc(
									Main.trainingArenas.getConfig().getString("arenas." + b + ".plotPasteLocation"),
									false);
							TrainingType type = TrainingType
									.valueOf(Main.trainingArenas.getConfig().getString("arenas." + b + ".type"));

							TrainingArena a = new TrainingArena(id, world, playerSpawn, plotPasteLocation, type);
							trainingArenas.add(a);
							arenasint++;
						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(arenasint) + " Arenas Loaded!");
				}
				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public void loadTrainingPlots() {
		new BukkitRunnable() {
			@Override
			public void run() {

				int plotsLoaded = 0;

				if (!Main.trainingPlots.getConfig().contains("plots")) {
					arenasLoaded = true;
					return;
				}

				Set<String> plots = Main.trainingPlots.getConfig().getConfigurationSection("plots").getKeys(false);

				for (String key : plots) {
					try {
						String base = "plots." + key;

						String id = key;
						String schematic = Main.trainingPlots.getConfig().getString(base + ".schematic");
						int maxPlayers = Main.trainingPlots.getConfig().getInt(base + ".maxPlayers");
						boolean timer = Main.trainingPlots.getConfig().getBoolean(base + ".timer");
						boolean offset = Main.trainingPlots.getConfig().getBoolean(base + ".offset");
						int blocksToBreak = Main.trainingPlots.getConfig().getInt(base + ".blocksToBreak");

						if (schematic == null || maxPlayers <= 0) {
							Bukkit.getLogger().warning("Invalid plot config: " + key);
							continue;
						}

						if (maxPlayers == 1) {
							TrainingPlot plot = new TrainingPlot(id, schematic, 1, blocksToBreak, timer, offset);
							trainingPlots.put(normalize(plot.getName()), plot);
						} else {

							Map<Integer, Vector> spawnOffsets = new HashMap<>();

							if (Main.trainingPlots.getConfig().contains(base + ".spawns")) {
								Set<String> spawnKeys = Main.trainingPlots.getConfig()
										.getConfigurationSection(base + ".spawns").getKeys(false);

								for (String spawnKey : spawnKeys) {
									String spawnBase = base + ".spawns." + spawnKey;

									double x = Main.trainingPlots.getConfig().getDouble(spawnBase + ".x");
									double y = Main.trainingPlots.getConfig().getDouble(spawnBase + ".y");
									double z = Main.trainingPlots.getConfig().getDouble(spawnBase + ".z");

									spawnOffsets.put(Integer.parseInt(spawnKey), new Vector(x, y, z));
								}
							}

							TrainingPlot plot = new TrainingPlot(id, schematic, maxPlayers, blocksToBreak, timer,
									offset, spawnOffsets);

							trainingPlots.put(normalize(plot.getName()), plot);
						}

						plotsLoaded++;

					} catch (Exception e) {
						Bukkit.getLogger().severe("Failed to load plot: " + key);
						e.printStackTrace();
					}
				}

				Bukkit.getLogger().info(plotsLoaded + " Training Plots Loaded.");
				arenasLoaded = true;
			}
		}.runTaskAsynchronously(main);
	}

	public void loadSpleggFFAArenas() {
		new BukkitRunnable() {
			public void run() {
				int arenasCount = 0;

				if (!Main.spleggFFAArenas.getConfig().contains("arenas")) {
					Bukkit.getLogger().warning("No arenas found in config!");
					arenasLoaded = true;
					return;
				}

				Set<String> arenaKeys = Main.spleggFFAArenas.getConfig().getConfigurationSection("arenas")
						.getKeys(false);

				for (String key : arenaKeys) {
					try {
						String path = "arenas." + key;

						String id = key;

						World world = Bukkit.getWorld(Main.spleggFFAArenas.getConfig().getString(path + ".world"));

						String schematic = Main.spleggFFAArenas.getConfig().getString(path + ".schematic");

						Location pasteOrigin = Utils
								.getLoc(Main.spleggFFAArenas.getConfig().getString(path + ".pasteOrigin"), false);

						int fallLevel = Main.spleggFFAArenas.getConfig().getInt(path + ".fallLevel");

						String size = Main.spleggFFAArenas.getConfig().getString(path + ".size");

						List<Map<?, ?>> spawnsList = Main.spleggFFAArenas.getConfig().getMapList(path + ".spawns");

						Set<Location> spawns = new HashSet<>();

						if (spawnsList != null && world != null) {
							for (Map<?, ?> spawnGroup : spawnsList) {

								int amount = toInt(spawnGroup.get("number"));

								for (int i = 1; i <= amount; i++) {
									Object locObj = spawnGroup.get("location" + i);
									if (locObj == null)
										continue;

									Location loc = Utils.getLoc(String.valueOf(locObj), false);
									loc.set(loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5);
									spawns.add(loc);
								}
							}
						}

						int maxPlayers = spawns.size();

						SpleggFFAArena arena = new SpleggFFAArena(id, world, schematic, pasteOrigin, maxPlayers, spawns,
								fallLevel, size);

						spleggFFAArenas.add(arena);
						arenasCount++;

					} catch (Exception e) {
						Bukkit.getLogger().warning("Failed to load arena: " + key);
						e.printStackTrace();
					}
				}

				Bukkit.getLogger().info(arenasCount + " Splegg FFA Arenas Loaded!");
				arenasLoaded = true;
			}
		}.runTaskAsynchronously(main);
	}

	public void loadPrototypeFFAArenas() {
		new BukkitRunnable() {
			public void run() {
				int arenasCount = 0;

				if (!Main.prototypeFFAArenas.getConfig().contains("arenas")) {
					Bukkit.getLogger().warning("No arenas found in config!");
					arenasLoaded = true;
					return;
				}

				Set<String> arenaKeys = Main.prototypeFFAArenas.getConfig().getConfigurationSection("arenas")
						.getKeys(false);

				for (String key : arenaKeys) {
					try {
						String path = "arenas." + key;

						String id = key;

						String schematic = Main.prototypeFFAArenas.getConfig().getString(path + ".schematic");

						Location center = Utils.getLoc(Main.prototypeFFAArenas.getConfig().getString(path + ".center"),
								false);

						List<Map<?, ?>> spawnsList = Main.prototypeFFAArenas.getConfig()
								.getMapList(path + ".playerspawns");

						List<Location> spawns = new ArrayList<>();

						if (spawnsList != null) {
							for (Map<?, ?> spawnGroup : spawnsList) {

								for (int i = 1; i <= 12; i++) {
									Object locObj = spawnGroup.get("location" + i);
									if (locObj == null)
										continue;

									Location loc = Utils.getLoc(String.valueOf(locObj), false);
									spawns.add(loc);
								}
							}
						}

						List<Map<?, ?>> circleSpawnsList = Main.prototypeFFAArenas.getConfig()
								.getMapList(path + ".circlespawns");

						Set<Location> circleSpawns = new HashSet<>();

						if (circleSpawnsList != null) {
							for (Map<?, ?> spawnGroup : circleSpawnsList) {

								for (int i = 1; i <= 9; i++) {
									Object locObj = spawnGroup.get("location" + i);
									if (locObj == null)
										continue;

									Location loc = Utils.getLoc(String.valueOf(locObj), false);
									circleSpawns.add(loc);
								}
							}
						}

						// ✅ CREATE FFA ARENA
						PrototypeArena arena = new PrototypeArena(id, schematic, center, spawns, circleSpawns);

						prototypeFFAArenas.add(arena);
						arenasCount++;

					} catch (Exception e) {
						Bukkit.getLogger().warning("Failed to load arena: " + key);
						e.printStackTrace();
					}
				}

				Bukkit.getLogger().info(arenasCount + " Prototype FFA Arenas Loaded!");
				arenasLoaded = true;
			}
		}.runTaskAsynchronously(main);
	}
	
	public void loadParkourMaps() {
		(new BukkitRunnable() {
			public void run() {
				int mapsint = 0;
				if (Main.parkourMaps.getConfig().contains("maps")) {
					Set<String> maps2 = Main.parkourMaps.getConfig().getConfigurationSection("maps").getKeys(false);
					for (String b : maps2) {
						try {
							Location spawn1 = Utils
									.getLoc(Main.parkourMaps.getConfig().getString("maps." + b + ".spawnLoc1"), false);
							Location spawn2 = Utils
									.getLoc(Main.parkourMaps.getConfig().getString("maps." + b + ".spawnLoc2"), false);
							Location end1 = Utils
									.getLoc(Main.parkourMaps.getConfig().getString("maps." + b + ".endLoc1"), false);
							Location end2 = Utils
									.getLoc(Main.parkourMaps.getConfig().getString("maps." + b + ".endLoc2"), false);
							int fallLevel = Main.parkourMaps.getConfig().getInt("maps." + b + ".fallLevel");
							ParkourMap m = new ParkourMap(b, spawn1, spawn2, end1, end2, fallLevel);
							parkourMaps.add(m);
							mapsint++;
						} catch (Exception e) {
							Bukkit.getLogger().info(b);
							e.printStackTrace();
						}
					}

					Bukkit.getLogger().info(String.valueOf(mapsint) + " Parkour Maps Loaded!");
				}
				arenasLoaded = true;
			}
		}).runTaskAsynchronously(main);
	}

	public long getBestTimeMs(UUID uuid, String plot) {

		String sql = """
				SELECT best_time_ms
				FROM training_personal_bests
				WHERE player_uuid = ? AND plot_name = ?
				""";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setString(2, plot);

			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getLong("best_time_ms");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public Timestamp getBestTimeAchievedAt(UUID uuid, String plot) {

		String sql = """
				SELECT achieved_at
				FROM training_personal_bests
				WHERE player_uuid = ? AND plot_name = ?
				""";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setString(2, plot);

			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getTimestamp("achieved_at");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static int toInt(Object o) {
		if (o instanceof Number n)
			return n.intValue();
		return Integer.parseInt(String.valueOf(o).trim());
	}

	private static boolean toBool(Object o) {
		if (o instanceof Number n)
			return n.intValue() != 0;
		if (o instanceof Boolean b)
			return b;
		return Boolean.parseBoolean(String.valueOf(o));
	}

	public long[] getTotalAndAverageSessionTime(UUID uuid, String plot) {

		String sql = """
				SELECT total_session_time_ms, session_count
				FROM training_player_session_stats
				WHERE player_uuid = ? AND plot_name = ?
				""";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setString(2, plot);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {

				long total = rs.getLong("total_session_time_ms");
				int sessions = rs.getInt("session_count");

				long average = sessions > 0 ? total / sessions : 0;

				return new long[] { total, average };
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return new long[] { 0, 0 };
	}

	public long getTotalTimeMs(UUID uuid, String plot) {

		String sql = """
				SELECT total_session_time_ms
				FROM training_player_session_stats
				WHERE player_uuid = ? AND plot_name = ?
				""";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setString(2, plot);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong("total_session_time_ms");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void updatePersonalBest(UUID playerUuid, String plotName, double seconds) {
		long timeMs = (long) (seconds * 1000);

		String sql = "INSERT INTO training_personal_bests " + "(player_uuid, plot_name, best_time_ms) "
				+ "VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE "
				+ "best_time_ms = LEAST(best_time_ms, VALUES(best_time_ms)), "
				+ "achieved_at = IF(VALUES(best_time_ms) < best_time_ms, CURRENT_TIMESTAMP, achieved_at)";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, playerUuid.toString());
			ps.setString(2, plotName);
			ps.setLong(3, timeMs);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addSessionAverage(UUID playerUuid, String plotName, double averageSeconds) {
		long avgMillis = (long) (averageSeconds * 1000);

		String sql = "INSERT INTO training_player_session_stats "
				+ "(player_uuid, plot_name, total_session_time_ms, session_count) " + "VALUES (?, ?, ?, 1) "
				+ "ON DUPLICATE KEY UPDATE "
				+ "total_session_time_ms = total_session_time_ms + VALUES(total_session_time_ms), "
				+ "session_count = session_count + 1";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, playerUuid.toString());
			ps.setString(2, plotName);
			ps.setLong(3, avgMillis);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Map<UUID, Integer> getBlocksToBreakInPlot() {
		return new HashMap<>(blocksToBreakInPlot);
	}

	public void setBlocksToBreakInPlot(Map<UUID, Integer> blocks) {
		blocksToBreakInPlot.clear();
		if (blocks != null) {
			blocksToBreakInPlot.putAll(blocks);
		}
	}

	public int getBlocksToBreak(UUID uuid) {
		return blocksToBreakInPlot.getOrDefault(uuid, 0);
	}

	public void setBlocksToBreak(UUID uuid, int amount) {
		blocksToBreakInPlot.put(uuid, amount);
	}

	public void incrementBlocksToBreak(UUID uuid) {
		blocksToBreakInPlot.merge(uuid, 1, Integer::sum);
	}

	public void decrementBlocksToBreak(UUID uuid) {
		blocksToBreakInPlot.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
	}

	public void clearBlocksToBreak(UUID uuid) {
		blocksToBreakInPlot.remove(uuid);
	}

	public Set<Arena> getArenas() {
		return this.arenas;
	}

	public Set<BanningArea> getBanningAreas() {
		return this.banningAreas;
	}

	public Set<SpleggArena> getSpleggArenas() {
		return this.spleggArenas;
	}

	public Set<SpleggRankedArena> getSpleggRankedArenas() {
		return this.spleggRankedArenas;
	}

	public Set<SpleggFFAArena> getSpleggFFAArenas() {
		return this.spleggFFAArenas;
	}

	public Set<PrototypeArena> getPrototypeFFAArenas() {
		return this.prototypeFFAArenas;
	}
	
	public Set<ParkourMap> getParkourMaps() {
		return this.parkourMaps;
	}


	public Set<TrainingArena> getTrainingArenas() {
		return this.trainingArenas;
	}

	public TrainingPlot getTrainingPlot(String name) {
		if (name == null)
			return null;
		return trainingPlots.get(normalize(name));
	}

	public Collection<TrainingPlot> getTrainingPlots() {
		return trainingPlots.values();
	}

	public boolean hasTrainingPlot(String name) {
		return trainingPlots.containsKey(normalize(name));
	}

	private String normalize(String name) {
		return name.toLowerCase().replace(" ", "");
	}

	public void queues(boolean b) {
		this.queuesClosed = b;
	}

	public Set<UUID> getPlayersAffectedByTntRun() {
		return this.tntRunAffectedPlayers;
	}

	public void addPlayerAffectedByTntRun(UUID uuid) {
		tntRunAffectedPlayers.add(uuid);
	}

	public void removePlayerAffectedByTntRun(UUID uuid) {
		tntRunAffectedPlayers.remove(uuid);
	}
	
	public Set<UUID> getProtectedPlayers() {
		return this.protectedPlayers;
	}

	public void addProtectedPlayer(UUID uuid) {
		protectedPlayers.add(uuid);
	}

	public void removeProtectedPlayer(UUID uuid) {
		protectedPlayers.remove(uuid);
	}

	public void addPlayer(UUID uuid, SpleefPlayer sp) {
		this.players.put(uuid, sp);
	}

	public List<SpleefPlayer> getPlayers() {
		return new ArrayList<>(this.players.values());
	}

	public SpleefPlayer getPlayer(UUID uuid) {
		return this.players.get(uuid);
	}

	public SpleefPlayer getSpleefPlayer(OfflinePlayer p) {
		if (p == null) {
			Bukkit.getLogger().warning("getSpleefPlayer was called with a null player!");
			return null;
		}

		SpleefPlayer spleefPlayer = main.getDataLoader().getPlayer(p.getUniqueId());

		if (spleefPlayer == null) {
			Bukkit.getLogger().warning("getSpleefPlayer: No SpleefPlayer for UUID " + p.getUniqueId());
		}

		return spleefPlayer;
	}

	public Map<SpleefGame, List<SpleefSpectator>> getSpectators() {
		return spectators;
	}

	public void setSpectators(Map<SpleefGame, List<SpleefSpectator>> spectators) {
		this.spectators = new HashMap<>(spectators);
	}

	public void addSpectator(SpleefGame game, SpleefSpectator playerId) {
		spectators.computeIfAbsent(game, k -> new ArrayList<>()).add(playerId);
	}

	public void removeSpectator(SpleefGame game, SpleefSpectator playerId) {
		List<SpleefSpectator> list = spectators.get(game);
		if (list != null) {
			list.remove(playerId);
			if (list.isEmpty()) {
				spectators.remove(game);
			}
		}
	}

	public void clearSpectators(SpleefGame game) {
		spectators.remove(game);
	}

	public List<SpleefSpectator> getSpectatorsForGame(SpleefGame game) {
		return spectators.getOrDefault(game, Collections.emptyList());
	}

	public List<SpleefSpectator> getSpectatorsForSpleggGame(SpleggGame game) {
		return spectators.getOrDefault(game, Collections.emptyList());
	}

	public Integer getTeamCount(UUID playerUUID) {
		return teamCounts.get(playerUUID);
	}

	public void setTeamCount(UUID playerUUID, int teamCount) {
		if (teamCounts.containsKey(playerUUID)) {
			teamCounts.replace(playerUUID, teamCount);
		} else {
			teamCounts.put(playerUUID, teamCount);
		}
	}

	public void removeTeamCount(UUID playerUUID) {
		teamCounts.remove(playerUUID);
	}

	public HashMap<UUID, Integer> getSpleggFFAStats() {
		return spleggFFAStats;
	}

	public Integer getSpleggFFAStatsInt(UUID playerUUID) {
		return spleggFFAStats.getOrDefault(playerUUID, 0);
	}

	public void setSpleggFFAStatsInt(UUID playerUUID, int teamCount) {
		if (spleggFFAStats.containsKey(playerUUID)) {
			spleggFFAStats.replace(playerUUID, teamCount);
		} else {
			spleggFFAStats.put(playerUUID, teamCount);
		}
	}

	public void removeSpleggFFAStatsInt(UUID playerUUID) {
		spleggFFAStats.remove(playerUUID);
	}

	public Boolean getRandomized(UUID playerUUID) {
		if (isRandomized.get(playerUUID) != null) {
			return isRandomized.get(playerUUID);
		}
		return false;
	}

	public void setRandomized(UUID playerUUID, Boolean isRandom) {
		if (isRandomized.containsKey(playerUUID)) {
			isRandomized.replace(playerUUID, isRandom);
		} else {
			isRandomized.put(playerUUID, isRandom);
		}
	}

	public void removeRandomized(UUID playerUUID) {
		isRandomized.remove(playerUUID);
	}

	public boolean getSplegg(UUID uuid) {
		String sql = "SELECT splegg FROM player_powers WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean("splegg");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setSplegg(UUID uuid, boolean value) {
		String sql = "UPDATE player_powers SET splegg = ? WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, value);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean getPowers(UUID uuid) {
		String sql = "SELECT powers FROM player_powers WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean("powers");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void setPowers(UUID uuid, boolean value) {
		String sql = "UPDATE player_powers SET powers = ? WHERE uuid = ?";
		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, value);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createInteractableShovelRowIfMissing(UUID uuid) {
		String sql = "INSERT IGNORE INTO player_interactable_shovels (uuid) VALUES (?)";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasFoundInteractableShovel(UUID uuid, String shovelColumn) {
		createInteractableShovelRowIfMissing(uuid);

		String sql = "SELECT " + shovelColumn + " FROM player_interactable_shovels WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean(shovelColumn);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public void setFoundInteractableShovel(UUID uuid, String shovelColumn, boolean found) {
		createInteractableShovelRowIfMissing(uuid);

		String sql = "UPDATE player_interactable_shovels SET " + shovelColumn + " = ? WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, found);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ShovelPowers[] getCachedPowers(UUID uuid) {
		return playerPowersCache.get(uuid);
	}

	public void setCachedPowers(UUID uuid, ShovelPowers power1, ShovelPowers power2) {
		playerPowersCache.put(uuid, new ShovelPowers[] { power1, power2 });
	}

	public List<Player> getOnlinePlayers(UUID key) {
		return onlinePlayers.getOrDefault(key, new ArrayList<>());
	}

	public void setOnlinePlayers(UUID key, List<Player> players) {
		onlinePlayers.put(key, players);
	}

	public void addOnlinePlayer(UUID key, Player player) {
		onlinePlayers.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
	}

	public void removeOnlinePlayer(UUID key, Player player) {
		List<Player> players = onlinePlayers.get(key);
		if (players != null) {
			players.remove(player);
			if (players.isEmpty()) {
				onlinePlayers.remove(key);
			}
		}
	}

	public void addTeam(UUID playerUUID, Integer teamNumber) {
		playerTeams.putIfAbsent(playerUUID, new HashMap<>());
		Map<Integer, List<UUID>> teams = playerTeams.get(playerUUID);
		teams.putIfAbsent(teamNumber, new ArrayList<>());
	}

	public void addPlayerToTeam(UUID playerUUID, Integer teamNumber, UUID playerToAdd) {
		playerTeams.putIfAbsent(playerUUID, new HashMap<>());
		Map<Integer, List<UUID>> teams = playerTeams.get(playerUUID);
		teams.putIfAbsent(teamNumber, new ArrayList<>());

		List<UUID> team = teams.get(teamNumber);
		if (!team.contains(playerToAdd)) {
			team.add(playerToAdd);
		}
	}

	public void removePlayerFromTeam(UUID playerUUID, Integer teamNumber, UUID playerToRemove) {
		Map<Integer, List<UUID>> teams = playerTeams.get(playerUUID);
		if (teams != null) {
			List<UUID> team = teams.get(teamNumber);
			if (team != null) {
				team.remove(playerToRemove);

				if (team.isEmpty()) {
					teams.remove(teamNumber);
				}

				if (teams.isEmpty()) {
					playerTeams.remove(playerUUID);
				}
			}
		}
	}

	public Map<Integer, List<UUID>> getPlayerTeams(UUID playerUUID) {
		Map<Integer, List<UUID>> copy = new HashMap<>();
		Map<Integer, List<UUID>> teams = playerTeams.get(playerUUID);
		if (teams != null) {
			for (Map.Entry<Integer, List<UUID>> entry : teams.entrySet()) {
				copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
		}
		return copy;
	}

	public void clearTeams(UUID playerUUID) {
		playerTeams.remove(playerUUID);
	}

	public List<UUID> getTeam(UUID playerUUID, Integer teamNumber) {
		Map<Integer, List<UUID>> teams = playerTeams.get(playerUUID);
		if (teams != null) {
			return teams.getOrDefault(teamNumber, new ArrayList<>());
		}
		return new ArrayList<>();
	}

	public void addPlayerToRandomized(UUID uuid, Player player) {
		randomizedPlayers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(player);
	}

	public void removePlayerFromRandomized(UUID uuid, Player player) {
		List<Player> players = randomizedPlayers.get(uuid);
		if (players != null) {
			players.remove(player);
			if (players.isEmpty()) {
				randomizedPlayers.remove(uuid);
			}
		}
	}

	public List<Player> getRandomizedPlayers(UUID uuid) {
		if (randomizedPlayers.get(uuid) != null) {
			return randomizedPlayers.get(uuid);
		}
		return new ArrayList<>();
	}

	public void clearRandomized(UUID playerUUID) {
		randomizedPlayers.remove(playerUUID);
	}

	public void addPlayerToTraining(UUID uuid, TrainingSession session) {
		playersInTraining.put(uuid, session);
	}

	public void removePlayerFromTraining(UUID uuid) {
		playersInTraining.remove(uuid);
	}

	public TrainingSession getTrainingSession(UUID uuid) {
		return playersInTraining.get(uuid);
	}

	public void clearTraining(UUID uuid) {
		playersInTraining.remove(uuid);
	}

	public SpleggRankedArena getSpleggRankedArenaById(String id) {
		if (id == null) {
			return null;
		}

		for (SpleggRankedArena arena : getSpleggRankedArenas()) {
			if (arena != null && arena.getId() != null && arena.getId().equalsIgnoreCase(id)) {
				return arena;
			}
		}

		return null;
	}

	public boolean hasFavoritedArena(UUID uuid, SpleggRankedArena arena) {
		if (uuid == null || arena == null) {
			return false;
		}

		return getFavoritedArenas(uuid).stream()
				.anyMatch(a -> a != null && a.getId() != null && a.getId().equalsIgnoreCase(arena.getId()));
	}

	public List<SpleggRankedArena> getFavoritedArenas(UUID uuid) {
		return favoritedArenas.computeIfAbsent(uuid, k -> new ArrayList<>());
	}

	public void loadAllFavoritedArenas() {
		String sql = "SELECT player_uuid, arena_id FROM ranked_favorite_maps";

		favoritedArenas.clear();

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				UUID uuid = UUID.fromString(rs.getString("player_uuid"));
				String arenaId = rs.getString("arena_id");

				SpleggRankedArena arena = getSpleggRankedArenaById(arenaId);
				if (arena != null) {
					favoritedArenas.computeIfAbsent(uuid, k -> new ArrayList<>()).add(arena);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void loadFavoritedArenas(UUID uuid) {
		if (uuid == null) {
			return;
		}

		String sql = "SELECT arena_id FROM ranked_favorite_maps WHERE player_uuid = ?";

		List<SpleggRankedArena> loaded = new ArrayList<>();

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String arenaId = rs.getString("arena_id");
					SpleggRankedArena arena = getSpleggRankedArenaById(arenaId);
					if (arena != null) {
						loaded.add(arena);
					}
				}
			}

			if (loaded.isEmpty()) {
				favoritedArenas.remove(uuid);
			} else {
				favoritedArenas.put(uuid, loaded);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Map<UUID,Location> getRecallLocations() {
		return recallLocations;
	}
	
	public void removeRecallLocation(UUID uuid) {
		recallLocations.remove(uuid);
	}
	
	public void addRecallLocation(UUID uuid, Location location) {
		recallLocations.put(uuid, location);
	}

	public Map<UUID, UUID> getAllGravityAffected() {
		return gravityAffected;
	}

	public void setAllGravityAffected(Map<UUID, UUID> gravityAffectedMap) {
		gravityAffected.clear();

		if (gravityAffectedMap != null) {
			gravityAffected.putAll(gravityAffectedMap);
		}
	}

	public void addGravityAffected(UUID affectedUUID, UUID vortexOwnerUUID) {
		if (affectedUUID == null || vortexOwnerUUID == null) {
			return;
		}

		gravityAffected.put(affectedUUID, vortexOwnerUUID);
	}

	public void removeGravityAffected(UUID affectedUUID) {
		if (affectedUUID == null) {
			return;
		}

		gravityAffected.remove(affectedUUID);
	}

	public boolean isGravityAffected(UUID affectedUUID) {
		if (affectedUUID == null) {
			return false;
		}

		return gravityAffected.containsKey(affectedUUID);
	}

	public UUID getGravityVortexOwner(UUID affectedUUID) {
		if (affectedUUID == null) {
			return null;
		}

		return gravityAffected.get(affectedUUID);
	}

	public boolean isGravityAffectedBy(UUID affectedUUID, UUID vortexOwnerUUID) {
		if (affectedUUID == null || vortexOwnerUUID == null) {
			return false;
		}

		return vortexOwnerUUID.equals(gravityAffected.get(affectedUUID));
	}

	public List<UUID> getPlayersAffectedByVortex(UUID vortexOwnerUUID) {
		List<UUID> affectedPlayers = new ArrayList<>();

		if (vortexOwnerUUID == null) {
			return affectedPlayers;
		}

		for (Map.Entry<UUID, UUID> entry : gravityAffected.entrySet()) {
			UUID affectedUUID = entry.getKey();
			UUID ownerUUID = entry.getValue();

			if (vortexOwnerUUID.equals(ownerUUID)) {
				affectedPlayers.add(affectedUUID);
			}
		}

		return affectedPlayers;
	}

	public void removeAllGravityAffectedByVortex(UUID vortexOwnerUUID) {
		if (vortexOwnerUUID == null) {
			return;
		}

		gravityAffected.entrySet().removeIf(entry -> vortexOwnerUUID.equals(entry.getValue()));
	}

	public void clearGravityAffected() {
		gravityAffected.clear();
	}

	public void addBlockedWindCharge(WindCharge windCharge, Player shooter) {
		if (windCharge == null || shooter == null) {
			return;
		}

		blockedWindCharges.put(windCharge.getUniqueId(), shooter.getUniqueId());
	}

	public UUID getShooterByWindCharge(WindCharge windCharge) {
		if (windCharge == null) {
			return null;
		}

		return blockedWindCharges.get(windCharge.getUniqueId());
	}

	public boolean isBlockedWindCharge(WindCharge windCharge) {
		if (windCharge == null) {
			return false;
		}

		return blockedWindCharges.containsKey(windCharge.getUniqueId());
	}

	public void removeBlockedWindCharge(WindCharge windCharge) {
		if (windCharge == null) {
			return;
		}

		blockedWindCharges.remove(windCharge.getUniqueId());
	}

	public void setFavoritedArenas(UUID uuid, List<SpleggRankedArena> arenas) {
		favoritedArenas.put(uuid, arenas);
	}

	public void addFavoritedArena(UUID uuid, SpleggRankedArena arena) {
		if (uuid == null || arena == null || arena.getId() == null) {
			return;
		}

		String sql = "INSERT IGNORE INTO ranked_favorite_maps (player_uuid, arena_id) VALUES (?, ?)";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.setString(2, arena.getId());
			ps.executeUpdate();

			getFavoritedArenas(uuid).add(arena);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void removeFavoritedArena(UUID uuid, SpleggRankedArena arena) {
		if (uuid == null || arena == null || arena.getId() == null) {
			return;
		}

		String sql = "DELETE FROM ranked_favorite_maps WHERE player_uuid = ? AND arena_id = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.setString(2, arena.getId());
			ps.executeUpdate();

			List<SpleggRankedArena> favorites = favoritedArenas.get(uuid);
			if (favorites != null) {
				favorites.removeIf(a -> a != null && arena.getId().equalsIgnoreCase(a.getId()));
				if (favorites.isEmpty()) {
					favoritedArenas.remove(uuid);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void removeAllFavoritedArenas(UUID uuid) {
		favoritedArenas.remove(uuid);
	}

	public boolean hasFavoritedArenas(UUID uuid) {
		return favoritedArenas.containsKey(uuid);
	}

	public Map<UUID, List<SpleggRankedArena>> getAllFavoritedArenas() {
		return Collections.unmodifiableMap(favoritedArenas);
	}

	public List<Player> getFinalizedPlayers(UUID uuid) {
		if (finalizedPlayers.get(uuid) != null) {
			return finalizedPlayers.get(uuid);
		}
		return new ArrayList<>();
	}

	public void clearFinalized(UUID playerUUID) {
		finalizedPlayers.remove(playerUUID);
	}

	public void addPlayerToFinalized(UUID uuid, Player player) {
		finalizedPlayers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(player);
	}

	public void removePlayerFromFinalized(UUID uuid, Player player) {
		List<Player> players = finalizedPlayers.get(uuid);
		if (players != null) {
			players.remove(player);
			if (players.isEmpty()) {
				finalizedPlayers.remove(uuid);
			}
		}
	}

	public Boolean getFinalized(UUID playerUUID) {
		if (isFinalized.get(playerUUID) != null) {
			return isFinalized.get(playerUUID);
		}
		return false;
	}

	public void setFinalized(UUID playerUUID, Boolean isRandom) {
		if (isFinalized.containsKey(playerUUID)) {
			isFinalized.replace(playerUUID, isRandom);
		} else {
			isFinalized.put(playerUUID, isRandom);
		}
	}

	public void removeFinalized(UUID playerUUID) {
		isFinalized.remove(playerUUID);
	}

	public HashMap<UUID, Integer> getPlayToInts() {
		return playToInts;
	}

	public void setPlayToInts(HashMap<UUID, Integer> playToInts) {
		this.playToInts = playToInts;
	}

	public Integer getPlayToInt(UUID uuid) {
		return playToInts.get(uuid);
	}

	public void setPlayToInt(UUID uuid, Integer value) {
		playToInts.put(uuid, value);
	}

	public void removePlayToInt(UUID uuid) {
		playToInts.remove(uuid);
	}

	public HashMap<UUID, Integer> getPowerInts() {
		return playToInts;
	}

	public void setPowerInts(HashMap<UUID, Integer> playToInts) {
		this.playToInts = playToInts;
	}

	public Integer getPowerInt(UUID uuid) {
		return playToInts.get(uuid);
	}

	public void setPowerInt(UUID uuid, Integer value) {
		playToInts.put(uuid, value);
	}

	public void removePowerInt(UUID uuid) {
		playToInts.remove(uuid);
	}

	public List<Location> getIceSpawned(UUID uuid) {
		return iceSpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setIceSpawned(UUID uuid, List<Location> locations) {
		iceSpawned.put(uuid, locations);
	}

	public void addIceSpawned(UUID uuid, Location location) {
		iceSpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeIceSpawned(UUID uuid, Location location) {
		List<Location> list = iceSpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				iceSpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllIceSpawned(UUID uuid) {
		iceSpawned.remove(uuid);
	}

	public boolean hasIceSpawned(UUID uuid) {
		return iceSpawned.containsKey(uuid);
	}

	public List<Location> getSnowSpawnedByIce(UUID uuid) {
		return snowSpawnedByIce.getOrDefault(uuid, new ArrayList<>());
	}

	public void setSnowSpawnedByIce(UUID uuid, List<Location> locations) {
		snowSpawnedByIce.put(uuid, locations);
	}

	public void addSnowSpawnedByIce(UUID uuid, Location location) {
		snowSpawnedByIce.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeSnowSpawnedByIce(UUID uuid, Location location) {
		List<Location> list = snowSpawnedByIce.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				snowSpawnedByIce.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllSnowSpawnedByIce(UUID uuid) {
		snowSpawnedByIce.remove(uuid);
	}

	public boolean hasSnowSpawnedByIce(UUID uuid) {
		return snowSpawnedByIce.containsKey(uuid);
	}

	public Boolean getSnareUsed(UUID uuid) {
		return snareUsed.getOrDefault(uuid, false);
	}

	public void setSnareUsed(UUID uuid, Boolean value) {
		snareUsed.put(uuid, value);
	}

	public void removeSnareUsed(UUID uuid) {
		snareUsed.remove(uuid);
	}

	public Boolean getRecallActive(UUID uuid) {
		return recallActive.getOrDefault(uuid, false);
	}

	public void setRecallActive(UUID uuid, Boolean value) {
		recallActive.put(uuid, value);
	}

	public void removeRecallActive(UUID uuid) {
		recallActive.remove(uuid);
	}

	public Map<UUID, List<Location>> getAllIceSpawned() {
		return Collections.unmodifiableMap(iceSpawned);
	}

	public List<Location> getDecaySpawned(UUID uuid) {
		return decaySpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setDecaySpawned(UUID uuid, List<Location> locations) {
		decaySpawned.put(uuid, locations);
	}

	public void addDecaySpawned(UUID uuid, Location location) {
		decaySpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeDecaySpawned(UUID uuid, Location location) {
		List<Location> list = decaySpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				decaySpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllDecaySpawned(UUID uuid) {
		decaySpawned.remove(uuid);
	}

	public boolean hasDecaySpawned(UUID uuid) {
		return decaySpawned.containsKey(uuid);
	}

	public Map<UUID, List<Location>> getAllDecaySpawned() {
		return Collections.unmodifiableMap(decaySpawned);
	}

	public List<Location> getVortexSpawned(UUID uuid) {
		return vortexSpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setVortexSpawned(UUID uuid, List<Location> locations) {
		vortexSpawned.put(uuid, locations);
	}

	public void addVortexSpawned(UUID uuid, Location location) {
		vortexSpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeVortexSpawned(UUID uuid, Location location) {
		List<Location> list = vortexSpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				vortexSpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllVortexSpawned(UUID uuid) {
		vortexSpawned.remove(uuid);
	}

	public boolean hasVortexSpawned(UUID uuid) {
		return vortexSpawned.containsKey(uuid);
	}

	public Map<UUID, List<Location>> getAllVortexSpawned() {
		return Collections.unmodifiableMap(vortexSpawned);
	}

	public List<Location> getSmokeSpawned(UUID uuid) {
		return smokeSpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setSmokeSpawned(UUID uuid, List<Location> locations) {
		smokeSpawned.put(uuid, locations);
	}

	public void addSmokeSpawned(UUID uuid, Location location) {
		smokeSpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeSmokeSpawned(UUID uuid, Location location) {
		List<Location> list = smokeSpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				smokeSpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllSmokeSpawned(UUID uuid) {
		smokeSpawned.remove(uuid);
	}

	public boolean hasSmokeSpawned(UUID uuid) {
		return smokeSpawned.containsKey(uuid);
	}

	public Map<UUID, List<Location>> getAllSmokeSpawned() {
		return Collections.unmodifiableMap(smokeSpawned);
	}

	public List<Location> getTNTRunDecaySpawned(UUID uuid) {
		return tntRunDecaySpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setTNTRunDecaySpawned(UUID uuid, List<Location> locations) {
		tntRunDecaySpawned.put(uuid, locations);
	}

	public void addTNTRunDecaySpawned(UUID uuid, Location location) {
		tntRunDecaySpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeTNTRunDecaySpawned(UUID uuid, Location location) {
		List<Location> list = tntRunDecaySpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				tntRunDecaySpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllTNTRunDecaySpawned(UUID uuid) {
		tntRunDecaySpawned.remove(uuid);
	}

	public boolean hasTNTRunDecaySpawned(UUID uuid) {
		return tntRunDecaySpawned.containsKey(uuid);
	}

	public Map<UUID, List<Location>> getAllTNTRunDecaySpawned() {
		return Collections.unmodifiableMap(tntRunDecaySpawned);
	}

	public List<Location> getSnowSpawned(UUID uuid) {
		return snowSpawned.getOrDefault(uuid, new ArrayList<>());
	}

	public void setSnowSpawned(UUID uuid, List<Location> locations) {
		snowSpawned.put(uuid, locations);
	}

	public void addSnowSpawned(UUID uuid, Location location) {
		snowSpawned.computeIfAbsent(uuid, k -> new ArrayList<>()).add(location);
	}

	public void removeSnowSpawned(UUID uuid, Location location) {
		List<Location> list = snowSpawned.get(uuid);
		if (list != null) {
			list.remove(location);
			if (list.isEmpty()) {
				snowSpawned.remove(uuid); // Clean up if empty
			}
		}
	}

	public void removeAllSnowSpawned(UUID uuid) {
		snowSpawned.remove(uuid);
	}

	public boolean hasSnowSpawned(UUID uuid) {
		return snowSpawned.containsKey(uuid);
	}

	public Map<UUID, List<Location>> getAllSnowSpawned() {
		return Collections.unmodifiableMap(snowSpawned);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (!main.getDataLoader().players.containsKey(uuid)) {
			SpleefPlayer spleefPlayer = new SpleefPlayer(main, uuid);
			main.getDataLoader().addPlayer(uuid, spleefPlayer);
		}
	}

	public int getEggsShot(UUID uuid) {
		String sql = "SELECT eggs_fired FROM splegg_ffa_eggs_fired WHERE uuid = ?";
		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt("eggs_fired");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getWins(UUID uuid) {
		String sql = "SELECT wins FROM splegg_ffa_wins WHERE uuid = ?";
		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt("wins");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getPoints(UUID uuid) {
		String sql = "SELECT points FROM splegg_ffa_points WHERE uuid = ?";
		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt("points");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getBlocksBroken(UUID uuid) {
		String sql = "SELECT blocks_broken FROM splegg_ffa_blocks_broken WHERE uuid = ?";
		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt("blocks_broken");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public List<LeaderboardEntry> getTopSpleggFfaBlocksBroken(int limit) {
		String sql = "SELECT uuid, blocks_broken FROM splegg_ffa_blocks_broken ORDER BY blocks_broken DESC LIMIT ?";

		List<LeaderboardEntry> entries = new ArrayList<>();

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					UUID uuid = UUID.fromString(rs.getString("uuid"));
					double value = rs.getDouble("blocks_broken");
					entries.add(new LeaderboardEntry(uuid, value));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return entries;
	}

	public void addBlockBroken(UUID uuid) {
		String updateSql = "UPDATE splegg_ffa_blocks_broken SET blocks_broken = blocks_broken + 1 WHERE uuid = ?";
		String insertSql = "INSERT INTO splegg_ffa_blocks_broken (uuid, blocks_broken) VALUES (?, 1)";

		try (Connection conn = main.getConnection()) {

			// Try updating first
			try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
				ps.setString(1, uuid.toString());
				int rows = ps.executeUpdate();

				// If no row exists, insert it
				if (rows == 0) {
					try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
						insert.setString(1, uuid.toString());
						insert.executeUpdate();
					}
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addPoints(Connection connection, UUID uuid, int amount) {
		addCurrency(uuid,(amount*2));
		String sql = "UPDATE splegg_ffa_points SET points = points + ? WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, amount);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getSpleggElo(UUID uuid) {
		String selectSql = "SELECT elo FROM ranked_splegg_elo WHERE uuid = ?";
		String insertSql = "INSERT INTO ranked_splegg_elo (uuid, elo) VALUES (?, 1000)";

		try (Connection conn = main.getConnection()) {

			try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
				ps.setString(1, uuid.toString());

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("elo");
					}
				}
			}

			try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
				ps.setString(1, uuid.toString());
				ps.executeUpdate();
			}

			return 1000;

		} catch (SQLException e) {
			e.printStackTrace();
			return 1000;
		}
	}

	public void setSpleggElo(UUID uuid, int elo) {
		String sql = "INSERT INTO ranked_splegg_elo (uuid, elo) VALUES (?, ?) "
				+ "ON DUPLICATE KEY UPDATE elo = VALUES(elo)";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.setInt(2, elo);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addSpleggElo(UUID uuid, int amount) {
		int current = getSpleggElo(uuid);
		setSpleggElo(uuid, current + amount);
	}

	public void removeSpleggElo(UUID uuid, int amount) {
		int current = getSpleggElo(uuid);
		setSpleggElo(uuid, current - amount);
	}

	public void ensureRankedStats(UUID uuid, String table) {
		String sql = "INSERT INTO " + table + " (uuid) VALUES (?) ON DUPLICATE KEY UPDATE uuid = uuid";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addWin(UUID uuid, String table) {
		ensureRankedStats(uuid, table);

		String sql = "UPDATE " + table + " SET wins = wins + 1 WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addGame(UUID uuid, String table) {
		ensureRankedStats(uuid, table);

		String sql = "UPDATE " + table + " SET games_played = games_played + 1 WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<LeaderboardEntry> getTopSpleggFfaShells(int limit) {
		return fetchTopInt("splegg_ffa_points", "points", limit);
	}

	public List<LeaderboardEntry> getTopSpleggFfaEggs(int limit) {
		return fetchTopInt("splegg_ffa_eggs_fired", "eggs_fired", limit);
	}

	public List<LeaderboardEntry> getTopSpleggFfaWins(int limit) {
		return fetchTopInt("splegg_ffa_wins", "wins", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleggElo(int limit) {
		return fetchTopInt("ranked_splegg_elo", "elo", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleggWins(int limit) {
		return fetchTopInt("ranked_splegg_stats", "wins", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleggWinRate(int limit) {
		return fetchTopWinRate("ranked_splegg_stats", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleefElo(int limit) {
		return fetchTopInt("ranked_spleef_elo", "elo", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleefWins(int limit) {
		return fetchTopInt("ranked_spleef_stats", "wins", limit);
	}

	public List<LeaderboardEntry> getTopRankedSpleefWinRate(int limit) {
		return fetchTopWinRate("ranked_spleef_stats", limit);
	}

	private List<LeaderboardEntry> fetchTopInt(String table, String column, int limit) {
		List<LeaderboardEntry> list = new ArrayList<>();

		String sql = "SELECT uuid, " + column + " FROM " + table + " ORDER BY " + column + " DESC LIMIT ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String uuidString = rs.getString("uuid");
					double value = rs.getDouble(column);

					try {
						UUID uuid = UUID.fromString(uuidString);
						list.add(new LeaderboardEntry(uuid, value));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	private List<LeaderboardEntry> fetchTopWinRate(String table, int limit) {
		List<LeaderboardEntry> list = new ArrayList<>();

		String sql = "SELECT uuid, " + "ROUND((wins / NULLIF(games_played, 0)) * 100, 1) AS win_rate " + "FROM " + table
				+ " " + "WHERE games_played > 0 " + "ORDER BY win_rate DESC, wins DESC " + "LIMIT ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String uuidString = rs.getString("uuid");
					double value = rs.getDouble("win_rate");

					try {
						UUID uuid = UUID.fromString(uuidString);
						list.add(new LeaderboardEntry(uuid, value));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	private List<LeaderboardEntry> fetchTopWinRate(String winsTable, String winsColumn, String gamesTable,
			String gamesColumn, int limit) {
		List<LeaderboardEntry> list = new ArrayList<>();

		String sql = "SELECT uuid, " + "ROUND((wins / NULLIF(games_played, 0)) * 100, 1) AS win_rate " + "FROM "
				+ winsTable + " " + "WHERE games_played > 0 " + "ORDER BY win_rate DESC, wins DESC " + "LIMIT ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String uuidString = rs.getString("uuid");
					double value = rs.getDouble("win_rate");

					try {
						UUID uuid = UUID.fromString(uuidString);
						list.add(new LeaderboardEntry(uuid, value));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public long getRequiredShells(ShellRank rank) {
		switch (rank) {
		case FARMER:
			return 0;
		case COOK:
			return 2500;
		case CHEF:
			return 5000;
		case MASTER:
			return 50000;
		case HUMPTY_DUMPTY:
			return 125000;
		case RAMSAY:
			return 250000;
		case HOT_N_SPICY:
			return 500000;
		case SPLEGG_HEAD:
			return 600000;
		case BACON:
			return 750000;
		case SUNNYSIDE:
			return 1000000;
		case EASTER_BUNNY:
			return 2000000;
		case EGGSPERT:
			return -1;
		default:
			return 0;
		}
	}

	public Map<ShellRank, Boolean> getShellRanksAvailable(UUID uuid) {
		Map<ShellRank, Boolean> ranks = new HashMap<>();

		long shells = getPoints(uuid);

		ranks.put(ShellRank.FARMER, shells >= 0);
		ranks.put(ShellRank.COOK, shells >= 2500);
		ranks.put(ShellRank.CHEF, shells >= 5000);
		ranks.put(ShellRank.MASTER, shells >= 50000);
		ranks.put(ShellRank.HUMPTY_DUMPTY, shells >= 125000);
		ranks.put(ShellRank.RAMSAY, shells >= 250000);
		ranks.put(ShellRank.HOT_N_SPICY, shells >= 500000);
		ranks.put(ShellRank.SPLEGG_HEAD, shells >= 600000);
		ranks.put(ShellRank.BACON, shells >= 750000);
		ranks.put(ShellRank.SUNNYSIDE, shells >= 1000000);
		ranks.put(ShellRank.EASTER_BUNNY, shells >= 2000000);

		boolean eggspert = isTopPoints(uuid);
		ranks.put(ShellRank.EGGSPERT, eggspert);

		return ranks;
	}

	public long getRequiredShards(ShardShovel shovel) {
		switch (shovel) {
		case GLORY_RED: {
			return 10000;
		}
		case GLORY_BLUE: {
			return 25000;
		}
		case GLORY_GREEN: {
			return 50000;
		}
		case GLORY_PURPLE: {
			return 100000;
		}
		case GLORY_LIGHTBLUE: {
			return 200000;
		}
		case GLORY_PINK: {
			return 500000;
		}
		case GLORY_BLACK: {
			return 1000000;
		}
		}
		return 0;
	}

	public Map<ShardShovel, Boolean> getShardShovelsAvailable(UUID uuid) {
		Map<ShardShovel, Boolean> shovels = new HashMap<>();

		long shards = getShards(uuid);

		shovels.put(ShardShovel.GLORY_RED, shards >= 10000);
		shovels.put(ShardShovel.GLORY_BLUE, shards >= 25000);
		shovels.put(ShardShovel.GLORY_GREEN, shards >= 50000);
		shovels.put(ShardShovel.GLORY_PURPLE, shards >= 100000);
		shovels.put(ShardShovel.GLORY_LIGHTBLUE, shards >= 200000);
		shovels.put(ShardShovel.GLORY_PINK, shards >= 500000);
		shovels.put(ShardShovel.GLORY_BLACK, shards >= 1000000);

		return shovels;
	}

	public boolean isTopPoints(UUID uuid) {
		List<DataLoader.LeaderboardEntry> top = getTopSpleggFfaShells(1);
		return !top.isEmpty() && top.get(0).getUuid().equals(uuid);
	}

	public boolean hasAccessToShardShovel(UUID uuid, ShardShovel shovel) {
		Map<ShardShovel, Boolean> shovels = new HashMap<>();

		long shards = getShards(uuid);

		shovels.put(ShardShovel.GLORY_RED, shards >= 10000);
		shovels.put(ShardShovel.GLORY_BLUE, shards >= 25000);
		shovels.put(ShardShovel.GLORY_GREEN, shards >= 50000);
		shovels.put(ShardShovel.GLORY_PURPLE, shards >= 100000);
		shovels.put(ShardShovel.GLORY_LIGHTBLUE, shards >= 200000);
		shovels.put(ShardShovel.GLORY_PINK, shards >= 500000);
		shovels.put(ShardShovel.GLORY_BLACK, shards >= 1000000);

		if (shovels.get(shovel)) {
			return true;
		}
		return false;
	}

	public boolean hasAccessToRank(UUID uuid, Ranks wantedRank) {
		if (wantedRank == Ranks.DEFAULT) {
			return true;
		}

		Ranks highest = getHighestRank(uuid);
		return highest.ordinal() >= wantedRank.ordinal();
	}

	public Ranks getHighestRank(UUID uuid) {
		String sql = "SELECT highest_rank FROM player_ranks WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Ranks.valueOf(rs.getString("highest_rank"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return Ranks.DEFAULT;
	}

	public Ranks getSelectedRank(UUID uuid) {
		String sql = "SELECT selected_rank FROM player_ranks WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Ranks.valueOf(rs.getString("selected_rank"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return Ranks.DEFAULT;
	}

	public List<LeaderboardEntry> getTopSpleefFfaShards(int limit) {
		return getTopFromTable("spleef_ffa_stats", "shells", limit);
	}

	public List<LeaderboardEntry> getTopSpleefFfaWins(int limit) {
		return getTopFromTable("spleef_ffa_stats", "wins", limit);
	}

	public List<LeaderboardEntry> getTopSpleefFfaBlocksBroken(int limit) {
		return getTopFromTable("spleef_ffa_stats", "blocks_broken", limit);
	}

	public List<LeaderboardEntry> getTopFromTable(String tableName, String columnName, int limit) {
		List<LeaderboardEntry> entries = new ArrayList<>();

		String sql = "SELECT uuid, " + columnName + " AS value FROM " + tableName + " WHERE " + columnName + " > 0"
				+ " ORDER BY " + columnName + " DESC" + " LIMIT ?";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					UUID uuid = UUID.fromString(rs.getString("uuid"));
					double value = rs.getDouble("value");

					entries.add(new LeaderboardEntry(uuid, value));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return entries;
	}

	public void ensureSpleefFFAStats(UUID uuid) {
		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT IGNORE INTO spleef_ffa_stats (uuid, shells, wins, blocks_broken) VALUES (?, 0, 0, 0)")) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getShards(UUID uuid) {
		ensureSpleefFFAStats(uuid);

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement("SELECT shells FROM spleef_ffa_stats WHERE uuid = ?")) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("shells");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void setShards(UUID uuid, int shells) {
		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO spleef_ffa_stats (uuid, shells, wins, blocks_broken) VALUES (?, ?, 0, 0) "
								+ "ON DUPLICATE KEY UPDATE shells = VALUES(shells)")) {

			ps.setString(1, uuid.toString());
			ps.setInt(2, shells);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addShards(UUID uuid, int amount) {
		addCurrency(uuid,(amount*2));
		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO spleef_ffa_stats (uuid, shells, wins, blocks_broken) VALUES (?, ?, 0, 0) "
								+ "ON DUPLICATE KEY UPDATE shells = shells + VALUES(shells)")) {

			ps.setString(1, uuid.toString());
			ps.setInt(2, amount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getSpleefFFAWins(UUID uuid) {
		ensureSpleefFFAStats(uuid);

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement("SELECT wins FROM spleef_ffa_stats WHERE uuid = ?")) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("wins");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void addSpleefFFAWins(UUID uuid, int amount) {
		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO spleef_ffa_stats (uuid, shells, wins, blocks_broken) VALUES (?, 0, ?, 0) "
								+ "ON DUPLICATE KEY UPDATE wins = wins + VALUES(wins)")) {

			ps.setString(1, uuid.toString());
			ps.setInt(2, amount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addSpleefFFAWin(UUID uuid) {
		addSpleefFFAWins(uuid, 1);
	}

	public int getSpleefFFABlocksBroken(UUID uuid) {
		ensureSpleefFFAStats(uuid);

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn
						.prepareStatement("SELECT blocks_broken FROM spleef_ffa_stats WHERE uuid = ?")) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("blocks_broken");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void addSpleefFFABlocksBroken(UUID uuid, int amount) {
		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO spleef_ffa_stats (uuid, shells, wins, blocks_broken) VALUES (?, 0, 0, ?) "
								+ "ON DUPLICATE KEY UPDATE blocks_broken = blocks_broken + VALUES(blocks_broken)")) {

			ps.setString(1, uuid.toString());
			ps.setInt(2, amount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addSpleefFFABlockBroken(UUID uuid) {
		addSpleefFFABlocksBroken(uuid, 1);
	}

	public void createCosmeticRankDataIfMissing(UUID uuid) {
		String sql = "INSERT IGNORE INTO player_cosmetic_ranks (uuid) VALUES (?)";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createCosmeticRowIfNotExists(UUID uuid) {
		String sql = "INSERT IGNORE INTO player_cosmetic_ranks (uuid) VALUES (?)";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasPurchasableRank(UUID uuid, PurchasableRank rank) {
		createCosmeticRowIfNotExists(uuid);

		String column = switch (rank) {
		case POLAR -> "polar";
		case CUSTOM -> "custom_rank";
		case GLACIER -> "glacier";
		};

		String sql = "SELECT " + column + " FROM player_cosmetic_ranks WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean(column);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public String getCustomRankName(UUID uuid) {
		String sql = "SELECT custom_name FROM player_cosmetic_ranks WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String rank = rs.getString("custom_name");
					return rank == null ? "" : rank;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return "";
	}

	public void setCustomRankName(UUID uuid, String rank) {
		String sql = "UPDATE player_cosmetic_ranks SET custom_name = ?, custom_rank = ? WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, rank == null ? "" : rank);
			ps.setInt(2, (rank == null || rank.isBlank()) ? 0 : 1);
			ps.setString(3, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean hasCustomRank(UUID uuid) {
		String sql = "SELECT custom_rank FROM player_cosmetic_ranks WHERE uuid = ?";

		try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("custom_rank") == 1;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public int getModeSpleefElo(UUID uuid, String tableName) {
		String insert = "INSERT IGNORE INTO " + tableName + " (uuid, elo) VALUES (?, 1000)";
		String select = "SELECT elo FROM " + tableName + " WHERE uuid = ?";

		try (PreparedStatement insertPs = main.getConnection().prepareStatement(insert);
				PreparedStatement selectPs = main.getConnection().prepareStatement(select)) {

			insertPs.setString(1, uuid.toString());
			insertPs.executeUpdate();

			selectPs.setString(1, uuid.toString());
			try (ResultSet rs = selectPs.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("elo");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 1000;
	}

	public void setModeSpleefElo(UUID uuid, String tableName, int elo) {
		String sql = "INSERT INTO " + tableName + " (uuid, elo) VALUES (?, ?) "
				+ "ON DUPLICATE KEY UPDATE elo = VALUES(elo)";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setInt(2, elo);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getTeamsElo(UUID uuid) {
		String sql = "SELECT elo FROM teams_elo WHERE uuid = ?";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("elo");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		setTeamsElo(uuid, 1000);
		return 1000;
	}

	public void setTeamsElo(UUID uuid, int elo) {
		String sql = """
				    INSERT INTO teams_elo (uuid, elo)
				    VALUES (?, ?)
				    ON DUPLICATE KEY UPDATE elo = VALUES(elo)
				""";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setString(1, uuid.toString());
			ps.setInt(2, elo);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addTeamsElo(UUID uuid, int amount) {
		int current = getTeamsElo(uuid);
		setTeamsElo(uuid, current + amount);
	}

	public List<LeaderboardEntry> getTopTeamsElo(int limit) {
		List<LeaderboardEntry> entries = new ArrayList<>();

		String sql = "SELECT uuid, elo FROM teams_elo ORDER BY elo DESC LIMIT ?";

		try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
			ps.setInt(1, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					UUID uuid = UUID.fromString(rs.getString("uuid"));
					int elo = rs.getInt("elo");

					entries.add(new LeaderboardEntry(uuid, elo));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return entries;
	}

	public void addModeSpleefElo(UUID uuid, String tableName, int amount) {
		int current = getModeSpleefElo(uuid, tableName);
		setModeSpleefElo(uuid, tableName, current + amount);
	}

	public int getPowerSpleefElo(UUID uuid) {
		return getModeSpleefElo(uuid, "ranked_powerspleef_elo");
	}

	public void setPowerSpleefElo(UUID uuid, int elo) {
		setModeSpleefElo(uuid, "ranked_powerspleef_elo", elo);
	}

	public void addPowerSpleefElo(UUID uuid, int amount) {
		addModeSpleefElo(uuid, "ranked_powerspleef_elo", amount);
	}

	public int getAirSpleefElo(UUID uuid) {
		return getModeSpleefElo(uuid, "ranked_airspleef_elo");
	}

	public void setAirSpleefElo(UUID uuid, int elo) {
		setModeSpleefElo(uuid, "ranked_airspleef_elo", elo);
	}

	public void addAirSpleefElo(UUID uuid, int amount) {
		addModeSpleefElo(uuid, "ranked_airspleef_elo", amount);
	}

	public int getMiniSpleefElo(UUID uuid) {
		return getModeSpleefElo(uuid, "ranked_minispleef_elo");
	}

	public void setMiniSpleefElo(UUID uuid, int elo) {
		setModeSpleefElo(uuid, "ranked_minispleef_elo", elo);
	}

	public void addMiniSpleefElo(UUID uuid, int amount) {
		addModeSpleefElo(uuid, "ranked_minispleef_elo", amount);
	}

	public static class LeaderboardEntry {
		private final UUID uuid;
		private final double value;

		public LeaderboardEntry(UUID uuid, double value) {
			this.uuid = uuid;
			this.value = value;
		}

		public UUID getUuid() {
			return uuid;
		}

		public double getValue() {
			return value;
		}
	}
	
	public record PowerStat(long picks, long bans) {
	}

	public record WinStat(int wins, int gamesPlayed) {

		public double getWinRate() {
			if (gamesPlayed <= 0) {
				return 0.0;
			}

			return (wins * 100.0) / gamesPlayed;
		}
	}
	public Map<ShovelPowers, PowerStat> getPersonalPowerspleefPowerStats(UUID uuid) {
		Map<ShovelPowers, PowerStat> stats = new EnumMap<>(ShovelPowers.class);

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"SELECT power, picks, bans "
								+ "FROM ranked_powerspleef_player_power_stats "
								+ "WHERE uuid = ?")) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String powerName = rs.getString("power");

					try {
						ShovelPowers power = ShovelPowers.valueOf(powerName);

						if (power != ShovelPowers.NONE) {
							stats.put(power, new PowerStat(
									rs.getLong("picks"),
									rs.getLong("bans")));
						}
					} catch (IllegalArgumentException ignored) {
						// Ignores old or invalid power names stored in MySQL.
					}
				}

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return stats;
	}
	public Map<ShovelPowers, PowerStat> getGlobalPowerspleefPowerStats() {
		Map<ShovelPowers, PowerStat> stats = new EnumMap<>(ShovelPowers.class);

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"SELECT power, picks, bans "
								+ "FROM ranked_powerspleef_global_power_stats")) {

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String powerName = rs.getString("power");

					try {
						ShovelPowers power = ShovelPowers.valueOf(powerName);

						if (power != ShovelPowers.NONE) {
							stats.put(power, new PowerStat(
									rs.getLong("picks"),
									rs.getLong("bans")));
						}
					} catch (IllegalArgumentException ignored) {
						// Ignores old or invalid power names stored in MySQL.
					}
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return stats;
	}
	public void addRankedPowerspleefPowerPick(UUID uuid, ShovelPowers power, int amount) {
		if (uuid == null || power == null || power == ShovelPowers.NONE || amount <= 0) {
			return;
		}

		String personalSql =
				"INSERT INTO ranked_powerspleef_player_power_stats "
						+ "(uuid, power, picks, bans) VALUES (?, ?, ?, 0) "
						+ "ON DUPLICATE KEY UPDATE picks = picks + VALUES(picks)";

		String globalSql =
				"INSERT INTO ranked_powerspleef_global_power_stats "
						+ "(power, picks, bans) VALUES (?, ?, 0) "
						+ "ON DUPLICATE KEY UPDATE picks = picks + VALUES(picks)";

		try (Connection conn = main.getConnection()) {
			boolean previousAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);

			try (PreparedStatement personalPs = conn.prepareStatement(personalSql);
					PreparedStatement globalPs = conn.prepareStatement(globalSql)) {

				personalPs.setString(1, uuid.toString());
				personalPs.setString(2, power.name());
				personalPs.setInt(3, amount);
				personalPs.executeUpdate();

				globalPs.setString(1, power.name());
				globalPs.setInt(2, amount);
				globalPs.executeUpdate();

				conn.commit();

			} catch (SQLException e) {
				conn.rollback();
				throw e;

			} finally {
				conn.setAutoCommit(previousAutoCommit);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void addGlobalPowerspleefPowerBan(ShovelPowers power, int amount) {
		if (power == null || power == ShovelPowers.NONE || amount <= 0) {
			return;
		}

		String sql =
				"INSERT INTO ranked_powerspleef_global_power_stats "
						+ "(power, picks, bans) VALUES (?, 0, ?) "
						+ "ON DUPLICATE KEY UPDATE bans = bans + VALUES(bans)";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, power.name());
			ps.setInt(2, amount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void addRankedPowerspleefPowerBan(UUID uuid, ShovelPowers power, int amount) {
		if (uuid == null || power == null || power == ShovelPowers.NONE || amount <= 0) {
			return;
		}

		String personalSql =
				"INSERT INTO ranked_powerspleef_player_power_stats "
						+ "(uuid, power, picks, bans) VALUES (?, ?, 0, ?) "
						+ "ON DUPLICATE KEY UPDATE bans = bans + VALUES(bans)";

		String globalSql =
				"INSERT INTO ranked_powerspleef_global_power_stats "
						+ "(power, picks, bans) VALUES (?, 0, ?) "
						+ "ON DUPLICATE KEY UPDATE bans = bans + VALUES(bans)";

		try (Connection conn = main.getConnection()) {
			boolean previousAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);

			try (PreparedStatement personalPs = conn.prepareStatement(personalSql);
					PreparedStatement globalPs = conn.prepareStatement(globalSql)) {

				personalPs.setString(1, uuid.toString());
				personalPs.setString(2, power.name());
				personalPs.setInt(3, amount);
				personalPs.executeUpdate();

				globalPs.setString(1, power.name());
				globalPs.setInt(2, amount);
				globalPs.executeUpdate();

				conn.commit();

			} catch (SQLException e) {
				conn.rollback();
				throw e;

			} finally {
				conn.setAutoCommit(previousAutoCommit);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public long getCurrency(UUID uuid) {
		String sql = "SELECT currency FROM player_currency WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getLong("currency");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}
	public void addCurrency(UUID uuid, long amount) {
		String sql =
				"INSERT INTO player_currency (uuid, currency) VALUES (?, ?) "
						+ "ON DUPLICATE KEY UPDATE currency = currency + VALUES(currency)";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.setLong(2, amount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void setCurrency(UUID uuid, long amount) {
		long safeAmount = Math.max(0, amount);

		String sql =
				"INSERT INTO player_currency (uuid, currency) VALUES (?, ?) "
						+ "ON DUPLICATE KEY UPDATE currency = VALUES(currency)";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());
			ps.setLong(2, safeAmount);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public boolean removeCurrency(UUID uuid, long amount) {
		if (amount <= 0) {
			return false;
		}

		String sql =
				"UPDATE player_currency "
						+ "SET currency = currency - ? "
						+ "WHERE uuid = ? AND currency >= ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, amount);
			ps.setString(2, uuid.toString());
			ps.setLong(3, amount);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}
	public int getSpleggFFAWins(UUID uuid) {
		String sql = "SELECT wins FROM splegg_ffa_wins WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("wins");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}
	public int getSpleggFFAEggsFired(UUID uuid) {
		String sql = "SELECT eggs_fired FROM splegg_ffa_eggs_fired WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("eggs_fired");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}
	public int getSpleggFFABlocksBroken(UUID uuid) {
		String sql = "SELECT blocks_broken FROM splegg_ffa_blocks_broken WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("blocks_broken");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}
	public WinStat getRankedSpleggWinStats(UUID uuid) {
		String sql = "SELECT wins, games_played FROM ranked_splegg_stats WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new WinStat(
							rs.getInt("wins"),
							rs.getInt("games_played"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return new WinStat(0, 0);
	}
	public WinStat getRankedSpleefWinStats(UUID uuid) {
		String sql = "SELECT wins, games_played FROM ranked_spleef_stats WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new WinStat(
							rs.getInt("wins"),
							rs.getInt("games_played"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return new WinStat(0, 0);
	}
	private int getEloFromTable(UUID uuid, String tableName) {
		Set<String> permittedTables = Set.of(
				"ranked_airspleef_elo",
				"ranked_minispleef_elo",
				"ranked_powerspleef_elo",
				"ranked_splegg_elo");

		if (!permittedTables.contains(tableName)) {
			throw new IllegalArgumentException("Invalid ELO table: " + tableName);
		}

		String sql = "SELECT elo FROM " + tableName + " WHERE uuid = ?";

		try (Connection conn = main.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("elo");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 1000;
	}
	public int getRankedAirSpleefElo(UUID uuid) {
		return getEloFromTable(uuid, "ranked_airspleef_elo");
	}
	public int getRankedMiniSpleefElo(UUID uuid) {
		return getEloFromTable(uuid, "ranked_minispleef_elo");
	}
	public int getRankedPowerSpleefElo(UUID uuid) {
		return getEloFromTable(uuid, "ranked_powerspleef_elo");
	}
	public int getRankedSpleggElo(UUID uuid) {
		return getEloFromTable(uuid, "ranked_splegg_elo");
	}
}
