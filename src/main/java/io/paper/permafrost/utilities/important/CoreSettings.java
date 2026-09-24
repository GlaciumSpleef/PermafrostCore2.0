package io.paper.permafrost.utilities.important;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.ShovelPowers;

public class CoreSettings {
	private final static CoreSettings instance = new CoreSettings();
	private File file;
	private YamlConfiguration config;
	
	private ShovelPowers powerType;
	
	private CoreSettings() {
	}
	
	public void load() {
		file = new File(Main.getInstance().getDataFolder(), "settings.yml");
		if(!file.exists()) {
			Main.getInstance().saveResource("settings.yml", false);
		}
		
		config = new YamlConfiguration();
		config.options().parseComments(true);
		
		try {
			config.load(file);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		powerType = ShovelPowers.valueOf(config.getString("Powers.Power_Type"));
	}
	
	public void save() {
		try {
			config.save(file);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void set(String path, Object value) {
		config.set(path, value);
		save();
	}
	
	public ShovelPowers getShovelPower() {
		return powerType;
	}
	
	public void setShovelPower(ShovelPowers powerType) {
		this.powerType = powerType;
		
		set("Powers.Power_Type", powerType.name());
	}
	
	public static CoreSettings getInstance() {
		return instance;
	}
}
