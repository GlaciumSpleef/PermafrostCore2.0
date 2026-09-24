package io.paper.permafrost.commands;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import io.paper.permafrost.core.game.tasks.CountdownTask;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class EndGameCommand implements CommandExecutor {
	private final Main main;
	private final Map<SpleefGame, Set<UUID>> accepted = new HashMap<>();

	public EndGameCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player p))
			return msg(sender, "§cPlayers only.");

		SpleefPlayer sp = main.getDataLoader().getPlayer(p.getUniqueId());
		if (sp == null || sp.getGame() == null)
			return msg(p, "§cYou are not in a game.");

		SpleefGame game = sp.getGame();
		Set<UUID> required = getPlayers(game);

		if (!required.contains(p.getUniqueId()))
			return msg(p, "§cSpectators cannot vote.");

		Set<UUID> set = accepted.computeIfAbsent(game, g -> new HashSet<>());

		if (!set.add(p.getUniqueId())) {
			msg(p, "§eYou already voted.");
			return showRemaining(game, required, set);
		}

		sp.setAcceptedEndgame(true);

		if (set.size() == 1)
			startVote(game, p, required, set);
		else
			announceAccept(game, p, required, set);

		if (set.containsAll(required))
			end(game);

		return true;
	}

	// --- CORE ---

	private Set<UUID> getPlayers(SpleefGame game) {
		Set<UUID> set = new HashSet<>();

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p == null)
				continue;

			if (main.getDataLoader().getSpectatorsForGame(game) != null
					&& main.getDataLoader().getSpectatorsForGame(game).contains(spp.getSpectator()))
				continue;

			set.add(p.getUniqueId());
		}
		return set;
	}

	// --- MESSAGES ---

	private void startVote(SpleefGame game, Player starter, Set<UUID> req, Set<UUID> acc) {
		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p == null || !req.contains(p.getUniqueId()))
				continue;

			p.sendMessage("§6⚠ §lEND GAME VOTE §7- §f" + starter.getName() + " §7wants to end the game.");
			p.sendMessage("§eClick here or type §f/endgame");

			TextComponent tc = new TextComponent("§6§l[ACCEPT]");
			tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/endgame"));
			tc.setHoverEvent(
					new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to accept").create()));
			p.spigot().sendMessage(tc);
		}
		showRemaining(game, req, acc);
	}

	private void announceAccept(SpleefGame game, Player p, Set<UUID> req, Set<UUID> acc) {
		for (SpleefPlayer spp : game.getPlayers()) {
			Player pl = spp.getPlayer();
			if (pl != null && req.contains(pl.getUniqueId()))
				pl.sendMessage("§a✔ §f" + p.getName() + " §7accepted.");
		}
		showRemaining(game, req, acc);
	}

	private boolean showRemaining(SpleefGame game, Set<UUID> req, Set<UUID> acc) {
		List<String> left = new ArrayList<>();

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p != null && req.contains(p.getUniqueId()) && !acc.contains(p.getUniqueId()))
				left.add(p.getName());
		}

		String msg = left.isEmpty() ? "§a✔ Everyone accepted." : "§7Waiting: §f" + String.join("§7, §f", left);

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p != null && req.contains(p.getUniqueId()))
				p.sendMessage(msg);
		}
		return true;
	}

	// --- END ---

	private void end(SpleefGame game) {
		accepted.remove(game);

		for (SpleefPlayer spp : game.getPlayers())
			spp.setAcceptedEndgame(false);

		TeamSpleefTeam winner = null;
		int best = -1;

		for (TeamSpleefTeam t : game.getTeamSpleefTeams())
			if (t.getPoints() > best) {
				best = t.getPoints();
				winner = t;
			}

		if (winner != null)
			winner.setWinner(true);

		for (SpleefPlayer spp : game.getPlayers()) {
			Player p = spp.getPlayer();
			if (p != null)
				p.sendMessage("§a✔ §lGAME ENDED §7- §fAll players accepted.");
		}

		if (game.getPlayers().stream().anyMatch(SpleefPlayer::getInTeamGame)) {
			game.setEndedByCommand(true);
			game.endTeamGame();
		} else
			game.endGame();
	}

	private boolean msg(CommandSender s, String m) {
		s.sendMessage(m);
		return true;
	}
}
