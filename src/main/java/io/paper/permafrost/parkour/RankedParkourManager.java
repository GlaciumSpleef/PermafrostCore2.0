package io.paper.permafrost.parkour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.utilities.SpleefMode;

public class RankedParkourManager {
	private final Main main;

	private final Set<ParkourGame> games = new HashSet<>();

	// Separate queues
	private final Set<UUID> playersInQueue;

	// Separate "last opponent" maps
	private final Map<UUID, UUID> playersLastPlayed;

	// Separate priority player
	private UUID priorityPlayer;

	// Separate countdown task and seconds
	private BukkitRunnable countdownTask;
	private Integer countdownSeconds;

	public RankedParkourManager(Main main) {
		this.main = main;
		playersInQueue = new HashSet<>();
		playersLastPlayed = new HashMap<>();
		priorityPlayer = null;
		countdownTask = null;
		countdownSeconds = 0;
	}

	public Set<ParkourGame> getGames() {
		return games;
	}

	public void addGame(ParkourGame game) {
		this.games.add(game);
	}

	public Set<UUID> getPlayersInQueue() {
		return playersInQueue;
	}

	public int getQueueSize() {
		return playersInQueue.size();
	}

	public boolean isInQueue(UUID uuid) {
		return playersInQueue.contains(uuid);
	}

	public void addPlayerToQueue(UUID uuid) {
		if (!playersInQueue.add(uuid)) {
			return;
		}

		updateRankedHologram();
		checkCountdownStart();
	}

	public void removePlayerFromQueue(UUID uuid) {
		if (!playersInQueue.remove(uuid)) {
			return;
		}

		updateRankedHologram();
		checkCountdownCancel();
	}

	private void checkCountdownStart() {
		if (playersInQueue.size() >= 2 && countdownTask == null) {
			startCountdown();
		}
	}

