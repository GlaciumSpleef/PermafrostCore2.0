package io.paper.permafrost.parkour;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.utilities.ShovelPowers;
import net.md_5.bungee.api.ChatColor;

public class ParkourGame {
	private Main main;
	private boolean ranked;
	private UUID player1;
	private UUID player2;
	private ParkourMap map;
	private boolean inCountdownPhase;
	private boolean inMatchPhase;

	public ParkourGame(Main main, boolean ranked, UUID player1, UUID player2) {
		this.main = main;
		this.ranked = ranked;
		this.player1 = player1;
		this.player2 = player2;
		inCountdownPhase = false;
		inMatchPhase = false;
	}

	public boolean isInCountdownPhase() {
		return inCountdownPhase;
	}
	
	public boolean isInMatchPhase() {
		return inMatchPhase;
	}
	
	public ParkourMap getMap() {
		return map;
	}
	
	public boolean isPlayerOne(UUID uuid) {
		if(uuid == player1)
			return true;
		return false;
	}

	public void startMatch() {
		Set<UUID> players = new HashSet<>();
		players.add(player1);
		players.add(player2);

		for (UUID uuid : players) {
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid));
			if (sp != null) {
				sp.setInParkourGame(true);
				sp.setParkourGame(this);
			}
		}

		for (ParkourMap map : main.getDataLoader().getParkourMaps()) {
			if (!map.isInMatch()) {
				this.map = map;
			}
		}

		if (map == null) {
			for (UUID uuid : players) {
				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid));
				if (sp != null) {
					sp.sendMessage(ChatColor.RED + "Couldn't find a valid parkour map to use. Cancelling match.");
					sp.setInParkourGame(false);
					sp.setParkourGame(null);
				}
			}
		} else {
			map.setInMatch(true);
			Bukkit.getPlayer(player1).sendMessage(ChatColor.GREEN + "Match Found! Starting your " + ChatColor.YELLOW
					+ "Parkour " + ChatColor.GREEN + " duel against " + Bukkit.getPlayer(player2).getName() + "!");
			Bukkit.getPlayer(player1).teleport(map.getSpawn1());
			Bukkit.getPlayer(player2).sendMessage(ChatColor.GREEN + "Match Found! Starting your " + ChatColor.YELLOW
					+ "Parkour " + ChatColor.GREEN + " duel against " + Bukkit.getPlayer(player1).getName() + "!");
			Bukkit.getPlayer(player2).teleport(map.getSpawn2());
		}

		BukkitTask countdownTask = new BukkitRunnable() {
			int seconds = 6;

			@Override
			public void run() {
				seconds--;
				Bukkit.getConsoleSender().sendMessage("Countdown Started.");
				inCountdownPhase = true;
				for (UUID uuid : players) {
					Player player = Bukkit.getPlayer(uuid);
					if (seconds <= 5 && seconds > 0) {
						String title = ChatColor.GREEN + "" + seconds;
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, 0.85F);
						player.sendTitle("", title, 5, 20, 5);
					} else {
						String title = ChatColor.GREEN + "GO!";
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1F);
						player.sendTitle("", title, 5, 20, 5);
						inMatchPhase = true;
						inCountdownPhase = false;
						this.cancel();
					}
				}
			}
		}.runTaskTimer(main, 0L, 20L);
	}
	
	public void endMatch(UUID winner) {
		Bukkit.getPlayer(winner).sendMessage("You won!");
		Set<UUID> players = new HashSet<>();
		players.add(player1);
		players.add(player2);
		for (UUID uuid : players) {
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid));
			if (sp != null) {
				sp.setInParkourGame(false);
				sp.setParkourGame(null);
				map.setInMatch(false);
			}
		}
	}
}
