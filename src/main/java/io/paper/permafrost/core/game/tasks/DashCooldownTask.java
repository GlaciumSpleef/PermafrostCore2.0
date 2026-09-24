package io.paper.permafrost.core.game.tasks;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class DashCooldownTask {
	private Main main;
	private SpleefPlayer sp;
	private double seconds;
	private int task;
	private ShovelPowers power;
	private boolean roulette;
	private final double maxSeconds;

	public DashCooldownTask(Main main, SpleefPlayer sp, double seconds) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		this.maxSeconds = seconds;
		sp.setDashCooldown(true);
		startCooldown();
	}

	public void startCooldown() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (sp.isRoundStarting()) {
					seconds = 0;
					sp.setDashTimeLeft(0);
					sp.setDashCooldown(false);
					cancel();
					return;
				}
				if (sp.getGame() == null && !(main.getSpleefQueueManager().isAliveInAnyFFA(sp.getUUID())
						&& main.getSpleefQueueManager().getFFAMode(sp.getUUID()).equals(SpleefFFAMode.POWERSPLEEF))) {
					seconds = 0;
					sp.setDashTimeLeft(0);
					sp.setDashCooldown(false);
					cancel();
					return;
				}
				sp.setDashTimeLeft((int) Math.ceil(seconds));
				if (seconds <= 0) {
					sp.getPlayer().sendMessage("§aYour ability is ready!");
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 1f);
					sp.setDashTimeLeft(0);
					sp.setDashCooldown(false);
					cancel();
					return;
				}
				seconds -= 0.5;
			}
		}.runTaskTimer(main, 0, 10);
	}
}
