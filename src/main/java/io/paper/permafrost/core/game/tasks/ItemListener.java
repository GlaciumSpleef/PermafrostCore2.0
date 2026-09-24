package io.paper.permafrost.core.game.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;

public class ItemListener implements Listener {
	private final Main main;
	private Timer timer;
	private final ItemStack windCharge = new ItemStack(Material.WIND_CHARGE);
	private final ItemStack windCharge2 = new ItemStack(Material.WIND_CHARGE, 2);
	private final ItemStack windCharge3 = new ItemStack(Material.WIND_CHARGE, 3);
	private final int delay = 2000;

	public ItemListener(Main main) {
		this.main = main;
	}

	class Helper extends TimerTask {
		private final PlayerInteractEvent event;

		public Helper(PlayerInteractEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			Player player = event.getPlayer();
			ItemStack currentItem = player.getInventory().getItem(40);
			if (currentItem != null) {
				if (!player.getInventory().contains(windCharge) || !player.getInventory().contains(windCharge2)
						|| !player.getInventory().contains(windCharge3)) {
					currentItem.setAmount(currentItem.getAmount() + 1);
				} 
			} if (currentItem == null) {
				player.getInventory().setItem(40, windCharge);
			}
		}
	}

	@EventHandler
	public void windChargeRegen(PlayerInteractEvent event) {
		if (event.getItem() != null) {
			if (event.getItem().equals(windCharge2) || event.getItem().equals(windCharge)
					|| event.getItem().equals(windCharge3)) {
				Helper task = new Helper(event);
				timer = new Timer();
				timer.schedule(task, delay);
			}
		}
	}
}