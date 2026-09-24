package io.paper.permafrost.parkour;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;

import io.paper.permafrost.Main;

public class ParkourMap {
	private String name;
	private Location spawn1;
	private Location spawn2;
	private Location end1;
	private Location end2;
	private int fallLevel;
	private boolean inMatch;

	public ParkourMap(String name, Location spawn1, Location spawn2, Location end1, Location end2, int fallLevel) {
		this.name = name;
		this.spawn1 = spawn1;
		this.spawn2 = spawn2;
		this.end1 = end1;
		this.end2 = end2;
		this.fallLevel = fallLevel;
		this.inMatch = false;
	}

	public String getName() {
		return name;
	}

	public List<Location> getPlayerSpawns() {
		List<Location> locations = new ArrayList<>();
		locations.add(spawn1);
		locations.add(spawn2);

		return locations;
	}

	public List<Location> getPlayerFinishes() {
		List<Location> locations = new ArrayList<>();
		locations.add(end1);
		locations.add(end2);

		return locations;
	}

	public Location getSpawn1() {
		return spawn1;
	}

	public Location getSpawn2() {
		return spawn2;
	}

	public Location getFinish1() {
		return end1;
	}

	public Location getFinish2() {
		return end2;
	}
	
	public int getFallLevel() {
		return fallLevel;
	}
	
	public boolean isInMatch() {
		return inMatch;
	}
	
	public void setInMatch(boolean inMatch) {
		this.inMatch = inMatch;
	}
}
