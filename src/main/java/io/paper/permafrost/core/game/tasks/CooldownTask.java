package io.paper.permafrost.core.game.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.GameState;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;

public class CooldownTask {
	private Main main;
	private SpleefPlayer sp;
	private double seconds;
	private int task;
	private ShovelPowers power;
	private boolean roulette;
	private boolean roulette2;
	private final double maxSeconds;

	public CooldownTask(Main main, SpleefPlayer sp, double seconds, ShovelPowers power, boolean roulette, boolean roulette2) {
		this.main = main;
		this.sp = sp;
		this.seconds = seconds;
		this.maxSeconds = seconds;
		this.power = power;

		if (sp.hasCooldown(power))
			return;

		this.roulette = roulette;
		this.roulette2 = roulette2;
		
		
		if (!roulette && !roulette2) {
			if (sp.getCooldown1() == null) {
				sp.setCooldown1(power);
			} else if (sp.getCooldown2() == null) {
				sp.setCooldown2(power);
			} else {
				return;
			}
		} else if (roulette){
			if (sp.getCooldown1() == null) {
				sp.setCooldown1(ShovelPowers.ROULETTE);
			} else if (sp.getCooldown2() == null) {
				sp.setCooldown2(ShovelPowers.ROULETTE);
			} else {
				return;
			}
		} else if (roulette2) {
			if (sp.getCooldown1() == null) {
				sp.setCooldown1(ShovelPowers.ROULETTE2);
			} else if (sp.getCooldown2() == null) {
				sp.setCooldown2(ShovelPowers.ROULETTE2);
			} else {
				return;
			}
		}
		startCooldown();
	}

	public void startCooldown() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (sp.isRoundStarting()) {
					seconds = 0;
					sp.setTimeLeft(0);
					sp.setCooldown1(null);
					sp.setCooldown2(null);
					Attribute reachAttr = resolveBlockReachAttr();
					AttributeInstance inst = sp.getPlayer().getAttribute(reachAttr);
					if (inst != null) {
						// final double before = inst.getBaseValue();
						inst.setBaseValue(5.5);

						new BukkitRunnable() {
							@Override
							public void run() {
								inst.setBaseValue(4.5);
							}
						}.runTaskLater(main, 100L);

					}
					cancel();
					return;
				}
				if (sp.getGame() == null && !(main.getSpleefQueueManager().isAliveInAnyFFA(sp.getUUID())
						&& main.getSpleefQueueManager().getFFAMode(sp.getUUID()).equals(SpleefFFAMode.POWERSPLEEF))) {
					seconds = 0;
					sp.setTimeLeft(0);
					sp.setCooldown1(null);
					sp.setCooldown2(null);
					Attribute reachAttr = resolveBlockReachAttr();
					AttributeInstance inst = sp.getPlayer().getAttribute(reachAttr);
					if (inst != null) {
						// final double before = inst.getBaseValue();
						inst.setBaseValue(5.5);

						new BukkitRunnable() {
							@Override
							public void run() {
								inst.setBaseValue(4.5);
							}
						}.runTaskLater(main, 100L);

					}
					cancel();
					return;
				}
				float progress = (float) Math.max(0.0, Math.min(1.0, seconds / maxSeconds));
				if (!roulette && !roulette2) {
					if (sp.getSelectedPower() == power) {
						sp.getPlayer().setLevel((int) Math.ceil(seconds));
						sp.getPlayer().setExp(progress);
					}
				} else if (roulette){
					if (sp.getSelectedPower() == ShovelPowers.ROULETTE) {
						sp.getPlayer().setLevel((int) Math.ceil(seconds));
						sp.getPlayer().setExp(progress);
					}
				} else if (roulette2) {
					if (sp.getSelectedPower() == ShovelPowers.ROULETTE2) {
						sp.getPlayer().setLevel((int) Math.ceil(seconds));
						sp.getPlayer().setExp(progress);
					}
				}
				sp.setTimeLeft((int) Math.ceil(seconds));

				if (seconds <= 0) {
					if (!roulette && !roulette2) {
						if (power.equals(sp.getCooldown1()))
							sp.setCooldown1(null);
						else if (power.equals(sp.getCooldown2()))
							sp.setCooldown2(null);
					} else if (roulette){
						if (ShovelPowers.ROULETTE.equals(sp.getCooldown1()))
							sp.setCooldown1(null);
						else if (ShovelPowers.ROULETTE.equals(sp.getCooldown2()))
							sp.setCooldown2(null);
					} else if (roulette2) {
						if (ShovelPowers.ROULETTE2.equals(sp.getCooldown1()))
							sp.setCooldown1(null);
						else if (ShovelPowers.ROULETTE2.equals(sp.getCooldown2()))
							sp.setCooldown2(null);
					}
					sp.getPlayer().sendMessage("§aYour ability is ready!");
					sp.getPlayer().playSound(sp.getPlayer().getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 1f);
					cancel();
					return;
				}
				seconds -= 0.5;
			}
		}.runTaskTimer(main, 0, 10);
	}

	private Attribute resolveBlockReachAttr() {
		try {
			return Attribute.valueOf("BLOCK_INTERACTION_RANGE");
		} catch (IllegalArgumentException ignored) {
			/* old name */ }
		return Attribute.valueOf("PLAYER_BLOCK_INTERACTION_RANGE");
	}
}
