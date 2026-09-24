package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.tasks.TeamSpleefListener;
import io.paper.permafrost.gui.TeamSpleefMenu;
import io.paper.permafrost.utilities.SpleefMode;
import net.md_5.bungee.api.ChatColor;

public class TeamsCommand implements CommandExecutor {
	private final Main main;

	public TeamsCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cThis command can only be run by a player.");
			return true;
		}

		if (Bukkit.getOnlinePlayers().size() < 2) {
			sender.sendMessage("§cThere are not enough people online to use this command.");
			return true;
		}

		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}
		if (main.getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}

		if (main.getRankedSpleggManager().getPlayersInQueue().contains(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ranked queue.");
			return true;
		}

		if (main.getSpleggQueueManager().getPlayers().containsKey(player.getUniqueId())) {
			player.sendMessage("§cThis command cannot be run in ffa queue.");
			return true;
		}

		if (main.getDataLoader().getSpleefPlayer(player).getInGame()
				|| main.getDataLoader().getSpleefPlayer(player).getInTeamGame()) {
			sender.sendMessage("§cYou cannot use this command in game.");
			return true;
		}
		
		if(!main.getQueuesOpen() && !player.hasPermission("permafrostcore.fstartgame")) {
			player.sendMessage("§cQueues are closed!");
			return true;
		}

		main.getDataLoader().setTeamCount(player.getUniqueId(), 1);

		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
		List<Player> toRemove = new ArrayList<>();
		for (Player p : onlinePlayers) {
			SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
			if (spleefPlayer.getGame() != null) {
				toRemove.add(p);
			}
		}
		onlinePlayers.removeAll(toRemove);
		main.getDataLoader().setOnlinePlayers(player.getUniqueId(), onlinePlayers);

		openGamemodeMenu(player);
		return true;
	}

	private void openGamemodeMenu(Player player) {
		Inventory gui = Bukkit.createInventory(null, 27, ChatColor.AQUA + "" + ChatColor.BOLD + "Select Team Duel Mode");

		gui.setItem(11, createItem(Material.EGG, "§e§lSplegg", Arrays.asList(
				"§7Click to use",
				"§eSplegg"
		)));

		gui.setItem(12, createItem(Material.DIAMOND_SHOVEL, "§b§lPowerspleef", Arrays.asList(
				"§7Click to use",
				"§bPowerspleef"
		)));

		gui.setItem(14, createItem(Material.FEATHER, "§f§lAirspleef", Arrays.asList(
				"§7Click to use",
				"§fAirspleef"
		)));

		gui.setItem(15, createItem(Material.RED_CONCRETE, "§c§lMinispleef", Arrays.asList(
				"§7Click to use",
				"§cMinispleef"
		)));

		player.openInventory(gui);
	}

	private ItemStack createItem(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		return item;
	}
}

