package io.paper.permafrost.training;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import net.md_5.bungee.api.ChatColor;

public class TrainingMenu {
	private Main main;
	private Player player;
	private int playersInSession;

	public TrainingMenu(Main main, Player player, int playersInSession) {
		this.main = main;
		this.player = player;
		this.playersInSession = playersInSession;
		openMenu();
	}

	@SuppressWarnings("deprecation")
	public void openMenu() {
		Inventory inventory = Bukkit.createInventory(player, 9 * 6, ChatColor.AQUA + "Presets Available");
		switch (playersInSession) {
		case 1: {
			ItemStack[] items = new ItemStack[2];
			items[0] = new ItemStack(Material.NETHER_STAR);
			ItemMeta meta = items[0].getItemMeta();

			meta.setDisplayName("§d§lShape Description");

			List<String> lore = List.of(
			        ChatColor.RED + "Break " + ChatColor.GRAY + "the circle of blocks",
			        ChatColor.GRAY + "around you and try to " + ChatColor.GREEN + "beat",
			        ChatColor.GRAY + "your past records!",
			        "",
			        ChatColor.DARK_RED + "■ ■ ■ ■ ■ ■ ■ ■ ■",
			        ChatColor.RED      + "■ " + ChatColor.GREEN + "TRAINING ARENA" + ChatColor.RED + " ■",
			        ChatColor.DARK_RED + "■ ■ ■ ■ ■ ■ ■ ■ ■",
			        "",
			        ChatColor.YELLOW + "⏱ Try to beat your fastest time",
			        ChatColor.GRAY + "Precision • Speed • Control"
			);

			meta.setLore(lore);

			//meta.setLore(Arrays.asList("§7This plot is formed as a §d§lround platform§7",
			//		"§7with a §dcircular snow ring§7.", "§7You will §f§lspawn on the center block§7.",
			//		"§7Your goal is to §d§lbreak the ring§7", "§7around you as §d§lfast as possible§7!"));

			items[0].setItemMeta(meta);
			items[1] = new ItemStack(Material.WITHER_SKELETON_SKULL);
			ItemMeta meta2 = items[1].getItemMeta();
			meta2.setDisplayName("§d§lShape Description");

			meta2.setLore(lore);
			
			int index = 0;
			for (ItemStack item : items) {
				inventory.setItem(index, items[index]);
				index++;
			}
			player.openInventory(inventory);
			break;

		}
		case 2: {
			break;
		}
		case 3: {

			break;
		}
		case 4: {

			break;
		}
		}
	}
	
	public class TrainingStatisticsMenu {

		public static void open(Player player, Main main) {

		    Inventory inv = Bukkit.createInventory(null, 27, "Training Statistics");

		    UUID uuid = player.getUniqueId();
		    String plot = "reachomnisolo1";

		    long bestTimeMs = main.getDataLoader().getBestTimeMs(uuid, plot);
		    Timestamp achievedAt = main.getDataLoader().getBestTimeAchievedAt(uuid, plot);

		    long[] stats = main.getDataLoader()
		            .getTotalAndAverageSessionTime(uuid, plot);

		    long totalTimeMs = stats[0];
		    long avgTimeMs = stats[1];

		    ItemStack item = new ItemStack(Material.CLOCK);
		    ItemMeta meta = item.getItemMeta();
		    meta.setDisplayName("§bReach Omni Solo");

		    List<String> lore = new ArrayList<>();

		    // Best time
		    if (bestTimeMs > 0 && achievedAt != null) {
		        lore.add("§7Best Time: §a" + formatMs(bestTimeMs));
		        lore.add("§7Achieved: §f" + achievedAt.toLocalDateTime().toLocalDate());
		    } else {
		        lore.add("§7Best Time: §cNo record");
		    }

		    lore.add("");

		    // Total + average OUTPUT (this is what you were missing)
		    if (totalTimeMs > 0) {
		        lore.add("§7Total Time Played: §e" + formatMs(totalTimeMs));
		        lore.add("§7Average Session: §f" + formatMs(avgTimeMs));
		    } else {
		        lore.add("§7Total Time Played: §c0.000s");
		    }

		    meta.setLore(lore);
		    item.setItemMeta(meta);

		    inv.setItem(13, item);
		    player.openInventory(inv);
		}
	    private static String formatMs(long ms) {
	        return String.format("%.3fs", ms / 1000.0);
	    }
	}
}
