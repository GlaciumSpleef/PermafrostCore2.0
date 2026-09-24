package io.paper.permafrost.training;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.Arena;
import io.paper.permafrost.core.game.SpleggArena;
import io.paper.permafrost.core.game.TeamSpleefTeam;
import io.paper.permafrost.core.game.tasks.CountdownTask;

public class TrainingSession {
	private Set<UUID> players = new HashSet<>();
	private UUID leader;
	private TrainingType type;
	private Main main;
	private TrainingArena arena;
	private TrainingPlot plot;
	private BukkitRunnable timerTask;
	private double secondsElapsed;
	private int roundCount;
	private TrainingState state;
	private double averageRoundTime;
	private double totalRoundTime;
	private double bestRoundTime;
	private final Set<UUID> invited = new HashSet<>();

	public TrainingSession(UUID uuid, TrainingType type, Main main) {
		players.add(uuid);
		leader = uuid;
		this.type = type;
		this.main = main;
		setArena();
		startSession();
	}

	public UUID getLeader() {
		return leader;
	}

	public Set<UUID> getPlayers() {
		return players;
	}

	public void addPlayer(UUID uuid) {
		players.add(uuid);
	}

	public void removePlayer(UUID uuid) {
		players.remove(uuid);
	}

	public TrainingType getType() {
		return type;
	}

	public TrainingState getTrainingState() {
		return state;
	}

	public void setTrainingState(TrainingState state) {
		this.state = state;
	}

	public TrainingArena getArena() {
		return arena;
	}

	public TrainingPlot getPlot() {
		return plot;
	}

	public void setPlot(TrainingPlot plot) {
		this.plot = plot;
	}

	public void setRoundCount(int roundCount) {
		this.roundCount = roundCount;
	}

	public int getRoundCount() {
		return this.roundCount;
	}

	public void setArena() {
		List<TrainingArena> arenas = new ArrayList<>(main.getDataLoader().getTrainingArenas());
		if (arenas.isEmpty())
			return;
		// Try random first
		TrainingArena random2 = arenas.get(new Random().nextInt(arenas.size()));
		if (!random2.isInGame()) {
			this.arena = random2;
		} else {
			// Fallback: first suitable
			for (TrainingArena b : arenas) {
				if (!b.isInGame()) {
					this.arena = b;
					break;
				}
			}
		}
		if (this.arena != null)
			this.arena.setInGame(true);
	}

