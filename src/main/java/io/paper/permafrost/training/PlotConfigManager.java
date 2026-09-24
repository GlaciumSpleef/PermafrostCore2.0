package io.paper.permafrost.training;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class PlotConfigManager {

	private final File file;
	private final FileConfiguration config;

	public PlotConfigManager(JavaPlugin plugin) {
		this.file = new File(plugin.getDataFolder(), "training_plots.yml");

		if (!file.exists()) {
			try {
				plugin.getDataFolder().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Could not create training_plots.yml", e);
			}
		}

		this.config = YamlConfiguration.loadConfiguration(file);
	}

	public void createPlot(String name, String schematic, int maxPlayers) {
		String key = normalize(name);

		config.set("plots." + key + ".schematic", schematic);
		config.set("plots." + key + ".maxPlayers", maxPlayers);
		config.set("plots." + key + ".timer", false);
		config.set("plots." + key + ".offset", false);

		save();
	}

	public void setTimer(String name, boolean value) {
		config.set("plots." + normalize(name) + ".timer", value);
		save();
	}

	public void setOffset(String name, boolean value) {
		config.set("plots." + normalize(name) + ".offset", value);
		save();
	}
	
	public void setBlocksToBreak(String name, int blocks) {
	    config.set("plots." + normalize(name) + ".blocksToBreak", blocks);
	    save();
	}

	public void setSpawn(String name, int index, double x, double y, double z) {
		String base = "plots." + normalize(name) + ".spawns." + index;
		config.set(base + ".x", x);
		config.set(base + ".y", y);
		config.set(base + ".z", z);
		save();
	}

	public boolean plotExists(String name) {
		return config.contains("plots." + normalize(name));
	}

	private void save() {
		try {
			config.save(file);
		} catch (IOException e) {
			throw new RuntimeException("Could not save training_plots.yml", e);
		}
	}

	private String normalize(String name) {
		return name.toLowerCase();
	}
}
