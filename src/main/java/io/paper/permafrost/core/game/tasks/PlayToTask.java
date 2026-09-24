package io.paper.permafrost.core.game.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.utilities.ShovelPowers;

public class PlayToTask {
	private Main main;
	private UUID uuid;
	private int playToScore;

	public PlayToTask(Main main, UUID uuid, int playToScore) {
		this.main = main;
		this.uuid = uuid;
		this.playToScore = playToScore;
		startDuelRequests();
	}

	public void startDuelRequests() {

		SpleefGame game = new SpleefGame(main, main.getDataLoader().getSplegg(uuid), false, null);
		game.setPlaytoScore(playToScore);
		for (int i = main.getDataLoader().getTeamCount(uuid); i > 0; i--) {
			for (UUID uuid : main.getDataLoader().getTeam(uuid, i)) {
				String color = "";
				switch (i) {
				case 1:
					color = "blue";
					break;
				case 2:
					color = "red";
					break;
				case 3:
					color = "green";
					break;
				case 4:
					color = "yellow";
					break;
				case 5:
					color = "orange";
					break;
				case 6:
					color = "purple";
					break;
				case 7:
					color = "pink";
					break;
				case 8:
					color = "light blue";
					break;
				case 9:
					color = "magenta";
					break;
				case 10:
					color = "brown";
					break;
				case 11:
					color = "cyan";
					break;
				case 12:
					color = "light gray";
					break;
				case 13:
					color = "gray";
					break;
				case 14:
					color = "dark green";
					break;
				case 15:
					color = "white";
					break;
				case 16:
					color = "black";
					break;
				}
				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid));
				TeamSpleefTeam team = new TeamSpleefTeam(main, color, main.getDataLoader().getTeam(uuid, i));
				game.setTeamSpleefTeam(color, main.getDataLoader().getTeam(uuid, i));
				sp.setTeam(team);

			}
		}
		game.setTeamCount(main.getDataLoader().getTeamCount(uuid));
		game.saveTeams(main.getDataLoader().getPlayerTeams(uuid));
		main.getDuelManager().setGame(game);
		List<Integer> teamSizes = new ArrayList<>();
		Set<UUID> players = new HashSet<>();
		for (int i = 1; i <= main.getDataLoader().getTeamCount(uuid); i++) {
			int teamSize = 0;
			for (UUID uuid2 : main.getDataLoader().getTeam(uuid, i)) {
				if (uuid2 != uuid)
					players.add(uuid2);
				teamSize++;
			}
			teamSizes.add(teamSize);
		}
		Bukkit.getPlayer(uuid).closeInventory();
		main.getDuelManager().sendRequest(uuid, players, teamSizes);
		main.getDataLoader().setTeamCount(uuid, 1);
		main.getDataLoader().clearTeams(uuid);
		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
		List<Player> toRemove = new ArrayList<>();
		for (Player p : onlinePlayers) {
			SpleefPlayer spleefPlayer = main.getDataLoader().getSpleefPlayer(p);
			if (spleefPlayer.getGame() != null) {
				toRemove.add(p);
			}
		}
		onlinePlayers.removeAll(toRemove);
		main.getDataLoader().setOnlinePlayers(uuid, onlinePlayers);

	}
}
