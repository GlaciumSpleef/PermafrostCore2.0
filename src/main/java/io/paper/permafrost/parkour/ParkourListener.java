package io.paper.permafrost.parkour;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefFFAGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;

public class ParkourListener implements Listener {
	private Main main;

	public ParkourListener(Main main) {
		this.main = main;
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (main.getDataLoader().getSpleefPlayer(player) == null) {
			return;
		}
		if (!main.getDataLoader().getSpleefPlayer(player).getInParkourGame()) {
			return;
		}

		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
		ParkourGame game = sp.getParkourGame();
		ParkourMap map = game.getMap();

		if (game.isInCountdownPhase()) {
			Location from = event.getFrom();
			Location to = event.getTo();

			if (to == null) {
				return;
			}

			boolean changedPosition =
					from.getX() != to.getX()
					|| from.getY() != to.getY()
					|| from.getZ() != to.getZ();

			if (!changedPosition) {
				return;
			}

			Location lockedLocation = from.clone();
			lockedLocation.setYaw(to.getYaw());
			lockedLocation.setPitch(to.getPitch());

			event.setTo(lockedLocation);
		}

		if (game.isInMatchPhase()) {
			if (player.getLocation().getY() <= map.getFallLevel()) {
				if (game.isPlayerOne(uuid)) {
					player.teleport(map.getSpawn1());
				} else {
					player.teleport(map.getSpawn2());
				}
				return;
			}
			if (game.isPlayerOne(uuid)) {
				if (isWithinOneBlockRadius(player, map.getFinish1())) {
					game.endMatch(uuid);
				}
			} else {
				if (isWithinOneBlockRadius(player, map.getFinish2())) {
					game.endMatch(uuid);
				}
			}
		}
	}

	@EventHandler
	public void onFallDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
			event.setCancelled(true);
		}
	}

	private boolean isWithinOneBlockRadius(Player player, Location centerBlockLocation) {
		if (player == null || centerBlockLocation == null) {
			return false;
		}

		Location playerLoc = player.getLocation();

		if (!playerLoc.getWorld().equals(centerBlockLocation.getWorld())) {
			return false;
		}

		double dx = playerLoc.getX() - (centerBlockLocation.getBlockX() + 0.5);
		double dz = playerLoc.getZ() - (centerBlockLocation.getBlockZ() + 0.5);

		return (dx * dx + dz * dz) <= 1.0;
	}
}