	@SuppressWarnings("deprecation")
	public void startSession() {
		if (type == null) {
			System.out.println("Instantiate the type.");
			return;
		}
		Player player = Bukkit.getPlayer(leader);
		switch (type) {
		case TrainingType.SPLEEF: {
			player.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + "Spleef training session started.\n"
					+ ChatColor.GRAY + "End the session with " + ChatColor.YELLOW + "/endsession" + ChatColor.GRAY
					+ " or invite others using " + ChatColor.YELLOW + "/invite <player>" + ChatColor.GRAY + ".");
			player.teleport(arena.getPlayerSpawn());
			giveStar(leader);
			break;
		}
		case TrainingType.SPLEGG: {

			break;
		}
		case TrainingType.PARKOUR: {

			break;
		}
		}
	}

	public void startTraining() {

		Player lead = Bukkit.getPlayer(leader);
		if (lead == null)
			return;

		Location pasteOrigin = arena.getPlotPasteLocation();

		int index = 1;

		if (plot.hasTimer()) {
			startTimer(main);
		}

		new TrainingCountdownTask(main, this);

		for (UUID uuid : players) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null)
				continue;

			PlayerInventory inventory = player.getInventory();

			ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
			ItemMeta meta = shovel.getItemMeta();
			meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
			meta.setUnbreakable(true);
			shovel.setItemMeta(meta);
			shovel.addItemFlags(ItemFlag.HIDE_ENCHANTS);

			if (!inventory.contains(shovel)) {
				inventory.addItem(shovel);
			}

			inventory.setItem(4, new ItemStack(Material.AIR));

		}
		int i = 0;
		for (UUID uuid : players) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null)
				continue;

			Location spawn = plot.resolveSpawn(pasteOrigin, index);
			Location loc = spawn.clone();

			if (!plot.hasOffset() && i == 0) {
				loc.set(loc.getX() - 0.5, loc.getY(), loc.getZ() - 0.5);
			loc.setYaw(plot.getYaw());
			}
			if (!plot.hasOffset() && i == 1) {
				loc.set(loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5);
				loc.setYaw((plot.getYaw()-180));
			}
			
			player.teleport(loc);
			index++;
			i++;
		}
	}

	public void endSession() {
		main.getDataLoader().addSessionAverage(leader, "reachomnisolo1", averageRoundTime);
		main.getDataLoader().updatePersonalBest(leader, "reachomnisolo1", bestRoundTime);
		Bukkit.getPlayer(leader)
				.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + "Training session complete. " + ChatColor.GRAY
						+ "Your best time: " + ChatColor.GOLD + formatSeconds(bestRoundTime) + "s" + ChatColor.GRAY
						+ "\nYour average time: " + ChatColor.BLUE + formatSeconds(averageRoundTime) + "s");
		main.getDataLoader().removePlayerFromTraining(leader);
		Bukkit.getPlayer(leader).performCommand("spawn");
		for (UUID uuid : players) {
			main.getDataLoader().removePlayerFromTraining(uuid);
			Bukkit.getPlayer(uuid).performCommand("spawn");
		}
		clearPlot();
		arena.setInGame(false);
		roundCount = 0;
	}

	public void giveStar(UUID uuid) {
		ItemStack star = new ItemStack(Material.NETHER_STAR);
		ItemMeta meta = star.getItemMeta();
		meta.setDisplayName("§5§lCustomizer");
		star.setItemMeta(meta);
		Bukkit.getPlayer(uuid).getInventory().setItem(8, star);
	}

	public double getSecondsElapsed() {
		return secondsElapsed;
	}

	public void startTimer(JavaPlugin plugin) {
		if (timerTask != null) {
			timerTask.cancel();
		}
		secondsElapsed = 0;
		timerTask = new BukkitRunnable() {
			@Override
			public void run() {
				secondsElapsed += 0.05;
			}
		};
		timerTask.runTaskTimer(plugin, 0L, 1L);
	}

	public void stopTimer(boolean premature) {
		if (!premature) {
			if (!(secondsElapsed >= 150)) {
				totalRoundTime += secondsElapsed;
				roundCount++;
				averageRoundTime = totalRoundTime / roundCount;
			}
			if (bestRoundTime == 0)
				bestRoundTime = secondsElapsed;
			if (bestRoundTime > secondsElapsed)
				bestRoundTime = secondsElapsed;

			Bukkit.getPlayer(leader).sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + "Round complete. "
					+ ChatColor.GRAY + "Time: " + ChatColor.GOLD + formatSeconds(secondsElapsed) + "s");
		} else {
			Bukkit.getPlayer(leader).sendMessage(ChatColor.RED + "✖ " + ChatColor.WHITE + "Round ended prematurely.");
		}
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	public static String formatSeconds(double seconds) {
		return String.format(Locale.US, "%.1f", seconds);
	}

	public void pastePlot() {
		File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", plot.getSchematic());
		main.getFAWEAPI().pasteSchematic(schemFile, arena.getPlotPasteLocation(), false);
	}

	public void clearPlot() {
		File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", "clearplot.schem");
		main.getFAWEAPI().pasteSchematic(schemFile, arena.getPlotPasteLocation(), false);
	}
	
	public Set<UUID> getInvited() {
	    return invited;
	}

	public void invite(UUID uuid) {
	    invited.add(uuid);
	}

	public void uninvite(UUID uuid) {
	    invited.remove(uuid);
	}

	public boolean isInvited(UUID uuid) {
	    return invited.contains(uuid);
	}
	public void consumeInvite(UUID uuid) {
	    invited.remove(uuid);
	}

	// leave, quit, endsession, endgame
	// time in session
	// drop shovel to reset
	// time inbetween first block broken and last, not between shovel drop

	// save PB
	// last 10 times in training session
	// upon drop, show that it was x amount faster/slower

	// timeattack, speedrun, fight, glacium
	// timer, snow layer
	// npc with presets, holograms with customizeable variables
	// /invite, /join, max players variable, certain presets with more/less players,
	// max max players 4
	// plot owners who can give permissions to change settings

	// admin commands
	// items in the players inventories
	// npcs in the lobby
	// clans
	// new lobby
	// admin starts tournament --> initiate tournament with /start tournament,
	// command to add players/remove, if teams, /team add
	// input scores manually into challonge after, with automatic scores reported in
	// discord channels/ announcements for winners
	// live 2v2 tournament starting now!! announcement
	// /tournament
	// tping to arenas
	// finish splegg
	// add parkour
	// add ranks and website
	// /nvn
	// translator
}
