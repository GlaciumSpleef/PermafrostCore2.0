package io.paper.permafrost.commands;

import java.util.HashSet;



import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.tasks.CountdownTask;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ResetCommand implements CommandExecutor {
	private Main main;
	private final Set<Player> confirmSet = new HashSet<>();
	private final Set<Player> allPlayers = new HashSet<>();
	private Player p1sender;
	private Player p2confirmer;

	public ResetCommand(Main main) {
		this.main = main;
		p1sender = null;
		p2confirmer = null;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		if (command.getName().equalsIgnoreCase("reset")) {
			Player player = (Player) sender;
			SpleefPlayer spleefPlayer = main.getDataLoader().getPlayer(player.getUniqueId());

			if (spleefPlayer == null) {
				player.sendMessage("You are not a registered Spleef Player.");
				return true;
			}
			SpleefGame game = spleefPlayer.getGame();
			if (game == null) {
				player.sendMessage("You are not currently in game.");
				return true;
			}
			if (spleefPlayer.isDead()) {
				player.sendMessage("§cYou cannot send reset requests when you are dead.");
				return true;
			}
			if (args.length == 0) {
				confirmSet.add(player);
				spleefPlayer.setAcceptedReset(true);
				for (SpleefPlayer sp : game.getPlayers()) {
					if (!sp.getPlayer().equals(player)) {
						TextComponent message = new TextComponent("§b§lA reset request has been sent to you by "
								+ player.getName() + "§l.\n§7Right click the item in your hotbar to accept!");
						sp.getPlayer().spigot().sendMessage(message);
						ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 1);
						PotionMeta meta = (PotionMeta) arrow.getItemMeta();
						if (meta != null) {
							meta.setBasePotionData(new PotionData(PotionType.HEALING, false, true));
							NamespacedKey key = new NamespacedKey("permafrostcore", "reset_arrow");
							meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
							arrow.setItemMeta(meta);
						}
						int heldSlot = player.getInventory().getHeldItemSlot();
						sp.getPlayer().getInventory().setItem(8, arrow);
						player.getInventory().setHeldItemSlot(heldSlot);
					} else {
						sp.getPlayer().sendMessage("§b§lAwaiting confirmation...");
						ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 1);
						PotionMeta meta = (PotionMeta) arrow.getItemMeta();
						if (meta != null) {
							meta.setBasePotionData(new PotionData(PotionType.HEALING, false, true));
							NamespacedKey key = new NamespacedKey("permafrostcore", "reset_arrow");
							meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
							arrow.setItemMeta(meta);
						}
						int heldSlot = player.getInventory().getHeldItemSlot();
						sp.getPlayer().getInventory().setItem(8, arrow);
						player.getInventory().setHeldItemSlot(heldSlot);
					}
				}
			}
			if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
				SpleefPlayer sp = main.getDataLoader().getPlayer(player.getUniqueId());
				for (SpleefPlayer spp : sp.getGame().getPlayers()) {
					if (spp.hasAcceptedReset() && !spp.isDead())
						confirmSet.add(spp.getPlayer());
					if (!spp.isDead())
						allPlayers.add(spp.getPlayer());
					if(!spp.hasAcceptedReset()) {
						confirmSet.remove(spp.getPlayer());
					}
				}
				if (confirmSet.equals(allPlayers)) {
					new CountdownTask(main, sp.getGame());
					for (Player p : allPlayers) {
						p.getInventory().setItem(8, new ItemStack(Material.AIR));
						SpleefPlayer spr = main.getDataLoader().getSpleefPlayer(p);
						spr.setResetStarting(true);
						spr.setAcceptedReset(false);
					}
					allPlayers.clear();
					confirmSet.clear();
					Location corner1 = sp.getGame().getArena().getCorner1();
					Location corner2 = sp.getGame().getArena().getCorner2();
					main.getFAWEAPI().createFlatSnowPlane(corner1, corner2, Material.SNOW_BLOCK);
					return true;
				} else {
					String msg = "§b§lRemaining players: ";
					for (SpleefPlayer spp : sp.getGame().getPlayers()) {
						if (!spp.isDead()) {
							if (!spp.hasAcceptedReset())
								msg += "§r§7" + spp.getPlayer().getName() + ", ";
						}
					}
					msg = msg.substring(0, msg.length() - 2);
					for (SpleefPlayer spp : sp.getGame().getPlayers()) {
						Player p = spp.getPlayer();
						p.sendMessage(msg);
					}
				}
				return true;
			}

		}
		return true;
	}
}
