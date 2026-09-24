package io.paper.permafrost.utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleefPlayer;

public class Utils {
	public static Location getLoc(String path, boolean pitch) {
		if (path == null || path.isEmpty()) {
			Bukkit.getLogger().warning("Provided path is null or empty: " + path);
			return null;
		}

		String[] locs = path.split(",");
		if (locs.length < 4) {
			Bukkit.getLogger().warning("Invalid location format, not enough values: " + path);
			return null;
		}

		// Try to get the world by name
		World world = Bukkit.getWorld(locs[0]);
		if (world == null) {
			Bukkit.getLogger().warning("World not found: " + locs[0]);
			return null; // Return null if the world isn't found
		}

		Bukkit.getLogger().info("Location world: " + world.getName());

		// Parse the coordinates and log them
		try {
			int x = Integer.parseInt(locs[1]);
			int y = Integer.parseInt(locs[2]);
			int z = Integer.parseInt(locs[3]);

			// Log the parsed location
			Bukkit.getLogger().info("Parsed location: World=" + locs[0] + ", X=" + x + ", Y=" + y + ", Z=" + z);

			// Return the location without pitch if pitch is false
			if (!pitch) {
				Location loc = new Location(world, x, y, z);
				loc.add(0.5D, 0.0D, 0.5D); // Adjust position
				return loc;
			}

			// Handle pitch if necessary
			if (locs.length < 6) {
				Bukkit.getLogger().warning("Invalid location format, missing pitch/yaw values: " + path);
				return null; // Return null if the format is incorrect
			}

			float yaw = Float.parseFloat(locs[4]);
			float pitchVal = Float.parseFloat(locs[5]);

			Location loc = new Location(world, x, y, z, yaw, pitchVal);
			loc.add(0.5D, 0.0D, 0.5D); // Adjust position
			return loc;

		} catch (NumberFormatException e) {
			// Catch any number parsing errors
			Bukkit.getLogger().warning("Invalid number format in location: " + path);
			return null;
		}
	}

	public static Location getLoc(String path) {
		return getLoc(path, false); // Use the first method by default with pitch=false
	}

	public static <T> boolean hasDuplicate(Iterable<T> all) {
		Set<T> set = new HashSet<T>();
		for (T each : all) {
			if (!set.add(each))
				return true;
		}
		return false;
	}

	public static String getPlayerNamesFromList(List<SpleefPlayer> list) {
		String p = "";
		for (SpleefPlayer sp : list) {
			if (p.equalsIgnoreCase("")) {
				p = sp.getOfflinePlayer().getName();
			} else {
				p = p + ", " + sp.getOfflinePlayer().getName();
			}
		}

		return p;
	}

	public static void sendTitles(Main main, List<SpleefPlayer> players, String titletext, String subtitletext,
			int fadeIn, int stay, int fadeOut) {
		new BukkitRunnable() {
			public void run() {
				for (SpleefPlayer sp : players) {
					Player player = sp.getPlayer();
					player.sendTitle(titletext, subtitletext, fadeIn, stay, fadeOut);

				}
			}
		}.runTask(main);
	}

	public static String getString(ConfigurationSection arenaSec, String key) {
		if (arenaSec == null) {
			Bukkit.getLogger().warning("getString: arena section is null");
			return null;
		}
		String val = arenaSec.getString(key);
		if (val == null) {
			Bukkit.getLogger()
					.warning("getString: missing key '" + key + "' in arena section '" + arenaSec.getName() + "'");
		}
		return val;
	}

	/** Get the World from the arena section's "world" name. */
	public static World getWorld(ConfigurationSection arenaSec) {
		String worldName = getString(arenaSec, "world");
		if (worldName == null)
			return null;
		World w = Bukkit.getWorld(worldName);
		if (w == null) {
			Bukkit.getLogger().warning("getWorld: World not loaded or not found: " + worldName);
		}
		return w;
	}

	/** Get the schematic path (string) from "schematic". */
	public static String getSchematic(ConfigurationSection arenaSec) {
		return getString(arenaSec, "schematic");
	}

	/** Get the paste origin as a Location using world + pasteOrigin{x,y,z}. */
	public static Location getPasteOrigin(ConfigurationSection arenaSec) {
		if (arenaSec == null)
			return null;
		World w = getWorld(arenaSec);
		if (w == null)
			return null;

		ConfigurationSection po = arenaSec.getConfigurationSection("pasteOrigin");
		if (po == null) {
			Bukkit.getLogger().warning("getPasteOrigin: pasteOrigin section missing");
			return null;
		}

		int x = po.getInt("x");
		int y = po.getInt("y");
		int z = po.getInt("z");
		return new Location(w, x, y, z);
	}

	/** Get team amount = number of entries under "teams". */
	public static int getTeamAmount(ConfigurationSection arenaSec) {
		if (arenaSec == null)
			return 0;
		List<?> teams = arenaSec.getList("teams");
		return teams == null ? 0 : teams.size();
	}

	/**
	 * Get relative spawn offsets (Vectors) from all teams' spawns. These are
	 * offsets from pasteOrigin, matching your YAML spawns {x,y,z}.
	 */
	public static List<Vector> getOffsets(ConfigurationSection arenaSec) {
		List<Vector> out = new ArrayList<>();
		if (arenaSec == null)
			return out;

		List<?> teams = arenaSec.getList("teams");
		if (teams == null)
			return out;

		for (int i = 0; i < teams.size(); i++) {
			Object raw = teams.get(i);
			if (!(raw instanceof ConfigurationSection)) {
				// Spigot may return a Map for inline lists; handle both
				@SuppressWarnings("unchecked")
				java.util.Map<String, Object> map = (java.util.Map<String, Object>) raw;
				Object spawnsObj = map.get("spawns");
				if (spawnsObj instanceof List<?> sList) {
					for (Object entry : sList) {
						if (entry instanceof java.util.Map<?, ?> sm) {
							int x = asInt(sm.get("x"));
							int y = asInt(sm.get("y"));
							int z = asInt(sm.get("z"));
							out.add(new Vector(x, y, z));
						}
					}
				}
				continue;
			}

			ConfigurationSection teamSec = (ConfigurationSection) raw;
			List<?> spawns = teamSec.getList("spawns");
			if (spawns == null)
				continue;

			for (Object entry : spawns) {
				if (entry instanceof ConfigurationSection sSec) {
					int x = sSec.getInt("x");
					int y = sSec.getInt("y");
					int z = sSec.getInt("z");
					out.add(new Vector(x, y, z));
				} else if (entry instanceof java.util.Map<?, ?> sm) {
					int x = asInt(sm.get("x"));
					int y = asInt(sm.get("y"));
					int z = asInt(sm.get("z"));
					out.add(new Vector(x, y, z));
				}
			}
		}
		return out;
	}

	/** Helper to coerce map values to int cleanly. */
	private static int asInt(Object o) {
		if (o instanceof Number n)
			return n.intValue();
		if (o instanceof String s)
			return Integer.parseInt(s.trim());
		throw new IllegalArgumentException("Expected integer-like value, got: " + Objects.toString(o));
	}
}
