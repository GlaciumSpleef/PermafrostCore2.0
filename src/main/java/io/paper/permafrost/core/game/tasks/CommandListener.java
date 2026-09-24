package io.paper.permafrost.core.game.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class CommandListener implements Listener {
	private final Main main;

	public CommandListener(Main main) {
		this.main = main;
	}

	@EventHandler
	public void onPlayerRightClick(PlayerInteractEvent event) {
		Player player = (Player) event.getPlayer();
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		ItemStack item = event.getItem();
		if (item != null && item.getType() == Material.TIPPED_ARROW) {
			ItemMeta meta = item.getItemMeta();
			if (meta instanceof PotionMeta) {
				NamespacedKey key = new NamespacedKey("permafrostcore", "reset_arrow");
				if (((PotionMeta) meta).getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
					event.setCancelled(true);
					if (sp.hasAcceptedReset()) {
						sp.setAcceptedReset(false);
						player.performCommand("reset confirm");
					} else {
						sp.setAcceptedReset(true);
						player.performCommand("reset confirm");
					}
				}
			}
		}
		item = event.getItem();
		if (item != null && item.getType() == Material.TIPPED_ARROW) {
			ItemMeta meta = item.getItemMeta();
			if (meta instanceof PotionMeta) {
				NamespacedKey key = new NamespacedKey("permafrostcoree", "endgame_arrow");
				if (((PotionMeta) meta).getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
					event.setCancelled(true);
					if (sp.hasAcceptedEndgame()) {
						sp.setAcceptedEndgame(false);
						player.performCommand("endgame confirm");
					} else {
						sp.setAcceptedEndgame(true);
						player.performCommand("endgame confirm");
					}
				}
			}
		}
		item = event.getItem();
		if (item != null && item.getType() == Material.TIPPED_ARROW) {
			ItemMeta meta = item.getItemMeta();
			if (meta instanceof PotionMeta) {
				NamespacedKey key = new NamespacedKey("permafrostcoreee", "playto_arrow");
				if (((PotionMeta) meta).getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
					event.setCancelled(true);
					if (sp.hasAcceptedPlayto()) {
						sp.setAcceptedPlayto(false);
						player.performCommand("playto confirm");
					} else {
						sp.setAcceptedPlayto(true);
						player.performCommand("playto confirm");
					}
				}
			}
		}
	}
}
