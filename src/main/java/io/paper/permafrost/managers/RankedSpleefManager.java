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
import io.paper.permafrost.utilities.SpleefMode;

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

public class RankedSpleefManager {
    private final Main main;
    private boolean splegg;

    private final Set<SpleefGame> games = new HashSet<>();

    // Separate queues per mode
    private final Map<SpleefMode, Set<UUID>> playersInQueue = new EnumMap<>(SpleefMode.class);

    // Separate "last opponent" maps per mode
    private final Map<SpleefMode, Map<UUID, UUID>> playersLastPlayed = new EnumMap<>(SpleefMode.class);

    // Separate priority player per mode
    private final Map<SpleefMode, UUID> priorityPlayers = new EnumMap<>(SpleefMode.class);

    // Separate countdown task and seconds per mode
    private final Map<SpleefMode, BukkitRunnable> countdownTasks = new EnumMap<>(SpleefMode.class);
    private final Map<SpleefMode, Integer> countdownSeconds = new EnumMap<>(SpleefMode.class);

    public RankedSpleefManager(Main main) {
        this.main = main;

        for (SpleefMode mode : SpleefMode.values()) {
            playersInQueue.put(mode, new HashSet<>());
            playersLastPlayed.put(mode, new HashMap<>());
            priorityPlayers.put(mode, null);
            countdownTasks.put(mode, null);
            countdownSeconds.put(mode, -1);
        }
    }

    public Set<SpleefGame> getGames() {
        return games;
    }

    public void addGame(SpleefGame game) {
        this.games.add(game);
    }

    public Set<UUID> getPlayersInQueue(SpleefMode mode) {
        return playersInQueue.get(mode);
    }

    public int getQueueSize(SpleefMode mode) {
        return playersInQueue.get(mode).size();
    }

    public int getTotalPlayersInQueue() {
        int total = 0;
        for (SpleefMode mode : SpleefMode.values()) {
            total += playersInQueue.get(mode).size();
        }
        return total;
    }

    public boolean isInAnyQueue(UUID uuid) {
        for (SpleefMode mode : SpleefMode.values()) {
            if (playersInQueue.get(mode).contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    public SpleefMode getQueueMode(UUID uuid) {
        for (SpleefMode mode : SpleefMode.values()) {
            if (playersInQueue.get(mode).contains(uuid)) {
                return mode;
            }
        }
        return null;
    }

    public void addPlayerToQueue(UUID uuid, SpleefMode mode) {
        if (mode == null) {
            return;
        }

        // Remove from any other queue first so they can only be in one mode queue at once
        removePlayerFromAllQueues(uuid);

        playersInQueue.get(mode).add(uuid);

        Player player = Bukkit.getPlayer(uuid);

        updateRankedHologram();
        checkCountdownStart(mode);
    }

    public void removePlayerFromQueue(UUID uuid, SpleefMode mode) {
        if (mode == null) {
            return;
        }

        playersInQueue.get(mode).remove(uuid);

        UUID priorityPlayer = priorityPlayers.get(mode);
        if (priorityPlayer != null && uuid.equals(priorityPlayer)) {
            priorityPlayers.put(mode, null);
        }

        updateRankedHologram();
        checkCountdownCancel(mode);
    }

    public void removePlayerFromAllQueues(UUID uuid) {
        for (SpleefMode mode : SpleefMode.values()) {
            boolean removed = playersInQueue.get(mode).remove(uuid);

            UUID priorityPlayer = priorityPlayers.get(mode);
            if (priorityPlayer != null && uuid.equals(priorityPlayer)) {
                priorityPlayers.put(mode, null);
            }

            if (removed) {
                checkCountdownCancel(mode);
            }
        }

        updateRankedHologram();
    }

    private void checkCountdownStart(SpleefMode mode) {
        if (playersInQueue.get(mode).size() >= 2 && countdownTasks.get(mode) == null) {
            startCountdown(mode);
        }
    }

    private void checkCountdownCancel(SpleefMode mode) {
        if (playersInQueue.get(mode).size() < 2 && countdownTasks.get(mode) != null) {
            BukkitRunnable task = countdownTasks.get(mode);
            if (task != null) {
                task.cancel();
            }

            countdownTasks.put(mode, null);
            countdownSeconds.put(mode, -1);

            for (UUID uuid : playersInQueue.get(mode)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                    p.sendMessage(ChatColor.RED + "Not enough players for " + formatModeName(mode) + ". Matchmaking cancelled.");
                }
            }
        }
    }

    public void startCountdown(SpleefMode mode) {
        countdownSeconds.put(mode, 15);

        for (UUID uuid : playersInQueue.get(mode)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
                p.sendMessage(ChatColor.GREEN + "Enough players found for " + formatModeName(mode) + "! Sending games in 15 seconds...");
            }
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (playersInQueue.get(mode).size() < 2) {
                    cancel();
                    countdownTasks.put(mode, null);
                    countdownSeconds.put(mode, -1);
                    return;
                }

                int seconds = countdownSeconds.get(mode);

                if (seconds == 5) {
                    for (UUID uuid : playersInQueue.get(mode)) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f);
                            p.sendMessage(ChatColor.GREEN + formatModeName(mode) + " games starting in 5 seconds...");
                        }
                    }
                }

