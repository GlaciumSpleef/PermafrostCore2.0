package io.paper.permafrost.core.game.tasks;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class RemoveIceTask {
	private Main main;
	private SpleefPlayer sp;
	private int seconds;

	public RemoveIceTask(Main main, SpleefPlayer sp, int seconds) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		startRemoval();
	}

	public void startRemoval() {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!sp.isRoundStarting()) {
					switch (seconds) {
					case (1): {
						for (Location loc : main.getDataLoader().getIceSpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR) {
								block.setType(Material.ICE);
							}
						}
						break;
					}
					case (0): {
						for (Location loc : main.getDataLoader().getIceSpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR) {
								if (block.getType() == Material.ICE) {
									block.setType(Material.SNOW_BLOCK);
								}
								if (sp.getGame() != null) {
									main.getDataLoader().addSnowSpawned(sp.getPlayer().getUniqueId(), loc);
									Bukkit.getLogger().info("snow added to data loader");
								} else if (main.getSpleefQueueManager().isAliveInAnyFFA(sp.getPlayer().getUniqueId())
										&& main.getSpleefQueueManager().getFFAMode(sp.getPlayer().getUniqueId())
												.equals(SpleefFFAMode.POWERSPLEEF)) {
									if (loc.getBlockY() > main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF).getArena().getCorner1().getBlockY()) {
										main.getDataLoader().addSnowSpawned(sp.getPlayer().getUniqueId(), loc);
										Bukkit.getLogger().info("snow added to data loader");
									}
								}
							}
						}
						main.getDataLoader().removeAllIceSpawned(sp.getPlayer().getUniqueId());
						cancel();
						break;
					}
					}
					seconds--;
				} else {
					for (Location loc : main.getDataLoader().getIceSpawned(sp.getPlayer().getUniqueId())) {
						Block block = loc.getBlock();
						block.setType(Material.SNOW_BLOCK);
						if (sp.getGame() != null) {
							main.getDataLoader().addSnowSpawned(sp.getPlayer().getUniqueId(), loc);
							Bukkit.getLogger().info("snow added to data loader");
						} else if (main.getSpleefQueueManager().isAliveInAnyFFA(sp.getPlayer().getUniqueId())
								&& main.getSpleefQueueManager().getFFAMode(sp.getPlayer().getUniqueId())
										.equals(SpleefFFAMode.POWERSPLEEF)) {
							if (loc.getBlockY() > main.getSpleefQueueManager().getGame(SpleefFFAMode.POWERSPLEEF).getArena().getCorner1().getBlockY()) {
								main.getDataLoader().addSnowSpawned(sp.getPlayer().getUniqueId(), loc);
								Bukkit.getLogger().info("snow added to data loader");
							}
						}
					}
					main.getDataLoader().removeAllIceSpawned(sp.getPlayer().getUniqueId());
					cancel();
				}
			}
		}.runTaskTimer(main, 0, 20);
	}
}
