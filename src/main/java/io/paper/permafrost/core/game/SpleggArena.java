package io.paper.permafrost.core.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SpleggArena {
	private String id;
	private World world;
	private String schematicPath;
	private Location pasteOrigin;
	private int teamAmount;
	private Set<SpawnPoint> playerSpawns = new HashSet<SpawnPoint>();
	private boolean inGame;
	private int fallLevel;

	public SpleggArena(String id, World world, String schematicPath, Location pasteOrigin, int teamAmount,
			Set<SpawnPoint> playerSpawns, int fallLevel) {
		this.id = id;
		this.world = world;
		this.schematicPath = schematicPath;
		this.pasteOrigin = pasteOrigin;
		this.teamAmount = teamAmount;
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

	public int getTeamAmount() {
		return teamAmount;
	}

	public void setTeamAmount(int teamAmount) {
		this.teamAmount = teamAmount;
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

	public Set<SpawnPoint> getPlayerSpawns() {
		return playerSpawns;
	}

	public static final class SpawnPoint {
		private final String worldName;
		private final int x, y, z;
		private final String teamColor;
		private final String teamName;
		private final boolean singleBlock;

		public SpawnPoint(Location loc, String teamColor, String teamName, boolean singleBlock) {
			this.worldName = loc.getWorld().getName();
			this.x = loc.getBlockX();
			this.y = loc.getBlockY();
			this.z = loc.getBlockZ();
			this.teamColor = teamColor;
			this.teamName = teamName;
			this.singleBlock = singleBlock;
		}

		public Location toLocation() {
			World w = Bukkit.getWorld(worldName);
			return new Location(w, x + 0.5, y, z + 0.5);
		}

		public String getTeamColor() {
			return teamColor;
		}

		public String getTeamName() {
			return teamName;
		}

		public boolean isSingleBlock() {
			return singleBlock;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof SpawnPoint s))
				return false;
			return x == s.x && y == s.y && z == s.z && worldName.equals(s.worldName);
		}

		@Override
		public int hashCode() {
			int r = worldName.hashCode();
			r = 31 * r + x;
			r = 31 * r + y;
			r = 31 * r + z;
			return r;
		}

		@Override
		public String toString() {
			return "SpawnPoint{" + worldName + " @ " + x + "," + y + "," + z + " team=" + teamName + " color="
					+ teamColor + " singleBlock=" + singleBlock + "}";
		}
	}
}