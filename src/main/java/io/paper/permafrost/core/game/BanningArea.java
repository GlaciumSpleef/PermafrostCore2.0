package io.paper.permafrost.core.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BanningArea {
	private String name;
	private Location spawn1;
	private Location spawn2;
	private Location banner1;
	private Location banner2;
	private boolean inGame;

	public BanningArea(String name, Location spawn1, Location spawn2, Location banner1, Location banner2) {
		this.name = name;
		this.spawn1 = spawn1;
		this.spawn2 = spawn2;
		this.banner1 = banner1;
		this.banner2 = banner2;
	}

	public String getName() {
		return this.name;
	}
	
	public boolean isInGame() {
		return inGame;
	}
	
	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public Location getSpawn1() {
		return spawn1;
	}
	
	public Location getSpawn2() {
		return spawn2;
	}
	
	public Location getBanner1() {
		return banner1;
	}
	
	public Location getBanner2() {
		return banner2;
	}
}
