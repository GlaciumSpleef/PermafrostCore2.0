package io.paper.permafrost.gui;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefDuel;
import io.paper.permafrost.core.game.SpleefPlayer;

public class AcceptDuelMenu extends MenuBuilder {

	Integer accept_slots[] = { 18, 19, 20, 27, 28, 29, 36, 37, 38 };
	Integer deny_slots[] = { 24, 25, 26, 33, 34, 35, 42, 43, 44 };
	private Main main;

	public AcceptDuelMenu(Main main, SpleefPlayer sp2, SpleefDuel duel) {
		super(main, "§b§lSpleef Duel Request Recieved!", 5);

		this.main = main;

		new BukkitRunnable() {
			public void run() {

				s(13, new ItemBuilder(Material.DIAMOND_SHOVEL).setTitle("§aDuel Request Received.")
						.addLore("§b" + duel.getChallenger().getOfflinePlayer().getName()
								+ " §7has invited you to a duel.")
						.addLore("§b§l" + getVersus(duel.getAllPlayers())).build());

				for (int i : accept_slots) {
					s(i, new ItemBuilder(Material.SNOW_BLOCK).setTitle("§a§l[ACCEPT DUEL]")
							.addLore("§8" + duel.getChallenger().getUUID()).build());
				}
				s(28, new ItemBuilder(Material.LIME_CONCRETE).setTitle("§a§l[ACCEPT DUEL]")
						.addLore("§8" + duel.getChallenger().getUUID()).build());
				for (int i : deny_slots) {
					s(i, new ItemBuilder(Material.SNOW_BLOCK, 1, (byte) 14).setTitle("§c§l[DENY DUEL]")
							.addLore("§8" + duel.getChallenger().getUUID()).build());
				}
				s(34, new ItemBuilder(Material.RED_CONCRETE, 1, (byte) 14).setTitle("§c§l[DENY DUEL]")
						.addLore("§8" + duel.getChallenger().getUUID()).build());
				new BukkitRunnable() {
					public void run() {
						buildInventory();
					}
				}.runTask(main);

			}
		}.runTaskAsynchronously(main);
	}

	public String getVersus(List<SpleefPlayer> players) {

		List<SpleefPlayer> list1 = new ArrayList<SpleefPlayer>();
		List<SpleefPlayer> list2 = new ArrayList<SpleefPlayer>();
		int x = 1;

		for (SpleefPlayer sp : players) {
			if (x <= players.size() / 2) {
				list1.add(sp);
			} else {
				list2.add(sp);
			}
			x++;
		}

		String p1 = "";
		for (SpleefPlayer sp : list1) {
			if (p1.equalsIgnoreCase("")) {
				p1 = sp.getName();
			} else {
				p1 = p1 + "-" + sp.getName();
			}
		}

		String p2 = "";
		for (SpleefPlayer sp : list2) {
			if (p2.equalsIgnoreCase("")) {
				p2 = sp.getName();
			} else {
				p2 = p2 + "-" + sp.getName();
			}
		}

		return p1 + " vs " + p2;
	}

	@Override
	public void onClick(SpleefPlayer sp, InventoryClickEvent e) {
		ItemStack stack = e.getCurrentItem();
		int slot = e.getSlot();
		List<Integer> a = Arrays.asList(accept_slots);
		List<Integer> b = Arrays.asList(deny_slots);

		if (!a.contains(slot) && !b.contains(slot))
			return;

		if (stack.getItemMeta().getLore() == null)
			return;

		OfflinePlayer player = Bukkit
				.getOfflinePlayer(UUID.fromString(ChatColor.stripColor(stack.getItemMeta().getLore().get(0))));
		if (!player.isOnline()) {
			sp.sendMessage("§cDuel has expired.");
			sp.getPlayer().closeInventory();
			return;
		}

		SpleefPlayer challenger = main.getDataLoader().getSpleefPlayer(player);

		SpleefDuel duel = challenger.getDuelByDueledPlayer(sp);

		if (duel == null) {
			Bukkit.getLogger().info("duel is null");
			sp.sendMessage("§cDuel has expired.");
			sp.getPlayer().closeInventory();
			return;
		}

		if (a.contains(slot)) {
			duel.acceptDuel(sp);
			sp.getPlayer().closeInventory();
		} else if (b.contains(slot)) {
			sp.getPlayer()
					.sendMessage("§cYou denied a duel request from §b" + duel.getChallenger().getName() + "§c.");
			for (SpleefPlayer dueled : duel.getAllPlayers()) {
				if (dueled != sp)
					dueled.getPlayer().sendMessage(
							"§cThe player §b" + sp.getName() + "§c has rejected your duel request.");
			}
			challenger.getDuels().remove(duel);
			sp.getPlayer().closeInventory();
		}

	}

}
