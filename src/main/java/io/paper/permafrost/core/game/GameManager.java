package io.paper.permafrost.core.game;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.tasks.CancelTask;
import io.paper.permafrost.core.game.tasks.TaskListener;

public class GameManager {

	private Main main;

	public GameManager(Main main) {
		this.main = main;
	}

	private HashMap<SpleefPlayer, Long> interact = new HashMap<>();

	private HashMap<SpleefPlayer, BukkitRunnable> runnables = new HashMap<>();

	public HashMap<SpleefPlayer, Long> getInteract() {
		return this.interact;
	}

	public HashMap<SpleefPlayer, BukkitRunnable> getRunnables() {
		return this.runnables;
	}

	public Set<SpleefGame> games = new HashSet<>();


	public SpleefGame duelGame(SpleefPlayer challenger, List<SpleefPlayer> sp2, SpleefType type) {

		Set<UUID> uuid1 = new HashSet<>();
		Set<UUID> uuid2 = new HashSet<>();

		uuid1.add(challenger.getUUID());

		for (SpleefPlayer sp : sp2) {
			if (uuid1.size() < (sp2.size() + 1) / 2) {
				uuid1.add(sp.getUUID());
			} else {
				uuid2.add(sp.getUUID());
			}

		}

		SpleefTeam team1 = new SpleefTeam(main, type, uuid1, 1);
		SpleefTeam team2 = new SpleefTeam(main, type, uuid2, 2);

		Set<SpleefTeam> teams = new HashSet<>();
		teams.add(team1);
		teams.add(team2);
		return createGame(teams, type);
	}

	public SpleefGame createGame(final Set<SpleefTeam> teams, SpleefType type) {
		int playersAmount = 0;
		for (SpleefTeam team : teams)
			playersAmount = playersAmount + team.getPlayers().size();

		Arena arena = findArena();
		SpleefGame game = null;
		if (arena != null) {
			game = new SpleefGame(main, arena, type);
			main.getServer().getPluginManager().registerEvents(new CancelTask(game), main); 
			this.games.add(game);

			for (SpleefTeam team : teams)
				game.getTeams().add(team);
		} else {
			for (SpleefTeam team : teams) {
				for (SpleefPlayer sp : team.getSpleefPlayers()) {
					sp.sendMessage("§cNo arena was found.");
				}
			}

		}

		return game;
	}

	public void startGame(Set<SpleefTeam> teams, SpleefType type) {
		Arena arena = findArena();
		SpleefGame game = null;
		if (arena != null) {
			game = new SpleefGame(main, arena, type);
			game.getTeams().addAll(teams);
		}
	}

	public Arena findArena() {

		List<Arena> arenas = new ArrayList<Arena>(main.getDataLoader().getArenas());

		for (Arena a : arenas) {

			return a;
		}

		return null;
	}

	public void sendSyncMessage(final Collection<SpleefPlayer> s, final String string) {
		(new BukkitRunnable() {
			public void run() {
				for (SpleefPlayer ss : s) {
					ss.sendMessage(string);
				}
			}
		}).runTask(main);
	}

	public Set<SpleefGame> getGames() {
		return this.games;
	}
}
