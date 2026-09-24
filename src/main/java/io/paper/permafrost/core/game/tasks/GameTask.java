package io.paper.permafrost.core.game.tasks;

import org.bukkit.Bukkit;

import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;

public class GameTask {
private int id;
	
	public GameTask(Main main,SpleefGame game) {
		
		id = new BukkitRunnable() {
			public void run() {
		    		if (game.getGameState().equals(GameState.GAME)) {
		    				int totaltime = game.getTotalTime()+1;
		    			game.setTotalTime(totaltime);
		    			int roundtime = game.getRoundTime()+1;
		    			game.setRoundTime(roundtime);
		    			}
			}
		}.runTaskTimerAsynchronously(main, 0L, 20L).getTaskId();
	}

	public void cancel() {
		Bukkit.getScheduler().cancelTask(id);
	}
}
