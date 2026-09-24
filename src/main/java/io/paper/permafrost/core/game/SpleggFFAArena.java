package io.paper.permafrost.core.game;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

public class SpleggFFAArena {

	private final String id;
	private final World world;
	private final String schematic;
	private final Location pasteOrigin;
	private final int maxPlayers;
	private final Set<Location> spawns;
	private final int fallLevel;
	private final String size;

	public SpleggFFAArena(String id, World world, String schematic, Location pasteOrigin, int maxPlayers,
			Set<Location> spawns, int fallLevel, String size) {

		this.id = id;
		this.world = world;
		this.schematic = schematic;
		this.pasteOrigin = pasteOrigin;
		this.maxPlayers = maxPlayers;
		this.spawns = spawns;
		this.fallLevel = fallLevel;
		this.size = size;
	}
	public String getId() {
        return id;
    }

    public World getWorld() {
        return world;
    }

    public String getSchematic() {
        return schematic;
    }

    public Location getPasteOrigin() {
        return pasteOrigin;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Set<Location> getSpawns() {
        return spawns;
    }

    public int getFallLevel() {
        return fallLevel;
    }

    public String getSize() {
        return size;
    }
 
}
