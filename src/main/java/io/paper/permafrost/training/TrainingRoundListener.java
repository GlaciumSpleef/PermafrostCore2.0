package io.paper.permafrost.training;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;

public class TrainingRoundListener implements Listener {
	private final Main main;

	public TrainingRoundListener(Main main) {
		this.main = main;
	}

	public boolean isSnow(Material bT) {
		if (bT.equals(Material.SNOW_BLOCK))
			return true;
		return false;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();

		if (main.getDataLoader().getTrainingSession(player.getUniqueId()) != null) {
			if (event.getBlock().getType().equals(Material.SNOW_BLOCK)) {
				main.getDataLoader().setBlocksToBreak(player.getUniqueId(),
						(main.getDataLoader().getBlocksToBreak(player.getUniqueId()) + 1));
				if (main.getDataLoader().getBlocksToBreak(player.getUniqueId()) == main.getDataLoader()
						.getTrainingSession(player.getUniqueId()).getPlot().getBlocksToBreak()) {
					if (main.getDataLoader().getTrainingSession(player.getUniqueId()).getPlot().hasTimer()) {
						main.getDataLoader().getTrainingSession(player.getUniqueId()).stopTimer(false);
					} else {
						main.getDataLoader().getTrainingSession(player.getUniqueId()).setRoundCount(
								main.getDataLoader().getTrainingSession(player.getUniqueId()).getRoundCount() + 1);
					}
					main.getDataLoader().getTrainingSession(player.getUniqueId()).startTraining();
					main.getDataLoader().setBlocksToBreak(player.getUniqueId(), 0);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (event.getFrom().getX() == event.getTo().getX() && event.getFrom().getY() == event.getTo().getY()
				&& event.getFrom().getZ() == event.getTo().getZ()) {
			return; // Head movement only — ignore
		}

		TrainingSession session = main.getDataLoader().getTrainingSession(player.getUniqueId());

		if (session == null)
			return;

		if (session.getTrainingState() == TrainingState.STARTING) {
			event.setCancelled(true);
		}

		if (main.getDataLoader().getTrainingSession(player.getUniqueId()) != null) {
			if (main.getDataLoader().getTrainingSession(player.getUniqueId()).getArena() != null) {
				if (player.getLocation().getBlockY() < (main.getDataLoader().getTrainingSession(player.getUniqueId())
						.getArena().getPlotPasteLocation().getBlockY() - 1)) {
					if (player.getGameMode() != GameMode.CREATIVE) {
						if (main.getDataLoader().getTrainingSession(player.getUniqueId()).getPlot().hasTimer()) {
							main.getDataLoader().getTrainingSession(player.getUniqueId()).stopTimer(true);
						} else {
							main.getDataLoader().getTrainingSession(player.getUniqueId()).setRoundCount(
									main.getDataLoader().getTrainingSession(player.getUniqueId()).getRoundCount() + 1);
						}
						main.getDataLoader().getTrainingSession(player.getUniqueId()).startTraining();
						main.getDataLoader().setBlocksToBreak(player.getUniqueId(), 0);
					}
				}
			}
		}

	}
}
