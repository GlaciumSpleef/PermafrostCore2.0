package io.paper.permafrost.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import github.scarsz.discordsrv.dependencies.alexh.Fluent.Map;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShellRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class RanksCommand implements CommandExecutor {

	private final Main main;

	public RanksCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cPlayers only.");
			return true;
		}

		if (command.getName().equalsIgnoreCase("ranks")) {
			openMenu(player);
			return true;
		}
		return true;
	}

	private void openMenu(Player player) {
		UUID uuid = player.getUniqueId();
		Inventory gui = Bukkit.createInventory(null, 54, ChatColor.AQUA + "" + ChatColor.BOLD + "View Your Ranks!");

		List<ItemStack> rankItems = new ArrayList<>();

		NamespacedKey typeKey = new NamespacedKey(main, "rank_type");

		// 🔹 DEFAULT ITEM
		ItemStack defaultItem = new ItemStack(Material.WHITE_DYE);
		ItemMeta defaultMeta = defaultItem.getItemMeta();

		defaultMeta.setDisplayName("§fUse Server Rank");
		defaultMeta.setLore(List.of("§7Removes your cosmetic rank override."));

		defaultMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "default");

		defaultItem.setItemMeta(defaultMeta);
		rankItems.add(defaultItem);

		// 🔹 SHELL RANKS
		for (ShellRank rank : ShellRank.values()) {

			boolean available = main.getDataLoader().getShellRanksAvailable(uuid).getOrDefault(rank, false);

			ItemStack item = new ItemStack(available ? Material.LIME_DYE : Material.GRAY_DYE);
			ItemMeta meta = item.getItemMeta();

			meta.setDisplayName("§6" + rank.getDisplayName());

			List<String> lore = new ArrayList<>();
			if (!rank.equals(ShellRank.EGGSPERT)) {
				lore.add((available ? "§a" : "§c") + main.getDataLoader().getRequiredShells(rank) + " shells required.");
			} else {
				lore.add((available ? "§a" : "§c") + "Top 1 required.");
			}

			meta.setLore(lore);

			meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

			meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "shell");
			meta.getPersistentDataContainer().set(new NamespacedKey(main, "rank_value"),
					PersistentDataType.STRING, rank.name());

			item.setItemMeta(meta);
			rankItems.add(item);
		}

		// 🔹 PURCHASABLE RANKS (NO CUSTOM HERE)
		for (PurchasableRank rank : PurchasableRank.values()) {

			boolean unlocked = main.getDataLoader().hasPurchasableRank(uuid, rank);

			ItemStack item = new ItemStack(unlocked ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE);
			ItemMeta meta = item.getItemMeta();

			String displayName;
			List<String> lore = new ArrayList<>();

			if (rank == PurchasableRank.GLACIER) {
				displayName = gradient("Glacier", "#B8F4FF", "#1E8FC2");
			} else if (rank == PurchasableRank.POLAR) {
				displayName = gradient("Polar", "#72FAF4", "#03C69B");
			} else {
				continue; // skip anything unexpected
			}

			lore.add(unlocked ? "§aUnlocked." : "§cLocked.");

			meta.setDisplayName(displayName);
			meta.setLore(lore);

			meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

			meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "purchasable");
			meta.getPersistentDataContainer().set(new NamespacedKey(main, "rank_value"),
					PersistentDataType.STRING, rank.name());

			item.setItemMeta(meta);
			rankItems.add(item);
		}

		// 🔹 CUSTOM RANK (NEW SYSTEM)
		String customName = main.getDataLoader().getCustomRankName(uuid);
		boolean hasCustom = main.getDataLoader().hasCustomRank(uuid);

		ItemStack customItem = new ItemStack(hasCustom ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE);
		ItemMeta customMeta = customItem.getItemMeta();

		String displayName;
		List<String> lore = new ArrayList<>();

		if (!hasCustom) {
			displayName = "§7Custom Rank";
			lore.add("§cYou do not have a custom rank.");
			lore.add("§7Use §b/customrank add§7 to set one.");
		} else {
			displayName = buildDisplayFromMini(customName);
			lore.add("§aUnlocked custom rank.");
			lore.add("§7Click to equip.");
		}

		customMeta.setDisplayName(displayName);
		customMeta.setLore(lore);

		customMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
		customMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

		customMeta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "custom");

		customItem.setItemMeta(customMeta);
		rankItems.add(customItem);

		// 🔹 PLACE ITEMS
		int i = 0;
		for (int index = 0; index < 54; index++) {
			if (index > 9 && index != 17 && index != 18 && index != 26 && index != 27 && index != 35
					&& index != 36 && index != 44 && index < 45) {

				if (i >= rankItems.size())
					break;

				gui.setItem(index, rankItems.get(i));
				i++;
			}
		}

		player.openInventory(gui);
	}

	// 🔹 MiniMessage → Legacy (gradient support)
	private String buildDisplayFromMini(String input) {
		try {
			Component comp = MiniMessage.miniMessage().deserialize(input);
			return LegacyComponentSerializer.legacySection().serialize(comp);
		} catch (Exception e) {
			return "§cInvalid Format";
		}
	}

	// 🔹 Gradient helper (used for purchasable ranks)
	public static String gradient(String text, String startHex, String endHex) {
		Color start = Color.decode(startHex);
		Color end = Color.decode(endHex);

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			double ratio = (double) i / (text.length() - 1);

			int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
			int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
			int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

			result.append(toHex(r, g, b)).append(text.charAt(i));
		}

		return result.toString();
	}

	private static String toHex(int r, int g, int b) {
		return String.format("§x§%1$X§%2$X§%3$X§%4$X§%5$X§%6$X",
				(r >> 4) & 0xF, r & 0xF,
				(g >> 4) & 0xF, g & 0xF,
				(b >> 4) & 0xF, b & 0xF);
	}
}