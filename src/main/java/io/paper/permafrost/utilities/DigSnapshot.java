package io.paper.permafrost.utilities;

import io.paper.permafrost.utilities.DigSnapshot;

public class DigSnapshot {
	private int blockX;
	private int blockY;
	private int blockZ;
	private long timeMillis;
	
	public DigSnapshot(int x, int y, int z, long timeMillis) {
		this.blockX = x;
		this.blockY = y;
		this.blockZ = z;
		this.timeMillis = timeMillis;
	}
	
	public int getX() {
		return blockX;
	}
	public int getY() {
		return blockY;
	}
	public int getZ() {
		return blockZ;
	}
	public long getMillis() {
		return timeMillis;
	}
}
