package io.paper.permafrost.commands;

import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CustomRankCommand implements CommandExecutor {

	private final Main main;
	private static final MiniMessage MINI = MiniMessage.miniMessage();

	public CustomRankCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (args.length == 0) {
			sendUsage(player);
			return true;
		}

		String sub = args[0].toLowerCase();

		// /customrank wipe
		if (sub.equals("wipe")) {
			if (args.length == 1) {
				handleSelfWipe(player);
				return true;
			}

			// /customrank wipe <player>  -> op only
			if (!player.isOp()) {
				player.sendMessage("§cOnly operators can wipe another player's custom rank.");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return true;
			}

			Player target = Bukkit.getPlayerExact(args[1]);
			if (target == null) {
				player.sendMessage("§cThat player is not online.");
				return true;
			}

			handleOpWipe(player, target);
			return true;
		}

		// /customrank add <player> <rank...>
		if (sub.equals("add")) {
			if (!player.isOp()) {
				player.sendMessage("§cOnly operators can assign custom ranks.");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return true;
			}

			if (args.length < 3) {
				player.sendMessage("§cUsage: /customrank add <player> <rank>");
				player.sendMessage("§7Example: /customrank add Josh <gradient:#00d2ff:#3a7bd5>Glacier</gradient>");
				return true;
			}

			Player target = Bukkit.getPlayerExact(args[1]);
			if (target == null) {
				player.sendMessage("§cThat player is not online.");
				return true;
			}

			String rawRankInput = joinArgs(args, 2).trim();

			if (rawRankInput.isBlank()) {
				player.sendMessage("§cRank text cannot be empty.");
				return true;
			}

			// Very small guard against absurdly long prefixes
			String strippedPreview = stripFormattingToPlainText(rawRankInput);
			if (strippedPreview.length() > 20) {
				player.sendMessage("§cThat custom rank is too long. Keep the visible text at 20 characters or less.");
				return true;
			}

			// validate format before saving
			String renderedPrefix;
			try {
				renderedPrefix = buildCustomPrefix(rawRankInput);
			} catch (Exception ex) {
				player.sendMessage("§cThat custom rank format is invalid.");
				player.sendMessage("§7Use plain text, hex, or MiniMessage gradients.");
				player.sendMessage("§7Example: <gradient:#00d2ff:#3a7bd5>Glacier</gradient>");
				return true;
			}

			UUID targetId = target.getUniqueId();

			// remove currently equipped custom/purchasable/shell prefix first
			main.getCosmeticRankManager().clearAllCustomRanks(targetId);

			// save the RAW input so gradients/hexcodes survive
			main.getDataLoader().setCustomRankName(targetId, rawRankInput);

			// if you have ownership flag / purchased rank boolean, set it here too
			// main.getDataLoader().setHasPurchasableRank(targetId, PurchasableRank.CUSTOM, true);

			// equip it immediately
			main.getCosmeticRankManager().applyStoredCustomRank(targetId);

			player.sendMessage("§aApplied custom rank to §f" + target.getName() + "§a.");
			player.sendMessage("§7Preview: " + renderedPrefix + target.getName());

			target.sendMessage("§aYou were given a custom rank.");
			target.sendMessage("§7Preview: " + renderedPrefix + target.getName());
			target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);

			return true;
		}

		sendUsage(player);
		return true;
	}

	private void handleSelfWipe(Player player) {
		UUID uuid = player.getUniqueId();

		// this should represent owning/having a custom rank
		String current = main.getDataLoader().getCustomRankName(uuid);
		if (current == null || current.isBlank()) {
			player.sendMessage("§cYou do not have a custom rank to wipe.");
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}

		main.getCosmeticRankManager().clearAllCustomRanks(uuid);
		main.getDataLoader().setCustomRankName(uuid, "");

		player.sendMessage("§aYour custom rank has been wiped.");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
	}

	private void handleOpWipe(Player sender, Player target) {
		UUID uuid = target.getUniqueId();
		String current = main.getDataLoader().getCustomRankName(uuid);

		if (current == null || current.isBlank()) {
			sender.sendMessage("§cThat player does not have a custom rank.");
			return;
		}

		main.getCosmeticRankManager().clearAllCustomRanks(uuid);
		main.getDataLoader().setCustomRankName(uuid, "");

		sender.sendMessage("§aWiped §f" + target.getName() + "§a's custom rank.");
		target.sendMessage("§cYour custom rank was wiped.");
	}

	private void sendUsage(Player player) {
		player.sendMessage("§b§lCustom Rank Commands");
		player.sendMessage("§7/customrank wipe");
		if (player.isOp()) {
			player.sendMessage("§7/customrank add <player> <rank>");
			player.sendMessage("§7/customrank wipe <player>");
			player.sendMessage("§7Example:");
			player.sendMessage("§f/customrank add Josh <gradient:#00d2ff:#3a7bd5>Glacier</gradient>");
		}
	}

	private String joinArgs(String[] args, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			if (i > start) {
				sb.append(" ");
			}
			sb.append(args[i]);
		}
		return sb.toString();
	}

	private String stripFormattingToPlainText(String input) {
		try {
			String normalized = normalizeHexShortcuts(input);
			Component component = MINI.deserialize(normalized);
			return LegacyComponentSerializer.legacySection().serialize(component).replaceAll("§[0-9a-fk-orx]", "");
		} catch (Exception ex) {
			return input.replaceAll("<[^>]+>", "");
		}
	}
	private String stripColorSafe(String input) {
		return input.replaceAll("§[0-9a-fk-orx]", "").toLowerCase();
	}

	private String buildCustomPrefix(String rawInput) {
		String normalized = normalizeHexShortcuts(rawInput);
		Component component = MINI.deserialize(normalized);
		String legacy = LegacyComponentSerializer.legacySection().serialize(component);
		return legacy + " §r";
	}

	// lets players type things like:
	// #ff0000Hello
	// and still use MiniMessage underneath
	private String normalizeHexShortcuts(String input) {
		// only convert a leading standalone hex code into a MiniMessage color tag.
		// gradients should already be entered as MiniMessage.
		if (Pattern.compile("^#[A-Fa-f0-9]{6}.*$").matcher(input).matches() && !input.startsWith("<")) {
			String hex = input.substring(0, 7);
			String rest = input.substring(7);
			return "<" + hex + ">" + rest;
		}
		return input;
	}
}
