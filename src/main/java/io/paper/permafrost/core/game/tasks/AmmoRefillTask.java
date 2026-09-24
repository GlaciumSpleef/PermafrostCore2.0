package io.paper.permafrost.core.game.tasks;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class AmmoRefillTask {
	private Main main;
	private SpleefPlayer sp;
	private double seconds;
	private int task;
	private ShovelPowers power;

	public AmmoRefillTask(Main main, SpleefPlayer sp, double seconds, ShovelPowers power) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		this.power = power;
		startRefill();
	}

	public void startRefill() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (sp.isRoundStarting()) {
					seconds = 0;
					cancel();
					return;
				}
				
				if ((sp.getGame() == null) && !(main.getSpleefQueueManager().isAliveInAnyFFA(sp.getPlayer().getUniqueId()) && main
						.getSpleefQueueManager().getFFAMode(sp.getPlayer().getUniqueId()).equals(SpleefFFAMode.POWERSPLEEF))) {
					seconds = 0;
					cancel();
					return;
				}

				if (seconds <= 0) {
					main.getAmmoManger().refill(power, sp);
					main.getAmmoManger().showAmmo(sp.getPlayer(), power,
							main.getAmmoManger().getAmmo(sp, power));
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 1f);
					cancel();
					return;
				}
				seconds -= 0.5;
			}
		}.runTaskTimer(main, 0, 10);
	}
}
