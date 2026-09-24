package io.paper.permafrost.commands;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import net.md_5.bungee.api.chat.TextComponent;

public class PlaytoCommand implements CommandExecutor {

	private final Main main;

	// game -> players who accepted
	private final Map<SpleefGame, Set<UUID>> accepted = new HashMap<>();

	public PlaytoCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player p))
			return msg(sender, "§cPlayers only.");

		SpleefPlayer sp = main.getDataLoader().getPlayer(p.getUniqueId());
		if (sp == null || sp.getGame() == null)
			return msg(p, "§cYou are not in a game.");

		if (args.length != 1)
			return msg(p, "§cUsage: /playto <number>");

		int target;
		try {
			target = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			return msg(p, "§cInvalid number.");
		}

		SpleefGame game = sp.getGame();
		Set<UUID> required = getPlayers(game);

		if (!required.contains(p.getUniqueId()))
			return msg(p, "§cSpectators cannot vote.");

		Set<UUID> set = accepted.computeIfAbsent(game, g -> new HashSet<>());

		// store requested play-to value (only first player matters)
		main.getDataLoader().setPlayToInt(p.getUniqueId(), target);

		if (!set.add(p.getUniqueId())) {
			msg(p, "§eYou already voted.");
			return showRemaining(game, required, set);
		}

		sp.setAcceptedPlayto(true);

		if (set.size() == 1)
			startVote(game, p, target, required, set);
		else
			announceAccept(game, p, required, set);

		if (set.containsAll(required))
			applyPlayTo(game);

		return true;
	}

	// --- CORE ---

	private Set<UUID> getPlayers(SpleefGame game) {
		Set<UUID> set = new HashSet<>();

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p == null) continue;

			if (main.getDataLoader().getSpectatorsForGame(game) != null &&
				main.getDataLoader().getSpectatorsForGame(game).contains(spp.getSpectator()))
				continue;

			set.add(p.getUniqueId());
		}
		return set;
	}

	// --- MESSAGES ---

	private void startVote(SpleefGame game, Player starter, int target,
						   Set<UUID> req, Set<UUID> acc) {

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p == null || !req.contains(p.getUniqueId())) continue;

			p.sendMessage("§b⚠ §lPLAY TO CHANGE §7- §f" + starter.getName()
					+ " §7wants to play to §f" + target);
			p.sendMessage("§eType §f/playto " + target + " §eto accept.");
		}

		showRemaining(game, req, acc);
	}

	private void announceAccept(SpleefGame game, Player p,
							   Set<UUID> req, Set<UUID> acc) {

		for (SpleefPlayer spp : game.getPlayers()) {
			Player pl = spp.getPlayer();
			if (pl != null && req.contains(pl.getUniqueId()))
				pl.sendMessage("§a✔ §f" + p.getName() + " §7accepted.");
		}

		showRemaining(game, req, acc);
	}

	private boolean showRemaining(SpleefGame game,
								 Set<UUID> req, Set<UUID> acc) {

		List<String> left = new ArrayList<>();

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p != null && req.contains(p.getUniqueId()) && !acc.contains(p.getUniqueId()))
				left.add(p.getName());
		}

		String msg = left.isEmpty()
				? "§a✔ Everyone accepted."
				: "§7Waiting: §f" + String.join("§7, §f", left);

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p != null && req.contains(p.getUniqueId()))
				p.sendMessage(msg);
		}

		return true;
	}

	// --- APPLY ---

	private void applyPlayTo(SpleefGame game) {
		Set<UUID> set = accepted.remove(game);

		if (set == null || set.isEmpty()) return;

		// get play-to value from any player who voted
		int playTo = 0;
		for (UUID uuid : set) {
			playTo = main.getDataLoader().getPlayToInt(uuid);
			if (playTo > 0) break;
		}

		game.setPlaytoScore(playTo);

		for (SpleefPlayer spp : game.getPlayers()) {
			spp.setAcceptedPlayto(false);

			Player p = spp.getPlayer();
			if (p != null)
				p.sendMessage("§b✔ §lNOW PLAYING TO §f" + playTo);
		}
	}

	private boolean msg(CommandSender s, String m) {
		s.sendMessage(m);
		return true;
	}
}
