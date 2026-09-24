package io.paper.permafrost.managers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import io.paper.permafrost.utilities.ShellRank;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;

public class CosmeticRankManager {

	private final Main main;
	private final LuckPerms luckPerms;
	private static final MiniMessage MINI = MiniMessage.miniMessage();

	public CosmeticRankManager(Main main, LuckPerms luckPerms) {
		this.main = main;
		this.luckPerms = luckPerms;
	}

	public void applyCosmeticRank(UUID uuid, PurchasableRank rank) {
		CompletableFuture<User> future = luckPerms.getUserManager().loadUser(uuid);

		future.thenAcceptAsync(user -> {
			removeAllCosmeticPrefixes(user);

			PrefixNode node = PrefixNode.builder(rank.getPrefix(), 100).build();
			user.data().add(node);

			luckPerms.getUserManager().saveUser(user);
		});
	}

	public void applyShellRank(UUID uuid, ShellRank rank) {
		if (rank == null) {
			return;
		}

		if (!hasShellRankUnlocked(uuid, rank)) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				player.sendMessage("§cYou have not unlocked that shell rank yet.");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			}
			return;
		}

		luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
			removeAllCustomPrefixes(user);

			String prefix = getShellPrefix(rank);
			Bukkit.getLogger().info("applyShellRank -> " + rank.name() + " -> " + prefix);

			if (prefix == null || prefix.isBlank()) {
				return;
			}

			PrefixNode node = PrefixNode.builder(prefix, 100).build();
			user.data().add(node);

			luckPerms.getUserManager().saveUser(user);
		});
	}

	public void applyPurchasableRank(UUID uuid, PurchasableRank rank) {
		luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
			removeAllCustomPrefixes(user);

			String prefix = getPurchasablePrefix(rank);
			if (prefix == null || prefix.isBlank()) {
				return;
			}

			PrefixNode node = PrefixNode.builder(prefix, 100).build();
			user.data().add(node);

			luckPerms.getUserManager().saveUser(user);
		});
	}

	public void applyStoredCustomRank(UUID uuid) {
		luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
			removeAllCustomPrefixes(user);

			String rawRank = main.getDataLoader().getCustomRankName(uuid);
			if (rawRank == null || rawRank.isBlank()) {
				luckPerms.getUserManager().saveUser(user);
				return;
			}

			String prefix = buildCustomPrefix(rawRank);
			if (prefix == null || prefix.isBlank()) {
				luckPerms.getUserManager().saveUser(user);
				return;
			}

			PrefixNode node = PrefixNode.builder(prefix, 100).build();
			user.data().add(node);

			luckPerms.getUserManager().saveUser(user);
		});
	}

	public boolean hasShellRankUnlocked(UUID uuid, ShellRank rank) {
		if (uuid == null || rank == null) {
			return false;
		}

		if (rank == ShellRank.EGGSPERT) {
			return main.getDataLoader().isTopPoints(uuid);
		}

		return main.getDataLoader().getPoints(uuid) >= main.getDataLoader().getRequiredShells(rank);
	}

	public void clearCosmeticRank(UUID uuid) {
		luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
			removeAllCustomPrefixes(user);

			// IMPORTANT: remove saved custom rank from DB too
			main.getDataLoader().setCustomRankName(uuid, null); // or make a deleteCustomRankName(uuid) method

			luckPerms.getUserManager().saveUser(user);
		});
	}

	public void clearAllCustomRanks(UUID uuid) {
		luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
			removeAllCustomPrefixes(user);
			luckPerms.getUserManager().saveUser(user);
		});
	}

	private String getPurchasablePrefix(PurchasableRank rank) {
		return rank.getPrefix();
	}

	private void removeAllCustomPrefixes(User user) {
		for (Node node : user.getNodes()) {
			if (!(node instanceof PrefixNode prefixNode)) {
				continue;
			}

			String prefix = prefixNode.getMetaValue();

			if (isShellPrefix(prefix)) {
				user.data().remove(node);
				continue;
			}

			if (prefix.equals(PurchasableRank.POLAR.getPrefix())) {
				user.data().remove(node);
				continue;
			}

			if (prefix.equals(PurchasableRank.GLACIER.getPrefix())) {
				user.data().remove(node);
				continue;
			}

			String customName = main.getDataLoader().getCustomRankName(user.getUniqueId());
			if (customName != null && !customName.isBlank()) {
				String customPrefix = buildCustomPrefix(customName);
				if (prefix.equals(customPrefix)) {
					user.data().remove(node);
				}
			}
		}
	}

	private void removeAllCosmeticPrefixes(User user) {
		for (Node node : user.getNodes()) {
			if (!(node instanceof PrefixNode prefixNode)) {
				continue;
			}

			String prefix = prefixNode.getMetaValue();

			if (prefix.equals(PurchasableRank.POLAR.getPrefix())
					|| prefix.equals(PurchasableRank.GLACIER.getPrefix())
					|| prefix.equals(PurchasableRank.CUSTOM.getPrefix())
					|| isShellPrefix(prefix)) {
				user.data().remove(node);
				continue;
			}

			String customName = main.getDataLoader().getCustomRankName(user.getUniqueId());
			if (customName != null && !customName.isBlank()) {
				String customPrefix = buildCustomPrefix(customName);
				if (prefix.equals(customPrefix)) {
					user.data().remove(node);
				}
			}
		}
	}

	private void removeAllShellPrefixes(User user) {
		for (Node node : user.getNodes()) {
			if (!(node instanceof PrefixNode prefixNode)) {
				continue;
			}

			String value = prefixNode.getMetaValue();

			for (ShellRank rank : ShellRank.values()) {
				if (value.equals(getShellPrefix(rank))) {
					user.data().remove(node);
				}
			}
		}
	}

	private boolean isShellPrefix(String prefix) {
		return prefix.equals("§7Farmer §r")
				|| prefix.equals("§6Cook §r")
				|| prefix.equals("§dChef §r")
				|| prefix.equals("§bMaster §r")
				|| prefix.equals("§5Humpty §r")
				|| prefix.equals("§5Humpty Dumpty §r")
				|| prefix.equals("§eRamsay §r")
				|| prefix.equals("§aSpicy §r")
				|| prefix.equals("§aHot'n'Spicy §r")
				|| prefix.equals("§bSplegg Head §r")
				|| prefix.equals("§7§lBacon §r")
				|| prefix.equals("§e§lSunnyside §r")
				|| prefix.equals("§a§lBunny §r")
				|| prefix.equals("§a§lEaster Bunny §r")
				|| prefix.equals("§d§lEggspert §r")
				|| prefix.equals("§d§lThe Eggspert §r");
	}

	private String buildCustomPrefix(String rawInput) {
		String normalized = normalizeHexShortcuts(rawInput);
		Component component = MINI.deserialize(normalized);
		String legacy = LegacyComponentSerializer.legacySection().serialize(component);
		return legacy + " §r";
	}

	private String normalizeHexShortcuts(String input) {
		if (input == null) {
			return "";
		}

		if (Pattern.compile("^#[A-Fa-f0-9]{6}.*$").matcher(input).matches() && !input.startsWith("<")) {
			String hex = input.substring(0, 7);
			String rest = input.substring(7);
			return "<" + hex + ">" + rest;
		}

		return input;
	}

	private String getShellPrefix(ShellRank rank) {
		switch (rank) {
		case FARMER:
			return "§7Farmer §r";
		case COOK:
			return "§6Cook §r";
		case CHEF:
			return "§dChef §r";
		case MASTER:
			return "§bMaster §r";
		case HUMPTY_DUMPTY:
			return "§5Humpty Dumpty §r";
		case RAMSAY:
			return "§eRamsay §r";
		case HOT_N_SPICY:
			return "§aHot'n'Spicy §r";
		case SPLEGG_HEAD:
			return "§bSplegg Head §r";
		case BACON:
			return "§7§lBacon §r";
		case SUNNYSIDE:
			return "§e§lSunnyside §r";
		case EASTER_BUNNY:
			return "§a§lEaster Bunny §r";
		case EGGSPERT:
			return "§d§lThe Eggspert §r";
		default:
			return "";
		}
	}
}
