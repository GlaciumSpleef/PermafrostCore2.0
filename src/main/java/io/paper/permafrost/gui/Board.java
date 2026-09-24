package io.paper.permafrost.gui;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleggQueueManager;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.training.TrainingSession;
import io.paper.permafrost.training.TrainingState;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class Board {
	private final static Board instance = new Board();
	private final static DataLoader dataLoader = Main.getInstance().getDataLoader();

	public Board() {
	}

	public void createNewScoreboard(Player player) {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = scoreboard.registerNewObjective("PermaFrostCore", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "GLACIUM SPLEEF");
		player.setScoreboard(scoreboard);
	}

	public void sendSpleggBoard(Player player, Objective objective) {
		if (player == null)
			return;

		SpleefPlayer sp = dataLoader.getSpleefPlayer(player);
		SpleefGame game = sp.getGame();

		if (!game.isRanked()) {
			sendUnrankedBoard(player, objective);
			return;
		}

		String mapName = "Unknown";
		if (game.getSpleggRankedArena() != null && game.getSpleggRankedArena().getId() != null) {
			mapName = game.getSpleggRankedArena().getId();
		}

		objective.getScore("§7 ").setScore(15);
		objective.getScore("§fMap §8» §f" + mapName).setScore(14);
		objective.getScore("§fGoal §8» §fFirst to §b" + game.getPlaytoScore()).setScore(13);
		objective.getScore("§fTime §8» §f" + game.getSecondsElapsed()).setScore(12);

		objective.getScore("§1 ").setScore(11);

		objective.getScore("§fELO §8» §a" + dataLoader.getSpleggElo(player.getUniqueId())).setScore(10);

		objective.getScore("§2 ").setScore(9);

		int score = 8;
		int index = 0;

		for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
			objective.getScore(getTeamStatusLine(game, team, index)).setScore(score);
			score--;
			index++;

			if (score <= 3) {
				break;
			}
		}

		addBoardFooter(objective, score);
	}

	public void sendSpleefBoard(Player player, Objective objective) {
		if (player == null)
			return;

		SpleefPlayer sp = dataLoader.getSpleefPlayer(player);
		SpleefGame game = sp.getGame();

		if (!game.isRanked()) {
			sendUnrankedBoard(player, objective);
			return;
		}

		String mapName = "Unknown";
		if (game.getArena() != null && game.getArena().getName() != null) {
			mapName = game.getArena().getName();
		}

		objective.getScore("§7 ").setScore(15);
		objective.getScore("§fMap §8» §f" + mapName).setScore(14);
		objective.getScore("§fGoal §8» §fFirst to §b" + game.getPlaytoScore()).setScore(13);
		objective.getScore("§fTime §8» §f" + game.getSecondsElapsed()).setScore(12);

		objective.getScore("§1 ").setScore(11);

		if (sp.getSelectedPower() != ShovelPowers.NONE) {
			objective.getScore(getPowerLine(sp)).setScore(10);
			if (sp.isOnDashCooldown()) {
				objective.getScore("§fDash §8» §c" + sp.getDashTimeLeft()).setScore(9);
			} else {
				objective.getScore("§fDash §8» §aREADY").setScore(9);
			}
			objective.getScore("§1 ").setScore(8);
		} else {
			objective.getScore("§fPower §8» §7None").setScore(10);
			if (sp.isOnDashCooldown()) {
				objective.getScore("§fDash §8» §c" + sp.getDashTimeLeft()).setScore(9);
			} else {
				objective.getScore("§fDash §8» §aREADY").setScore(9);
			}
			objective.getScore("§1 ").setScore(8);
		}

		String table = game.getRankedSpleefEloTable();
		int elo = dataLoader.getModeSpleefElo(player.getUniqueId(), table);
		String eloName = game.getRankedEloLabel();

		objective.getScore("§f" + eloName + " §8» §a" + elo).setScore(7);

		objective.getScore("§2 ").setScore(6);

		int score = 5;
		int index = 0;

		for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
			objective.getScore(getTeamStatusLine(game, team, index)).setScore(score);
			score--;
			index++;

			if (score <= 3) {
				break;
			}
		}

		addBoardFooter(objective, score);
	}

	public void sendUnrankedBoard(Player player, Objective objective) {
		if (player == null)
			return;

		SpleefPlayer sp = dataLoader.getSpleefPlayer(player);
		SpleefGame game = sp.getGame();

		objective.getScore("§7 ").setScore(15);
		objective.getScore("§fGoal §8» §fFirst to §b" + game.getPlaytoScore()).setScore(14);
		objective.getScore("§fTime §8» §f" + game.getSecondsElapsed()).setScore(13);

		if (!game.isSplegg() && sp.getSelectedPower() != ShovelPowers.NONE) {
			objective.getScore(getPowerLine(sp)).setScore(12);
			if (sp.isOnDashCooldown()) {
				objective.getScore("§fDash §8» §c" + sp.getDashTimeLeft()).setScore(11);
			} else {
				objective.getScore("§fDash §8» §aREADY").setScore(11);
			}
			objective.getScore("§1 ").setScore(10);
		} else {
			objective.getScore("§1 ").setScore(12);
		}

		int score = 9;
		int index = 0;

		for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
			objective.getScore(getUnrankedTeamStatusLine(game, sp, team, index)).setScore(score);
			score--;
			index++;

			if (score <= 3) {
				break;
			}
		}

		addBoardFooter(objective, score);
	}

	public void sendSpectatorBoard(Player player, Objective objective) {
		if (player == null)
			return;

		SpleefPlayer sp = dataLoader.getSpleefPlayer(player);
		SpleefGame game = sp.getGame();

		if (game.isRanked()) {
			String mapName = "Unknown";

			if (game.isSplegg()) {
				if (game.getSpleggRankedArena() != null && game.getSpleggRankedArena().getId() != null) {
					mapName = game.getSpleggRankedArena().getId();
				}
			} else {
				if (game.getArena() != null && game.getArena().getName() != null) {
					mapName = game.getArena().getName();
				}
			}

			objective.getScore("§7 ").setScore(15);
			objective.getScore("§fMap §8» §f" + mapName).setScore(14);
			objective.getScore("§fGoal §8» §fFirst to §b" + game.getPlaytoScore()).setScore(13);
			objective.getScore("§fTime §8» §f" + game.getSecondsElapsed()).setScore(12);

			objective.getScore("§1 ").setScore(11);

			int score = 10;
			int index = 0;

			for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
				objective.getScore(getTeamStatusLine(game, team, index)).setScore(score);
				score--;
				index++;

				if (score <= 3) {
					break;
				}
			}

			addBoardFooter(objective, score);
			return;
		}

		objective.getScore("§7 ").setScore(15);
		objective.getScore("§fGoal §8» §fFirst to §b" + game.getPlaytoScore()).setScore(14);
		objective.getScore("§fTime §8» §f" + game.getSecondsElapsed()).setScore(13);

		objective.getScore("§1 ").setScore(12);

		int score = 10;
		int index = 0;

		if (game.getTeamSpleefTeams() != null && !game.getTeamSpleefTeams().isEmpty()) {
			for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
				objective.getScore(getSpectatorUnrankedTeamLine(game, team, index)).setScore(score);
				score--;
				index++;

				if (score <= 3) {
					break;
				}
			}
		} else {
			for (SpleefPlayer p2 : game.getPlayers()) {
				objective.getScore("§fPlayer §8» §f" + p2.getName() + " §8| §6" + p2.getPoints()).setScore(score);
				score--;

				if (score <= 3) {
					break;
				}
			}
		}

		addBoardFooter(objective, score);
	}

	private String getPowerLine(SpleefPlayer sp) {
		String shownPowerName;
		String deselectedPowerName;
		if (sp.getDeselectedPower().equals(ShovelPowers.ROULETTE2)) {
			shownPowerName = "*" + sp.getFormattedRandomPower() + "*";

			deselectedPowerName = "*" + sp.getFormattedRandomPower2() + "*";
		} else if (sp.getSelectedPower().equals(ShovelPowers.ROULETTE2)) {
			shownPowerName = "*" + sp.getFormattedRandomPower2() + "*";

			deselectedPowerName = "*" + sp.getFormattedRandomPower() + "*";
		} else {
			shownPowerName = (sp.getRandomPower() != null && sp.getSelectedPower().equals(ShovelPowers.ROULETTE))
					? ("*" + sp.getFormattedRandomPower() + "*")
					: sp.getFormattedSelectedPower();

			deselectedPowerName = (sp.getRandomPower() != null
					&& sp.getDeselectedPower().equals(ShovelPowers.ROULETTE))
							? ("*" + sp.getFormattedRandomPower() + "*")
							: sp.getFormattedDeselectedPower();
		}

		ShovelPowers shownCooldownPower = sp.getSelectedPower();
		ShovelPowers deselectedCooldownPower = sp.getDeselectedPower();

		boolean shownPowerOnCooldown = sp.getCooldown1() == shownCooldownPower
				|| sp.getCooldown2() == shownCooldownPower;
		boolean deselectedPowerOnCooldown = sp.getCooldown1() == deselectedCooldownPower
				|| sp.getCooldown2() == deselectedCooldownPower;

		String shownColor = shownPowerOnCooldown ? "§c" : "§a";
		String deselectedColor = deselectedPowerOnCooldown ? "§c" : "§a";

		return "§fPower §8» " + shownColor + shownPowerName + " §8/ " + deselectedColor + deselectedPowerName;
	}

	private String getTeamStatusLine(SpleefGame game, TeamSpleefTeam team, int index) {
		String colorCode = game.getChatColor(team.getColor());
		String teamName = getTeamDisplayName(game, team, index);

		int totalPlayers = 0;
		int alivePlayers = 0;

		for (UUID uuid : team.getPlayers()) {
			totalPlayers++;

			Player target = Bukkit.getPlayer(uuid);
			SpleefPlayer teamSp = target != null ? dataLoader.getSpleefPlayer(target) : null;

			if (teamSp != null && !teamSp.isDead()) {
				alivePlayers++;
			}
		}

		if (alivePlayers <= 0) {
			return "§8" + teamName + " §8» §8" + team.getPoints() + " §8| §7" + alivePlayers + "/" + totalPlayers;
		}

		return colorCode + teamName + " §8» §6" + team.getPoints() + " §8| §a" + alivePlayers + "§7/" + totalPlayers;
	}

	private String getUnrankedTeamStatusLine(SpleefGame game, SpleefPlayer sp, TeamSpleefTeam team, int index) {
		String colorCode = game.getChatColor(team.getColor());
		String teamName = getTeamDisplayName(game, team, index);

		int totalPlayers = 0;
		int alivePlayers = 0;

		for (UUID uuid : team.getPlayers()) {
			totalPlayers++;

			Player target = Bukkit.getPlayer(uuid);
			SpleefPlayer teamSp = target != null ? dataLoader.getSpleefPlayer(target) : null;

			if (teamSp != null && !teamSp.isDead()) {
				alivePlayers++;
			}
		}

		boolean ownTeam = sp.getTeam() != null && team.getColor().equals(sp.getTeam().getColor());

		if (team.isDead() || alivePlayers <= 0) {
			if (ownTeam) {
				return "§f> §7§l" + teamName + "§r §8» §8" + team.getPoints() + " §8| §7" + alivePlayers + "/"
						+ totalPlayers;
			}

			return "§8" + teamName + " §8» §8" + team.getPoints() + " §8| §7" + alivePlayers + "/" + totalPlayers;
		}

		if (ownTeam) {
			return "§f> " + colorCode + "§l" + teamName + "§r §8» §6" + team.getPoints() + " §8| §a" + alivePlayers
					+ "§7/" + totalPlayers;
		}

		return colorCode + teamName + " §8» §6" + team.getPoints() + " §8| §a" + alivePlayers + "§7/" + totalPlayers;
	}

	private String getSpectatorUnrankedTeamLine(SpleefGame game, TeamSpleefTeam team, int index) {
		String colorCode = game.getChatColor(team.getColor());
		String teamName = getTeamDisplayName(game, team, index);

		int totalPlayers = 0;
		int alivePlayers = 0;

		for (UUID uuid : team.getPlayers()) {
			totalPlayers++;

			Player target = Bukkit.getPlayer(uuid);
			SpleefPlayer teamSp = target != null ? dataLoader.getSpleefPlayer(target) : null;

			if (teamSp != null && !teamSp.isDead()) {
				alivePlayers++;
			}
		}

		if (team.isDead() || alivePlayers <= 0) {
			return "§8" + teamName + " §8» §8" + team.getPoints() + " §8| §7" + alivePlayers + "/" + totalPlayers;
		}

		return colorCode + teamName + " §8» §6" + team.getPoints() + " §8| §a" + alivePlayers + "§7/" + totalPlayers;
	}

	private String getTeamDisplayName(SpleefGame game, TeamSpleefTeam team, int index) {
		String colorName = game.capitalize(team.getColor());

		if (colorName == null || colorName.trim().isEmpty()) {
			return "Team " + (index + 1);
		}

		if (team.getPlayers().size() == 1) {
			return "" + Bukkit.getPlayer(team.getPlayers().get(0)).getName();
		}

		return colorName;
	}

	private void addBoardFooter(Objective objective, int score) {
		if (score >= 2) {
			objective.getScore("§8 ").setScore(score);
			objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(score - 1);
		} else {
			objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(score);
		}
	}

	public void updateScoreboard(Player player) {
		SpleefPlayer sp = dataLoader.getSpleefPlayer(player);
		if (sp == null) {
			return;
		}

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = scoreboard.registerNewObjective("PermaFrostCore", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "GLACIUM SPLEEF");

		// Team Spleef / Team Splegg
		if (sp.getInTeamGame() && sp.getGame() != null) {
			if (sp.getGame().isSplegg()) {
				sendSpleggBoard(player, objective);
			} else {
				sendSpleefBoard(player, objective);
			}

			player.setScoreboard(scoreboard);
			return;
		}

		// Team game spectator
		if (sp.getSpectator() != null && sp.getGame() != null) {
			sendSpectatorBoard(player, objective);

			player.setScoreboard(scoreboard);
			return;
		}

		// Splegg FFA in game
		if (Main.getInstance().getSpleggQueueManager().isInGame() && Main.getInstance().getSpleggQueueManager()
				.getPlayersAliveInGame().containsKey(player.getUniqueId())) {

			SpleggQueueManager manager = Main.getInstance().getSpleggQueueManager();

			objective.getScore("").setScore(15);
			objective.getScore("§c§lPlayers Remaining").setScore(14);
			objective.getScore("§f" + manager.getPlayersRemaining()).setScore(13);
			objective.getScore("§f§7").setScore(12);
			objective.getScore("§e§lTime: §f" + manager.getTimeOngoing()).setScore(11);
			objective.getScore("§f").setScore(10);
			objective.getScore("§e§lYour Stats").setScore(9);

			int eggs = getEggsFired(Main.getInstance().getConnection(), player.getUniqueId());
			String formatted = formatNumber(eggs);

			objective.getScore("§7Eggs Fired: §f" + formatted).setScore(8);
			objective.getScore("§7Games Won: §f" + getWins(Main.getInstance().getConnection(), player.getUniqueId()))
					.setScore(7);
			objective.getScore("§7Shells: §f" + getPoints(Main.getInstance().getConnection(), player.getUniqueId()))
					.setScore(6);
			objective.getScore(" ").setScore(5);
			objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(4);

			player.setScoreboard(scoreboard);
			return;
		}

		// Splegg FFA waiting
		if (Main.getInstance().getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
			SpleggQueueManager manager = Main.getInstance().getSpleggQueueManager();

			objective.getScore("").setScore(15);
			objective.getScore("§c§lPlayers Waiting").setScore(14);
			objective.getScore("§f" + (manager.getPlayerCount() - manager.getPlayersInFFA())).setScore(13);
			objective.getScore("§c§lPlayers In Game").setScore(12);
			objective.getScore("§f" + manager.getPlayersInFFA()).setScore(11);
			objective.getScore("§c").setScore(10);

			if (manager.getSecondsTillGame() != 0) {
				objective.getScore("§e§lTime Until Game §f").setScore(9);
				objective.getScore("§f" + manager.getSecondsTillGame() + "s").setScore(8);
			} else {
				objective.getScore("§7Wait for players to join ").setScore(9);
				objective.getScore("§7or the game to end!").setScore(8);
			}

			objective.getScore(" ").setScore(7);
			objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(6);

			player.setScoreboard(scoreboard);
			return;
		}

		// Spleef FFA in game / spectating
		if (Main.getInstance().getSpleefQueueManager().getAlivePlayerFFA(player.getUniqueId()) != null
				|| Main.getInstance().getSpleefQueueManager().getAllSpectators().contains(player.getUniqueId())) {

			SpleefQueueManager manager = Main.getInstance().getSpleefQueueManager();
			SpleefFFAGame game = manager.getGame(manager.getFFAMode(player.getUniqueId()));

			if (game == null) {
				sendLobbyBoard(player, objective);
				player.setScoreboard(scoreboard);
				return;
			}

			if (manager.getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF)) {
				objective.getScore("").setScore(15);
				objective.getScore("§c§lPlayers Remaining").setScore(14);
				objective.getScore("§f" + game.getAlivePlayers()).setScore(13);
				objective.getScore("§f§7").setScore(12);
				objective.getScore("§e§lTime: §f" + game.getTimeOngoing() + "s").setScore(11);
				objective.getScore("§f").setScore(10);

				if (sp.getSelectedPower() != ShovelPowers.NONE) {
					objective.getScore(getPowerLine(sp)).setScore(9);
					if (sp.isOnDashCooldown()) {
						objective.getScore("§fDash §8» §c" + sp.getDashTimeLeft()).setScore(8);
					} else {
						objective.getScore("§fDash §8» §aREADY").setScore(7);
					}
					objective.getScore("§1 ").setScore(6);
				}

				objective.getScore("§e§l§7").setScore(5);
				objective.getScore("§e§lYour Stats").setScore(4);
				objective.getScore("§7Blocks Broken: §f" + dataLoader.getSpleefFFABlocksBroken(player.getUniqueId()))
						.setScore(6);
				objective.getScore("§7Games Won: §f" + dataLoader.getSpleefFFAWins(player.getUniqueId())).setScore(3);
				objective.getScore("§7Shards: §f" + dataLoader.getShards(player.getUniqueId())).setScore(2);
				objective.getScore(" ").setScore(1);
				objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(0);
			} else if (manager.getFFAMode(player.getUniqueId()).equals(SpleefFFAMode.PROTOTYPE)) {
				if (game.getTimes() != null) {
					List<Map.Entry<UUID, Double>> sortedTimes = new ArrayList<>(game.getTimes().entrySet());

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

				objective.getScore("").setScore(15);

				if (game.getTopOne() != null && Bukkit.getPlayer(game.getTopOne()) != null) {
					objective.getScore("§b§l1st: §r§f" + Bukkit.getPlayer(game.getTopOne()).getName() + " §7(§d"
							+ formatTwoDecimals(game.getTimes().get(game.getTopOne())) + "§7)").setScore(14);
				}

				if (game.getTopTwo() != null && Bukkit.getPlayer(game.getTopTwo()) != null) {
					objective.getScore("§6§l2nd: §r§f" + Bukkit.getPlayer(game.getTopTwo()).getName() + " §7(§d"
							+ formatTwoDecimals(game.getTimes().get(game.getTopTwo())) + "§7)").setScore(13);
				}

				if ((game.getAlivePlayers() + game.getPlayersSpectating().size()) >= 3) {
					if (game.getTopThree() != null && Bukkit.getPlayer(game.getTopThree()) != null) {
						objective
								.getScore("§7§l3rd: §r§f" + Bukkit.getPlayer(game.getTopThree()).getName() + " §7(§d"
										+ formatTwoDecimals(game.getTimes().get(game.getTopThree())) + "§7)")
								.setScore(12);
					}
				}

				objective.getScore("§f§7").setScore(11);
				objective.getScore("§e§lTimer: §f" + formatTime(game.getTimeOngoing())).setScore(10);
				objective.getScore("§e§lYour Score: §d" + formatTwoDecimals(game.getTime(player.getUniqueId())) + "s")
						.setScore(9);
				objective.getScore("§f").setScore(8);
				objective.getScore("§fReach 90 points to win!").setScore(3);
				objective.getScore("§8 ").setScore(2);
				objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(1);
			} else {
				objective.getScore("").setScore(15);
				objective.getScore("§c§lPlayers Remaining").setScore(14);
				objective.getScore("§f" + game.getAlivePlayers()).setScore(13);
				objective.getScore("§f§7").setScore(12);
				objective.getScore("§e§lTime: §f" + game.getTimeOngoing() + "s").setScore(11);
				objective.getScore("§f").setScore(10);
				objective.getScore("§e§lYour Stats").setScore(9);
				objective.getScore("§7Blocks Broken: §f" + dataLoader.getSpleefFFABlocksBroken(player.getUniqueId()))
						.setScore(8);
				objective.getScore("§7Games Won: §f" + dataLoader.getSpleefFFAWins(player.getUniqueId())).setScore(7);
				objective.getScore("§7Shards: §f" + dataLoader.getShards(player.getUniqueId())).setScore(6);
				objective.getScore(" ").setScore(5);
				objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(4);
			}

			player.setScoreboard(scoreboard);
			return;
		}

		// Spleef FFA waiting
		if (Main.getInstance().getSpleefQueueManager().getInFFA(player.getUniqueId())) {
			SpleefQueueManager manager = Main.getInstance().getSpleefQueueManager();
			SpleefFFAGame game = manager.getGame(manager.getFFAMode(player.getUniqueId()));

			if (game == null) {
				sendLobbyBoard(player, objective);
				player.setScoreboard(scoreboard);
				return;
			}

			objective.getScore("").setScore(15);
			objective.getScore("§c§lPlayers Waiting").setScore(14);
			objective.getScore("§f" + game.getWaitingPlayers()).setScore(13);
			objective.getScore("§c§lPlayers In Game").setScore(12);
			objective.getScore("§f" + (game.getPlayersSpectating().size() + game.getAlivePlayers())).setScore(11);
			objective.getScore("§c").setScore(10);

			if (game.getSecondsUntilStart() != 0) {
				objective.getScore("§e§lTime Until Game §f").setScore(9);
				objective.getScore("§f" + game.getSecondsUntilStart() + "s").setScore(8);
			} else {
				objective.getScore("§7Wait for players to join ").setScore(9);
				objective.getScore("§7or the game to end!").setScore(8);
			}

			objective.getScore(" ").setScore(7);
			objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(6);

			player.setScoreboard(scoreboard);
			return;
		}

		// Lobby
		sendLobbyBoard(player, objective);
		player.setScoreboard(scoreboard);
	}

	private void sendLobbyBoard(Player player, Objective objective) {
		objective.getScore("§3Name §8» §f" + player.getName()).setScore(14);

		if (dataLoader.hasPurchasableRank(player.getUniqueId(), PurchasableRank.CUSTOM)) {
			objective.getScore("§5Rank §8» §d" + dataLoader.getCustomRankName(player.getUniqueId())).setScore(13);
		} else if (dataLoader.hasPurchasableRank(player.getUniqueId(), PurchasableRank.POLAR)) {
			objective.getScore("§3Rank §8» " + gradient("Polar", "#72FAF4", "#03C69B")).setScore(13);
		} else if (dataLoader.hasPurchasableRank(player.getUniqueId(), PurchasableRank.GLACIER)) {
			objective.getScore("§3Rank §8» " + gradient("Glacier", "#B8F4FF", "#1E8FC2")).setScore(13);
		} else {
			objective.getScore("§3Rank §8» §7None").setScore(13);
		}

		objective.getScore("§1 ").setScore(12);

		objective.getScore("§6Currency §8» §f0").setScore(11);
		objective.getScore("§aRanked Wins §8» §f" + dataLoader.getWins(player.getUniqueId())).setScore(10);

		int averageElo = (int) Math.ceil((dataLoader.getAirSpleefElo(player.getUniqueId())
				+ dataLoader.getMiniSpleefElo(player.getUniqueId()) + dataLoader.getPowerSpleefElo(player.getUniqueId())
				+ dataLoader.getSpleggElo(player.getUniqueId())) / 4.0);

		objective.getScore("§bAverage ELO §8» §f" + averageElo).setScore(9);

		objective.getScore("§2 ").setScore(8);

		objective.getScore("§dFriends §8» §f0").setScore(7);
		objective.getScore("§3Guild §8» §7None").setScore(6);

		objective.getScore("§8 ").setScore(5);
		objective.getScore("§f<< §bmc.glacium.net §f>>").setScore(4);
	}

	public static String gradient(String text, String startHex, String endHex) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		Color start = Color.decode(startHex);
		Color end = Color.decode(endHex);

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			double ratio = text.length() == 1 ? 0 : (double) i / (text.length() - 1);

			int r = (int) Math.round(start.getRed() + ratio * (end.getRed() - start.getRed()));
			int g = (int) Math.round(start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
			int b = (int) Math.round(start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

			result.append(toMinecraftHex(r, g, b)).append(text.charAt(i));
		}

		return result.toString();
	}

	private static String toMinecraftHex(int r, int g, int b) {
		String hex = String.format("%02X%02X%02X", r, g, b);

		StringBuilder result = new StringBuilder("§x");
		for (char c : hex.toCharArray()) {
			result.append('§').append(c);
		}

		return result.toString();
	}

	private static String toHex(int r, int g, int b) {
		return String.format("§x§%1$X§%2$X§%3$X§%4$X§%5$X§%6$X", (r >> 4) & 0xF, r & 0xF, (g >> 4) & 0xF, g & 0xF,
				(b >> 4) & 0xF, b & 0xF);
	}

	public void updateScoreboards() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			Scoreboard scoreboard = p.getScoreboard();
			if (scoreboard == null || scoreboard.getObjective("PermaFrostCore") == null) {
				createNewScoreboard(p);
			}
			updateScoreboard(p);
		}
	}

	public int getEggsFired(Connection connection, UUID uuid) {
		String sql = "SELECT eggs_fired FROM splegg_ffa_eggs_fired WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

	private String formatDecimal(double value) {
		if (value == (long) value) {
			return String.valueOf((long) value);
		} else {
			return String.format("%.1f", value);
		}
	}

	public String formatNumber(int number) {
		if (number >= 1_000_000_000) {
			return String.format("%.1fB", number / 1_000_000_000.0);
		} else if (number >= 1_000_000) {
			return String.format("%.1fM", number / 1_000_000.0);
		} else if (number >= 1_000) {
			return String.format("%.1fk", number / 1_000.0);
		} else {
			return String.valueOf(number);
		}
	}

	public int getWins(Connection connection, UUID uuid) {
		String sql = "SELECT wins FROM splegg_ffa_wins WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

	public int getPoints(Connection connection, UUID uuid) {
		String sql = "SELECT points FROM splegg_ffa_points WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, uuid.toString());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("points");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public String formatTime(int seconds) {
		int minutes = seconds / 60;
		int secs = seconds % 60;

		return String.format("%02d:%02d", minutes, secs);
	}

	public String formatTwoDecimals(double value) {
		return String.format("%.2f", value);
	}

	public static Board getInstance() {
		return instance;
	}
}