package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleefSpectator;
import io.paper.permafrost.utilities.SpleefMode;

public class FoundShovelCommand implements CommandExecutor {
	private Main main;

	public FoundShovelCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}

		Player player = (Player) sender;

		if (!command.getName().equalsIgnoreCase("foundshovel")) {
			return true;
		}

		if (args.length < 2) {
			return true;
		}

		String shovel = args[0].toLowerCase();
		String pass = args[1];

		if (!pass.equals("7401237203")) {
			return true;
		}

		if (!isValidShovel(shovel)) {
			player.sendMessage("§cInvalid shovel.");
			return true;
		}

		UUID uuid = player.getUniqueId();

		main.getDataLoader().createInteractableShovelRowIfMissing(uuid);

		if (main.getDataLoader().hasFoundInteractableShovel(uuid, shovel)) {
			player.sendMessage("§eYou already found this shovel.");
			return true;
		}

		main.getDataLoader().setFoundInteractableShovel(uuid, shovel, true);

		player.sendMessage("§aYou found the §e" + formatShovelName(shovel) + "§a shovel!");
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

		return true;
	}

	private boolean isValidShovel(String shovel) {
		return shovel.equals("spoon") || shovel.equals("chorizo") || shovel.equals("lost_trident")
				|| shovel.equals("ancient_tusk") || shovel.equals("colourful_rainbow") || shovel.equals("kings_blade")
				|| shovel.equals("sunflower") || shovel.equals("mystic") || shovel.equals("stop_sign")
				|| shovel.equals("natures_staff") || shovel.equals("valentines");
	}

	private String formatShovelName(String shovel) {
		switch (shovel) {
		case "spoon":
			return "Spoon";
		case "chorizo":
			return "Chorizo";
		case "lost_trident":
			return "Lost Trident";
		case "ancient_tusk":
			return "Ancient Tusk";
		case "colourful_rainbow":
			return "Colourful Rainbow";
		case "kings_blade":
			return "King's Blade";
		case "sunflower":
			return "Sunflower";
		case "mystic":
			return "Mystic";
		case "stop_sign":
			return "Stop Sign";
		case "natures_staff":
			return "Nature's Staff";
		case "valentines":
			return "Valentines";
		default:
			return shovel;
		}
	}
}
