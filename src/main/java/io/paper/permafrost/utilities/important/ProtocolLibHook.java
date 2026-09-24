package io.paper.permafrost.utilities.important;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.DigSnapshot;

public final class ProtocolLibHook {

	private static final Map<UUID, DigSnapshot> lastStartDigByPlayer = new HashMap<>();

	public static void register(Main main) {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();

		manager.addPacketListener(new PacketAdapter(main, ListenerPriority.MONITOR, PacketType.Play.Client.BLOCK_DIG) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				try {
					Player player = e.getPlayer();

					if (player == null || !player.isOnline()) {
						return;
					}

					EnumWrappers.PlayerDigType type = e.getPacket().getPlayerDigTypes().read(0);

					BlockPosition pos = e.getPacket().getBlockPositionModifier().read(0);

					if (!player.getWorld().getName().equalsIgnoreCase("voidworld") || !player.getWorld().getName().equalsIgnoreCase("ffa")) {
						return;
					}

					UUID uuid = player.getUniqueId();

					if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
						lastStartDigByPlayer.put(uuid,
								new DigSnapshot(pos.getX(), pos.getY(), pos.getZ(), System.currentTimeMillis()));
						return;
					}

					if (type == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
						Bukkit.getConsoleSender().sendMessage("LOG: STOP_DIG");
						Block block = player.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
						if (block.getType() == Material.SNOW_BLOCK)
							handleStopDig(main, player, pos);
					}

				} catch (Exception ex) {
					Bukkit.getLogger().warning("Error en DebugMiningListener BLOCK_DIG: " + ex.getMessage());
				}
			}
		});
	}

	private static void handleStopDig(Main main, Player player, BlockPosition pos) {
		UUID uuid = player.getUniqueId();

		DigSnapshot lastStart = lastStartDigByPlayer.remove(uuid);

		boolean hadMatchingStart = false;

		if (lastStart != null) {
			boolean sameBlock = lastStart.getX() == pos.getX() && lastStart.getY() == pos.getY()
					&& lastStart.getZ() == pos.getZ();

			long age = System.currentTimeMillis() - lastStart.getMillis();

			hadMatchingStart = sameBlock && age <= 1500;
		}

		/*
		 * Important: Even if hadMatchingStart is true, we still schedule the break.
		 * Why? Because for these "unbreakable" client/server edge cases, the normal
		 * Bukkit break may never happen.
		 */

		Bukkit.getScheduler().runTask(main, () -> {
			Player p = Bukkit.getPlayer(uuid);

			if (p == null || !p.isOnline()) {
				return;
			}

			if (!p.getWorld().getName().equalsIgnoreCase("voidworld")) {
				return;
			}

			Block block = p.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());

			if (!canFallbackBreakBlock(p, block)) {
				return;
			}

			block.setType(Material.AIR);
		});
	}

	private static boolean canFallbackBreakBlock(Player player, Block block) {
		if (player == null || block == null || !player.isOnline()) {
			return false;
		}

		Material type = block.getType();

		// Change this list to whatever your spleef arena blocks are.
		if (type != Material.SNOW_BLOCK && type != Material.SNOW && type != Material.GLASS
				&& type != Material.PACKED_ICE && type != Material.BLUE_ICE && type != Material.ICE) {
			return false;
		}

		if (player.getGameMode() == GameMode.SPECTATOR) {
			return false;
		}

		if (player.getWorld() != block.getWorld()) {
			return false;
		}

		Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);

		// Rough reach check.
		if (player.getEyeLocation().distanceSquared(blockCenter) > 36) {
			return false;
		}

		return true;
	}
}
