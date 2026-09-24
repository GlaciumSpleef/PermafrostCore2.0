package io.paper.permafrost.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class PlayerBubbleManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BlockDisplay> bubbles = new HashMap<>();

    public PlayerBubbleManager(JavaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Gives the player a glass bubble.
     */
    public void createBubble(Player player) {
        removeBubble(player);

        BlockDisplay bubble = player.getWorld().spawn(
                player.getLocation(),
                BlockDisplay.class,
                display -> {
                    display.setBlock(Material.GLASS.createBlockData());

                    float width = 2f;
                    float height = 2.4f;
                    float depth = 2f;

                    /*
                     * Because the display is mounted above the player,
                     * the negative Y translation moves it back down
                     * around the player's body.
                     */
                    display.setTransformation(new Transformation(
                            new Vector3f(
                                    -width / 2.0f,
                                    -2f,
                                    -depth / 2.0f
                            ),
                            new AxisAngle4f(),
                            new Vector3f(
                                    width,
                                    height,
                                    depth
                            ),
                            new AxisAngle4f()
                    ));
                    
                    display.setRotation(0, 0);
                    display.setBillboard(Display.Billboard.FIXED);
                    display.setBrightness(new Display.Brightness(15, 15));

                    display.setShadowRadius(0.0f);
                    display.setShadowStrength(0.0f);

                    display.setViewRange(64.0f);
                    display.setPersistent(false);
                    display.setGravity(false);
                    display.setInvulnerable(true);
                    display.setSilent(true);

                    /*
                     * Teleport interpolation is unnecessary because
                     * the display is attached as a passenger.
                     */
                    display.setTeleportDuration(0);
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(0);
                }
        );

        boolean attached = player.addPassenger(bubble);

        if (!attached) {
            bubble.remove();
            return;
        }

        bubbles.put(player.getUniqueId(), bubble);
    }

    /**
     * Removes the player's bubble.
     */
    public void removeBubble(Player player) {
        removeBubble(player.getUniqueId());
    }

    /**
     * Removes a bubble by player UUID.
     */
    public void removeBubble(UUID uuid) {
        BlockDisplay bubble = bubbles.remove(uuid);

        if (bubble != null && bubble.isValid()) {
            bubble.remove();
        }
    }

    /**
     * Returns true when the player currently has a valid bubble.
     */
    public boolean hasBubble(Player player) {
        BlockDisplay bubble = bubbles.get(player.getUniqueId());

        return bubble != null
                && bubble.isValid()
                && bubble.getVehicle() != null
                && bubble.getVehicle().getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Reattaches the existing bubble if Minecraft dismounts it.
     */
    public void reattachBubble(Player player) {
        BlockDisplay bubble = bubbles.get(player.getUniqueId());

        if (bubble == null || !bubble.isValid()) {
            bubbles.remove(player.getUniqueId());
            return;
        }

        Entity vehicle = bubble.getVehicle();

        if (vehicle != null && vehicle.getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        player.addPassenger(bubble);
    }

    /**
     * Removes every bubble. Call this from onDisable().
     */
    public void shutdown() {
        for (BlockDisplay bubble : bubbles.values()) {
            if (bubble != null && bubble.isValid()) {
                bubble.remove();
            }
        }

        bubbles.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBubble(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removeBubble(event.getEntity());
    }

    /*
     * Passengers are normally removed when changing worlds,
     * so recreate the bubble in the new world.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        BlockDisplay oldBubble = bubbles.remove(uuid);

        if (oldBubble == null) {
            return;
        }

        if (oldBubble.isValid()) {
            oldBubble.remove();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                createBubble(player);
            }
        });
    }

    /*
     * This only matters if you later decide to retain bubble state
     * through death. Currently onDeath removes it permanently.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                reattachBubble(player);
            }
        });
    }

    /*
     * Cleanup protection for plugin reloads where an invalid display
     * may remain stored.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        BlockDisplay bubble = bubbles.get(event.getPlayer().getUniqueId());

        if (bubble != null && !bubble.isValid()) {
            bubbles.remove(event.getPlayer().getUniqueId());
        }
    }
}
