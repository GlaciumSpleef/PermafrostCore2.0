package io.paper.permafrost.managers;

import java.util.ArrayList;
import java.util.Collections;
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

public class RankedSpleggManager {

	private final Main main;
	private boolean splegg;

	private final Set<UUID> playersInQueue = new HashSet<>();
	private final Set<SpleefGame> games = new HashSet<>();

	// key = player, value = last opponent they played
	private final Map<UUID, UUID> playersLastPlayed = new HashMap<>();

	// if someone was left out last round, they MUST be included next round
	private UUID priorityPlayer = null;

	private BukkitRunnable countdownTask;
	private int countdownSeconds = -1;

	public RankedSpleggManager(Main main) {
		this.main = main;
	}

	public Set<UUID> getPlayersInQueue() {
		return playersInQueue;
	}
	
	public void setSplegg(boolean splegg) {
		this.splegg = splegg;
	}
	
	public boolean isSplegg() {
		return splegg;
	}
	
	public void addGame(SpleefGame game) {
		this.games.add(game);
	}

	public void addPlayerToQueue(UUID uuid) {
		playersInQueue.add(uuid);

		Player player = Bukkit.getPlayer(uuid);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
				+ main.getRankedSpleggManager().getPlayersInQueue().size());
		checkCountdownStart();
	}
	
	

	public void removePlayerFromQueue(UUID uuid) {
		playersInQueue.remove(uuid);

		if (uuid.equals(priorityPlayer)) {
			priorityPlayer = null;
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
				+ main.getRankedSpleggManager().getPlayersInQueue().size());
		checkCountdownCancel();
	}

	private void checkCountdownStart() {
		if (playersInQueue.size() >= 2 && countdownTask == null) {
			startCountdown();
		}
	}

	private void checkCountdownCancel() {
		if (playersInQueue.size() < 2 && countdownTask != null) {
			countdownTask.cancel();
			countdownTask = null;
			countdownSeconds = -1;

			for (UUID uuid : playersInQueue) {
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
					p.sendMessage(ChatColor.RED + "Not enough players. Matchmaking cancelled.");
				}
			}
		}
	}
	
	public Set<SpleefGame> getGames() {
		return games;
	}

	public void startCountdown() {
		countdownSeconds = 15;

		for (UUID uuid : playersInQueue) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
				p.sendMessage(ChatColor.GREEN + "Enough players found! Sending games in 15 seconds...");
			}
		}

		countdownTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (playersInQueue.size() < 2) {
					cancel();
					countdownTask = null;
					countdownSeconds = -1;
					return;
				}

				if (countdownSeconds == 5) {
					for (UUID uuid : playersInQueue) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null) {
							p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
							p.sendMessage(ChatColor.GREEN + "Games starting in 5 seconds...");
						}
					}
				}

				if (countdownSeconds <= 0) {
					cancel();
					countdownTask = null;
					countdownSeconds = -1;
					createAndSendMatches();
					return;
				}

				countdownSeconds--;
			}
		};

		countdownTask.runTaskTimer(main, 20L, 20L);
	}

	private void createAndSendMatches() {
		if (playersInQueue.size() < 2) {
			return;
		}

		List<UUID> queued = new ArrayList<>(playersInQueue);
		Collections.shuffle(queued);

		List<UUID[]> pairings = buildPairings(queued);

		if (pairings.isEmpty()) {
			// This should only happen in weird edge cases
			for (UUID uuid : playersInQueue) {
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
					p.sendMessage(ChatColor.RED + "Could not create fair matchups this round.");
				}
			}
			return;
		}

		// Remove matched players from queue
		Set<UUID> matchedPlayers = new HashSet<>();
		for (UUID[] pair : pairings) {
			matchedPlayers.add(pair[0]);
			matchedPlayers.add(pair[1]);
		}
		playersInQueue.removeAll(matchedPlayers);

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
				p.sendMessage(ChatColor.YELLOW + "You were left out this round, but you will be included next round.");
			}
		}

		// Send one duel every 2 seconds
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

				// update last played
				playersLastPlayed.put(p1, p2);
				playersLastPlayed.put(p2, p1);

				Player player1 = Bukkit.getPlayer(p1);
				Player player2 = Bukkit.getPlayer(p2);
				
				player1.performCommand("duel " + player2.getName() + " true splegg true");

				// if your RankedGame needs a start method, call it here
				// game.start();

				index++;
			}
		}.runTaskTimer(main, 0L, 40L); // 40 ticks = 2 seconds

		// If there are still 2+ players left in queue after removing matched players,
		// start the next countdown automatically
		if (playersInQueue.size() >= 2) {
			startCountdown();
		}
		
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hologram edit RankedSplegg setline 4 &fIn Queue: &e"
				+ main.getRankedSpleggManager().getPlayersInQueue().size());
	}

	private List<UUID[]> buildPairings(List<UUID> queued) {
		List<UUID[]> bestPairings = new ArrayList<>();
		List<UUID> working = new ArrayList<>(queued);

		// If there is a priority player, force them to stay in the list
		if (priorityPlayer != null && working.contains(priorityPlayer)) {
			// good, leave them in
		}

		// If odd number of players, remove one player from THIS round
		if (working.size() % 2 != 0) {
			UUID toRemove = choosePlayerToLeaveOut(working);
			working.remove(toRemove);
		}

		Collections.shuffle(working);

		boolean success = pairPlayersRecursive(working, new ArrayList<>(), bestPairings);

		// fallback: if no perfect no-repeat solution exists, allow a repeat rather than fail
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

		// Never leave out the priority player if they are in queue
		for (UUID uuid : shuffled) {
			if (priorityPlayer != null && uuid.equals(priorityPlayer)) {
				continue;
			}
			return uuid;
		}

		// fallback, should almost never happen
		return shuffled.get(0);
	}

	private boolean pairPlayersRecursive(List<UUID> remaining, List<UUID[]> currentPairings, List<UUID[]> result) {
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
		UUID last1 = playersLastPlayed.get(p1);
		UUID last2 = playersLastPlayed.get(p2);

		return (last1 != null && last1.equals(p2)) || (last2 != null && last2.equals(p1));
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
