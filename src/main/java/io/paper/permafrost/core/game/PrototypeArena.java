package io.paper.permafrost.core.game;

import java.util.List;
import java.util.Set;

import org.bukkit.Location;

public class PrototypeArena {
	private final String id;
	private final String schematic;
	private final Location center;
	private final List<Location> spawns;
	private final Set<Location> circleSpawns;
	
	public PrototypeArena(String id, String schematic, Location center, List<Location> spawns, Set<Location> circleSpawns) {
		this.id = id;
		this.schematic = schematic;
		this.center = center;
		this.spawns = spawns;
		this.circleSpawns = circleSpawns;
	}
	
	public String getId() {
		return id;
	}
	
	public String getSchematicPath() {
		return schematic;
	}
	
	public Location getCenter() {
		return center;
	}
	
	public List<Location> getPlayerSpawns() {
		return spawns;
	}
	
	public Set<Location> getCircleSpawns() {
		return circleSpawns;
	}
}
