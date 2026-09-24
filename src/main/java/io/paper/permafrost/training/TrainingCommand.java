package io.paper.permafrost.training;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefSpectator;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("unused")
public class TrainingCommand implements CommandExecutor {
	private Main main;

	public TrainingCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		} else {
			Player player = (Player) sender;
			SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
			if (sp.getInGame() || sp.getInTeamGame()) {
				if (!main.getDataLoader().getSpectatorsForGame(sp.getGame()).contains(sp.getSpectator())) {
					sender.sendMessage("This command cannot be run in game.");
					return true;
				}
			}
			if (command.getName().equalsIgnoreCase("training") && args.length == 0) {

				Location loc = new Location(Bukkit.getWorld("training"), 991.5, 81, 1182.5);
				loc.setYaw(0f);
				player.teleport(loc);
				player.getInventory().clear();
				return true;
			} else if (command.getName().equalsIgnoreCase("training") && args.length > 0) {
				if (args[0].equals("presets")) {
					if (main.getDataLoader().getTrainingSession(player.getUniqueId()) != null) {
						TrainingMenu menu;
						switch (main.getDataLoader().getTrainingSession(player.getUniqueId()).getPlayers().size()) {
						case 1: menu = new TrainingMenu(main, player, 1);
						case 2: menu = new TrainingMenu(main, player, 2);
						case 3: menu = new TrainingMenu(main, player, 3);
						case 4: menu = new TrainingMenu(main, player, 4);
						}
					}
				} else {
					TrainingType type;
					try {
						type = TrainingType.valueOf(args[0].toUpperCase());
					} catch (IllegalArgumentException ex) {
						sender.sendMessage("Invalid training type.");
						return true;
					}
					TrainingSession session = new TrainingSession(player.getUniqueId(), type, main);
					main.getDataLoader().addPlayerToTraining(player.getUniqueId(), session);
					return true;
				}
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void openMenu(Player player) {
		Inventory inventory = Bukkit.createInventory(player, 9 * 3, ChatColor.AQUA + "Select Training Type");
		ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "REACH TRAINING");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "Start an instance", ChatColor.GRAY + "of reach training"));
		item.setItemMeta(meta);
		inventory.setItem(13, item);
		// LISTENER IS INSIDE OF POWERSLISTENER.JAVA
	}
}
