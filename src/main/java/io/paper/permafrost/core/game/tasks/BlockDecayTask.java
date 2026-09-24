package io.paper.permafrost.core.game.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class BlockDecayTask {
	private Main main;
	private SpleefPlayer sp;
	private int seconds;
	private Location loc;

	public BlockDecayTask(Main main, SpleefPlayer sp, int seconds, Location loc) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		this.loc = loc;
		startRemoval();
	}

	public void startRemoval() {
		new BukkitRunnable() {
			@Override
			public void run() {
				Block block = loc.getBlock();
				if (block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE
						|| block.getType() == Material.GREEN_CONCRETE_POWDER) {
					this.cancel();
				}
				if (!sp.isRoundStarting()) {
					switch (seconds) {
					case 2: {
						if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
								&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE) {
							block.setType(Material.ORANGE_CONCRETE_POWDER);
						}
						break;
					}
					case 1: {
						if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
								&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE) {
							block.setType(Material.RED_CONCRETE_POWDER);
						}
						break;
					}
					case 0: {
						if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
								&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE) {
							block.setType(Material.AIR);
						}
						cancel();
						break;
					}
					}
					seconds--;
				} else {

					cancel();
				}
			}
		}.runTaskTimer(main, 0, 5);
	}
}
