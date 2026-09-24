package io.paper.permafrost.commands;

import java.awt.Color;
import java.sql.PreparedStatement;
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

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShardShovel;
import io.paper.permafrost.utilities.ShellRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ShovelsCommand implements CommandExecutor {

	private final Main main;

	public ShovelsCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cPlayers only.");
			return true;
		}

		if (command.getName().equalsIgnoreCase("shovels")) {
			openMenu(player);
			return true;
		}
		return true;
	}

	private void openMenu(Player player) {
		UUID uuid = player.getUniqueId();
		Inventory gui = Bukkit.createInventory(null, 54, ChatColor.AQUA + "" + ChatColor.BOLD + "View Your Shovels!");

		List<ItemStack> shovelItems = new ArrayList<>();

		// 🔹 SHARD SHOVELS

		for (ShardShovel shovel : ShardShovel.values()) {

			if (shovel.equals(ShardShovel.POLAR_SHOVEL)) {
				boolean available = main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)
						|| main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM);
				ItemStack display = new ItemStack(Material.DIAMOND_SHOVEL);
				ItemMeta displayMeta = display.getItemMeta();

				if (displayMeta != null) {
					displayMeta.setCustomModelData(shovel.getCustomModelData());
					displayMeta.setDisplayName("§b" + shovel.getDisplayName());

					display.setItemMeta(displayMeta);
				}

				ItemStack item = null;

				if (available) {
					item = display;
				} else {
					item = new ItemStack(Material.GRAY_DYE);
				}

				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§6" + shovel.getDisplayName());

				List<String> lore = new ArrayList<>();
				lore.add((available ? "§a" : "§c") + "Purchasing the POLAR or CUSTOM rank is required.");

				meta.setLore(lore);

				meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

				item.setItemMeta(meta);
				shovelItems.add(item);

			} else {
				boolean available = main.getDataLoader().getShardShovelsAvailable(uuid).getOrDefault(shovel, false);
				ItemStack display = new ItemStack(Material.DIAMOND_SHOVEL);
				ItemMeta displayMeta = display.getItemMeta();

				if (displayMeta != null) {
					displayMeta.setCustomModelData(shovel.getCustomModelData());
					displayMeta.setDisplayName("§6" + shovel.getDisplayName());

					display.setItemMeta(displayMeta);
				}

				ItemStack item = null;

				if (available) {
					item = display;
				} else {
					item = new ItemStack(Material.GRAY_DYE);
				}

				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§6" + shovel.getDisplayName());

				List<String> lore = new ArrayList<>();
				lore.add((available ? "§a" : "§c") + main.getDataLoader().getRequiredShards(shovel)
						+ " shards required.");

				meta.setLore(lore);

				meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

				item.setItemMeta(meta);
				shovelItems.add(item);
			}
		}

		ItemStack item = new ItemStack(Material.DIAMOND_SHOVEL);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("§6Base Diamond Shovel");

		List<String> lore = new ArrayList<>();
		lore.add("§a0 shards required.");

		meta.setLore(lore);

		meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

		item.setItemMeta(meta);
		shovelItems.add(item);
		
		// 🔹 FOUND INTERACTABLE SHOVELS

		addFoundShovelItem(shovelItems, uuid, "spoon", "Spoon", 1017);
		addFoundShovelItem(shovelItems, uuid, "chorizo", "Chorizo", 1005);
		addFoundShovelItem(shovelItems, uuid, "lost_trident", "Lost Trident", 1001);
		addFoundShovelItem(shovelItems, uuid, "ancient_tusk", "Ancient Tusk", 1016);
		addFoundShovelItem(shovelItems, uuid, "colourful_rainbow", "Colourful Rainbow", 1002);
		addFoundShovelItem(shovelItems, uuid, "kings_blade", "King's Blade", 1003);
		addFoundShovelItem(shovelItems, uuid, "sunflower", "Sunflower", 1015);
		addFoundShovelItem(shovelItems, uuid, "mystic", "Mystic", 1013);
		addFoundShovelItem(shovelItems, uuid, "stop_sign", "Stop Sign", 1019);
		addFoundShovelItem(shovelItems, uuid, "natures_staff", "Nature's Staff", 1014);
		addFoundShovelItem(shovelItems, uuid, "valentines", "Valentines", 1018);

		// 🔹 PLACE ITEMS
		int i = 0;
		for (int index = 0; index < 54; index++) {
			if (index > 9 && index != 17 && index != 18 && index != 26 && index != 27 && index != 35 && index != 36
					&& index != 44 && index < 45) {

				if (i >= shovelItems.size())
					break;

				gui.setItem(index, shovelItems.get(i));
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
	
	private void addFoundShovelItem(List<ItemStack> shovelItems, UUID uuid, String column, String displayName,
			int customModelData) {

		boolean found = main.getDataLoader().hasFoundInteractableShovel(uuid, column);

		ItemStack item;

		if (found) {
			item = new ItemStack(Material.DIAMOND_SHOVEL);
			ItemMeta meta = item.getItemMeta();

			if (meta != null) {
				meta.setCustomModelData(customModelData);
				meta.setDisplayName("§6" + displayName);

				List<String> lore = new ArrayList<>();
				lore.add("§aFound!");

				meta.setLore(lore);
				meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

				item.setItemMeta(meta);
			}
		} else {
			item = new ItemStack(Material.GRAY_DYE);
			ItemMeta meta = item.getItemMeta();

			if (meta != null) {
				meta.setDisplayName("§7" + displayName);

				List<String> lore = new ArrayList<>();
				lore.add("§cNot found yet.");

				meta.setLore(lore);
				meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

				item.setItemMeta(meta);
			}
		}

		shovelItems.add(item);
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
		return String.format("§x§%1$X§%2$X§%3$X§%4$X§%5$X§%6$X", (r >> 4) & 0xF, r & 0xF, (g >> 4) & 0xF, g & 0xF,
				(b >> 4) & 0xF, b & 0xF);
	}
}