	private void checkCountdownCancel() {
		if (playersInQueue.size() < 2 && countdownTask != null) {
			BukkitRunnable task = countdownTask;
			if (task != null) {
				task.cancel();
			}

			countdownTask = null;
			countdownSeconds = -1;

			for (UUID uuid : playersInQueue) {
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
					p.sendMessage(ChatColor.RED + "Not enough players for ranked parkour. Matchmaking cancelled.");
				}
			}
		}
	}

	public void startCountdown() {
		countdownSeconds = 15;

		for (UUID uuid : playersInQueue) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
				p.sendMessage(ChatColor.GREEN + "Enough players found for ranked parkour! Sending games in 15 seconds...");
			}
		}

		BukkitRunnable task = new BukkitRunnable() {
			@Override
			public void run() {
				if (playersInQueue.size() < 2) {
					cancel();
					countdownTask = null;
					countdownSeconds = -1;
					return;
				}

				int seconds = countdownSeconds;

				if (seconds == 5) {
					for (UUID uuid : playersInQueue) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null) {
							p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
							p.sendMessage(ChatColor.GREEN + "Ranked parkour games starting in 5 seconds...");
						}
					}
				}

				if (seconds <= 0) {
					cancel();
					countdownTask = null;
					countdownSeconds = -1;
					createAndSendMatches();
					return;
				}

				countdownSeconds = seconds - 1;
			}
		};

		countdownTask = task;
		task.runTaskTimer(main, 20L, 20L);
	}

	private void createAndSendMatches() {
		Set<UUID> queue = playersInQueue;

		if (queue.size() < 2) {
			return;
		}

		List<UUID> queued = new ArrayList<>(queue);
		Collections.shuffle(queued);

		List<UUID[]> pairings = buildPairings(queued);

		if (pairings.isEmpty()) {
			for (UUID uuid : queue) {
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
					p.sendMessage(
							ChatColor.RED + "Could not create fair ranked parkour matchups this round.");
				}
			}
			return;
		}

		// Remove matched players from this mode queue
		Set<UUID> matchedPlayers = new HashSet<>();
		for (UUID[] pair : pairings) {
			matchedPlayers.add(pair[0]);
			matchedPlayers.add(pair[1]);
		}
		queue.removeAll(matchedPlayers);

		// Figure out who got left out
		UUID leftOut = null;
		for (UUID uuid : queued) {
			if (!matchedPlayers.contains(uuid)) {
				leftOut = uuid;
				break;
			}
		}

		priorityPlayer = leftOut;

		if (leftOut != null) {
			Player p = Bukkit.getPlayer(leftOut);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
				p.sendMessage(ChatColor.YELLOW + "You were left out of ranked parkour this round, but you will be included next round.");
			}
		}

		new BukkitRunnable() {
			int index = 0;

			@Override
			public void run() {
				if (index >= pairings.size()) {
					cancel();
					return;
				}

				UUID[] pair = pairings.get(index);
				UUID p1 = pair[0];
				UUID p2 = pair[1];

				playersLastPlayed.put(p1, p2);
				playersLastPlayed.put(p2, p1);

				Player player1 = Bukkit.getPlayer(p1);
				Player player2 = Bukkit.getPlayer(p2);

				if (player1 != null && player2 != null) {
					player1.performCommand(buildRankedDuelCommand(player2));
				}

				index++;
			}
		}.runTaskTimer(main, 0L, 40L);

		checkCountdownStart();
		updateRankedHologram();
	}

	private List<UUID[]> buildPairings(List<UUID> queued) {
		List<UUID[]> bestPairings = new ArrayList<>();
		List<UUID> working = new ArrayList<>(queued);

		if (working.size() % 2 != 0) {
			UUID toRemove = choosePlayerToLeaveOut(working);
			working.remove(toRemove);
		}

		Collections.shuffle(working);

		boolean success = pairPlayersRecursive(working, new ArrayList<>(), bestPairings);

		if (!success) {
			bestPairings.clear();
			List<UUID> fallback = new ArrayList<>(working);
			Collections.shuffle(fallback);

			for (int i = 0; i < fallback.size(); i += 2) {
				bestPairings.add(new UUID[] { fallback.get(i), fallback.get(i + 1) });
			}
		}

		return bestPairings;
	}

	private UUID choosePlayerToLeaveOut(List<UUID> players) {
		List<UUID> shuffled = new ArrayList<>(players);
		Collections.shuffle(shuffled);

		for (UUID uuid : shuffled) {
			if (priorityPlayer != null && uuid.equals(priorityPlayer)) {
				continue;
			}
			return uuid;
		}

		return shuffled.get(0);
	}

	private boolean pairPlayersRecursive(List<UUID> remaining, List<UUID[]> currentPairings,
			List<UUID[]> result) {
		if (remaining.isEmpty()) {
			result.clear();
			result.addAll(currentPairings);
			return true;
		}

		UUID first = remaining.get(0);

		List<UUID> candidates = new ArrayList<>(remaining.subList(1, remaining.size()));
		Collections.shuffle(candidates);

		for (UUID second : candidates) {
			if (justPlayed(first, second)) {
				continue;
			}

			List<UUID> nextRemaining = new ArrayList<>(remaining);
			nextRemaining.remove(first);
			nextRemaining.remove(second);

			List<UUID[]> nextPairings = new ArrayList<>(currentPairings);
			nextPairings.add(new UUID[] { first, second });

			if (pairPlayersRecursive(nextRemaining, nextPairings, result)) {
				return true;
			}
		}

		return false;
	}

	private boolean justPlayed(UUID p1, UUID p2) {
		Map<UUID, UUID> modeMap = playersLastPlayed;

		UUID last1 = modeMap.get(p1);
		UUID last2 = modeMap.get(p2);

		return (last1 != null && last1.equals(p2)) || (last2 != null && last2.equals(p1));
	}

	private void updateRankedHologram() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"hologram edit RankedParkour setline 4 &fIn Queue: &e" + getQueueSize());
	}

	private String buildRankedDuelCommand(Player target) {
		return "duel " + target.getName() + " true parkour true";
	}

	private String getPlayerName(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if (player != null) {
			return player.getName();
		}

		OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
		return offline.getName() != null ? offline.getName() : "Unknown";
	}
}
