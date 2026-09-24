package io.paper.permafrost.core.game;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class TeamSpleefTeam {
	private Main main;
	private String color;
	private List<UUID> players;
	private int points;
	private boolean winner;
	private boolean dead;
	private boolean isOffline;

	public TeamSpleefTeam(Main main, String color, List<UUID> players) {
		this.main = main;
		this.color = color;
		this.players = players;
		this.points = 0;
		winner = false;
	}

	public TeamSpleefTeam() {

	}

	public List<UUID> getPlayers() {
		return players;
	}

	public void setPlayers(List<UUID> players) {
		this.players = players;
	}

	public void removePlayer(Player player) {
		players.remove(player);
	}

	public void addPoint() {
		points++;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int i) {
		points = i;
	}

	public void setWinner(boolean b) {
		winner = b;
	}

	public boolean isWinner() {
		return winner;
	}

	public String getColor() {
		return color;
	}

	public boolean isOffline() {
		return isOffline;
	}

	public void setOffline(boolean isOffline) {
		this.isOffline = isOffline;
	}

	public boolean isDead() {
		int count = this.getPlayers().size();
		for (UUID uuid : this.getPlayers()) {
			if (Bukkit.getPlayer(uuid) != null) {
				SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(Bukkit.getPlayer(uuid));
				if (sp != null) {
					if (sp.isDead()) {
						count--;
					}
				}
			} else {
				count--;
			}
		}
		if (count == 0)
			return true;
		return false;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}

	public SpleefPlayer getWinner(SpleefPlayer sp1, SpleefPlayer sp2) {
		if (sp1.getPoints() > sp2.getPoints()) {
			sp1.setWinner(true);
			return sp1;
		} else if (sp1.getPoints() < sp2.getPoints()) {
			sp2.setWinner(true);
			return sp2;
		}
		return null;
	}
}
