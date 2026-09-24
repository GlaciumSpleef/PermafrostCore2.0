
package io.paper.permafrost.core.game;

import static net.kyori.adventure.text.Component.text;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.google.common.base.Function;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleggArena.SpawnPoint;
import io.paper.permafrost.core.game.tasks.CancelTask;
import io.paper.permafrost.core.game.tasks.CountdownTask;
import io.paper.permafrost.core.game.tasks.GameTask;
import io.paper.permafrost.core.game.tasks.PowersListener;
import io.paper.permafrost.gui.ShovelPowersManager;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class SpleefGame implements Listener {
	private BukkitTask banEndTask;
	private BukkitTask powerSelectTask;
	private BukkitTask gameStartTask;
	private BukkitTask pregameCountdownTask;
	private boolean pregameCancelled = false;
	private SpleefGame game = this;
	private boolean splegg;
	private Main main;
	private Arena arena;
	private SpleggArena spleggArena;
	private SpleggRankedArena spleggRankedArena;
	private SpleefType spleefType;
	private DataLoader dataLoader;
	private int totalTime;
	private int roundTime;
	private int round;
	private Set<SpleefTeam> teams;
	private GameState gameState;
	private SpleefType type;
	private GameTask gameTask;
	private CancelTask cancel;
	private String winner;
	private String loser;
	private String wpoints;
	private String lpoints;
	private int roundCount;
	private int teamCount;
	private int playToInt;
	private boolean teamGame;
	private int secondsElapsed = 0;
	private BukkitRunnable timerTask;
	private boolean startingGame;
	private boolean roundEnding;
	private Map<String, TeamSpleefTeam> teamSpleefTeams = new HashMap<>();
	private Map<Integer, List<UUID>> playerTeams = new HashMap<>();
	private Location arenaCorner1;
	private Location arenaCorner2;
	private boolean ranked;
	private boolean endedByCommand = false;
	private SpleefMode spleefMode;
	private boolean gameEnding;
	private BukkitRunnable modeRoundTask;
	private int modeResetCount = 0;
	private int currentArenaSize = -1; // width/length, assumes square arena
	private Location currentArenaCorner1;
	private Location currentArenaCorner2;
	private BukkitRunnable modeWarningTask;
	private BukkitTask outerLayerDecayTask;
	private BukkitRunnable randomDecayTask;
	private static final long ROUND_RELEASE_DELAY_TICKS = 70L;
	private static final long PASSIVE_DECAY_INTERVAL_TICKS = 10L;
	private boolean ban = false;
	private ShovelPowers ban1;
	private ShovelPowers ban2;
	private Player banner1;
	private Player banner2;
	private BanningArea area;
	private boolean isInBanningPhase;
	private boolean isInPowerPhase;

	private final List<BukkitTask> activeShrinkTasks = new ArrayList<>();

	// private Set<SpleefTeam> teams;
	public SpleefGame(Main main, Arena arena, SpleefType spleefType) {
		this.teams = new HashSet<>();
		this.main = main;
		this.spleefType = spleefType;
		this.type = spleefType;
		this.arena = arena;
		dataLoader = new DataLoader(main);
		roundCount = 0;
	}

	public SpleefGame(Main main, boolean splegg, boolean ranked, SpleefMode spleefMode) {
		this.main = main;
		this.splegg = splegg;
		this.teams = new HashSet<>();
		dataLoader = new DataLoader(main);
		roundCount = 0;
		this.ranked = ranked;
		this.spleefMode = spleefMode;
	}

	public SpleefMode getSpleefMode() {
		return spleefMode;
	}

	public void setSpleefMode(SpleefMode spleefMode) {
		this.spleefMode = spleefMode;
	}

	public GameState getGameState() {
		return gameState;
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	public boolean isSplegg() {
		return splegg;
	}

	public void setBan(ShovelPowers ban, UUID uuid) {
		for (TeamSpleefTeam team : this.getTeamSpleefTeams()) {
			if (team.getPlayers().contains(uuid)) {
				if (team.getColor().equalsIgnoreCase("blue")) {
					ban1 = ban;
				} else if (team.getColor().equalsIgnoreCase("red")) {
					ban2 = ban;
				}

				return;
			}
		}
	}

	public void setBanner1(Player banner1) {
		this.banner1 = banner1;
	}

	public void setBanner2(Player banner2) {
		this.banner2 = banner2;
	}

	public Player getBanner1() {
		return banner1;
	}

	public Player getBanner2() {
		return banner2;
	}

	public boolean isInBanningPhase() {
		return isInBanningPhase;
	}

	public boolean isInPowerPhase() {
		return isInPowerPhase;
	}

	public void setInBanningPhase(boolean isInBanningPhase) {
		this.isInBanningPhase = isInBanningPhase;
	}

	public void setInPowerPhase(boolean isInPowerPhase) {
		this.isInPowerPhase = isInPowerPhase;
	}

	public ShovelPowers getBan1() {
		return ban1;
	}

	public ShovelPowers getBan2() {
		return ban2;
	}

	public boolean isBanned(ShovelPowers ban) {
		return ban == ban1 || ban == ban2;
	}

	public void removePlayer() {

	}

	public BanningArea getArea() {
		return area;
	}

	public void setBanning(boolean ban) {
		this.ban = ban;
	}

	public boolean isBanning() {
		return ban;
	}

	public void setGameEnding(boolean gameEnding) {
		this.gameEnding = gameEnding;
	}

	public boolean isGameEnding() {
		return gameEnding;
	}

	public boolean isRoundEnding() {
		return roundEnding;
	}

	public void setRoundEnding(boolean roundEnding) {
		this.roundEnding = roundEnding;
	}

	public SpleggRankedArena getSpleggRankedArena() {
		return spleggRankedArena;
	}

	public int getTotalTime() {
		return this.totalTime;
	}

	public void setSplegg(boolean splegg) {
		this.splegg = splegg;
	}

	public void setTotalTime(int time) {
		this.totalTime = time;
	}

	public int getRoundTime() {
		return this.roundTime;
	}

	public boolean isEndedByCommand() {
		return endedByCommand;
	}

	public void setEndedByCommand(boolean endedByCommand) {
		this.endedByCommand = endedByCommand;
	}

	public void setRoundTime(int i) {
		this.roundTime = i;
	}

	public int getRound() {
		return this.round;
	}

	public boolean getTeamGame() {
		return teamGame;
	}

	public void setTeamGame(boolean teamGame) {
		this.teamGame = teamGame;
	}

	public void setTeamCount(int teamCount) {
		this.teamCount = teamCount;
	}

	public boolean getStartingGame() {
		return startingGame;
	}

	public void setStartingGame(boolean startingGame) {
		this.startingGame = startingGame;
	}

	public int getTeamCount() {
		return teamCount;
	}

	public boolean isRanked() {
		return ranked;
	}

	public void setRanked(boolean ranked) {
		this.ranked = ranked;
	}

	public SpleefTeam getTeam(SpleefPlayer sp) {
		for (SpleefTeam team : this.teams) {
			for (UUID u : team.getPlayers()) {
				if (u.equals(sp.getUUID()))
					return team;
			}
		}
		return null;
	}

	private SpleefTeam checkEndRound() {
		List<SpleefTeam> aliveTeams = new ArrayList<>();
		for (SpleefTeam teams : this.teams) {
			for (SpleefPlayer sp : teams.getSpleefPlayers()) {
				if (!sp.isSpectating()) {
					aliveTeams.add(teams);
					break;
				}
			}
		}
		if (aliveTeams.size() == 1)
			return aliveTeams.get(0);
		return null;
	}

	public void setArena() {
		if (!splegg) {
			List<Arena> availableArenas = new ArrayList<>();

			int count = 0;
			for (TeamSpleefTeam team : teamSpleefTeams.values()) {
				for (UUID player : team.getPlayers()) {
					count++;
				}
			}

			for (Arena a : main.getDataLoader().getArenas()) {
				if (!a.isInGame() && a.getMaxPlayers() >= count) {
					availableArenas.add(a);
				}
			}

			if (availableArenas.isEmpty()) {
				return;
			}

			this.arena = availableArenas.get(new Random().nextInt(availableArenas.size()));
			this.arena.setInGame(true);
			return;
		}

		int teamCount = teamSpleefTeams.size();

		List<SpleggRankedArena> selectedFavoritesPool = new ArrayList<>();
		Set<SpleggRankedArena> combinedFavorites = new HashSet<>();
		Set<SpleggRankedArena> sharedFavorites = null;

		for (SpleefPlayer sp : this.getPlayers()) {
			if (sp == null || sp.getPlayer() == null) {
				continue;
			}

			UUID uuid = sp.getPlayer().getUniqueId();

			if (!main.getDataLoader().getAllFavoritedArenas().containsKey(uuid)) {
				continue;
			}

			Set<SpleggRankedArena> playerFavorites = new HashSet<>(main.getDataLoader().getFavoritedArenas(uuid));
			playerFavorites.removeIf(SpleggRankedArena::isInGame);

			if (playerFavorites.isEmpty()) {
				continue;
			}

			combinedFavorites.addAll(playerFavorites);

			if (sharedFavorites == null) {
				sharedFavorites = new HashSet<>(playerFavorites);
			} else {
				sharedFavorites.retainAll(playerFavorites);
			}
		}

		if (sharedFavorites != null && !sharedFavorites.isEmpty()) {
			selectedFavoritesPool.addAll(sharedFavorites);
		} else if (!combinedFavorites.isEmpty()) {
			selectedFavoritesPool.addAll(combinedFavorites);
		}

		SpleggRankedArena selectedArena = null;

		if (!selectedFavoritesPool.isEmpty()) {
			selectedArena = chooseArenaWithSpawnPriority(selectedFavoritesPool, teamCount, true);
		}

		if (selectedArena == null) {
			List<SpleggRankedArena> available = new ArrayList<>();

			for (SpleggRankedArena arena : main.getDataLoader().getSpleggRankedArenas()) {
				if (!arena.isInGame()) {
					available.add(arena);
				}
			}

			if (available.isEmpty()) {
				return;
			}

			selectedArena = chooseArenaWithSpawnPriority(available, teamCount, false);
		}

		this.spleggRankedArena = selectedArena;
		this.spleggRankedArena.setInGame(true);
	}

	private SpleggRankedArena chooseArenaWithSpawnPriority(List<SpleggRankedArena> arenas, int teamCount,
			boolean favoritesPool) {
		if (arenas == null || arenas.isEmpty()) {
			return null;
		}

		List<SpleggRankedArena> ideal = new ArrayList<>();
		List<SpleggRankedArena> usable = new ArrayList<>();
		List<SpleggRankedArena> bad = new ArrayList<>();

		for (SpleggRankedArena arena : arenas) {
			int score = getArenaSpawnScore(arena, teamCount, favoritesPool);

			if (score >= 200) {
				ideal.add(arena);
			} else if (score >= 50) {
				usable.add(arena);
			} else {
				bad.add(arena);
			}
		}

		Random random = new Random();

		if (!ideal.isEmpty()) {
			return ideal.get(random.nextInt(ideal.size()));
		}

		if (!usable.isEmpty()) {
			return usable.get(random.nextInt(usable.size()));
		}

		if (!bad.isEmpty()) {
			return bad.get(random.nextInt(bad.size()));
		}

		return null;
	}

	private int getArenaSpawnScore(SpleggRankedArena arena, int teamCount, boolean favoritesPool) {
		if (arena == null || arena.getPlayerSpawns() == null) {
			return -1000;
		}

		int spawnCount = arena.getPlayerSpawns().size();

		if (spawnCount <= 0) {
			return -1000;
		}

		if (spawnCount < teamCount) {
			return -500;
		}

		int score = 0;

		/*
		 * Favorites still get a bonus, but not enough to make a bad 4-spawn arena beat
		 * a much better 8-spawn arena for awkward team counts.
		 */
		if (favoritesPool) {
			score += 40;
		}

		// 1v1 or normal 2-team games are fine on 2+ spawns.
		if (teamCount <= 2) {
			score += 200;
			score += spawnCount;
			return score;
		}

		// More spawns are generally better for multi-team games.
		score += spawnCount * 10;

		// Perfectly even split is best.
		if (spawnCount % teamCount == 0) {
			score += 150;
		} else {
			int remainder = spawnCount % teamCount;
			score -= remainder * 25;
		}

		/*
		 * 4-spawn arenas are bad for 3 teams, 5 teams, 6 teams, etc. They are only
		 * ideal for 4 teams, and acceptable for 2 teams.
		 */
		if (spawnCount == 4 && teamCount != 4) {
			score -= 120;
		}

		/*
		 * 8-spawn arenas are preferred for awkward team counts like 3, 5, 6, 7.
		 */
		if (spawnCount >= 8) {
			score += 60;
		}

		return score;
	}

	public void saveTeams(Map<Integer, List<UUID>> playerTeams) {
		this.playerTeams = new HashMap<>();
		for (Map.Entry<Integer, List<UUID>> entry : playerTeams.entrySet()) {
			this.playerTeams.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
	}
	// teamspleef command end

	public Set<SpleefTeam> getTeams() {
		return teams;
	}

	// public void setTeams(Set<SpleefTeam> teams) {
	// this.teams = teams;
	// }

	public Set<SpleefPlayer> getPlayers() {
		if (teamGame == true) {
			Set<SpleefPlayer> set = new HashSet<>();
			for (Map.Entry<Integer, List<UUID>> entry : playerTeams.entrySet()) {
				Integer teamId = entry.getKey();
				List<UUID> teamPlayers = entry.getValue();
				for (UUID uuid : teamPlayers) {
					{
						SpleefPlayer sp = main.getDataLoader().getPlayer(uuid);
						if (sp != null) {
							set.add(sp);
						}
					}
				}
			}
			return set;
		} else {
			Set<SpleefPlayer> set = new HashSet<>();

			for (SpleefTeam team : this.teams) {
				for (UUID u : team.getPlayers()) {
					SpleefPlayer sp = main.getDataLoader().getPlayer(u);
					if (sp != null) {
						set.add(sp);
					}
				}
			}
			return set;
		}
	}

	public Arena getArena() {
		return this.arena;
	}

	public List<TeamSpleefTeam> getTeamSpleefTeams() {
		Set<TeamSpleefTeam> teams = new HashSet<>();
		List<String> colors = new ArrayList<>();
		for (SpleefPlayer p : this.getPlayers()) {
			String color = p.getTeam().getColor();
			if (!colors.contains(color)) {
				teams.add(p.getTeam());
				colors.add(color);
			}
		}
		return new ArrayList<>(teams);
	}

	public TeamSpleefTeam getTeamSpleefTeam(String color) {
		return teamSpleefTeams.get(color);
	}

	public void setTeamSpleefTeam(String color, List<UUID> players) {
		TeamSpleefTeam team = new TeamSpleefTeam(main, color, players);
		teamSpleefTeams.put(color, team);
	}

	public void setTeamSpleefTeamDead(String color, boolean dead) {
		TeamSpleefTeam team = new TeamSpleefTeam(main, color, teamSpleefTeams.get(color).getPlayers());
		if (team != null) {
			team.setDead(dead);
		}
	}

	public boolean isTeamSpleefTeamDead(String color) {
		TeamSpleefTeam team = new TeamSpleefTeam(main, color, teamSpleefTeams.get(color).getPlayers());
		return team.isDead();
	}

	// TEAMSPLEEF START

	public void startTeamGame() {
		if (this.ban == true) {
			for (BanningArea b : main.getDataLoader().getBanningAreas()) {
				if (!b.isInGame()) {
					this.area = b;
					area.setInGame(true);
					break;
				}
			}
			if (area == null) {
				for (TeamSpleefTeam team : teamSpleefTeams.values()) {
					for (UUID p : team.getPlayers()) {
						Player online = Bukkit.getPlayer(p);
						if (online != null) {
							SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(online);
							if (sp != null) {
								sp.setDead(true);
							}
							online.sendMessage("Banning Area was null! Game cancelled.");
						}
					}
				}
			} else {
				this.isInBanningPhase = true;
				Bukkit.getConsoleSender().sendMessage("Ban phase = true");
				Random random = new Random();

				Set<Player> pp2 = new HashSet<>();
				Player captain1 = null;
				Player captain2 = null;

				int teamNum = 1;

				for (TeamSpleefTeam team : teamSpleefTeams.values()) {
					List<Player> onlinePlayers = new ArrayList<>();

					for (UUID uuid : team.getPlayers()) {
						Player pll = Bukkit.getPlayer(uuid);

						if (pll == null || !pll.isOnline()) {
							continue;
						}

						pp2.add(pll);

						SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(pll);
						sp.setGame(this);

						onlinePlayers.add(pll);
					}

					if (onlinePlayers.isEmpty()) {
						teamNum++;
						continue;
					}

					Player randomCaptain = onlinePlayers.get(random.nextInt(onlinePlayers.size()));

					if (teamNum == 1) {
						captain1 = randomCaptain;
						setBanner1(randomCaptain);
					} else if (teamNum == 2) {
						captain2 = randomCaptain;
						setBanner2(randomCaptain);
					}

					teamNum++;
				}
				startPregameCountdown(pp2);
				teamNum = 1;
				for (TeamSpleefTeam team : teamSpleefTeams.values()) {
					for (UUID p : team.getPlayers()) {
						Player online = Bukkit.getPlayer(p);
						if (online != null) {
							online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
							if (teamNum == 1) {
								if (online.equals(captain1)) {
									online.teleport(area.getBanner1());
									faceLocation(online, area.getSpawn1());
									online.sendMessage(
											"§e[PowerSpleef] > §cYou were randomly selected to choose your teams ban! You have §e30 §cseconds to do so.");
								} else {
									online.sendMessage("§e[PowerSpleef] > §aPre-match has started! "
											+ captain1.getName() + " has §e30 §aseconds to choose your teams ban.");
									online.teleport(area.getSpawn1());
								}
							}
							if (teamNum == 2) {
								if (online.equals(captain2)) {
									online.teleport(area.getBanner2());
									faceLocation(online, area.getSpawn2());
									online.sendMessage(
											"§e[PowerSpleef] > §cYou were randomly selected to choose your teams ban! You have §e30 §cseconds to do so.");
								} else {
									online.teleport(area.getSpawn2());
									online.sendMessage("§e[PowerSpleef] > §aPre-match has started! "
											+ captain2.getName() + " has §e30 §aseconds to choose your teams ban.");
								}
							}
						}
					}
					teamNum++;
				}
				banEndTask = new BukkitRunnable() {
					@Override
					public void run() {
						if (pregameCancelled) {
							cancel();
							return;
						}
						isInBanningPhase = false;
						Bukkit.getConsoleSender().sendMessage("Ban phase = false");
						for (TeamSpleefTeam team : teamSpleefTeams.values()) {
							for (UUID p : team.getPlayers()) {
								Player online = Bukkit.getPlayer(p);

								if (online == null) {
									cancelPregame("A player left during ban selection.");
									return;
								}

								online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

								if (ban1 == null) {
									ban1 = ShovelPowers.NONE;
								}

								if (ban2 == null) {
									ban2 = ShovelPowers.NONE;
								}
								online.sendMessage(
										"§e[PowerSpleef] > §aBans have been selected! Blue team has chosen the §c"
												+ ban1 + "§a power, while Red team has chosen the §c" + ban2
												+ " §a power!");
								dataLoader.addGlobalPowerspleefPowerBan(ban1, 1);
								dataLoader.addGlobalPowerspleefPowerBan(ban2, 1);
							}
						}
					}
				}.runTaskLater(main, 600L);
				powerSelectTask = new BukkitRunnable() {
					@Override
					public void run() {
						if (pregameCancelled) {
							cancel();
							return;
						}

						for (TeamSpleefTeam team : teamSpleefTeams.values()) {
							for (UUID p : team.getPlayers()) {
								Player online = Bukkit.getPlayer(p);

								if (online == null) {
									cancelPregame("A player left before power selection.");
									return;
								}

								online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
								online.sendMessage(
										"§e[PowerSpleef] > §aYou now have 30 seconds to choose your powers!");
								main.getDataLoader().setPowerInt(online.getUniqueId(), 0);

								ShovelPowersManager menu = new ShovelPowersManager(main);
								menu.openPowersMenu(online);
							}
						}
						isInPowerPhase = true;
						Bukkit.getConsoleSender().sendMessage("Power phase = true");
					}
				}.runTaskLater(main, 800L);
				gameStartTask = new BukkitRunnable() {
					@Override
					public void run() {
						if (pregameCancelled) {
							cancel();
							return;
						}

						for (TeamSpleefTeam team : teamSpleefTeams.values()) {
							for (UUID uuid : team.getPlayers()) {
								Player online = Bukkit.getPlayer(uuid);

								if (online == null) {
									cancelPregame("A player left before the game started.");
									return;
								}
							}
						}

						startingGame = true;
						playToInt = 5;

						for (TeamSpleefTeam team : teamSpleefTeams.values()) {
							for (UUID uuid : team.getPlayers()) {
								Player online = Bukkit.getPlayer(uuid);

								online.sendMessage("§e[PowerSpleef] > §aGame starting now!");
								main.getDataLoader().getSpleefPlayer(online).setDead(false);
							}
						}
						isInPowerPhase = false;
						Bukkit.getConsoleSender().sendMessage("Power phase = false");
						sendStartDiscordMessage();

						for (TeamSpleefTeam team : teamSpleefTeams.values()) {
							for (UUID uuid : team.getPlayers()) {
								Player p = Bukkit.getPlayer(uuid);

								if (p == null) {
									cancelPregame("A player left before powers were applied.");
									return;
								}

								p.setAllowFlight(false);

								String[] powers = getPlayerPowers(uuid);

								ShovelPowers p1 = powers[0] == null ? null : ShovelPowers.valueOf(powers[0]);
								ShovelPowers p2 = powers[1] == null ? null : ShovelPowers.valueOf(powers[1]);

								main.getDataLoader().setCachedPowers(uuid, p1, p2);

								SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);

								sp.setAvailable(true);

								if (powers[0] != null) {
									sp.setSelectedPower(ShovelPowers.valueOf(powers[0].toUpperCase()));
									dataLoader.addRankedPowerspleefPowerPick(p.getUniqueId(),
											ShovelPowers.valueOf(powers[0].toUpperCase()), 1);
								}

								if (powers[1] != null) {
									sp.setDeselectedPower(ShovelPowers.valueOf(powers[1].toUpperCase()));
									dataLoader.addRankedPowerspleefPowerPick(p.getUniqueId(),
											ShovelPowers.valueOf(powers[1].toUpperCase()), 1);
								}
							}
						}

						removePlayersFromSpectatingOwnGame();
						Bukkit.broadcast(buildStartMessage());

						setArena();
						teamGame = true;
						startTeamRound();
						startTimer(main);
					}
				}.runTaskLater(main, 1400L);
			}
		} else {
			startingGame = true;
			this.playToInt = 5;

			for (TeamSpleefTeam team : teamSpleefTeams.values()) {
				for (UUID p : team.getPlayers()) {
					Player online = Bukkit.getPlayer(p);
					if (online != null) {
						main.getDataLoader().getSpleefPlayer(online).setDead(false);
					}
				}
			}

			sendStartDiscordMessage();

			for (TeamSpleefTeam team : teamSpleefTeams.values()) {
				for (UUID uuid : team.getPlayers()) {
					Player p = Bukkit.getPlayer(uuid);
					p.setAllowFlight(false);
					if (p == null)
						continue;

					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(p);

					String[] powers = getPlayerPowers(uuid);

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
			}

			removePlayersFromSpectatingOwnGame();
			Bukkit.broadcast(buildStartMessage());

			setArena();
			teamGame = true;
			startTeamRound();
			startTimer(main);
		}
	}

	public void startTeamRound() {
		if (!splegg) {
			cancelModeTasks();
			SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;

			if (mode == SpleefMode.AIRSPLEEF) {
				startAirSpleefRound();
				return;
			}

			if (mode == SpleefMode.MINISPLEEF) {
				startMiniSpleefRound();
				return;
			}

			startPowerSpleefRound();
			return;
		}

		if (ranked) {
			startRankedSpleggRound();
			return;
		}

		startNormalSpleggRound();
	}

	private void startPowerSpleefRound() {
		if (this.area != null) {
			area.setInGame(false);
			this.area = null;
		}
		this.roundTime = 0;
		for (SpleefPlayer sp : this.getPlayers()) {
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

			if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
				for (Location ice : main.getDataLoader().getIceSpawned(sp.getUUID())) {
					ice.getBlock().setType(Material.AIR);
				}
				main.getDataLoader().removeAllIceSpawned(sp.getUUID());
			}
			main.getAmmoManger().setAmmo(sp, ShovelPowers.BLACKGOO, 3);
			main.getAmmoManger().setAmmo(sp, ShovelPowers.LEVITATION, 3);
			if (sp.getPlayer() != null) {
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
			}
		}

		for (SpleefPlayer sp : this.getPlayers()) {
			sp.setSilenced(false);
			sp.setRoundStarting(true);
			sp.setGame(this);
			sp.setInTeamGame(true);
		}

		int count = 0;
		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			for (UUID uuid : team.getPlayers()) {
				Player player = Bukkit.getPlayer(uuid);
				count++;
				if (player != null) {
					Attribute reachAttr = resolveBlockReachAttr();
					AttributeInstance inst = player.getAttribute(reachAttr);
					inst.setBaseValue(4.5);
				}

				if (player == null || !player.isOnline()) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(offlinePlayer);
					if (sp != null) {
						sp.setDead(true);
					}
				}
			}
		}

		if (count > 4) {
			Bukkit.getLogger().info("count >4" + "  " + (Math.floor(count - 4) / 2) + "  " + ((count - 4) / 2));
			Location corner1 = arena.getCorner1().clone();
			Location corner2 = arena.getCorner2().clone();
			corner1.setX(corner1.getX() - Math.floor((count - 4) / 2));
			corner1.setZ(corner1.getZ() + Math.floor((count - 4) / 2));
			corner2.setX(corner2.getX() + Math.floor((count - 4) / 2));
			corner2.setZ(corner2.getZ() - Math.floor((count - 4) / 2));
			main.getFAWEAPI().createFlatSnowPlane(corner1, corner2, Material.SNOW_BLOCK);
			arenaCorner1 = corner1.clone();
			arenaCorner2 = corner2.clone();
		} else {
			main.getFAWEAPI().createFlatSnowPlane(arena.getCorner1(), arena.getCorner2(), Material.SNOW_BLOCK);
			arenaCorner1 = arena.getCorner1().clone();
			arenaCorner2 = arena.getCorner2().clone();
		}

		int cnt = 0;
		for (Map.Entry<Integer, List<UUID>> entry : playerTeams.entrySet()) {
			Integer teamId = entry.getKey();
			List<UUID> teamPlayers = entry.getValue();

			Location playerSpawn;
			for (UUID uuid : teamPlayers) {
				OfflinePlayer player2 = Bukkit.getOfflinePlayer(uuid);
				if (player2.isOnline()) {
					if (cnt == 0) {
						playerSpawn = getSpawnLocation(arena.getCenter(), -180 + (360.0 / teamCount) * teamId, 9, 90f,
								0f);
					} else {
						playerSpawn = getSpawnLocation(arena.getCenter(), -180 + (360.0 / teamCount) * teamId, 9, 270f,
								0f);
					}

					Player player = Bukkit.getPlayer(uuid);
					playerSpawn.setX(Math.floor(playerSpawn.getX()) + 0.5);
					playerSpawn.setZ(Math.floor(playerSpawn.getZ()) + 0.5);
					player.teleport(playerSpawn);
					faceLocation(player, arena.getCenter());

					PlayerInventory inventory = player.getPlayer().getInventory();
					inventory.clear();

					int modelData = main.getPlayerShovelModelData(player);

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

					if (!inventory.contains(shovel)) {
						inventory.addItem(shovel);
					}
					if (!ranked) {
						SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
						for (UUID uuid2 : sp.getTeam().getPlayers()) {
							Player p = Bukkit.getPlayer(uuid2);
							if (p != null)
								giveColoredLeatherTunic(p, sp.getTeam().getColor());
						}
					} else if (ranked && this.getPlayers().size() >= 3) {
						SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
						for (UUID uuid2 : sp.getTeam().getPlayers()) {
							Player p = Bukkit.getPlayer(uuid2);
							if (p != null)
								giveColoredLeatherTunic(p, sp.getTeam().getColor());
						}
					}

					player.setGameMode(GameMode.SURVIVAL);
					createGlassCase(player.getPlayer());
					new CountdownTask(main, this);

					new BukkitRunnable() {
						Location loc = player.getPlayer().getLocation();
						int px = loc.getBlockX();
						int py = loc.getBlockY();
						int pz = loc.getBlockZ();

						public void run() {
							for (int x = -1; x <= 1; x++) {
								for (int y = 0; y < 3; y++) {
									for (int z = -1; z <= 1; z++) {
										if (x == 0 && z == 0)
											continue;
										Location blockLoc = new Location(player.getPlayer().getWorld(), px + x, py + y,
												pz + z);
										blockLoc.getBlock().setType(Material.AIR);
									}
								}
							}
							Location blockLoc = new Location(player.getPlayer().getWorld(), px, py + 2, pz);
							blockLoc.getBlock().setType(Material.AIR);
						}
					}.runTaskLater(main, 70L);

					cnt++;
				}
			}
		}
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

	private void startAirSpleefRound() {
		this.roundTime = 0;
		this.modeResetCount = 0;

		prepareSpleefRoundPlayers(false);

		int count = getAliveTeamPlayerCount();
		setupArenaForMode(count, false);
		teleportAndEquipSpleefPlayers(SpleefMode.AIRSPLEEF);

		startModeResetTask(SpleefMode.AIRSPLEEF);
	}

	private void startMiniSpleefRound() {
		this.roundTime = 0;
		this.modeResetCount = 0;

		prepareSpleefRoundPlayers(false);
		setupArenaForMode(0, true);
		teleportAndEquipSpleefPlayers(SpleefMode.MINISPLEEF);

		startModeResetTask(SpleefMode.MINISPLEEF);
	}

	private void startRankedSpleggRound() {

		this.roundTime = 0;

		for (SpleefPlayer sp : this.getPlayers()) {
			sp.getPlayer().getInventory().clear();
			sp.setRoundStarting(true);
			sp.setSpectating(false);
			sp.setGame(this);
			sp.setInTeamGame(true);
		}

		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			for (UUID uuid : team.getPlayers()) {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null || !player.isOnline()) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(offlinePlayer);
					if (sp != null)
						sp.setDead(true);
				}
			}
		}

		if (spleggRankedArena != null) {
			File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", spleggRankedArena.getSchematicPath());
			main.getFAWEAPI().pasteSchematic(schemFile, spleggRankedArena.getPasteOrigin(), false);
		}

		if (spleggRankedArena != null) {
			List<Location> spawns = spleggRankedArena.getPlayerSpawns() == null ? new ArrayList<>()
					: new ArrayList<>(spleggRankedArena.getPlayerSpawns());

			List<TeamSpleefTeam> teams = new ArrayList<>(teamSpleefTeams.values());

			for (int i = 0; i < teams.size(); i++) {
				TeamSpleefTeam team = teams.get(i);

				Location point;

				if (i < spawns.size()) {
					point = spawns.get(i).clone();
				} else {
					point = getSpleggTeamSpawn(i, teams.size());
				}

				for (UUID uuid : team.getPlayers()) {
					Player player = Bukkit.getPlayer(uuid);
					if (player == null)
						continue;

					player.teleport(point);
					faceLocation(player, spleggRankedArena.getPasteOrigin());
				}
			}
		}

		for (List<UUID> teamPlayers : playerTeams.values()) {
			for (UUID uuid : teamPlayers) {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null)
					continue;

				PlayerInventory inventory = player.getInventory();
				inventory.clear();

				ItemStack shovel = new ItemStack(Material.GOLDEN_SHOVEL);
				ItemMeta meta = shovel.getItemMeta();
				if (meta != null) {
					meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
					meta.setUnbreakable(true);
					meta.setDisplayName("§9Splegg Gun");
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
					shovel.setItemMeta(meta);
				}

				if (!inventory.contains(Material.GOLDEN_SHOVEL)) {
					inventory.addItem(shovel);
				}

				String[] powers = getPlayerPowers(uuid);
				SpleefPlayer splayer = main.getDataLoader().getSpleefPlayer(player);
				if (splayer != null) {
					splayer.setAvailable(true);
					if (powers[0] != null)
						splayer.setSelectedPower(ShovelPowers.valueOf(powers[0].toUpperCase()));
					if (powers[1] != null)
						splayer.setDeselectedPower(ShovelPowers.valueOf(powers[1].toUpperCase()));
				}

				player.setGameMode(GameMode.SURVIVAL);
				createGlassCase(player);
				new CountdownTask(main, this);

				new BukkitRunnable() {
					final Location loc = player.getLocation();
					final int px = loc.getBlockX();
					final int py = loc.getBlockY();
					final int pz = loc.getBlockZ();

					@Override
					public void run() {
						for (int x = -1; x <= 1; x++) {
							for (int y = 0; y < 3; y++) {
								for (int z = -1; z <= 1; z++) {
									if (x == 0 && z == 0)
										continue;
									new Location(player.getWorld(), px + x, py + y, pz + z).getBlock()
											.setType(Material.AIR);
								}
							}
						}
						new Location(player.getWorld(), px, py + 2, pz).getBlock().setType(Material.AIR);
					}
				}.runTaskLater(main, 70L);
			}
		}
	}

	private void startNormalSpleggRound() {
		this.roundTime = 0;

		for (SpleefPlayer sp : this.getPlayers()) {
			sp.getPlayer().getInventory().clear();
			sp.setRoundStarting(true);
			sp.setGame(this);
			sp.setInTeamGame(true);
			sp.setSpectating(false);
		}

		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			for (UUID uuid : team.getPlayers()) {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null || !player.isOnline()) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(offlinePlayer);
					if (sp != null)
						sp.setDead(true);
				}
			}
		}

		if (spleggRankedArena != null) {
			File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", spleggRankedArena.getSchematicPath());
			main.getFAWEAPI().pasteSchematic(schemFile, spleggRankedArena.getPasteOrigin(), false);
		}

		if (spleggRankedArena != null && spleggRankedArena.getPlayerSpawns() != null) {
			List<Location> spawns = new ArrayList<>(spleggRankedArena.getPlayerSpawns());
			List<TeamSpleefTeam> teams = new ArrayList<>(teamSpleefTeams.values());

			if (spawns.isEmpty()) {
				Bukkit.getLogger().warning("No spawns found for arena!");
				return;
			}

			if (teams.size() > spawns.size()) {
				Bukkit.getLogger().warning("Not enough spawns for teams!");
			}

			int teamCount = teams.size();
			int spawnCount = spawns.size();

			for (int teamIndex = 0; teamIndex < teams.size(); teamIndex++) {
				TeamSpleefTeam team = teams.get(teamIndex);

				int spawnIndex;

				if (teamCount > 2) {
					// Spread teams evenly around all available spawns
					spawnIndex = (int) Math.round((double) teamIndex * spawnCount / teamCount);

					// Safety clamp
					if (spawnIndex >= spawnCount) {
						spawnIndex = spawnCount - 1;
					}
				} else {
					// For 1v1 or 2-team games, use your normal configured first spawns
					spawnIndex = teamIndex;
				}

				Location point = spawns.get(spawnIndex);

				for (UUID uuid : team.getPlayers()) {
					Player player = Bukkit.getPlayer(uuid);

					if (player == null) {
						continue;
					}

					player.teleport(point);
					faceLocation(player, spleggRankedArena.getPasteOrigin());
				}
			}
		}

		for (List<UUID> teamPlayers : playerTeams.values()) {
			for (UUID uuid : teamPlayers) {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null)
					continue;

				PlayerInventory inventory = player.getInventory();
				inventory.clear();

				ItemStack shovel = new ItemStack(Material.GOLDEN_SHOVEL);
				ItemMeta meta = shovel.getItemMeta();
				if (meta != null) {
					meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
					meta.setUnbreakable(true);
					meta.setDisplayName("§9Splegg Gun");
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
					shovel.setItemMeta(meta);
				}

				if (!inventory.contains(Material.GOLDEN_SHOVEL)) {
					inventory.addItem(shovel);
				}

				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
				if (sp != null && sp.getTeam() != null) {
					for (UUID mate : sp.getTeam().getPlayers()) {
						Player p = Bukkit.getPlayer(mate);
						if (p != null)
							giveColoredLeatherTunic(p, sp.getTeam().getColor());
					}
				}

				String[] powers = getPlayerPowers(uuid);
				SpleefPlayer splayer = main.getDataLoader().getSpleefPlayer(player);
				if (splayer != null) {
					splayer.setAvailable(true);
					if (powers[0] != null)
						splayer.setSelectedPower(ShovelPowers.valueOf(powers[0].toUpperCase()));
					if (powers[1] != null)
						splayer.setDeselectedPower(ShovelPowers.valueOf(powers[1].toUpperCase()));
				}

				player.setGameMode(GameMode.SURVIVAL);
				createGlassCase(player);
				new CountdownTask(main, this);

				new BukkitRunnable() {
					final Location loc = player.getLocation();
					final int px = loc.getBlockX();
					final int py = loc.getBlockY();
					final int pz = loc.getBlockZ();

					@Override
					public void run() {
						for (int x = -1; x <= 1; x++) {
							for (int y = 0; y < 3; y++) {
								for (int z = -1; z <= 1; z++) {
									if (x == 0 && z == 0)
										continue;
									new Location(player.getWorld(), px + x, py + y, pz + z).getBlock()
											.setType(Material.AIR);
								}
							}
						}
						new Location(player.getWorld(), px, py + 2, pz).getBlock().setType(Material.AIR);
					}
				}.runTaskLater(main, 70L);
			}
		}
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

	public void endTeamGame() {
		resetTimer();
		gameEnding = true;
		for (SpleefPlayer sp : this.getPlayers()) {
			if (sp.getPlayer() != null) {
				if (main.getDataLoader().getSnowSpawned(sp.getUUID()) != null) {
					for (Location snow : main.getDataLoader().getSnowSpawned(sp.getUUID())) {
						snow.getBlock().setType(Material.AIR);
					}
					main.getDataLoader().removeAllSnowSpawned(sp.getUUID());
				}

				if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
					for (Location ice : main.getDataLoader().getIceSpawned(sp.getUUID())) {
						ice.getBlock().setType(Material.AIR);
					}
					main.getDataLoader().removeAllIceSpawned(sp.getUUID());
				}
				String snareOwnerTag = "SnareOwner" + sp.getPlayer().getUniqueId().toString().replace("-", "");

				main.getDataLoader().setSnareUsed(sp.getPlayer().getUniqueId(), false);

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"kill @e[type=block_display,tag=" + snareOwnerTag + "]");
				sp.setRandomPower(null);
			}
		}

		List<TeamSpleefTeam> teams = new ArrayList<>(this.getTeamSpleefTeams());
		teams.sort(Comparator.comparingInt(TeamSpleefTeam::getPoints).reversed());

		boolean draw = false;

		for (TeamSpleefTeam team : teams) {
			team.setWinner(false);
		}

		if (!teams.isEmpty()) {
			int topScore = teams.get(0).getPoints();
			List<TeamSpleefTeam> topScoringTeams = teams.stream().filter(t -> t.getPoints() == topScore)
					.collect(Collectors.toList());

			if (topScoringTeams.size() == 1) {
				topScoringTeams.get(0).setWinner(true);
			} else {
				draw = true;
			}
		}

		int playerCount = teams.stream().mapToInt(t -> t.getPlayers().size()).sum();
		if (!splegg) {
			cancelModeTasks();
			if (playerCount > 4) {
				double delta = Math.floor((playerCount - 4) / 2.0);
				Bukkit.getLogger().info("count >4  " + delta);

				Location corner1 = arena.getCorner1().clone();
				Location corner2 = arena.getCorner2().clone();
				corner1.setX(corner1.getX() - delta);
				corner1.setZ(corner1.getZ() + delta);
				corner2.setX(corner2.getX() + delta);
				corner2.setZ(corner2.getZ() - delta);

				main.getFAWEAPI().createFlatSnowPlane(corner1, corner2, Material.AIR);
			} else {
				main.getFAWEAPI().createFlatSnowPlane(arena.getCorner1(), arena.getCorner2(), Material.AIR);
			}
			arena.setInGame(false);
		} else if (ranked && splegg) {
			spleggRankedArena.setInGame(false);
		} else if (splegg) {
			spleggRankedArena.setInGame(false);
		}

		StringBuilder scoreBuilder = new StringBuilder("[");
		for (int i = 0; i < teams.size(); i++) {
			scoreBuilder.append(teams.get(i).getPoints());
			if (i < teams.size() - 1) {
				scoreBuilder.append("-");
			}
		}
		scoreBuilder.append("]");
		String scoreString = scoreBuilder.toString();

		Function<TeamSpleefTeam, Component> createHoverTeamComponent = team -> {
			String colorName = capitalize(team.getColor());
			NamedTextColor textColor = getAdventureColor(team.getColor());

			StringBuilder hover = new StringBuilder();
			hover.append("§l").append(getChatColor(colorName)).append("§l").append(colorName)
					.append(" §lTeam\n§r§f§lPlayers:\n§r");

			for (int i = 0; i < team.getPlayers().size(); i++) {
				UUID uuid = team.getPlayers().get(i);
				OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
				String name = (p != null && p.getName() != null) ? p.getName() : "Unknown";
				hover.append("§r- §f").append(name);
				if (i < team.getPlayers().size() - 1) {
					hover.append("\n");
				}
			}

			return Component.text(colorName).color(textColor).decorate(TextDecoration.BOLD)
					.hoverEvent(HoverEvent.showText(Component.text(hover.toString())));
		};

		BiFunction<List<Component>, Boolean, Component> joinComponents = (components, useAnd) -> {
			Component joined = Component.empty();

			for (int i = 0; i < components.size(); i++) {
				if (i > 0 && components.size() > 2) {
					joined = joined.append(Component.text(", ", NamedTextColor.GRAY));
				}
				if (i == components.size() - 1 && i > 0 && useAnd) {
					joined = joined.append(Component.text(" and ", NamedTextColor.GRAY));
				}
				joined = joined.append(components.get(i));
			}

			return joined;
		};

		TeamSpleefTeam winnerTeam = teams.stream().filter(TeamSpleefTeam::isWinner).findFirst().orElse(null);
		List<TeamSpleefTeam> losingTeams = teams.stream().filter(t -> !t.isWinner()).collect(Collectors.toList());

		int winnerOldElo = -1;
		int loserOldElo = -1;
		int winnerNewElo = -1;
		int loserNewElo = -1;
		int winnerDelta = 0;
		int loserDelta = 0;
		UUID winnerUuid = null;
		UUID loserUuid = null;
		boolean rankedEloApplied = false;
		String eloLabel = "Teams ELO";

		boolean prematureRankedEnding = false;
		SpleefPlayer leftPlayer = null;
		TeamSpleefTeam leftPlayerTeam = null;
		TeamSpleefTeam otherTeam = null;
		boolean leftPlayerWasLosing = false;

		if (ranked && teams.size() == 2) {
			for (SpleefPlayer sp : this.getPlayers()) {
				if (sp.getPlayer() == null) {
					prematureRankedEnding = true;
					leftPlayer = sp;
					break;
				}
			}

			if (prematureRankedEnding && leftPlayer != null) {
				for (TeamSpleefTeam team : teams) {
					if (team.getPlayers().contains(leftPlayer.getUUID())) {
						leftPlayerTeam = team;
					} else {
						otherTeam = team;
					}
				}

				if (leftPlayerTeam != null && otherTeam != null) {
					leftPlayerWasLosing = leftPlayerTeam.getPoints() < otherTeam.getPoints();
				}
			}
		}

		boolean blockRankedEloBecauseCommand = this.isEndedByCommand();

		boolean allowRankedElo = ranked && !draw && !blockRankedEloBecauseCommand && teams.size() == 2
				&& winnerTeam != null && losingTeams.size() == 1 && (!prematureRankedEnding || leftPlayerWasLosing);

		if (allowRankedElo) {
			TeamSpleefTeam loserTeam = losingTeams.get(0);

			winnerUuid = !winnerTeam.getPlayers().isEmpty() ? winnerTeam.getPlayers().get(0) : null;
			loserUuid = !loserTeam.getPlayers().isEmpty() ? loserTeam.getPlayers().get(0) : null;

			// --- NEW: mode-based ELO getter ---
			Function<UUID, Integer> getElo = uuid -> {
				if (splegg)
					return main.getDataLoader().getSpleggElo(uuid);
				SpleefMode mode = this.getSpleefMode();
				switch (mode) {
				case AIRSPLEEF:
					return main.getDataLoader().getAirSpleefElo(uuid);
				case MINISPLEEF:
					return main.getDataLoader().getMiniSpleefElo(uuid);
				default:
					return main.getDataLoader().getPowerSpleefElo(uuid);
				}
			};

			// --- NEW: mode-based ELO setter ---
			BiConsumer<UUID, Integer> setElo = (uuid, newElo) -> {
				if (splegg) {
					main.getDataLoader().setSpleggElo(uuid, newElo);
					return;
				}
				SpleefMode mode = this.getSpleefMode();
				switch (mode) {
				case AIRSPLEEF:
					main.getDataLoader().setAirSpleefElo(uuid, newElo);
					break;
				case MINISPLEEF:
					main.getDataLoader().setMiniSpleefElo(uuid, newElo);
					break;
				default:
					main.getDataLoader().setPowerSpleefElo(uuid, newElo);
					break;
				}
			};

			double winnerAvgElo = winnerTeam.getPlayers().stream().mapToInt(getElo::apply).average().orElse(1000);

			double loserAvgElo = loserTeam.getPlayers().stream().mapToInt(getElo::apply).average().orElse(1000);

			winnerOldElo = (int) Math.round(winnerAvgElo);
			loserOldElo = (int) Math.round(loserAvgElo);

			int diffSteps = (int) (Math.abs(winnerAvgElo - loserAvgElo) / 50.0);
			int change = 20;

			if (winnerAvgElo < loserAvgElo) {
				change += diffSteps;
			} else if (winnerAvgElo > loserAvgElo) {
				change -= diffSteps;
			}

			change = Math.max(1, Math.min(40, change));

			winnerDelta = change;
			loserDelta = -change;

			winnerNewElo = winnerOldElo + winnerDelta;
			loserNewElo = loserOldElo + loserDelta;

			for (UUID uuid : winnerTeam.getPlayers()) {
				int oldElo = getElo.apply(uuid);
				setElo.accept(uuid, oldElo + winnerDelta);
				if (splegg) {
					Player winnerPlayer = Bukkit.getPlayer(uuid);
					if (winnerPlayer != null) {
						winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
						if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
							main.getDataLoader().addPoints(main.getConnection(), uuid, 200);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+200 shells)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
							main.getDataLoader().addPoints(main.getConnection(), uuid, 175);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+175 shells)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
							main.getDataLoader().addPoints(main.getConnection(), uuid, 150);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+150 shells)");
						} else {
							main.getDataLoader().addPoints(main.getConnection(), uuid, 100);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+100 shells)");
						}
					}
				}
			}

			for (UUID uuid : loserTeam.getPlayers()) {
				int oldElo = getElo.apply(uuid);
				setElo.accept(uuid, oldElo + loserDelta);
			}

			DataLoader dl = main.getDataLoader();
			for (UUID uuid : winnerTeam.getPlayers()) {
				if (splegg) {
					dl.addWin(uuid, "ranked_splegg_stats");
					dl.addGame(uuid, "ranked_splegg_stats");
				} else {
					dl.addWin(uuid, "ranked_spleef_stats");
					dl.addGame(uuid, "ranked_spleef_stats");
					if (!prematureRankedEnding) {
						if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
							dl.addShards(uuid, 200);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+200 shards)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
							dl.addShards(uuid, 175);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+175 shards)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
							dl.addShards(uuid, 150);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+150 shards)");
						} else {
							dl.addShards(uuid, 100);
							Bukkit.getPlayer(uuid).sendMessage("§aYou won! §f(+100 shards)");
						}
					}
				}
			}

			for (UUID uuid : loserTeam.getPlayers()) {
				if (splegg) {
					dl.addGame(uuid, "ranked_splegg_stats");
				} else {
					dl.addGame(uuid, "ranked_spleef_stats");

					Player loserPlayer = Bukkit.getPlayer(uuid);
					if (loserPlayer != null && !prematureRankedEnding) {
						if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
							dl.addShards(uuid, 100);
							loserPlayer.sendMessage("§cYou lost! §f(+100 shards)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
							dl.addShards(uuid, 87);
							Bukkit.getPlayer(uuid).sendMessage("§cYou lost! §f(+87 shards)");
						} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
							dl.addShards(uuid, 75);
							Bukkit.getPlayer(uuid).sendMessage("§cYou lost! §f(+75 shards)");
						} else {
							dl.addShards(uuid, 50);
							loserPlayer.sendMessage("§cYou lost! §f(+50 shards)");
						}
					}
				}
			}

			main.getSpleggStatsManager().updateAllHolograms();
			rankedEloApplied = true;
		}

		Component endMessage;

		if (ranked) {
			endMessage = buildRankedEndMessage(teams, draw, winnerTeam, losingTeams, scoreString, rankedEloApplied,
					winnerUuid, loserUuid, winnerOldElo, winnerNewElo, winnerDelta, loserOldElo, loserNewElo,
					loserDelta, blockRankedEloBecauseCommand, prematureRankedEnding, leftPlayerWasLosing);
		} else {
			endMessage = buildNormalEndMessage(teams, draw, winnerTeam, losingTeams, scoreString,
					createHoverTeamComponent, joinComponents);

			// Normal Spleef duel shard rewards.
			// Only applies to non-ranked, non-splegg, non-draw games with one winner team.
			if (!splegg && !draw && winnerTeam != null) {
				boolean premature = false;
				for (TeamSpleefTeam team : this.getTeamSpleefTeams()) {
					if (team != null)
						if (team.getPoints() != 5) {
							premature = true;
						}
				}
				if (!premature) {
					for (UUID uuid : winnerTeam.getPlayers()) {
						Player winnerPlayer = Bukkit.getPlayer(uuid);
						if (winnerPlayer != null && !draw) {
							if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
								main.getDataLoader().addShards(uuid, 100);
								winnerPlayer.sendMessage("§aYou won! §f(+100 shards)");
							} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
								main.getDataLoader().addShards(uuid, 87);
								winnerPlayer.sendMessage("§aYou won! §f(+87 shards)");
							} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
								main.getDataLoader().addShards(uuid, 75);
								winnerPlayer.sendMessage("§aYou won! §f(+75 shards)");
							} else {
								main.getDataLoader().addShards(uuid, 50);
								winnerPlayer.sendMessage("§aYou won! §f(+50 shards)");
							}
						}
					}

					for (TeamSpleefTeam losingTeam : losingTeams) {
						for (UUID uuid : losingTeam.getPlayers()) {
							Player loserPlayer = Bukkit.getPlayer(uuid);
							if (loserPlayer != null && !draw) {
								if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
									main.getDataLoader().addShards(uuid, 50);
									loserPlayer.sendMessage("§cYou lost! §f(+50 shards)");
								} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
									main.getDataLoader().addShards(uuid, 44);
									Bukkit.getPlayer(uuid).sendMessage("§cYou lost! §f(+44 shards)");
								} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
									main.getDataLoader().addShards(uuid, 37);
									Bukkit.getPlayer(uuid).sendMessage("§cYou lost! §f(+37 shards)");
								} else {
									main.getDataLoader().addShards(uuid, 25);
									loserPlayer.sendMessage("§cYou lost! §f(+25 shards)");
								}
							}
						}
					}
				}
			}
		}

		Bukkit.broadcast(endMessage);
		sendEndDiscordMessage(teams, draw, winnerTeam, losingTeams, scoreString, rankedEloApplied, winnerUuid,
				loserUuid, winnerNewElo, winnerDelta, loserNewElo, loserDelta, eloLabel, blockRankedEloBecauseCommand,
				prematureRankedEnding, leftPlayerWasLosing);

		Location spawn = new Location(Bukkit.getWorld("lobby"), 9012.5, 194, 8287.5);
		spawn.setYaw(180f);
		if (!this.splegg) {
			for (SpleefPlayer sp : this.getPlayers()) {
				if (sp != null && sp.getPlayer() != null && sp.getPlayer().isOnline()) {
					sp.setHasBeenRequested(false);
					sp.setPoints(0);
					sp.getPlayer().getInventory().clear();
					sp.getPlayer().teleport(spawn);
					sp.setInTeamGame(false);
					sp.setSpectating(false);
					sp.setGame(null);

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
					sp.getPlayer().getInventory().setItem(7, stats);

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

					sp.getPlayer().getInventory().setItem(8, item);
					sp.getPlayer().setExp(0);
					sp.getPlayer().setGameMode(GameMode.ADVENTURE);

				}

				if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
					main.getDataLoader().removeAllIceSpawned(sp.getUUID());
				}
				if (main.getDataLoader().getDecaySpawned(sp.getUUID()) != null) {
					main.getDataLoader().removeAllDecaySpawned(sp.getUUID());
				}
			}

		} else {
			for (SpleefPlayer sp : this.getPlayers()) {
				if (sp != null && sp.getPlayer() != null && sp.getPlayer().isOnline()) {
					sp.setHasBeenRequested(false);
					sp.setPoints(0);
					sp.getPlayer().getInventory().clear();
					sp.getPlayer().teleport(spawn);
					sp.setInTeamGame(false);
					sp.setSpectating(false);
					sp.setGame(null);

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
					sp.getPlayer().getInventory().setItem(7, stats);

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

					sp.getPlayer().getInventory().setItem(8, item);
					sp.getPlayer().setGameMode(GameMode.ADVENTURE);
				}

				if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
					main.getDataLoader().removeAllIceSpawned(sp.getUUID());
				}
				if (main.getDataLoader().getDecaySpawned(sp.getUUID()) != null) {
					main.getDataLoader().removeAllDecaySpawned(sp.getUUID());
				}
			}
		}

		for (SpleefSpectator spectator : main.getDataLoader().getSpectatorsForGame(this)) {
			spectator.setGame(null);
			spectator.setInGame(false);
			spectator.setInTeamGame(false);

			Player spectatorPlayer = Bukkit.getPlayer(spectator.getUUID());
			if (spectatorPlayer != null) {
				SpleefPlayer spectatorSp = main.getDataLoader().getSpleefPlayer(spectatorPlayer);
				if (spectatorSp != null) {
					spectatorSp.setGame(null);
				}

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
				spectatorPlayer.getInventory().setItem(7, stats);

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

				spectatorPlayer.getInventory().setItem(8, item);
				spectatorPlayer.teleport(spawn);
				spectatorPlayer.setGameMode(GameMode.SURVIVAL);
			}
		}

		main.getDataLoader().clearSpectators(this);

		teamGame = false;
		this.setEndedByCommand(false);
		main.getGameManager().getGames().remove(this);
		gameEnding = false;
	}

	private Location getSpleggTeamSpawn(int teamIndex, int totalTeams) {
		Location center = spleggRankedArena.getPasteOrigin().clone();

		double radius = 9.0;
		double angle = (2.0 * Math.PI * teamIndex) / totalTeams;

		double x = center.getX() + Math.cos(angle) * radius;
		double z = center.getZ() + Math.sin(angle) * radius;

		Location spawn = center.clone();
		spawn.setX(Math.floor(x) + 0.5);
		spawn.setZ(Math.floor(z) + 0.5);
		spawn.setY(center.getY() + 1);

		return spawn;
	}

	private Component buildStartMessage() {
		UUID spectateTarget = null;

		if (ranked) {
			Component message = Component.text("Started a ranked game of ", NamedTextColor.GRAY)
					.append(text(getRankedModeDisplayName(), getRankedModeColor()))
					.append(text(" between ", NamedTextColor.GRAY));

			List<TeamSpleefTeam> teams = new ArrayList<>(teamSpleefTeams.values());

			for (int i = 0; i < teams.size(); i++) {
				TeamSpleefTeam team = teams.get(i);

				if (spectateTarget == null && !team.getPlayers().isEmpty()) {
					spectateTarget = team.getPlayers().get(0);
				}

				if (i > 0) {
					message = message.append(text(" vs. ", NamedTextColor.GRAY));
				}

				message = message.append(buildRankedTeamDisplayComponent(team, i + 1));
			}

			if (spectateTarget != null) {
				Player spectatePlayer = Bukkit.getPlayer(spectateTarget);
				String spectateName;

				if (spectatePlayer != null) {
					spectateName = spectatePlayer.getName();
				} else {
					OfflinePlayer offline = Bukkit.getOfflinePlayer(spectateTarget);
					spectateName = offline != null && offline.getName() != null ? offline.getName() : "Unknown";
				}

				return Component.text("[RANKED] ", NamedTextColor.GOLD).append(message)
						.clickEvent(ClickEvent.runCommand("/spectate " + spectateName)).hoverEvent(HoverEvent.showText(
								Component.text("Click to spectate this ranked team match", NamedTextColor.YELLOW)));
			}

			return Component.text("[RANKED] ", NamedTextColor.GOLD).append(message);
		}

		Component message = Component.text("Started a game of ", NamedTextColor.GRAY)
				.append(text(splegg ? "SPLEGG" : "SPLEEF", splegg ? NamedTextColor.YELLOW : NamedTextColor.AQUA))
				.append(text(" between ", NamedTextColor.GRAY));

		List<Component> teamComponents = new ArrayList<>();

		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			String colorName = capitalize(team.getColor());
			NamedTextColor textColor = getAdventureColor(team.getColor());

			StringBuilder hover = new StringBuilder();
			hover.append("§l").append(getChatColor(colorName)).append("§l").append(colorName)
					.append(" §lTeam\n§r§f§lPlayers:\n§r");

			int count = team.getPlayers().size();

			for (UUID uuid : team.getPlayers()) {
				Player p = Bukkit.getPlayer(uuid);
				if (p == null)
					continue;

				String name = p.getName();
				count--;

				if (count == 0) {
					hover.append("§r- §f").append(name);
				} else {
					hover.append("§r- §f").append(name).append("\n");
				}
			}

			Component teamComponent = text(colorName).color(textColor).decorate(TextDecoration.BOLD)
					.hoverEvent(HoverEvent.showText(text(hover.toString())));

			teamComponents.add(teamComponent);
		}

		for (int i = 0; i < teamComponents.size(); i++) {
			if (i > 0 && teamComponents.size() > 2) {
				message = message.append(text(", ", NamedTextColor.GRAY));
			}
			if (i == teamComponents.size() - 1 && teamComponents.size() > 1) {
				message = message.append(text(" and ", NamedTextColor.GRAY));
			}
			message = message.append(teamComponents.get(i));
		}

		return message.append(text(" team", NamedTextColor.GRAY));
	}

	private Component buildRankedEndMessage(List<TeamSpleefTeam> teams, boolean draw, TeamSpleefTeam winnerTeam,
			List<TeamSpleefTeam> losingTeams, String scoreString, boolean rankedEloApplied, UUID winnerUuid,
			UUID loserUuid, int winnerOldElo, int winnerNewElo, int winnerDelta, int loserOldElo, int loserNewElo,
			int loserDelta, boolean blockRankedEloBecauseCommand, boolean prematureRankedEnding,
			boolean leftPlayerWasLosing) {

		if (draw) {
			Component message = Component.text("[RANKED] ", NamedTextColor.GOLD)
					.append(Component.text(getRankedModeDisplayName(), getRankedModeColor()))
					.append(Component.text(" team result: draw between ", NamedTextColor.GRAY));

			List<TeamSpleefTeam> drawTeams = teams.stream()
					.filter(t -> !teams.isEmpty() && t.getPoints() == teams.get(0).getPoints())
					.collect(Collectors.toList());

			for (int i = 0; i < drawTeams.size(); i++) {
				if (i > 0) {
					message = message.append(Component.text(" vs. ", NamedTextColor.GRAY));
				}

				message = message.append(buildRankedTeamDisplayComponent(drawTeams.get(i), i + 1));
			}

			return message.append(Component.text(" " + scoreString, NamedTextColor.DARK_GRAY));
		}

		TeamSpleefTeam loserTeam = !losingTeams.isEmpty() ? losingTeams.get(0) : null;

		Component message = Component.text("[RANKED] ", NamedTextColor.GOLD)
				.append(Component.text(getRankedModeDisplayName(), getRankedModeColor()))
				.append(Component.text(" team result: ", NamedTextColor.GRAY));

		if (winnerTeam != null) {
			int winnerTeamNumber = getTeamNumber(teams, winnerTeam);
			message = message.append(buildRankedTeamDisplayComponent(winnerTeam, winnerTeamNumber));
		} else {
			message = message.append(Component.text("Unknown Team", NamedTextColor.WHITE));
		}

		message = message.append(Component.text(" won against vs. ", NamedTextColor.GRAY));

		if (loserTeam != null) {
			int loserTeamNumber = getTeamNumber(teams, loserTeam);
			message = message.append(buildRankedTeamDisplayComponent(loserTeam, loserTeamNumber));
		} else {
			message = message.append(Component.text("Unknown Team", NamedTextColor.WHITE));
		}

		message = message.append(Component.text(" " + scoreString, NamedTextColor.DARK_GRAY));

		if (rankedEloApplied) {
			message = message.append(Component.text(" ", NamedTextColor.GRAY))
					.append(Component.text("(" + (winnerDelta >= 0 ? "+" : "") + winnerDelta + " / " + loserDelta + " "
							+ getRankedEloLabel() + ")", NamedTextColor.DARK_GRAY));
		}

		if (blockRankedEloBecauseCommand) {
			message = message
					.append(Component.text(" [no " + getRankedEloLabel() + ": ended by command]", NamedTextColor.RED));
		} else if (prematureRankedEnding && !leftPlayerWasLosing) {
			message = message.append(Component.text(" [no " + getRankedEloLabel() + ": player left while not losing]",
					NamedTextColor.RED));
		} else if (prematureRankedEnding && leftPlayerWasLosing) {
			message = message.append(Component.text(" [" + getRankedEloLabel() + " counted: player left while losing]",
					NamedTextColor.YELLOW));
		}

		return message;
	}

	private Component buildRankedTeamDisplayComponent(TeamSpleefTeam team, int teamNumber) {
		Component component = Component.text("Team " + teamNumber + " ", NamedTextColor.GRAY)
				.append(Component.text("(", NamedTextColor.DARK_GRAY));

		for (int i = 0; i < team.getPlayers().size(); i++) {
			UUID uuid = team.getPlayers().get(i);

			if (i > 0 && team.getPlayers().size() > 2) {
				component = component.append(Component.text(", ", NamedTextColor.GRAY));
			}

			if (i == team.getPlayers().size() - 1 && i > 0) {
				component = component.append(Component.text(" and ", NamedTextColor.GRAY));
			}

			component = component.append(buildRankedPlayerWithEloComponent(uuid));
		}

		component = component.append(Component.text(")", NamedTextColor.DARK_GRAY));

		return component;
	}

	private Component buildRankedPlayerWithEloComponent(UUID uuid) {
		OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
		String name = offline != null && offline.getName() != null ? offline.getName() : "Unknown";

		int elo = getRankedElo(uuid);
		String eloLabel = getRankedEloLabel();

		return Component.text(name, NamedTextColor.WHITE).append(Component.text(" (", NamedTextColor.DARK_GRAY))
				.append(Component.text(String.valueOf(elo), NamedTextColor.GOLD))
				.append(Component.text(" " + eloLabel, NamedTextColor.GRAY))
				.append(Component.text(")", NamedTextColor.DARK_GRAY));
	}

	private int getTeamNumber(List<TeamSpleefTeam> teams, TeamSpleefTeam targetTeam) {
		for (int i = 0; i < teams.size(); i++) {
			if (teams.get(i) == targetTeam) {
				return i + 1;
			}
		}

		return 1;
	}

	private Component buildNormalEndMessage(List<TeamSpleefTeam> teams, boolean draw, TeamSpleefTeam winnerTeam,
			List<TeamSpleefTeam> losingTeams, String scoreString,
			Function<TeamSpleefTeam, Component> createHoverTeamComponent,
			BiFunction<List<Component>, Boolean, Component> joinComponents) {

		if (draw) {
			List<TeamSpleefTeam> drawTeams = teams.stream().filter(t -> t.getPoints() == teams.get(0).getPoints())
					.collect(Collectors.toList());

			List<Component> drawComponents = drawTeams.stream().map(createHoverTeamComponent)
					.collect(Collectors.toList());

			return Component.text(splegg ? "SPLEGG" : "SPLEEF", splegg ? NamedTextColor.YELLOW : NamedTextColor.AQUA)
					.append(Component.text(" result: ", NamedTextColor.GRAY))
					.append(Component.text("draw between ", NamedTextColor.GRAY))
					.append(joinComponents.apply(drawComponents, true))
					.append(Component.text(" " + scoreString, NamedTextColor.DARK_GRAY));
		}

		List<Component> loserComponents = losingTeams.stream().map(createHoverTeamComponent)
				.collect(Collectors.toList());

		return Component.text(splegg ? "SPLEGG" : "SPLEEF", splegg ? NamedTextColor.YELLOW : NamedTextColor.AQUA)
				.append(Component.text(" result: ", NamedTextColor.GRAY))
				.append(winnerTeam != null ? createHoverTeamComponent.apply(winnerTeam)
						: Component.text("Unknown Team", NamedTextColor.WHITE))
				.append(Component.text(" defeated ", NamedTextColor.GRAY))
				.append(joinComponents.apply(loserComponents, true))
				.append(Component.text(" " + scoreString, NamedTextColor.DARK_GRAY));
	}

	private Component buildRankedTeamNamesComponent(TeamSpleefTeam team, boolean showHover) {
		Component component = Component.empty();

		for (int i = 0; i < team.getPlayers().size(); i++) {
			UUID uuid = team.getPlayers().get(i);

			if (i > 0 && team.getPlayers().size() > 2) {
				component = component.append(Component.text(", ", NamedTextColor.DARK_GRAY));
			}
			if (i == team.getPlayers().size() - 1 && i > 0) {
				component = component.append(Component.text(" and ", NamedTextColor.GRAY));
			}

			component = component.append(buildRankedPlayerStartComponent(uuid));
		}

		if (!showHover) {
			return component;
		}

		StringBuilder hover = new StringBuilder();
		hover.append("§b§lRanked Team\n§r§f§lPlayers:\n§r");

		for (int i = 0; i < team.getPlayers().size(); i++) {
			UUID uuid = team.getPlayers().get(i);
			OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
			String name = p != null && p.getName() != null ? p.getName() : "Unknown";

			hover.append("§r- §f").append(name);

			if (i < team.getPlayers().size() - 1) {
				hover.append("\n");
			}
		}

		return component.hoverEvent(HoverEvent.showText(Component.text(hover.toString())));
	}

	private Component buildRankedTeamResultComponent(TeamSpleefTeam team, int oldAvgElo, int newAvgElo, int delta) {
		Component names = buildRankedTeamNamesComponent(team, false);

		String sign = delta >= 0 ? "+" : "";
		String eloLabel = getRankedEloLabel();

		StringBuilder hover = new StringBuilder();
		hover.append("§b§lRanked Team\n");
		hover.append("§7Average ").append(eloLabel).append(": §f").append(oldAvgElo).append(" §7→ §f").append(newAvgElo)
				.append("\n");
		hover.append(delta >= 0 ? "§a" : "§c").append(sign).append(delta).append(" ").append(eloLabel).append("\n\n");
		hover.append("§f§lPlayers:\n§r");

		for (int i = 0; i < team.getPlayers().size(); i++) {
			UUID uuid = team.getPlayers().get(i);
			OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
			String name = p != null && p.getName() != null ? p.getName() : "Unknown";
			int currentElo = getRankedElo(uuid);

			hover.append("§r- §f").append(name).append(" §7(").append(currentElo).append(" ").append(eloLabel)
					.append(")");

			if (i < team.getPlayers().size() - 1) {
				hover.append("\n");
			}
		}

		return names
				.append(Component.text(" (" + sign + delta + ")",
						delta >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
				.hoverEvent(HoverEvent.showText(Component.text(hover.toString())));
	}

	private void sendStartDiscordMessage() {
		List<TeamSpleefTeam> teamsForDiscord = new ArrayList<>(teamSpleefTeams.values());

		Function<TeamSpleefTeam, String> teamWithPlayers = (team) -> {
			String teamName = capitalize(team.getColor()) + " team";

			String players = team.getPlayers().stream().map(uuid -> {
				OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
				String name = (p != null && p.getName() != null) ? p.getName() : "Unknown";

				if (ranked) {
					int elo = getRankedElo(uuid);
					return name + " (" + elo + " " + getRankedEloLabel() + ")";
				}

				return name;
			}).collect(Collectors.joining(", "));

			return players.isEmpty() ? teamName : teamName + " (" + players + ")";
		};

		BiFunction<List<String>, String, String> joinHuman = (items, lastSep) -> {
			if (items.isEmpty())
				return "";
			if (items.size() == 1)
				return items.get(0);
			if (items.size() == 2)
				return items.get(0) + " " + lastSep + " " + items.get(1);

			String middle = String.join(", ", items.subList(0, items.size() - 1));
			return middle + ", " + lastSep + " " + items.get(items.size() - 1);
		};

		StringBuilder msg2 = new StringBuilder("Started a game of **")
				.append(ranked ? getRankedModePrettyName().toUpperCase() : (splegg ? "SPLEGG" : "SPLEEF"))
				.append("** between ");

		List<String> parts = teamsForDiscord.stream().map(teamWithPlayers).collect(Collectors.toList());
		msg2.append(joinHuman.apply(parts, "and"));

		sendMessaage(msg2.toString());
	}

	private void sendEndDiscordMessage(List<TeamSpleefTeam> teams, boolean draw, TeamSpleefTeam winnerTeam,
			List<TeamSpleefTeam> losingTeams, String scoreString, boolean rankedEloApplied, UUID winnerUuid,
			UUID loserUuid, int winnerNewElo, int winnerDelta, int loserNewElo, int loserDelta, String eloLabel,
			boolean blockRankedEloBecauseCommand, boolean prematureRankedEnding, boolean leftPlayerWasLosing) {

		String modeName = ranked ? getRankedModePrettyName().toUpperCase() : (splegg ? "SPLEGG" : "SPLEEF");
		StringBuilder discordMessage = new StringBuilder(modeName).append(" ");

		Function<TeamSpleefTeam, String> teamWithPlayers = team -> {
			String teamName = capitalize(team.getColor()) + " Team";
			String players = team.getPlayers().stream().map(uuid -> {
				OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
				return (p != null && p.getName() != null) ? p.getName() : "Unknown";
			}).collect(Collectors.joining(", "));
			return teamName + " (" + players + ")";
		};

		BiFunction<List<String>, String, String> joinHuman = (items, lastSep) -> {
			if (items.isEmpty())
				return "";
			if (items.size() == 1)
				return items.get(0);
			if (items.size() == 2)
				return items.get(0) + " " + lastSep + " " + items.get(1);

			String middle = String.join(", ", items.subList(0, items.size() - 1));
			return middle + ", " + lastSep + " " + items.get(items.size() - 1);
		};

		if (ranked && rankedEloApplied) {
			OfflinePlayer winnerOffline = Bukkit.getOfflinePlayer(winnerUuid);
			OfflinePlayer loserOffline = Bukkit.getOfflinePlayer(loserUuid);

			String winnerName = (winnerOffline != null && winnerOffline.getName() != null) ? winnerOffline.getName()
					: "Unknown";
			String loserName = (loserOffline != null && loserOffline.getName() != null) ? loserOffline.getName()
					: "Unknown";

			discordMessage.insert(0, "**[RANKED]** ");
			discordMessage.append("result: ").append(winnerName).append(" (").append(winnerNewElo).append(" ")
					.append(eloLabel).append(", +").append(winnerDelta).append(")").append(" defeated ")
					.append(loserName).append(" (").append(loserNewElo).append(" ").append(eloLabel).append(", ")
					.append(loserDelta).append(")").append(" ").append(scoreString);

		} else if (draw) {
			List<TeamSpleefTeam> drawTeams = teams.stream().filter(t -> t.getPoints() == teams.get(0).getPoints())
					.collect(Collectors.toList());

			List<String> drawParts = drawTeams.stream().map(teamWithPlayers).collect(Collectors.toList());

			discordMessage.append("game tied between ").append(joinHuman.apply(drawParts, "and"))
					.append(" [game ended prematurely]");

		} else {
			List<String> loserParts = losingTeams.stream().map(teamWithPlayers).collect(Collectors.toList());

			discordMessage.append("game won by ")
					.append(winnerTeam != null ? teamWithPlayers.apply(winnerTeam) : "Unknown Team").append(" against ")
					.append(joinHuman.apply(loserParts, "and")).append(" ").append(scoreString);

			if (ranked && blockRankedEloBecauseCommand) {
				discordMessage.append(" [no ").append(eloLabel.toLowerCase()).append(": ended by command]");
			} else if (ranked && prematureRankedEnding && !leftPlayerWasLosing) {
				discordMessage.append(" [no ").append(eloLabel.toLowerCase()).append(": player left while not losing]");
			} else if (ranked && prematureRankedEnding && leftPlayerWasLosing) {
				discordMessage.append(" [").append(eloLabel.toLowerCase())
						.append(" counted: player left while losing]");
			}
		}

		sendMessaage(discordMessage.toString());
	}

	private String getRankedModeDisplayName() {
		if (splegg) {
			return "SPLEGG";
		}

		SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;
		switch (mode) {
		case AIRSPLEEF:
			return "AIRSPLEEF";
		case MINISPLEEF:
			return "MINISPLEEF";
		case POWERSPLEEF:
		default:
			return "POWERSPLEEF";
		}
	}

	private String getRankedModePrettyName() {
		if (splegg) {
			return "Splegg";
		}

		SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;
		switch (mode) {
		case AIRSPLEEF:
			return "AirSpleef";
		case MINISPLEEF:
			return "MiniSpleef";
		case POWERSPLEEF:
		default:
			return "PowerSpleef";
		}
	}

	public String getRankedEloLabel() {
		if (splegg) {
			return "Splegg ELO";
		}

		SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;
		switch (mode) {
		case AIRSPLEEF:
			return "Air ELO";
		case MINISPLEEF:
			return "Mini ELO";
		case POWERSPLEEF:
		default:
			return "Power ELO";
		}
	}

	public String getRankedSpleefEloTable() {
		SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;
		switch (mode) {
		case AIRSPLEEF:
			return "ranked_airspleef_elo";
		case MINISPLEEF:
			return "ranked_minispleef_elo";
		case POWERSPLEEF:
		default:
			return "ranked_powerspleef_elo";
		}
	}

	private int getRankedElo(UUID uuid) {
		if (splegg) {
			return main.getDataLoader().getSpleggElo(uuid);
		}

		String tableName = getRankedSpleefEloTable();
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

	private void setRankedElo(UUID uuid, int elo) {
		if (splegg) {
			main.getDataLoader().setSpleggElo(uuid, elo);
			return;
		}

		String tableName = getRankedSpleefEloTable();
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

	private NamedTextColor getRankedModeColor() {
		if (splegg) {
			return NamedTextColor.YELLOW;
		}

		SpleefMode mode = this.spleefMode == null ? SpleefMode.POWERSPLEEF : this.spleefMode;
		switch (mode) {
		case AIRSPLEEF:
			return NamedTextColor.AQUA;
		case MINISPLEEF:
			return NamedTextColor.LIGHT_PURPLE;
		case POWERSPLEEF:
		default:
			return NamedTextColor.GREEN;
		}
	}

	private void removePlayersFromSpectatingOwnGame() {
		for (SpleefGame game : main.getDataLoader().getSpectators().keySet()) {
			for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(game)) {
				for (SpleefPlayer sp : this.getPlayers()) {
					if (sp.getPlayer().getUniqueId() == ss.getUUID()) {
						if (sp != null) {
							SpleefSpectator spectator = sp.getSpectator();
							if (spectator != null) {
								if (spectator.isInGame() || spectator.isInTeamGame()) {
									if (game != null) {
										main.getDataLoader().getSpectatorsForGame(game).remove(spectator);
									}

									spectator.setGame(null);
									spectator.setInGame(false);
									spectator.setInTeamGame(false);

									sp.setSpectator(null);
									sp.setGame(null);
								}
							}
						}
					}
				}
			}
		}
	}

	private void prepareSpleefRoundPlayers(boolean powersEnabled) {
		for (SpleefPlayer sp : this.getPlayers()) {
			if (sp.getPlayer() == null) {
				continue;
			}

			if (main.getDataLoader().getSnowSpawned(sp.getUUID()) != null) {
				for (Location snow : main.getDataLoader().getSnowSpawned(sp.getUUID())) {
					snow.getBlock().setType(Material.AIR);
				}
				main.getDataLoader().removeAllSnowSpawned(sp.getUUID());
			}

			if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
				for (Location ice : main.getDataLoader().getIceSpawned(sp.getUUID())) {
					ice.getBlock().setType(Material.AIR);
				}
				main.getDataLoader().removeAllIceSpawned(sp.getUUID());
			}

			main.getAmmoManger().setAmmo(sp, ShovelPowers.BLACKGOO, powersEnabled ? 3 : 0);
			main.getAmmoManger().setAmmo(sp, ShovelPowers.LEVITATION, powersEnabled ? 3 : 0);

			for (PotionEffect effect : sp.getPlayer().getActivePotionEffects()) {
				if (effect.getType() != PotionEffectType.NIGHT_VISION)
					sp.getPlayer().removePotionEffect(effect.getType());
			}

			sp.setSilenced(false);
			sp.setRoundStarting(true);
			sp.setGame(this);
			sp.setInTeamGame(true);
			sp.setDead(false);

			if (!powersEnabled) {
				sp.setSelectedPower(ShovelPowers.NONE);
				sp.setDeselectedPower(ShovelPowers.NONE);
				sp.setAvailable(false);
			}
		}

		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			for (UUID uuid : team.getPlayers()) {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null || !player.isOnline()) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(offlinePlayer);
					if (sp != null) {
						sp.setDead(true);
					}
					continue;
				}

				Attribute reachAttr = resolveBlockReachAttr();
				AttributeInstance inst = player.getAttribute(reachAttr);
				if (inst != null) {
					inst.setBaseValue(4.5);
				}
			}
		}
	}

	private void setupArenaForMode(int count, boolean miniStart) {
		Location corner1;
		Location corner2;

		Location center = arena.getCenter().clone();

		if (miniStart) {
			corner1 = center.clone().add(-4, 0, -4);
			corner2 = center.clone().add(4, 0, 4);
			currentArenaSize = 9;
		} else {
			// 1v1 = 17x17, radius 8
			int radius = 8;

			// +2 radius per 1 extra player
			if (count > 2) {
				radius += ((count - 2) / 2);
			}

			corner1 = center.clone().add(-radius, 0, -radius);
			corner2 = center.clone().add(radius, 0, radius);

			currentArenaSize = radius * 2 + 1;
		}

		normalizeCorners(corner1, corner2);

		main.getFAWEAPI().createFlatSnowPlane(corner1, corner2, Material.SNOW_BLOCK);

		arenaCorner1 = corner1.clone();
		arenaCorner2 = corner2.clone();
		currentArenaCorner1 = corner1.clone();
		currentArenaCorner2 = corner2.clone();
	}

	private void teleportAndEquipSpleefPlayers(SpleefMode mode) {
		Location spawnCenter = (mode == SpleefMode.POWERSPLEEF || mode == SpleefMode.AIRSPLEEF) ? arena.getCenter()
				: (currentArenaCorner1 != null && currentArenaCorner2 != null
						? getArenaCenter(currentArenaCorner1, currentArenaCorner2)
						: arena.getCenter());

		for (Map.Entry<Integer, List<UUID>> entry : playerTeams.entrySet()) {
			Integer teamId = entry.getKey();
			List<UUID> teamPlayers = entry.getValue();

			int teamPlayerIndex = 0;

			for (UUID uuid : teamPlayers) {
				OfflinePlayer player2 = Bukkit.getOfflinePlayer(uuid);
				if (!player2.isOnline()) {
					continue;
				}

				Player player = Bukkit.getPlayer(uuid);
				if (player == null) {
					continue;
				}

				Location playerSpawn = getEdgeSpawnLocationForMode(mode, teamId, teamPlayerIndex);

				player.teleport(playerSpawn);
				faceLocation(player, spawnCenter);

				PlayerInventory inventory = player.getInventory();
				inventory.clear();

				int modelData = main.getPlayerShovelModelData(player);

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

				if (!inventory.contains(shovel)) {
					inventory.addItem(shovel);
				}

				if (!ranked) {
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
					for (UUID uuid2 : sp.getTeam().getPlayers()) {
						Player p = Bukkit.getPlayer(uuid2);
						if (p != null)
							giveColoredLeatherTunic(p, sp.getTeam().getColor());
					}
				} else if (ranked && this.getPlayers().size() >= 3) {
					SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
					for (UUID uuid2 : sp.getTeam().getPlayers()) {
						Player p = Bukkit.getPlayer(uuid2);
						if (p != null)
							giveColoredLeatherTunic(p, sp.getTeam().getColor());
					}
				}

				player.setGameMode(GameMode.SURVIVAL);
				createGlassCase(player);
				new CountdownTask(main, this);

				new BukkitRunnable() {
					final Location loc = player.getLocation();
					final int px = loc.getBlockX();
					final int py = loc.getBlockY();
					final int pz = loc.getBlockZ();

					@Override
					public void run() {
						for (int x = -1; x <= 1; x++) {
							for (int y = 0; y < 3; y++) {
								for (int z = -1; z <= 1; z++) {
									if (x == 0 && z == 0)
										continue;
									new Location(player.getWorld(), px + x, py + y, pz + z).getBlock()
											.setType(Material.AIR);
								}
							}
						}
						new Location(player.getWorld(), px, py + 2, pz).getBlock().setType(Material.AIR);
					}
				}.runTaskLater(main, 70L);

				teamPlayerIndex++;
			}
		}
	}

	private Location getEdgeSpawnLocationForMode(SpleefMode mode, int teamId, int cnt) {
		Location c1 = currentArenaCorner1 != null ? currentArenaCorner1 : arena.getCorner1();
		Location c2 = currentArenaCorner2 != null ? currentArenaCorner2 : arena.getCorner2();

		int minX = Math.min(c1.getBlockX(), c2.getBlockX());
		int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
		int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
		int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());
		int y = c1.getBlockY() + 1;

		boolean mini = mode == SpleefMode.MINISPLEEF;
		int inset = mini ? 0 : 1;

		int spawnMinX = minX + inset;
		int spawnMaxX = maxX - inset;
		int spawnMinZ = minZ + inset;
		int spawnMaxZ = maxZ - inset;

		if (spawnMinX > spawnMaxX) {
			spawnMinX = minX;
			spawnMaxX = maxX;
		}
		if (spawnMinZ > spawnMaxZ) {
			spawnMinZ = minZ;
			spawnMaxZ = maxZ;
		}

		int x;
		int z;

		if (teamCount <= 2) {
			if (teamId == 1) {
				x = (spawnMinX + spawnMaxX) / 2;
				z = spawnMinZ;
			} else {
				x = (spawnMinX + spawnMaxX) / 2;
				z = spawnMaxZ;
			}
		} else {
			int side = Math.floorMod(teamId - 1, 4);
			switch (side) {
			case 0 -> {
				x = (spawnMinX + spawnMaxX) / 2;
				z = spawnMinZ;
			}
			case 1 -> {
				x = spawnMaxX;
				z = (spawnMinZ + spawnMaxZ) / 2;
			}
			case 2 -> {
				x = (spawnMinX + spawnMaxX) / 2;
				z = spawnMaxZ;
			}
			default -> {
				x = spawnMinX;
				z = (spawnMinZ + spawnMaxZ) / 2;
			}
			}
		}

		Location loc = new Location(c1.getWorld(), x + 0.5, y, z + 0.5);

		Location center = getArenaCenter(c1, c2);
		Vector dir = center.toVector().subtract(loc.toVector());
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
		loc.setYaw(yaw);
		loc.setPitch(0f);

		return loc;
	}

	private Location getArenaCenter(Location c1, Location c2) {
		double x = (c1.getX() + c2.getX()) / 2.0;
		double y = c1.getY();
		double z = (c1.getZ() + c2.getZ()) / 2.0;
		return new Location(c1.getWorld(), x, y, z);
	}

	private void startModeResetTask(SpleefMode mode) {

		cancelModeTasks();

		final int resetSeconds = (mode == SpleefMode.AIRSPLEEF ? 12 : 8);
		final long resetTicks = resetSeconds * 20L;

		outerLayerDecayTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					cancel();
					outerLayerDecayTask = null;
					return;
				}

				if (mode == SpleefMode.AIRSPLEEF) {
					for (int i = 0; i < 3; i++) {
						final int index = i;

						BukkitTask task = new BukkitRunnable() {
							@Override
							public void run() {
								if (!teamGame)
									return;
								decayRandomOuterLayerBlockAnimated(3);
							}
						}.runTaskLater(main, index * 5L);

						activeShrinkTasks.add(task);
					}
				} else if (mode == SpleefMode.MINISPLEEF) {
					BukkitTask task = new BukkitRunnable() {
						@Override
						public void run() {
							if (!teamGame)
								return;

							decayRandomBlockAnimated();
						}
					}.runTaskLater(main, 0L);

					activeShrinkTasks.add(task);
				}
			}
		}.runTaskTimer(main, ROUND_RELEASE_DELAY_TICKS + PASSIVE_DECAY_INTERVAL_TICKS, PASSIVE_DECAY_INTERVAL_TICKS);

		modeWarningTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					cancel();
					modeWarningTask = null;
					return;
				}

				new BukkitRunnable() {

					int seconds = 3;

					final boolean willShrink = (mode == SpleefMode.AIRSPLEEF && (modeResetCount + 1) % 2 == 0
							&& currentArenaSize > 9)
							|| (mode == SpleefMode.MINISPLEEF && (modeResetCount + 1) % 4 == 0 && currentArenaSize > 7);

					BossBar bossBar = Bukkit.createBossBar(
							willShrink ? "§cArena shrinking in §e" + seconds + "§c..."
									: "§eArena resetting in §c" + seconds + "§e...",
							willShrink ? BarColor.RED : BarColor.YELLOW, BarStyle.SOLID);

					{
						for (SpleefPlayer sp : getPlayers()) {
							if (sp == null || sp.getPlayer() == null || sp.isRoundStarting()) {
								continue;
							}

							bossBar.addPlayer(sp.getPlayer());
						}

						if (main.getDataLoader().getSpectatorsForGame(game) != null) {
							for (SpleefSpectator spec : main.getDataLoader().getSpectatorsForGame(game)) {
								if (spec == null || Bukkit.getPlayer(spec.getUUID()) == null) {
									continue;
								}

								bossBar.addPlayer(Bukkit.getPlayer(spec.getUUID()));
							}
						}
					}

					@Override
					public void run() {
						if (!teamGame) {
							bossBar.removeAll();
							cancel();
							return;
						}

						String msg = willShrink ? "§cArena shrinking in §e" + seconds + "§c..."
								: "§eArena resetting in §c" + seconds + "§e...";

						if (seconds == 3) {
							for (SpleefPlayer sp : getPlayers()) {
								if (sp == null || sp.getPlayer() == null || sp.isRoundStarting()) {
									continue;
								}

								sp.getPlayer().sendMessage(msg);
							}

							if (main.getDataLoader().getSpectatorsForGame(game) != null) {
								for (SpleefSpectator spec : main.getDataLoader().getSpectatorsForGame(game)) {
									Player p = Bukkit.getPlayer(spec.getUUID());

									if (p == null) {
										continue;
									}

									p.sendMessage(msg);
								}
							}
						}

						for (SpleefPlayer sp : getPlayers()) {
							if (sp == null || sp.getPlayer() == null || sp.isRoundStarting()) {
								continue;
							}

							Player p = sp.getPlayer();
							p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
						}

						if (main.getDataLoader().getSpectatorsForGame(game) != null) {
							for (SpleefSpectator spec : main.getDataLoader().getSpectatorsForGame(game)) {
								Player p = Bukkit.getPlayer(spec.getUUID());

								if (p == null) {
									continue;
								}

								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
							}
						}

						bossBar.setTitle(msg);
						bossBar.setProgress(Math.max(0.0, seconds / 3.0));

						if (seconds <= 0) {
							bossBar.setTitle(willShrink ? "§cShrinking..." : "§aResetting...");
							bossBar.setProgress(0);

							Bukkit.getScheduler().runTaskLater(main, bossBar::removeAll, 20L);
							cancel();
							return;
						}

						seconds--;
					}
				}.runTaskTimer(main, 0L, 20L);
			}
		};

		modeWarningTask.runTaskTimer(main, ROUND_RELEASE_DELAY_TICKS + resetTicks - 60L, resetTicks);

		modeRoundTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					cancel();
					modeRoundTask = null;
					return;
				}

				modeResetCount++;

				cancelPendingShrinkTasks();
				resetCurrentArena();

				if (mode == SpleefMode.AIRSPLEEF) {
					if (modeResetCount % 2 == 0 && currentArenaSize > 9) {
						shrinkEvenLayer();
					}
				} else if (mode == SpleefMode.MINISPLEEF) {
					if (modeResetCount % 4 == 0 && currentArenaSize > 7) {
						shrinkMiniOuterLayerUneven();
					}
				}
			}
		};
		modeRoundTask.runTaskTimer(main, ROUND_RELEASE_DELAY_TICKS + resetTicks, resetTicks);
	}

	private void decayRandomOuterLayerBlockAnimated(int layers) {
		if (currentArenaCorner1 == null || currentArenaCorner2 == null) {
			return;
		}

		List<Location> outerBlocks = getOuterLayers(currentArenaCorner1, currentArenaCorner2, layers);

		if (outerBlocks.isEmpty()) {
			return;
		}

		Collections.shuffle(outerBlocks);

		Location chosen = null;

		for (Location loc : outerBlocks) {
			Material type = loc.getBlock().getType();

			if (type != Material.AIR && type != Material.RED_CONCRETE_POWDER
					&& type != Material.ORANGE_CONCRETE_POWDER) {
				chosen = loc;
				break;
			}
		}

		if (chosen == null) {
			return;
		}

		Location finalChosen = chosen;
		finalChosen.getBlock().setType(Material.ORANGE_CONCRETE_POWDER);

		BukkitTask redTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}

				if (finalChosen.getBlock().getType() == Material.ORANGE_CONCRETE_POWDER) {
					finalChosen.getBlock().setType(Material.RED_CONCRETE_POWDER);
				}
			}
		}.runTaskLater(main, 20L);
		activeShrinkTasks.add(redTask);

		BukkitTask airTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}

				finalChosen.getBlock().setType(Material.AIR);
			}
		}.runTaskLater(main, 40L);
		activeShrinkTasks.add(airTask);
	}

	private List<Location> getOuterLayers(Location c1, Location c2, int layers) {
		List<Location> blocks = new ArrayList<>();

		int minX = Math.min(c1.getBlockX(), c2.getBlockX());
		int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
		int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
		int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());
		int y = c1.getBlockY();
		World world = c1.getWorld();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int distanceFromEdge = Math.min(Math.min(x - minX, maxX - x), Math.min(z - minZ, maxZ - z));

				if (distanceFromEdge < layers) {
					blocks.add(new Location(world, x, y, z));
				}
			}
		}

		return blocks;
	}

	private void decayRandomBlockAnimated() {
		if (currentArenaCorner1 == null || currentArenaCorner2 == null) {
			return;
		}

		int minX = Math.min(currentArenaCorner1.getBlockX(), currentArenaCorner2.getBlockX());
		int maxX = Math.max(currentArenaCorner1.getBlockX(), currentArenaCorner2.getBlockX());
		int minZ = Math.min(currentArenaCorner1.getBlockZ(), currentArenaCorner2.getBlockZ());
		int maxZ = Math.max(currentArenaCorner1.getBlockZ(), currentArenaCorner2.getBlockZ());
		int y = currentArenaCorner1.getBlockY();

		Location chosen = null;

		for (int attempts = 0; attempts < 25; attempts++) {
			int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
			int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

			Location loc = new Location(currentArenaCorner1.getWorld(), x, y, z);
			Material type = loc.getBlock().getType();

			if (type != Material.AIR && type != Material.RED_CONCRETE_POWDER
					&& type != Material.ORANGE_CONCRETE_POWDER) {
				chosen = loc;
				break;
			}
		}

		if (chosen == null) {
			return;
		}

		Location finalChosen = chosen;
		finalChosen.getBlock().setType(Material.ORANGE_CONCRETE_POWDER);

		BukkitTask orangeTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}
				if (finalChosen.getBlock().getType() == Material.ORANGE_CONCRETE_POWDER) {
					finalChosen.getBlock().setType(Material.RED_CONCRETE_POWDER);
				}
			}
		}.runTaskLater(main, 20L);
		activeShrinkTasks.add(orangeTask);

		BukkitTask airTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}
				finalChosen.getBlock().setType(Material.AIR);
			}
		}.runTaskLater(main, 40L);
		activeShrinkTasks.add(airTask);
	}

	private void resetCurrentArena() {
		if (currentArenaCorner1 == null || currentArenaCorner2 == null) {
			return;
		}

		main.getFAWEAPI().createFlatSnowPlane(currentArenaCorner1, currentArenaCorner2, Material.SNOW_BLOCK);
		arenaCorner1 = currentArenaCorner1.clone();
		arenaCorner2 = currentArenaCorner2.clone();
	}

	private void cancelPendingShrinkTasks() {
		for (BukkitTask task : activeShrinkTasks) {
			if (task != null) {
				task.cancel();
			}
		}
		activeShrinkTasks.clear();
	}

	private void cancelModeTasks() {
		if (modeRoundTask != null) {
			modeRoundTask.cancel();
			modeRoundTask = null;
		}
		if (modeWarningTask != null) {
			modeWarningTask.cancel();
			modeWarningTask = null;
		}
		if (outerLayerDecayTask != null) {
			outerLayerDecayTask.cancel();
			outerLayerDecayTask = null;
		}

		cancelPendingShrinkTasks();
	}

	private void shrinkEvenLayer() {
		List<Location> edgeBlocks = getOuterLayer(currentArenaCorner1, currentArenaCorner2);
		if (edgeBlocks.isEmpty()) {
			return;
		}

		for (Location loc : edgeBlocks) {
			loc.getBlock().setType(Material.ORANGE_CONCRETE_POWDER);
		}

		BukkitTask orangeTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}
				for (Location loc : edgeBlocks) {
					if (loc.getBlock().getType() == Material.ORANGE_CONCRETE_POWDER) {
						loc.getBlock().setType(Material.RED_CONCRETE_POWDER);
					}
				}
			}
		}.runTaskLater(main, 20L);
		activeShrinkTasks.add(orangeTask);

		BukkitTask airTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!teamGame) {
					return;
				}

				for (Location loc : edgeBlocks) {
					loc.getBlock().setType(Material.AIR);
				}

				currentArenaCorner1.add(1, 0, 1);
				currentArenaCorner2.add(-1, 0, -1);
				currentArenaSize -= 2;

				arenaCorner1 = currentArenaCorner1.clone();
				arenaCorner2 = currentArenaCorner2.clone();
			}
		}.runTaskLater(main, 40L);
		activeShrinkTasks.add(airTask);
	}

	private void shrinkMiniOuterLayerUneven() {
		if (currentArenaCorner1 == null || currentArenaCorner2 == null) {
			return;
		}

		Location oldC1 = currentArenaCorner1.clone();
		Location oldC2 = currentArenaCorner2.clone();
		List<Location> edgeBlocks = getOuterLayer(oldC1, oldC2);
		if (edgeBlocks.isEmpty()) {
			return;
		}

		Location newC1 = oldC1.clone().add(1, 0, 1);
		Location newC2 = oldC2.clone().add(-1, 0, -1);
		normalizeCorners(newC1, newC2);

		currentArenaCorner1 = newC1;
		currentArenaCorner2 = newC2;
		arenaCorner1 = newC1.clone();
		arenaCorner2 = newC2.clone();
		currentArenaSize = Math.abs(newC2.getBlockX() - newC1.getBlockX()) + 1;

		Collections.shuffle(edgeBlocks);

		long delay = 0L;
		for (Location loc : edgeBlocks) {
			BukkitTask redTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (!teamGame)
						return;
					loc.getBlock().setType(Material.ORANGE_CONCRETE_POWDER);
				}
			}.runTaskLater(main, delay);
			activeShrinkTasks.add(redTask);

			BukkitTask orangeTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (!teamGame)
						return;
					if (loc.getBlock().getType() == Material.ORANGE_CONCRETE_POWDER) {
						loc.getBlock().setType(Material.RED_CONCRETE_POWDER);
					}
				}
			}.runTaskLater(main, delay + 20L);
			activeShrinkTasks.add(orangeTask);

			BukkitTask airTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (!teamGame)
						return;
					loc.getBlock().setType(Material.AIR);
				}
			}.runTaskLater(main, delay + 40L);
			activeShrinkTasks.add(airTask);

			delay += 2L;
		}

		Bukkit.getLogger().info("Minispleef shrunk. New size: " + currentArenaSize);
	}

	private void startRandomDecayTask(int blocksPerSecond, long secondSpacing, long intraSpacing) {
		for (int i = 0; i < blocksPerSecond; i++) {
			final int index = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					decayRandomBlockInArena();
				}
			}.runTaskLater(main, index * intraSpacing);
		}
	}

	private void decayRandomBlockInArena() {
		if (currentArenaCorner1 == null || currentArenaCorner2 == null) {
			return;
		}

		int minX = Math.min(currentArenaCorner1.getBlockX(), currentArenaCorner2.getBlockX());
		int maxX = Math.max(currentArenaCorner1.getBlockX(), currentArenaCorner2.getBlockX());
		int minZ = Math.min(currentArenaCorner1.getBlockZ(), currentArenaCorner2.getBlockZ());
		int maxZ = Math.max(currentArenaCorner1.getBlockZ(), currentArenaCorner2.getBlockZ());
		int y = currentArenaCorner1.getBlockY();

		for (int attempts = 0; attempts < 20; attempts++) {
			int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
			int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

			Location loc = new Location(currentArenaCorner1.getWorld(), x, y, z);
			Material type = loc.getBlock().getType();

			if (type != Material.AIR) {
				loc.getBlock().setType(Material.AIR);
				return;
			}
		}
	}

	private List<Location> getOuterLayer(Location c1, Location c2) {
		List<Location> edge = new ArrayList<>();

		int minX = Math.min(c1.getBlockX(), c2.getBlockX());
		int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
		int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
		int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());
		int y = c1.getBlockY();
		World world = c1.getWorld();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (x == minX || x == maxX || z == minZ || z == maxZ) {
					edge.add(new Location(world, x, y, z));
				}
			}
		}

		return edge;
	}

	private void normalizeCorners(Location c1, Location c2) {
		double minX = Math.min(c1.getX(), c2.getX());
		double maxX = Math.max(c1.getX(), c2.getX());
		double minZ = Math.min(c1.getZ(), c2.getZ());
		double maxZ = Math.max(c1.getZ(), c2.getZ());

		c1.setX(minX);
		c1.setZ(minZ);
		c2.setX(maxX);
		c2.setZ(maxZ);
	}

	private int getAliveTeamPlayerCount() {
		int count = 0;
		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			count += team.getPlayers().size();
		}
		return count;
	}

	public void startGame() {
		startingGame = true;
		this.playToInt = 5;
		int i = 0;
		String msg = "§bStarted a game of §f§lSPLEEF §bbetween";
		String msg2 = "Started a game of **SPLEEF** between";
		for (SpleefTeam team : this.teams) {
			msg = msg + " §3§l" + team.getName();
			msg2 = msg2 + " " + team.getName();
			if (i < 1) {
				msg += "§b and";
				msg2 += " and";
			}
			i++;
		}
		startRound();
	}

	public void startRound() {
		roundCount++;
		this.round++;
		this.roundTime = 0;
		Location corner1 = null;
		Location corner2 = null;
		setGameState(GameState.STARTING);
		switch (roundCount) {
		case 1:
			setArena();
			corner1 = arena.getCorner1();
			corner2 = arena.getCorner2();
			break;
		}
		main.getFAWEAPI().createFlatSnowPlane(corner1, corner2, Material.SNOW_BLOCK);
		Location playerSpawn1 = new Location(Bukkit.getWorld("voidworld"), (arena.getCorner1().getBlockX() + 1.5),
				arena.getCorner1().getBlockY() + 1, (arena.getCorner1().getBlockZ() - 9.5));
		playerSpawn1.setYaw(270f);
		ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
		ItemMeta meta = shovel.getItemMeta();
		meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
		meta.setUnbreakable(true);
		shovel.setItemMeta(meta);
		shovel.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		for (SpleefPlayer p : this.getPlayers()) {
			p.getPlayer().teleport(playerSpawn1);
			p.setGame(this);
			playerSpawn1 = new Location(Bukkit.getWorld("voidworld"), playerSpawn1.getX() + 18, playerSpawn1.getY(),
					playerSpawn1.getZ());
			playerSpawn1.setYaw(90f);
			PlayerInventory inventory = p.getPlayer().getInventory();
			inventory.clear();
			if (!inventory.contains(shovel)) {
				inventory.addItem(shovel);
			}
			SpleefGame game = this;
			new BukkitRunnable() {
				public void run() {
					for (SpleefTeam team : teams) {
						for (SpleefPlayer sp : team.getSpleefPlayers()) {
							sp.getPlayer().setGameMode(GameMode.SURVIVAL);
						}
					}
					p.setInGame(true);
				}
			}.runTaskLater(main, 2L);
			createGlassCase(p.getPlayer());
			new CountdownTask(main, game);
			new BukkitRunnable() {
				Location loc = p.getPlayer().getLocation();
				int px = loc.getBlockX();
				int py = loc.getBlockY();
				int pz = loc.getBlockZ();

				public void run() {
					for (int x = -1; x <= 1; x++) {
						for (int y = 0; y < 3; y++) {
							for (int z = -1; z <= 1; z++) {
								if (x == 0 && z == 0)
									continue;
								Location blockLoc = new Location(p.getPlayer().getWorld(), px + x, py + y, pz + z);

								blockLoc.getBlock().setType(Material.AIR);
							}
						}
					}
					Location blockLoc = new Location(p.getPlayer().getWorld(), px, py + 2, pz);
					blockLoc.getBlock().setType(Material.AIR);
				}
			}.runTaskLater(main, 70L);
			for (SpleefPlayer sp : this.getPlayers()) {
				sp.setRoundStarting(false);
			}
		}
		startTimer(main);
	}

	public void endGame() {
		if (arena != null) {
			arena.setInGame(false);
		} else if (spleggRankedArena != null) {
			spleggRankedArena.setInGame(false);
		}

		resetTimer();

		Location spawn = new Location(Bukkit.getWorld("lobby"), 8431.5, 201, 8849.5);
		setGameState(GameState.ENDING);

		for (SpleefSpectator ss : main.getDataLoader().getSpectatorsForGame(this)) {
			Player p = Bukkit.getPlayer(ss.getUUID());

			if (ss != null && p != null && p.isOnline()) {
				p.getInventory().clear();
				p.teleport(spawn);
				ss.setInTeamGame(false);
				ss.setGame(null);
				p.setGameMode(GameMode.SURVIVAL);
			}
		}

		int elo = 0;
		List<SpleefTeam> losers = new ArrayList<SpleefTeam>(this.teams);

		for (SpleefPlayer sp : this.getPlayers()) {
			if (sp == null || !sp.isOnline() || sp.getPlayer() == null) {
				continue;
			}

			if (sp.getGame() == this) {
				if (sp.isWinner()) {
					winner = sp.getName();
					wpoints = "" + sp.getPoints();
				} else {
					loser = sp.getName();
					lpoints = "" + sp.getPoints();
				}

				sp.setPoints(0);
				sp.getPlayer().getInventory().clear();
			}
		}

		for (SpleefPlayer sp : this.getPlayers()) {
			if (sp == null || sp.getPlayer() == null || !sp.getPlayer().isOnline()) {
				continue;
			}

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
			sp.getPlayer().getInventory().setItem(7, stats);

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

			sp.getPlayer().getInventory().setItem(8, item);
			sp.getPlayer().teleport(spawn);
			sp.setInGame(false);
			sp.setSpectating(false);
			sp.setGame(null);
			sp.getPlayer().setGameMode(GameMode.SURVIVAL);
		}
		if (arena != null) {
			arena.setInGame(false);
		} else if (spleggRankedArena != null) {
			spleggRankedArena.setInGame(false);
		}

		main.getGameManager().getGames().remove(this);
	}

	public void endRound(SpleefTeam winner) {
		if (this.gameState.equals(GameState.FINISHING))
			return;
		setGameState(GameState.FINISHING);
	}

	private String formatTeamListColoredAnd(List<String> teams, String accentColor) {
		if (teams.isEmpty())
			return "Unknown";
		if (teams.size() == 1)
			return teams.get(0);
		if (teams.size() == 2)
			return teams.get(0) + accentColor + " and " + teams.get(1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < teams.size(); i++) {
			if (i == teams.size() - 1) {
				sb.append(accentColor).append("and ").append(teams.get(i));
			} else {
				sb.append(teams.get(i)).append(", ");
			}
		}
		return sb.toString();
	}

	private String formatTeamListPlain(List<String> teams) {
		List<String> stripped = new ArrayList<>();
		for (String t : teams)
			stripped.add(stripColor(t));
		if (stripped.isEmpty())
			return "Unknown";
		if (stripped.size() == 1)
			return stripped.get(0);
		if (stripped.size() == 2)
			return stripped.get(0) + " and " + stripped.get(1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stripped.size(); i++) {
			if (i == stripped.size() - 1) {
				sb.append("and ").append(stripped.get(i));
			} else {
				sb.append(stripped.get(i)).append(", ");
			}
		}
		return sb.toString();
	}

	public NamedTextColor getAdventureColor(String colorName) {
		switch (colorName.toLowerCase()) {
		case "red":
			return NamedTextColor.RED;
		case "blue":
			return NamedTextColor.BLUE;
		case "green":
			return NamedTextColor.GREEN;
		case "yellow":
			return NamedTextColor.YELLOW;
		case "orange":
			return NamedTextColor.GOLD;
		case "purple":
			return NamedTextColor.LIGHT_PURPLE;
		case "pink":
			return NamedTextColor.LIGHT_PURPLE;
		case "light blue":
			return NamedTextColor.AQUA;
		case "magenta":
			return NamedTextColor.LIGHT_PURPLE;
		case "brown":
			return NamedTextColor.DARK_RED;
		case "cyan":
			return NamedTextColor.DARK_AQUA;
		case "light gray":
			return NamedTextColor.GRAY;
		case "gray":
			return NamedTextColor.DARK_GRAY;
		case "dark gray":
			return NamedTextColor.DARK_GRAY;
		case "white":
			return NamedTextColor.WHITE;
		case "black":
			return NamedTextColor.BLACK;
		default:
			return NamedTextColor.WHITE;
		}
	}

	public String getChatColor(String colorName) {
		switch (colorName.toLowerCase()) {
		case "red":
			return "§c";
		case "blue":
			return "§9";
		case "green":
			return "§a";
		case "yellow":
			return "§e";
		case "orange":
			return "§6";
		case "purple":
			return "§5";
		case "pink":
			return "§d";
		case "light blue":
			return "§b";
		case "magenta":
			return "§d";
		case "brown":
			return "§6";
		case "cyan":
			return "§3";
		case "light gray":
			return "§7";
		case "gray":
			return "§8";
		case "dark gray":
			return "§8";
		case "white":
			return "§f";
		case "black":
			return "§0";
		default:
			return "§f";
		}
	}

	private Color getBukkitColor(String colorName) {
		switch (colorName.toLowerCase()) {
		case "red":
			return Color.fromRGB(151, 52, 49);
		case "blue":
			return Color.fromRGB(60, 68, 170);
		case "green":
			return Color.fromRGB(72, 179, 58);
		case "yellow":
			return Color.fromRGB(249, 236, 78);
		case "orange":
			return Color.fromRGB(249, 128, 29);
		case "purple":
			return Color.fromRGB(137, 50, 183);
		case "pink":
			return Color.fromRGB(243, 139, 170);
		case "light blue":
			return Color.fromRGB(58, 179, 218);
		case "magenta":
			return Color.fromRGB(200, 78, 189);
		case "brown":
			return Color.fromRGB(131, 84, 50);
		case "cyan":
			return Color.fromRGB(22, 156, 156);
		case "light gray":
			return Color.fromRGB(157, 157, 151);
		case "gray":
			return Color.fromRGB(71, 79, 82);
		case "dark gray":
			return Color.fromRGB(41, 49, 51);
		case "white":
			return Color.fromRGB(255, 255, 255);
		case "black":
			return Color.fromRGB(29, 29, 33);
		default:
			return Color.WHITE;
		}
	}

	public void startTimer(JavaPlugin plugin) {
		if (timerTask != null) {
			timerTask.cancel();
		}
		secondsElapsed = 0;

		timerTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!splegg) {
					int count = 0;
					for (TeamSpleefTeam team : teamSpleefTeams.values()) {
						for (UUID uuid : team.getPlayers()) {
							count++;
						}
					}

					double interval = 0;

					if (roundTime >= 150) {
						interval = 5;
					} else if (roundTime >= 120) {
						interval = 5.5;
					} else if (roundTime >= 90) {
						interval = 6;
					} else if (roundTime >= 60) {
						interval = 6.5;
					} else {
						interval = 7;
					}

					if (secondsElapsed % interval == 0 && secondsElapsed != 0) {
						if (spleefMode != null && spleefMode == SpleefMode.POWERSPLEEF) {
							int maxX = arena.getCorner2().getBlockX() - 3;
							int maxZ = arena.getCorner1().getBlockZ() - 3;
							int minX = arena.getCorner1().getBlockX() + 3;
							int minZ = arena.getCorner2().getBlockZ() + 3;
							Location randomBlock = new Location(arena.getCorner1().getWorld(),
									((int) (Math.random() * (maxX - minX + 1)) + minX), arena.getCorner1().getBlockY(),
									((int) (Math.random() * (maxZ - minZ + 1)) + minZ));
							startParticles(randomBlock, 3, 1);
							new BukkitRunnable() {
								@Override
								public void run() {
									createCylinder(randomBlock, Material.SNOW_BLOCK, 3, 1);
								}
							}.runTaskLater(plugin, 40L);
						}
					}
				}
				secondsElapsed++;
				roundTime++;
			}
		};
		timerTask.runTaskTimer(plugin, 0L, 20L);
	}

	public void stopTimer() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	public String getSecondsElapsed() {
		int minutes = secondsElapsed / 60;
		int seconds = secondsElapsed % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	public void resetTimer() {
		stopTimer();
		secondsElapsed = 0;
	}

	public void giveColoredLeatherTunic(Player player, String colorName) {
		ItemStack tunic = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		LeatherArmorMeta meta = (LeatherArmorMeta) tunic.getItemMeta();
		if (meta != null) {
			meta.setColor(getBukkitColor(colorName));
			tunic.setItemMeta(meta);
		}
		player.getInventory().setChestplate(tunic);
	}

	public String capitalize(String input) {
		if (input == null || input.isEmpty())
			return input;
		return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
	}

	private String stripColor(String input) {
		return org.bukkit.ChatColor.stripColor(input);
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

	public void createGlassCase(Player player) {
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		Location loc = player.getLocation();
		int px = loc.getBlockX();
		int py = loc.getBlockY();
		int pz = loc.getBlockZ();

		Material glass = Material.GLASS;
		for (int x = -1; x <= 1; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && z == 0)
						continue;
					Location blockLoc = new Location(player.getWorld(), px + x, py + y, pz + z);
					Block block = blockLoc.getBlock();
					block.setType(glass);
				}
			}
		}
		Location blockLoc = new Location(player.getWorld(), px, py + 2, pz);
		Block block = blockLoc.getBlock();
		block.setType(glass);
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

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		event.setDropItems(false);
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(event.getPlayer());
		if (sp.isRoundStarting()) {
			event.setCancelled(true);
		}
	}

	public static void sendMessaage(String message) {
		TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById("1401653713889788065");
		if (channel != null) {
			channel.sendMessage(message).queue();
		}
	}

	public void setPlaytoScore(int playToInt) {
		this.playToInt = playToInt;
	}

	public int getPlaytoScore() {
		return this.playToInt;
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

	private Component buildEloNumberComponent(int elo) {
		return Component.text(String.valueOf(elo), NamedTextColor.GOLD);
	}

	private Component buildEloChangeComponent(int delta) {
		if (delta > 0) {
			return Component.text(" (+" + delta + ")", NamedTextColor.GREEN);
		} else if (delta < 0) {
			return Component.text(" (" + delta + ")", NamedTextColor.RED);
		}
		return Component.text(" (0)", NamedTextColor.GRAY);
	}

	private Component buildRankedPlayerStartComponent(UUID uuid) {
		OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
		String name = (offline != null && offline.getName() != null) ? offline.getName() : "Unknown";

		int elo;
		String label;

		if (splegg) {
			elo = main.getDataLoader().getSpleggElo(uuid);
			label = "ELO";
		} else {
			String table = getRankedSpleefEloTable();
			elo = main.getDataLoader().getModeSpleefElo(uuid, table);
			label = getRankedEloLabel(); // Power ELO / Air ELO / Mini ELO
		}

		return Component.text(name, NamedTextColor.WHITE).append(Component.text(" (", NamedTextColor.GRAY))
				.append(buildEloNumberComponent(elo)).append(Component.text(" " + label + ")", NamedTextColor.GRAY));
	}

	private Component buildRankedResultPlayerComponent(String name, int oldElo, int newElo, int delta) {
		Component hover = Component.text("Beginning ELO: ", NamedTextColor.GRAY)
				.append(Component.text(String.valueOf(oldElo), NamedTextColor.GOLD)).append(Component.newline())
				.append(Component.text("Ending ELO: ", NamedTextColor.GRAY))
				.append(Component.text(String.valueOf(newElo), NamedTextColor.GOLD));

		return Component.text(name, NamedTextColor.WHITE).append(Component.text(" (", NamedTextColor.GRAY))
				.append(buildEloNumberComponent(newElo)).append(Component.text(" ELO", NamedTextColor.GRAY))
				.append(buildEloChangeComponent(delta)).append(Component.text(")", NamedTextColor.GRAY))
				.hoverEvent(HoverEvent.showText(hover));
	}

	public void startPregameCountdown(Collection<Player> players) {
		pregameCountdownTask = new BukkitRunnable() {
			int totalSeconds = 70;

			@Override
			public void run() {
				if (pregameCancelled) {
					cancel();
					return;
				}

				if (totalSeconds < 0) {
					for (Player player : players) {
						if (player == null || !player.isOnline()) {
							continue;
						}

						player.setLevel(0);
						player.setExp(0.0f);
					}

					cancel();
					return;
				}

				int displaySeconds;

				if (totalSeconds > 40) {
					displaySeconds = totalSeconds - 40;
				} else if (totalSeconds > 30) {
					displaySeconds = totalSeconds - 30;
				} else {
					displaySeconds = totalSeconds;
				}

				for (Player player : players) {
					if (player == null || !player.isOnline()) {
						cancelPregame("A player left during pregame.");
						return;
					}

					player.setLevel(displaySeconds);
					player.setExp(Math.max(0.0f, Math.min(1.0f, totalSeconds / 70.0f)));
				}

				totalSeconds--;
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	public void cancelPregame(String reason) {
		pregameCancelled = true;

		if (banEndTask != null) {
			banEndTask.cancel();
			banEndTask = null;
		}

		if (powerSelectTask != null) {
			powerSelectTask.cancel();
			powerSelectTask = null;
		}

		if (gameStartTask != null) {
			gameStartTask.cancel();
			gameStartTask = null;
		}

		if (pregameCountdownTask != null) {
			pregameCountdownTask.cancel();
			pregameCountdownTask = null;
		}

		for (TeamSpleefTeam team : teamSpleefTeams.values()) {
			for (UUID uuid : team.getPlayers()) {
				Player online = Bukkit.getPlayer(uuid);

				if (online == null) {
					continue;
				}

				online.closeInventory();
				online.setLevel(0);
				online.setExp(0.0f);
				online.playSound(online.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
				online.sendMessage("§e[PowerSpleef] > §cPregame cancelled. " + reason);

				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(online);
				if (sp != null) {
					sp.setGame(null);
					sp.setDead(true);
				}
			}
		}

		ban1 = null;
		ban2 = null;
		startingGame = false;
		teamGame = false;
	}

	private Attribute resolveBlockReachAttr() {
		try {
			return Attribute.valueOf("BLOCK_INTERACTION_RANGE");
		} catch (IllegalArgumentException ignored) {
			/* old name */ }
		return Attribute.valueOf("PLAYER_BLOCK_INTERACTION_RANGE");
	}

}
