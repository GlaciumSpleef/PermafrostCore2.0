package io.paper.permafrost.core.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Arena {
	private String name;
	private int maxRadious;
	private Location corner1;
	private Location corner2;
	private Location center;
	private boolean inGame;
	private int width;
	private int length;
	private Player p1, p2;
	private Location border;
	private int maxPlayers;

	public Arena(String name, Location center, int maxRadious) {
		this.name = name;
		this.maxRadious = maxRadious;
		this.center = center;
	}

	public Arena(String name, Location corner1, Location corner2, Location center, Location border, int maxPlayers) {
		this.name = name;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.center = center;
		this.border = border;
		this.maxPlayers = maxPlayers;
	}

	public Arena(String name, Location corner1, Location corner2, Player p1, Player p2) {
		this.name = name;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.p1 = p1;
		this.p2 = p2;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public String getName() {
		return this.name;
	}

	public int getMaxRadious() {
		return maxRadious;
	}

	public void setMaxRadious(int maxRadious) {
		this.maxRadious = maxRadious;
	}

	public Location getCenter() {
		return center;
	}

	public boolean isInGame() {
		return inGame;
	}

	public Location getCorner1() {
		return this.corner1;
	}

	public Location getCorner2() {
		return this.corner2;
	}

	public Location getBorder() {
		return this.border;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}
	
	public List<Block> getBlocks() {
	    List<Block> blocks = new ArrayList<>();

	    int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
	    int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
	    int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
	    int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
	    int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
	    int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

	    World world = corner1.getWorld();

	    for (int x = minX; x <= maxX; x++) {
	        for (int y = minY; y <= maxY; y++) {
	            for (int z = minZ; z <= maxZ; z++) {
	                Block block = world.getBlockAt(x, y, z);
	                blocks.add(block);
	            }
	        }
	    }

	    return blocks;
	}

	@Override
	public String toString() {
		return "Arena [name=" + name + ", center=" + center + ", inGame=" + inGame + "]";
	}
}
