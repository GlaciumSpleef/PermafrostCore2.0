package io.paper.permafrost.core.game.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;

public class DuelTask {
	private Main main;
	private final Set<UUID> players;
	private final List<Integer> teamSizes;
	private final Map<UUID, Boolean> responses = new HashMap<>();
	private final UUID sender;
	private final BukkitTask timeoutTask;

	public DuelTask(Main main, UUID sender, Set<UUID> players, List<Integer> teamSizes, Runnable onExpire) {
		this.main = main;
		this.players = players;
		for (UUID uuid : players) {
		    responses.put(uuid, null);
		    SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getOfflinePlayer(uuid));
		    sp.setHasBeenRequested(true);
		}
		this.sender = sender;
		this.teamSizes = teamSizes;
		this.timeoutTask = Bukkit.getScheduler().runTaskLater(main.getInstance(), onExpire, 20 * 90L);
	}

	public List<Integer> getTeamSizes() {
		return teamSizes;
	}

	public void respond(UUID player, boolean accepted) {
		if (responses.containsKey(player)) {
			responses.put(player, accepted);
		}
	}

	public boolean isComplete() {
		return responses.values().stream().noneMatch(Objects::isNull);
	}

	public boolean allAccepted() {
		return responses.values().stream().allMatch(Boolean::booleanValue);
	}

	public boolean isRecipient(UUID player) {
		return players.contains(player);
	}

	public void cancel() {
		timeoutTask.cancel();
	}

	public UUID getSender() {
		return sender;
	}

	public Set<UUID> getRecipients() {
		return players;
	}

	public Map<UUID, Boolean> getResponses() {
		return responses;
	}

	public List<UUID> getPending() {
	    List<UUID> pending = responses.entrySet().stream()
	        .filter(e -> e.getValue() == null)
	        .map(Map.Entry::getKey)
	        .collect(Collectors.toList());
	    return pending;
	}
}
