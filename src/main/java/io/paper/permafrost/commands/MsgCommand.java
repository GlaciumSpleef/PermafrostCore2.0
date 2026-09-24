package io.paper.permafrost.commands;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgCommand implements CommandExecutor {

	private final Main main;

	// player -> last person who messaged them
	private final Map<UUID, UUID> lastMessenger = new HashMap<>();

	public MsgCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}

		if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
			return handleReply(player, args);
		}

		return handleMsg(player, args);
	}

	private boolean handleMsg(Player player, String[] args) {
		if (args.length < 2) {
			player.sendMessage("§cUsage: /msg <player> <message>");
			return true;
		}

		Player target = Bukkit.getPlayerExact(args[0]);

		if (target == null || !target.isOnline()) {
			player.sendMessage("§cThat player is not online.");
			return true;
		}

		if (target.equals(player)) {
			player.sendMessage("§cYou cannot message yourself.");
			return true;
		}

		String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

		sendPrivateMessage(player, target, message);

		return true;
	}

	private boolean handleReply(Player player, String[] args) {
		if (args.length < 1) {
			player.sendMessage("§cUsage: /r <message>");
			return true;
		}

		UUID lastMessengerUUID = lastMessenger.get(player.getUniqueId());

		if (lastMessengerUUID == null) {
			player.sendMessage("§cYou have nobody to reply to.");
			return true;
		}

		Player target = Bukkit.getPlayer(lastMessengerUUID);

		if (target == null || !target.isOnline()) {
			player.sendMessage("§cThat player is no longer online.");
			return true;
		}

		if (target.equals(player)) {
			player.sendMessage("§cYou cannot message yourself.");
			return true;
		}

		String message = String.join(" ", args);

		sendPrivateMessage(player, target, message);

		return true;
	}

	private void sendPrivateMessage(Player sender, Player target, String message) {
		sender.sendMessage("§6[me -> " + target.getName() + "] §f" + message);
		target.sendMessage("§6[" + sender.getName() + " -> me] §f" + message);

		// Target can now /r back to sender
		lastMessenger.put(target.getUniqueId(), sender.getUniqueId());

		// Sender can also /r again to same target
		lastMessenger.put(sender.getUniqueId(), target.getUniqueId());
	}
}