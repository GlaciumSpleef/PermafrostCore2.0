package io.paper.permafrost.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.PowerType;

public class ShovelPowersManager {

	private final Main main;

	public ShovelPowersManager(Main main) {
		this.main = main;
	}

	public void openPowersMenu(Player player) {
		openUnifiedMenu(player,
				"" + ChatColor.BLUE + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your First Power!");
	}

	public void openPower2Menu(Player player) {
		openUnifiedMenu(player,
				"" + ChatColor.BLUE + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your Second Power!");
	}
	
	public void openBansMenu(Player player) {
		openUnifiedMenu(player,
				"" + ChatColor.RED + ChatColor.BOLD + ChatColor.UNDERLINE + "Select Your Teams Ban!");
	}

	private void openUnifiedMenu(Player player, String title) {
		Inventory menu = Bukkit.createInventory(null, 54, title);

		ItemStack border = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
		ItemMeta borderMeta = border.getItemMeta();
		borderMeta.setDisplayName(" ");

		List<String> lore = new ArrayList<>();
		lore.add("§7" + "");

		borderMeta.setLore(lore);

		// Top border
		for (int slot = 0; slot <= 8; slot++) {
			menu.setItem(slot, border);
		}

		// Bottom border
		for (int slot = 45; slot <= 53; slot++) {
			menu.setItem(slot, border);
		}

		// Left and right borders
		int[] sideSlots = {
				9, 17,
				18, 26,
				27, 35,
				36, 44
		};

		for (int slot : sideSlots) {
			menu.setItem(slot, border);
		}

		// Header
		menu.setItem(4, createItem(
				Material.DIAMOND_SHOVEL,
				"§b§lPower Selection",
				"§7Choose your powers for the match"
		));

		// -------- MOBILITY / UTILITY --------

		menu.setItem(10, createItem(
				Material.SUGAR,
				"§b§lSpeed",
				"§7Surge forward with increased movement speed",
				"§eHas a cooldown of §f8 seconds"
		));

		menu.setItem(11, createItem(
				Material.ENDER_PEARL,
				"§d§lEnderPearl",
				"§7Teleport across the arena",
				"§eHas a cooldown of §f7 seconds"
		));

		menu.setItem(12, createItem(
				Material.ELYTRA,
				"§f§lDoubleJump",
				"§7Launch yourself upward",
				"§eHas a cooldown of §f2.5 seconds"
		));

		menu.setItem(13, createItem(
				Material.RESPAWN_ANCHOR,
				"§e§lRecall",
				"§7Return to your saved position",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(14, createItem(
				Material.SNOW_BLOCK,
				"§a§lBuilder",
				"§7Confuse enemies with walls of snow",
				"§eHas a cooldown of §f4 seconds"
		));

		menu.setItem(15, createItem(
				Material.BLUE_ICE,
				"§3§lIceGrenade",
				"§7Create decaying ice on impact",
				"§eHas a cooldown of §f5.5 seconds"
		));

		menu.setItem(16, createItem(
				Material.CLOCK,
				"§6§lRoulette",
				"§7Receive a random power each round",
				"§7You may choose this power twice",
				"§eHas the same cooldown as the power it selects"
		));

		// Separator row
		for (int slot = 19; slot <= 25; slot++) {
			menu.setItem(slot, border);
		}

		// -------- OFFENSIVE / CONTROL --------

		menu.setItem(28, createItem(
				Material.COBWEB,
				"§c§lSnare",
				"§7Trap your enemies",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(29, createItem(
				Material.TNT,
				"§6§lTNTRun",
				"§7Decay snow beneath your enemies",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(30, createItem(
				Material.REDSTONE,
				"§9§lSwap",
				"§7Change locations with your enemy",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(31, createItem(
				Material.BLACK_CONCRETE,
				"§8§lBlackGoo",
				"§7Blind and slow your opponent",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(32, createItem(
				Material.SHULKER_SHELL,
				"§d§lLevitation",
				"§7Disorient your enemy with levitation",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(33, createItem(
				Material.RED_CONCRETE_POWDER,
				"§4§lDecayGrenade",
				"§7Decay snow to make your enemies fall",
				"§eHas a cooldown of §f5.5 seconds"
		));

		menu.setItem(34, createItem(
				Material.WITHER_SKELETON_SKULL,
				"§5§lVortexGrenade",
				"§7Prevent your enemies from jumping",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(37, createItem(
				Material.SCULK_SHRIEKER,
				"§8§lSilencer",
				"§7Temporarily disable your enemy's abilities",
				"§eHas a cooldown of §f8 seconds"
		));

		menu.setItem(38, createItem(
				Material.CAMPFIRE,
				"§7§lSmokeGrenade",
				"§7Hide yourself in smoke or disorient your enemies",
				"§eHas a cooldown of §f10 seconds"
		));

		menu.setItem(39, createItem(
				Material.WIND_CHARGE,
				"§f§lWindcharge",
				"§7Blast yourself into the air or your enemies off the edge",
				"§eHas a cooldown of §f2.5 seconds",
				"§7(4 seconds if two people in your team have windcharge)"
		));

		menu.setItem(40, createItem(
				Material.SHIELD,
				"§b§lPurgingShield",
				"§7Protect yourself from enemy abilities",
				"§eHas a cooldown of §f15 seconds"
		));
		
		menu.setItem(41, createItem(
				Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
				"§b§lJumpPad",
				"§7You and your team can coordinate attacks",
				"§eHas a cooldown of §f15 seconds"
		));

		// Decorative empty spaces
		menu.setItem(42, border);
		menu.setItem(43, border);

		player.openInventory(menu);
	}

	// -----------------------------
	// ITEM CREATOR
	// -----------------------------
	private ItemStack createItem(Material material, String name, String... loreLines) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(name);

		List<String> lore = new ArrayList<>();
		for (String line : loreLines) {
			lore.add("§7" + line);
		}

		lore.add("");
		lore.add("§aClick to select");

		meta.setLore(lore);
		item.setItemMeta(meta);

		return item;
	}

	// -----------------------------
	// FILL EMPTY SLOTS
	// -----------------------------
	private void fillEmpty(Inventory inv) {
		ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = filler.getItemMeta();
		meta.setDisplayName(" ");
		filler.setItemMeta(meta);

		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) == null) {
				inv.setItem(i, filler);
			}
		}
	}
}
