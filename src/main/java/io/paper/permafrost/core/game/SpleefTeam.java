package io.paper.permafrost.core.game;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class SpleefTeam implements Comparable<SpleefTeam> {

	private Main main;
	private Set<UUID> players;
	private int points;
	private int ELO;
	private Location spawn;
	private SpleefType spleefType;
	private String name;
	private int number;
	
	public SpleefTeam(Main main,SpleefType spleefType,Set<UUID> players, int number) {
		this.main = main;
		this.players = players;
		this.spleefType = spleefType;
		this.number = number;
	}
	
	
	public SpleefTeam(Main main, SpleefType spleefType,int number,UUID... players) {
	    this(main,spleefType, new HashSet<UUID>(Arrays.asList(players)), number);
	}
	
	public Location getSpawn() {
		return spawn;
	}
	public void setSpawn(Location spawn) {
		this.spawn = spawn;
	}


	public String getName() {
		if (this.name==null) {
			if (this.players.size()<=1) {
				for (UUID u : players) {
					this.name = Bukkit.getOfflinePlayer(u).getName();
					break;
				}
			} else {
				this.name = "Team " + number;
			}
		}
		
	return this.name;
	}

	public Set<UUID> getPlayers() {
		return players;
	}
	
	public Set<SpleefPlayer> getSpleefPlayers() {
		Set<SpleefPlayer> set = new HashSet<>();
		for (UUID u : this.players) {
			SpleefPlayer sp = main.getDataLoader().getPlayer(u);
			if (sp!=null) set.add(sp);
		}
		return set;
	}

	
	public int getAlivePlayers() {
		int alive =0;
		for (SpleefPlayer sp : getSpleefPlayers()) {
			if (!sp.isSpectating()) alive++;
		}
		return alive;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}


	@Override
	public int compareTo(SpleefTeam o) {
		return Integer.compare(o.getPoints(), this.points);
	}
		@Override
		public String toString() {
			return "SpleefTeam [players=" + players + ", points=" + points + ", ELO=" + ELO + ", spawn=" + spawn
					+ ", name=" + name + ", number=" + number + "]";
		}

}
