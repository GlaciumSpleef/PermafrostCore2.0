package io.paper.permafrost.core.game;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import io.paper.permafrost.core.game.SpleggArena.SpawnPoint;

public class SpleggRankedArena {
	private String id;
	private World world;
	private String schematicPath;
	private Location pasteOrigin;
	private Set<Location> playerSpawns = new HashSet<Location>();
	private boolean inGame;
	private int fallLevel;

	public SpleggRankedArena(String id, World world, String schematicPath, Location pasteOrigin,
			Set<Location> playerSpawns, int fallLevel) {
		this.id = id;
		this.world = world;
		this.schematicPath = schematicPath;
		this.pasteOrigin = pasteOrigin;
		this.playerSpawns = playerSpawns;
		this.fallLevel = fallLevel;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public World getWorld() {
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public String getSchematicPath() {
		return schematicPath;
	}

	public void setSchematicPath(String schematicPath) {
		this.schematicPath = schematicPath;
	}

	public Location getPasteOrigin() {
		return pasteOrigin;
	}

	public void setPasteOrigin(Location pasteOrigin) {
		this.pasteOrigin = pasteOrigin;
	}

	public boolean isInGame() {
		return inGame;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public int getFallLevel() {
		return fallLevel;
	}

	public void setFallLevel(int fallLevel) {
		this.fallLevel = fallLevel;
	}

	@Override
	public String toString() {
		return "Arena [name=" + id + ", center=" + pasteOrigin + ", inGame=" + inGame + "]";
	}

	public Set<Location> getPlayerSpawns() {
		return playerSpawns;
	}
}
