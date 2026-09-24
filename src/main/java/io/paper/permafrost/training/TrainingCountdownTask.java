package io.paper.permafrost.training;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.core.game.SpleggGame;
import io.paper.permafrost.core.game.TeamSpleefTeam;

public class TrainingCountdownTask {
	private Main main;
	private TrainingSession session;
	private int time, i;
	private int task;

	public TrainingCountdownTask(Main main, TrainingSession session) {
		this.main = main;
		this.session = session;
		this.time = 3;
		this.i = 0;
		this.session.setTrainingState(TrainingState.STARTING);
		task();
	}

	private void task() {
		task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
			public void run() {
				if (session != null) {
					if (!session.getTrainingState().equals(TrainingState.STARTING)) {
						Bukkit.getScheduler().cancelTask(task);
						return;
					}

					float d = 0.85F;
					String title = "";

					switch (time) {
					case 3:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "READY";
						break;
					case 2:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "SET";
						break;
					case 1:
						title = ChatColor.AQUA + "" + ChatColor.BOLD + "GO!!!";
						d = 1F;
						i = 1;
						break;
					}
					for (UUID uuid : session.getPlayers()) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null) {
							if (p.isOnline()) {
								if (p != null && i == 0) {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								} else {
									p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, d);
									p.sendTitle("", title, 5, 20, 5);
								}
							}
						}
					}
					Player p = Bukkit.getPlayer(session.getLeader());
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, d);
					p.sendTitle("", title, 5, 20, 5);
					time--;

					if (time <= 0) {
						session.setTrainingState(TrainingState.ONGOING);
						session.pastePlot();
						if (session.getPlot().hasTimer())
							session.startTimer(main);
						Bukkit.getScheduler().cancelTask(task);
					}
				}
			}
		}, 2L, 10L);
	}
}
