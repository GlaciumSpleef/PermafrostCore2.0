package io.paper.permafrost.training;

import org.bukkit.Location;
import org.bukkit.World;

public class TrainingArena {

	private String id;
	private World world;
	private Location playerSpawn;
	private Location plotPasteLocation;
	private TrainingType type;
	private boolean inGame;

	public TrainingArena(String id, World world, Location playerSpawn, Location plotPasteLocation, TrainingType type) {
		this.id = id;
		this.world = world;
		this.playerSpawn = playerSpawn;
		this.plotPasteLocation = plotPasteLocation;
		this.type = type;
	}

	public String getName() {
		return id;
	}

	public World getWorld() {
		return world;
	}

	public Location getPlayerSpawn() {
		return playerSpawn;
	}

	public Location getPlotPasteLocation() {
		return plotPasteLocation;
	}

	public TrainingType getType() {
		return type;
	}
	
	public boolean isInGame() {
		return inGame;
	}
	
	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}
}