                if (seconds <= 0) {
                    cancel();
                    countdownTasks.put(mode, null);
                    countdownSeconds.put(mode, -1);
                    createAndSendMatches(mode);
                    return;
                }

                countdownSeconds.put(mode, seconds - 1);
            }
        };

        countdownTasks.put(mode, task);
        task.runTaskTimer(main, 20L, 20L);
    }

    private void createAndSendMatches(SpleefMode mode) {
        Set<UUID> queue = playersInQueue.get(mode);

        if (queue.size() < 2) {
            return;
        }

        List<UUID> queued = new ArrayList<>(queue);
        Collections.shuffle(queued);

        List<UUID[]> pairings = buildPairings(mode, queued);

        if (pairings.isEmpty()) {
            for (UUID uuid : queue) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                    p.sendMessage(ChatColor.RED + "Could not create fair " + formatModeName(mode) + " matchups this round.");
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

        priorityPlayers.put(mode, leftOut);

        if (leftOut != null) {
            Player p = Bukkit.getPlayer(leftOut);
            if (p != null) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                p.sendMessage(ChatColor.YELLOW + "You were left out of ranked " + formatModeName(mode)
                        + " this round, but you will be included next round.");
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

                playersLastPlayed.get(mode).put(p1, p2);
                playersLastPlayed.get(mode).put(p2, p1);

                Player player1 = Bukkit.getPlayer(p1);
                Player player2 = Bukkit.getPlayer(p2);

                if (player1 != null && player2 != null) {
                    player1.performCommand(buildRankedDuelCommand(player2, mode));
                }

                index++;
            }
        }.runTaskTimer(main, 0L, 40L);

        if (queue.size() >= 2) {
            startCountdown(mode);
        }

        updateRankedHologram();
    }

    private List<UUID[]> buildPairings(SpleefMode mode, List<UUID> queued) {
        List<UUID[]> bestPairings = new ArrayList<>();
        List<UUID> working = new ArrayList<>(queued);

        UUID priorityPlayer = priorityPlayers.get(mode);

        if (working.size() % 2 != 0) {
            UUID toRemove = choosePlayerToLeaveOut(mode, working);
            working.remove(toRemove);
        }

        Collections.shuffle(working);

        boolean success = pairPlayersRecursive(mode, working, new ArrayList<>(), bestPairings);

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

    private UUID choosePlayerToLeaveOut(SpleefMode mode, List<UUID> players) {
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        UUID priorityPlayer = priorityPlayers.get(mode);

        for (UUID uuid : shuffled) {
            if (priorityPlayer != null && uuid.equals(priorityPlayer)) {
                continue;
            }
            return uuid;
        }

        return shuffled.get(0);
    }

    private boolean pairPlayersRecursive(SpleefMode mode, List<UUID> remaining, List<UUID[]> currentPairings, List<UUID[]> result) {
        if (remaining.isEmpty()) {
            result.clear();
            result.addAll(currentPairings);
            return true;
        }

        UUID first = remaining.get(0);

        List<UUID> candidates = new ArrayList<>(remaining.subList(1, remaining.size()));
        Collections.shuffle(candidates);

        for (UUID second : candidates) {
            if (justPlayed(mode, first, second)) {
                continue;
            }

            List<UUID> nextRemaining = new ArrayList<>(remaining);
            nextRemaining.remove(first);
            nextRemaining.remove(second);

            List<UUID[]> nextPairings = new ArrayList<>(currentPairings);
            nextPairings.add(new UUID[] { first, second });

            if (pairPlayersRecursive(mode, nextRemaining, nextPairings, result)) {
                return true;
            }
        }

        return false;
    }

    private boolean justPlayed(SpleefMode mode, UUID p1, UUID p2) {
        Map<UUID, UUID> modeMap = playersLastPlayed.get(mode);

        UUID last1 = modeMap.get(p1);
        UUID last2 = modeMap.get(p2);

        return (last1 != null && last1.equals(p2)) || (last2 != null && last2.equals(p1));
    }

    private void updateRankedHologram() {
        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "hologram edit RankedSpleef setline 4 &fIn Queue: &e" + getTotalPlayersInQueue()
        );
    }

    private String buildRankedDuelCommand(Player target, SpleefMode mode) {
        // Adjust this command format if your duel command expects mode differently.
        // Current output examples:
        // /duel Steve true powerspleef
        // /duel Steve true airspleef
        // /duel Steve true minispleef
        return "duel " + target.getName() + " true " + mode.name().toLowerCase() + " true";
    }

    public String formatModeName(SpleefMode mode) {
        return switch (mode) {
            case POWERSPLEEF -> "Spleef";
            case AIRSPLEEF -> "AirSpleef";
            case MINISPLEEF -> "MiniSpleef";
            case TESTSPLEEF -> "TestSpleef";
        };
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
