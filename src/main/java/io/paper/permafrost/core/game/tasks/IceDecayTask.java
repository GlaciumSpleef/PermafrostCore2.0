package io.paper.permafrost.core.game.tasks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class IceDecayTask {
	private Main main;
	private SpleefPlayer sp;
	private int seconds;
	private Location loc;

	public IceDecayTask(Main main, SpleefPlayer sp, int seconds, Location loc) {
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
				if (!sp.isRoundStarting()) {
					switch (seconds) {
					case 2: {
						if (loc.getBlock().getType() != Material.AIR) {
							loc.getBlock().setType(Material.PACKED_ICE);
						}
						break;
					}
					case 1: {
						if (loc.getBlock().getType() != Material.AIR) {
							loc.getBlock().setType(Material.ICE);
						}
						break;
					}
					case 0: {
						if (loc.getBlock().getType() != Material.AIR) {
							loc.getBlock().setType(Material.SNOW_BLOCK);
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
		}.runTaskTimer(main, 0, 10);
	}
}
