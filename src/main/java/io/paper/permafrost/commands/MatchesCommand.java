package io.paper.permafrost.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefTeam;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MatchesCommand implements CommandExecutor {
	private final Main main;

	public MatchesCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}

		Player player = (Player) sender;
		SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);


		if (!command.getName().equalsIgnoreCase("matches")) {
			return true;
		}

		Set<SpleefGame> games = new HashSet<>();
		for (Player online : Bukkit.getOnlinePlayers()) {
			SpleefPlayer sp2 = main.getDataLoader().getSpleefPlayer(online);
			if (sp2.getInGame() || sp2.getInTeamGame()) {
				if (!games.contains(sp2.getGame())) {
					games.add(sp2.getGame());
				}
			}
		}

		if (games.isEmpty()) {
			sender.sendMessage("There are currently no matches.");
			return true;
		}

		int count = 1;

		for (SpleefGame game : games) {
			Component gameInfo = Component.empty();

			Component gameHeader = Component.text(
					"Game " + count + ":\n",
					NamedTextColor.GOLD,
					TextDecoration.BOLD
			);

			gameInfo = gameInfo.append(gameHeader);

			if (game.getTeamGame()) {
				for (TeamSpleefTeam team : game.getTeamSpleefTeams()) {
					String teamColor = team.getColor().substring(0, 1).toUpperCase()
							+ team.getColor().substring(1);

					NamedTextColor color = NamedTextColor.NAMES.value(team.getColor().toLowerCase());
					if (color == null) {
						color = NamedTextColor.WHITE;
					}

					String playerComm = null;
					for (SpleefPlayer p : game.getPlayers()) {
						playerComm = p.getPlayer().getName();
					}

					String comm = "/spectate " + playerComm;

					Component teamLine = Component.text(
							teamColor + " Team ",
							color
					).append(Component.text("[" + team.getPoints() + "]: ", NamedTextColor.WHITE))
							.clickEvent(ClickEvent.runCommand(comm))
							.hoverEvent(HoverEvent.showText(
									Component.text("Click here to spectate this game")
							));

					List<UUID> players = team.getPlayers();

					if (players.isEmpty()) {
						teamLine = teamLine.append(Component.text("No players", NamedTextColor.GRAY));
					} else {
						for (int i = 0; i < players.size(); i++) {
							Player teamPlayer = Bukkit.getPlayer(players.get(i));
							String name = teamPlayer != null ? teamPlayer.getName() : "Unknown";

							teamLine = teamLine.append(Component.text(name, NamedTextColor.WHITE));

							if (i < players.size() - 1) {
								teamLine = teamLine.append(Component.text(", ", NamedTextColor.GRAY));
							}
						}
					}

					gameInfo = gameInfo.append(teamLine).append(Component.text(" "));
				}
			} else {
				int teamCount = 0;

				for (SpleefTeam team : game.getTeams()) {
					teamCount++;

					String playerComm = null;
					for (SpleefPlayer p : game.getPlayers()) {
						playerComm = p.getPlayer().getName();
					}

					String comm = "/spectate " + playerComm;

					Component teamLine = Component.text(
							"Team " + teamCount + ": ",
							NamedTextColor.AQUA
					).clickEvent(ClickEvent.runCommand(comm))
							.hoverEvent(HoverEvent.showText(
									Component.text("Click here to spectate this game")
							));

					Set<UUID> players = team.getPlayers();

					if (players.isEmpty()) {
						teamLine = teamLine.append(Component.text("No players", NamedTextColor.GRAY));
					} else {
						int i = 0;
						for (UUID uuid : players) {
							String name = Bukkit.getOfflinePlayer(uuid).getName();
							teamLine = teamLine.append(
									Component.text(name != null ? name : "Unknown", NamedTextColor.WHITE)
							);

							if (i < players.size() - 1) {
								teamLine = teamLine.append(Component.text(", ", NamedTextColor.GRAY));
							}
							i++;
						}
					}

					gameInfo = gameInfo.append(teamLine).append(Component.newline());
				}
			}

			player.sendMessage(gameInfo);
			count++;
		}

		return true;
	}
}
