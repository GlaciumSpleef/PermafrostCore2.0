package io.paper.permafrost.core.game.tasks;

import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;


public class CancelTask implements Listener{
	private SpleefGame game;

    public CancelTask(SpleefGame game) {
        this.game = game;
    }

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (game.getGameState() == GameState.STARTING) {
			event.setCancelled(true);
		}
	}
}
