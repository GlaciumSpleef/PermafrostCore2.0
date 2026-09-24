package io.paper.permafrost.core.game;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.ShovelPowers;

public class AmmoManager implements Listener {
	Main main;
	private final Map<UUID, Map<ShovelPowers, Integer>> ammo = new HashMap<>();

	public AmmoManager(Main main) {
		this.main = main;
		for (Player p : Bukkit.getOnlinePlayers()) {
			List<ShovelPowers> equipped = List.of(ShovelPowers.BLACKGOO, ShovelPowers.LEVITATION);
			seedAmmoFor(p, equipped);
		}
	}

	public void seedAmmoFor(Player player, Collection<ShovelPowers> equipped) {
		Map<ShovelPowers, Integer> byPower = ammo.computeIfAbsent(player.getUniqueId(),
				k -> new EnumMap<>(ShovelPowers.class));
		for (ShovelPowers p : equipped) {
			byPower.putIfAbsent(p, 3);
		}
	}

	public int getAmmo(SpleefPlayer sp, ShovelPowers power) {
		Map<ShovelPowers, Integer> playerAmmo = ammo.get(sp.getUUID());
		if (playerAmmo == null)
			return 0;
		return playerAmmo.getOrDefault(power, 0);
	}

	public void setAmmo(SpleefPlayer sp, ShovelPowers power, int amount) {
		Map<ShovelPowers, Integer> playerAmmo = ammo.computeIfAbsent(sp.getUUID(),
				k -> new EnumMap<>(ShovelPowers.class));
		playerAmmo.put(power, amount);
	}

	public void consume(ShovelPowers power, SpleefPlayer sp) {
		int currentAmount = getAmmo(sp, power);
		if (currentAmount == 0) {
			sp.getPlayer().sendMessage("§c You are out of ammo!");
		} else {
			currentAmount--;
			setAmmo(sp, power, currentAmount);
		}
	}

	public void refill(ShovelPowers power, SpleefPlayer sp) {
		int currentAmount = getAmmo(sp, power);
		currentAmount++;
		setAmmo(sp, power, currentAmount);
	}

	public void resetAmmo(SpleefPlayer sp, ShovelPowers power) {
		setAmmo(sp, power, 3);
	}

	public void removeAmmo(SpleefPlayer sp) {
		Map<ShovelPowers, Integer> playerAmmo = ammo.computeIfAbsent(sp.getUUID(),
				k -> new EnumMap<>(ShovelPowers.class));
		for (ShovelPowers power : ShovelPowers.values()) {
			playerAmmo.put(power, 3);
		}
	}

	public void showAmmo(Player player, ShovelPowers power, int ammo) {
		int max = 3;
		StringBuilder bar = new StringBuilder("§e");
		for (int i = 0; i < max; i++) {
			if (i < ammo)
				bar.append("⬛"); // filled square
			else
				bar.append("⬜"); // empty square
		}
		if (player != null)
			player.sendActionBar(Component.text(bar + " §7• §f" + power.name() + " Ammo"));
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		List<ShovelPowers> equipped = List.of(ShovelPowers.BLACKGOO, ShovelPowers.LEVITATION);

		seedAmmoFor(player, equipped);
	}
}
