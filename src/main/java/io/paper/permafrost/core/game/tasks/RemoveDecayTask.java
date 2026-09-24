package io.paper.permafrost.core.game.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class RemoveDecayTask {
	private Main main;
	private SpleefPlayer sp;
	private int seconds;
	private Set<Location> decayingBlocks;

	public RemoveDecayTask(Main main, SpleefPlayer sp, int seconds) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		decayingBlocks = new HashSet<>();
		startRemoval();
	}

	public void startRemoval() {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!sp.isRoundStarting()) {
					switch (seconds) {
					case (3): {
						for (Location loc : main.getDataLoader().getDecaySpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
									&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE
									&& block.getType() != Material.GREEN_CONCRETE_POWDER) {
								int random = ((int) ((Math.random() * 100) + 1));
								if (random <= 25) {
									new BlockDecayTask(main, sp, 2, loc);
									decayingBlocks.add(loc);
								}
							}
						}
						break;
					}
					case (2): {
						for (Location loc : main.getDataLoader().getDecaySpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
									&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE
									&& !decayingBlocks.contains(loc)
									&& block.getType() != Material.GREEN_CONCRETE_POWDER) {
								int random = ((int) ((Math.random() * 100) + 1));
								if (random <= 50) {
									new BlockDecayTask(main, sp, 2, loc);
									decayingBlocks.add(loc);
								}
							}
						}
						break;
					}
					case (1): {
						for (Location loc : main.getDataLoader().getDecaySpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
									&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE
									&& !decayingBlocks.contains(loc)
									&& block.getType() != Material.GREEN_CONCRETE_POWDER) {
								int random = ((int) ((Math.random() * 100) + 1));
								if (random <= 75) {
									new BlockDecayTask(main, sp, 2, loc);
									decayingBlocks.add(loc);
								}
							}
						}
						break;
					}
					case (0): {
						for (Location loc : main.getDataLoader().getDecaySpawned(sp.getPlayer().getUniqueId())) {
							Block block = loc.getBlock();
							if (block.getType() != Material.AIR && block.getType() != Material.BLUE_ICE
									&& block.getType() != Material.ICE && block.getType() != Material.PACKED_ICE
									&& !decayingBlocks.contains(loc)
									&& block.getType() != Material.GREEN_CONCRETE_POWDER) {
								new BlockDecayTask(main, sp, 2, loc);
								decayingBlocks.add(loc);
							}
						}
						main.getDataLoader().removeAllDecaySpawned(sp.getPlayer().getUniqueId());
						cancel();
						break;
					}
					}
					seconds--;

				} else {
					main.getDataLoader().removeAllDecaySpawned(sp.getPlayer().getUniqueId());
					cancel();
				}
			}
		}.runTaskTimer(main, 0, 10);
	}

	private boolean sameBlock(Location a, Location b) {
		if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
			return false;
		}

		return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockZ() == b.getBlockZ();
	}
}
