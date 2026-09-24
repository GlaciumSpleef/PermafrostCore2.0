package io.paper.permafrost.gui;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public abstract class MenuBuilder implements Listener {
	Main plugin;

	public MenuBuilder(Main plugin) {
		this.plugin = plugin;
	}

	Inventory _inv;
	HashMap<Integer, ItemStack> inventory = new HashMap<Integer, ItemStack>();

	@SuppressWarnings("deprecation")
	public MenuBuilder(Main plugin, String name, int rows) {
		this.plugin = plugin;
		_inv = Bukkit.createInventory(null, 9 * rows, ChatColor.translateAlternateColorCodes('&', name));

		plugin.getServer().getPluginManager().registerEvents(this, plugin);

	}

	public void a(ItemStack stack) {
		_inv.addItem(stack);
	}

	public void s(int i, ItemStack stack) {
		inventory.put(i, stack);
	}

	public void buildInventory() {
		_inv.clear();
		for (Entry<Integer, ItemStack> entry : inventory.entrySet()) {
			int i = entry.getKey();
			ItemStack stack = entry.getValue();
			_inv.setItem(i, stack);
		}

	}

	public Inventory i() {
		return _inv;
	}

	public void o(Player p) {
		new BukkitRunnable() {
			public void run() {
				p.playSound(p.getLocation(), "custom:abrir_inventario", 0.7F, 0.7F);
				p.openInventory(_inv);
			}
		}.runTask(plugin);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().equals(this.i())) {
			if (event.getCurrentItem() != null && this.i().contains(event.getCurrentItem())
					&& event.getWhoClicked() instanceof Player) {
				Player p = (Player) event.getWhoClicked();
				SpleefPlayer sp = plugin.getDataLoader().getSpleefPlayer(p);
				this.onClick(sp, event);
				p.playSound(p.getLocation(), "custom:interfaz_click", 1.0F, 1.3F);
			}
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getInventory().equals(this.i()) && event.getPlayer() instanceof Player) {
			this.onClose((Player) event.getPlayer());
		}
	}

	public void onClose(Player player) {
	}

	public abstract void onClick(SpleefPlayer sp, InventoryClickEvent e);
}