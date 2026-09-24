package io.paper.permafrost.core.game.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.utilities.ShovelPowers;

public class SwapCooldownTask {
	private Main main;
	private SpleefPlayer sp;
	private double seconds;
	private ShovelPowers power;

	public SwapCooldownTask(Main main, SpleefPlayer sp) {
		this.main = main;
		this.sp = sp;
		seconds = 2.5;
		sp.setOnSwapCooldown(true);
		startCooldown();
	}
	
	public void startCooldown() {

		new BukkitRunnable() {
		    @Override
		    public void run() {
		        if (seconds <= 0) {
		            sp.setOnSwapCooldown(false);
		            cancel();
		            return;
		        }
		        seconds -= 0.5;
		    }
		}.runTaskTimer(main, 0, 10);
	}
}
