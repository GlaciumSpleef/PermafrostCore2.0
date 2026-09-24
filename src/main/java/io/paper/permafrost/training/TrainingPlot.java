package io.paper.permafrost.training;

import java.util.List;

import org.bukkit.Location;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Map;

public class TrainingPlot {

    private final String name;
    private final String schematic;
    private final int maxPlayers;
    private final int blocksToBreak;
    private final boolean timer;
    private final boolean offset;
    private float yaw;
    private final Map<Integer, Vector> spawnOffsets;

    public TrainingPlot(String name,
                        String schematic,
                        int maxPlayers,
                        int blocksToBreak,
                        boolean timer,
                        boolean offset) {

        this.name = name;
        this.schematic = schematic;
        this.maxPlayers = maxPlayers;
        this.blocksToBreak = blocksToBreak;
        this.timer = timer;
        this.offset = offset;
        this.spawnOffsets = Collections.emptyMap();
    }

    public TrainingPlot(String name,
                        String schematic,
                        int maxPlayers,
                        int blocksToBreak,
                        boolean timer,
                        boolean offset,
                        Map<Integer, Vector> spawnOffsets) {

        this.name = name;
        this.schematic = schematic;
        this.maxPlayers = maxPlayers;
        this.blocksToBreak = blocksToBreak;
        this.timer = timer;
        this.offset = offset;
        this.spawnOffsets = spawnOffsets;
    }
    
    public float getYaw() {
    	return yaw;
    }
    
    public void setYaw(float yaw) {
    	this.yaw = yaw;
    }

    public String getName() {
        return name;
    }

    public String getSchematic() {
        return schematic;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getBlocksToBreak() {
        return blocksToBreak;
    }

    public boolean hasTimer() {
        return timer;
    }

    public boolean hasOffset() {
        return offset;
    }

    public boolean hasCustomSpawns() {
        return !spawnOffsets.isEmpty();
    }

    public Map<Integer, Vector> getSpawnOffsets() {
        return spawnOffsets;
    }

    public Location resolveSpawn(Location pasteOrigin, int index) {

        if (maxPlayers == 1 || !spawnOffsets.containsKey(index)) {
            return pasteOrigin.clone();
        }

        Vector offset = spawnOffsets.get(index);

        return pasteOrigin.clone().add(
                offset.getX(),
                offset.getY(),
                offset.getZ()
        );
    }
}


