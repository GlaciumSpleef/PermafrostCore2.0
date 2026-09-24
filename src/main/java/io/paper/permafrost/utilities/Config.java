package io.paper.permafrost.utilities;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Config {
	private File file;

	private FileConfiguration fileConfig;

	public Config(String fileName, Plugin plugin) {
		if (!fileName.contains(".yml"))
			fileName = String.valueOf(fileName) + ".yml";
		this.file = new File(plugin.getDataFolder(), fileName);
		if (!this.file.exists()) {
			this.file.getParentFile().mkdirs();
			plugin.saveResource(fileName, false);
		}
		this.fileConfig = (FileConfiguration) new YamlConfiguration();
		try {
			this.fileConfig.load(this.file);
		} catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
			e.printStackTrace();
		}
		if (!this.file.exists())
			try {
				this.fileConfig.save(this.file);
			} catch (IOException exception) {
				exception.printStackTrace();
			}
	}

	public FileConfiguration getConfig() {
		return this.fileConfig;
	}

	public void saveConfig() {
		try {
			this.fileConfig.save(this.file);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public void setLocation(String path, Location location) {
		this.fileConfig.set(String.valueOf(path) + ".World", location.getWorld().getName());
		this.fileConfig.set(String.valueOf(path) + ".X", Double.valueOf(location.getX()));
		this.fileConfig.set(String.valueOf(path) + ".Y", Double.valueOf(location.getY()));
		this.fileConfig.set(String.valueOf(path) + ".Z", Double.valueOf(location.getZ()));
		this.fileConfig.set(String.valueOf(path) + ".Pitch", Float.valueOf(location.getPitch()));
		this.fileConfig.set(String.valueOf(path) + ".Yaw", Float.valueOf(location.getYaw()));
		saveConfig();
	}

	public Location getLocation(String path) {
		if (this.fileConfig.getString(String.valueOf(path) + ".World") == null)
			return null;
		Location location = new Location(Bukkit.getWorld(this.fileConfig.getString(String.valueOf(path) + ".World")),
				this.fileConfig.getDouble(String.valueOf(path) + ".X"),
				this.fileConfig.getDouble(String.valueOf(path) + ".Y"),
				this.fileConfig.getDouble(String.valueOf(path) + ".Z"),
				(float) this.fileConfig.getDouble(String.valueOf(path) + ".Yaw"),
				(float) this.fileConfig.getDouble(String.valueOf(path) + ".Pitch"));
		return location;
	}
}
