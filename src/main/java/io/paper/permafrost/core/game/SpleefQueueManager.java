package io.paper.permafrost.core.game;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import github.scarsz.discordsrv.DiscordSRV;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefFFAMode;
import io.paper.permafrost.utilities.SpleefMode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class SpleefQueueManager {

	private final Map<SpleefFFAMode, SpleefFFAGame> games = new EnumMap<>(SpleefFFAMode.class);

	public SpleefQueueManager(Main main) {
		AirSpleefFFA airSpleef = new AirSpleefFFA(main);
		PowerSpleefFFA powerSpleef = new PowerSpleefFFA(main);
		PrototypeSpleefFFA prototype = new PrototypeSpleefFFA(main);

		games.put(SpleefFFAMode.AIRSPLEEF, airSpleef);
		games.put(SpleefFFAMode.POWERSPLEEF, powerSpleef);
		games.put(SpleefFFAMode.PROTOTYPE, prototype);

		Bukkit.getPluginManager().registerEvents(prototype, main);

		// Only register these if they implement Listener too.
		// Bukkit.getPluginManager().registerEvents(airSpleef, main);
		// Bukkit.getPluginManager().registerEvents(powerSpleef, main);
	}

	public void addPlayer(UUID uuid, SpleefFFAMode mode) {
		removePlayerFromAllModes(uuid);

		SpleefFFAGame game = games.get(mode);
		if (game == null) {
			return;
		}

		game.addPlayer(uuid);
	}

	public boolean getInFFA(UUID uuid) {
		for (SpleefFFAGame game : games.values()) {
			if (game.isInThisFFA(uuid)) {
				return true;
			}
		}

		return false;
	}

	public SpleefFFAMode getFFAMode(UUID uuid) {
		for (Map.Entry<SpleefFFAMode, SpleefFFAGame> entry : games.entrySet()) {
			if (entry.getValue().isInThisFFA(uuid)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public SpleefFFAGame getGame(SpleefFFAMode mode) {
		return games.get(mode);
	}

	public SpleefFFAGame getPlayerFFA(UUID uuid) {
		for (SpleefFFAGame game : games.values()) {
			if (game.isInThisFFA(uuid)) {
				return game;
			}
		}

		return null;
	}

	public SpleefFFAGame getAlivePlayerFFA(UUID uuid) {
		for (SpleefFFAGame game : games.values()) {
			if (game.isAlive(uuid)) {
				return game;
			}
		}

		return null;
	}

	public Set<UUID> getAllSpectators() {
		Set<UUID> allSpectators = new HashSet<>();

		for (SpleefFFAGame game : games.values()) {
			allSpectators.addAll(game.getPlayersSpectating());
		}

		return allSpectators;
	}

	public boolean isInAnyFFA(UUID uuid) {
		return getPlayerFFA(uuid) != null;
	}

	public boolean isAliveInAnyFFA(UUID uuid) {
		return getAlivePlayerFFA(uuid) != null;
	}

	public void removePlayerFromAllModes(UUID uuid) {
		for (SpleefFFAGame game : games.values()) {
			game.removePlayer(uuid);
		}
	}

	public int getTotalPlayersInQueue() {
		int total = 0;

		for (SpleefFFAGame game : games.values()) {
			total += game.getPlayersInQueueCount();
		}

		return total;
	}
}
