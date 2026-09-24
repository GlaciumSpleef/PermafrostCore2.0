package io.paper.permafrost.core.game;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import github.scarsz.discordsrv.DiscordSRV;
import io.paper.permafrost.Main;
import io.paper.permafrost.utilities.PurchasableRank;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import github.scarsz.discordsrv.DiscordSRV;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class SpleggQueueManager {

	private BukkitRunnable timerTask;
	private BukkitRunnable countdownTask;

	private int secondsElapsed;
	private final Main main;

	private boolean inGame;
	private boolean gameStarting;
	private boolean inMapSelection;
	private int secondsTillGame;
	private boolean gameEnding;
	private SpleggFFAArena voteSmallArena;
	private SpleggFFAArena voteMediumArena;
	private SpleggFFAArena voteLargeArena;

	// true = inGame, false = inQueue
	private final Map<UUID, Boolean> players = new HashMap<>();

	// true = alive, false = dead
	private final Map<UUID, Boolean> playersAliveInGame = new HashMap<>();

	private final Set<UUID> playersSpectating = new HashSet<>();

	// arenaId -> votes
	private final Map<String, Integer> mapVotes = new HashMap<>();

	// player -> arenaId
	private final Map<UUID, String> playerVotes = new HashMap<>();

	private SpleggFFAArena arena;

	public SpleggQueueManager(Main main) {
		this.main = main;
	}

	public boolean isInGame() {
		return this.inGame;
	}

	public boolean isSelectingMap() {
		return this.inMapSelection;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public boolean isGameStarting() {
		return this.gameStarting;
	}

	public void addSpectator(UUID uuid) {
		playersSpectating.add(uuid);
	}

	public Set<UUID> getPlayersSpectating() {
		return playersSpectating;
	}

	public void removeSpectator(UUID uuid) {
		playersSpectating.remove(uuid);
	}

	public void removePlayerFromGame(UUID uuid) {
		playersAliveInGame.remove(uuid);
	}

	public Boolean getInFFA(UUID playerUUID) {
		return players.get(playerUUID);
	}

	public void setInFFA(UUID playerUUID, boolean inFFA) {
		players.put(playerUUID, inFFA);
	}

	public void addPlayer(UUID playerUUID, Boolean inFFA) {
		players.put(playerUUID, inFFA);
		tryStartQueueCountdown();
	}

	public String getTimeOngoing() {
		int minutes = secondsElapsed / 60;
		int seconds = secondsElapsed % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	public void removePlayer(UUID playerUUID) {
		players.remove(playerUUID);
		playerVotes.remove(playerUUID);
		playersAliveInGame.remove(playerUUID);
		playersSpectating.remove(playerUUID);

		reevaluateQueueAfterLeave();
	}

	private void reevaluateQueueAfterLeave() {
		if (inGame || gameEnding) {
			return;
		}

		int waiting = getWaitingPlayers();

		if (waiting < 2) {
			cancelCountdownTask();
			arena = null;
			inGame = false;
			inMapSelection = false;
			gameStarting = false;
			secondsTillGame = 0;
			resetVotes();

			for (UUID uuid : players.keySet()) {
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					p.sendMessage(color("&c&lQueue Cancelled &8» &7Not enough players to start."));
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
				}
			}
			return;
		}

		if (gameStarting && inMapSelection) {
			sendVoteMenu();
		}
	}
	
	private int getWaitingPlayers() {
		int count = 0;
		for (UUID uuid : players.keySet()) {
			if (!Boolean.TRUE.equals(players.get(uuid))) {
				count++;
			}
		}
		return count;
	}

	private int getPregameCountdownSeconds() {
		return getWaitingPlayers() >= 6 ? 15 : 30;
	}

	public void tryStartQueueCountdown() {
		if (inGame || gameEnding || gameStarting) {
			return;
		}

		if (getWaitingPlayers() < 2) {
			return;
		}

		startCooldown();
	}

	private boolean shouldForceVoteStartNow() {
		if (players.size() < 2) {
			return false;
		}

		int voteStartVotes = 0;

		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null) {
				continue;
			}

			// Replace this with your real check if you store votestart votes somewhere else.
			// This fallback assumes you have some metadata or other way of checking.
			if (p.hasMetadata("splegg_votestart")) {
				voteStartVotes++;
			}
		}

		// 2/2, 2/3, 3/4, etc.
		return voteStartVotes >= Math.max(2, players.size() - 1);
	}

	public SpleggFFAArena getArena() {
		return arena;
	}
	
	public void faceLocation(Player player, Location target) {
		Location playerLoc = player.getLocation();
		Location targetLoc = target.clone().add(0.5, 0.5, 0.5);

		double dx = targetLoc.getX() - playerLoc.getX();
		double dz = targetLoc.getZ() - playerLoc.getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

		playerLoc.setYaw(yaw);
		player.teleport(playerLoc);
	}

	public void setDead(UUID uuid) {
		playersAliveInGame.put(uuid, false);
	}

	public Boolean isDead(UUID uuid) {
		return playersAliveInGame.get(uuid);
	}

	public Map<UUID, Boolean> getPlayersAliveInGame() {
		return playersAliveInGame;
	}

	public int getPlayersAlive() {
		int count = 0;
		for (UUID uuid : playersAliveInGame.keySet()) {
			if (Boolean.TRUE.equals(playersAliveInGame.get(uuid))) {
				count++;
			}
		}
		return count;
	}

	public int getPlayerCount() {
		return players.size();
	}

	public int getSecondsTillGame() {
		return secondsTillGame;
	}

	public int getPlayersRemaining() {
		int count = 0;
		for (UUID uuid : playersAliveInGame.keySet()) {
			if (Boolean.TRUE.equals(playersAliveInGame.get(uuid))) {
				count++;
			}
		}
		return count;
	}

	public Map<UUID, Boolean> getPlayers() {
		return players;
	}

	public boolean isGameEnding() {
		return gameEnding;
	}

	public String getSize() {
		int playerCount = 0;

		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				playerCount++;
			}
		}

		if (playerCount < 8) {
			return "small";
		} else if (playerCount <= 12) {
			return "medium";
		} else {
			return "large";
		}
	}

	public int getPlayersInFFA() {
		int count = 0;
		for (boolean inFFA : players.values()) {
			if (inFFA) {
				count++;
			}
		}
		return count;
	}

	private void cancelCountdownTask() {
		if (countdownTask != null) {
			countdownTask.cancel();
			countdownTask = null;
		}
	}

	public void startCooldown() {
		cancelCountdownTask();

		gameStarting = true;
		inMapSelection = true;
		resetVotes();

		int totalSeconds = getPregameCountdownSeconds();
		secondsTillGame = totalSeconds;

		broadcastQueueMessage("&a&lMatch Found! &7Map voting has started.");
		if (totalSeconds == 30) {
			broadcastQueueMessage("&7Game starts in &e30 seconds&7.");
		} else {
			broadcastQueueMessage("&7Game starts in &e15 seconds&7.");
		}

		playQueueSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
		sendQueueTitle("&aMap Voting", "&7Vote for the next map");
		sendVoteMenu();

		countdownTask = new BukkitRunnable() {
			int seconds = totalSeconds;

			@Override
			public void run() {
				int waiting = getWaitingPlayers();

				if (waiting < 2) {
					broadcastQueueMessage("&c&lSplegg FFA &8» &cNot enough players. Countdown cancelled.");
					playQueueSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);

					arena = null;
					inGame = false;
					inMapSelection = false;
					gameStarting = false;
					secondsTillGame = 0;
					countdownTask = null;
					resetVotes();
					cancel();
					return;
				}

				secondsTillGame = seconds;

				if (seconds == totalSeconds || seconds == 20 || seconds == 10 || seconds == 5) {
					sendVoteMenu();
				}

				if (seconds <= 5 && seconds >= 1) {
					sendCountdownState(seconds);
				}

				if (seconds == 0) {
					SpleggFFAArena winningArena = getWinningArena();
					inMapSelection = false;

					if (winningArena != null) {
						broadcastQueueMessage("&b&lMap Selected! &7" + winningArena.getId());
						playQueueSound(Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.4f);
						teleportPlayersToArena(winningArena);
						arena = winningArena;
						inGame = true;
						startFFAGame();
					} else {
						broadcastQueueMessage("&c&lMap Error &8» &cNo valid arena was selected.");
						playQueueSound(Sound.BLOCK_ANVIL_LAND, 0.7f, 1.0f);
						gameStarting = false;
						inMapSelection = false;
					}

					countdownTask = null;
					cancel();
					return;
				}

				seconds--;
			}
		};

		countdownTask.runTaskTimer(main, 0L, 20L);
	}

	public void startFFAGame() {
		if (arena != null) {
			File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", arena.getSchematic());
			main.getFAWEAPI().pasteSchematic(schemFile, arena.getPasteOrigin(), false);
		}

		Set<UUID> playersInFFA = new HashSet<>();
		for (UUID uuid : players.keySet()) {
			if (Boolean.TRUE.equals(players.get(uuid))) {
				playersInFFA.add(uuid);
				playersAliveInGame.put(uuid, true);
			}
		}

		new BukkitRunnable() {
			int seconds = 0;

			@Override
			public void run() {
				seconds++;

				if (seconds == 1) {
					broadcastGameMessage("&b&lSplegg FFA &8» &7Get ready...");
					sendGameTitle("&bSplegg FFA", "&7Match starting");
					playPlayersSound(playersInFFA, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
				}

				if (seconds == 2) {
					sendPlayersActionBar(playersInFFA, "&7Your shovel unlocks in &e3");
					sendPlayersTitle(playersInFFA, "&e3", "&7Prepare to fire", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.5f);
				}

				if (seconds == 3) {
					sendPlayersActionBar(playersInFFA, "&7Your shovel unlocks in &e2");
					sendPlayersTitle(playersInFFA, "&62", "&7Prepare to fire", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.6f);
				}

				if (seconds == 4) {
					sendPlayersActionBar(playersInFFA, "&7Your shovel unlocks in &c1");
					sendPlayersTitle(playersInFFA, "&c1", "&7Prepare to fire", 0, 15, 5);
					playPlayersSound(playersInFFA, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, 1.7f);
				}

				if (seconds == 5) {
					sendPlayersTitle(playersInFFA, "&a&lGO!", "&7Start shooting!", 0, 20, 10);
					sendPlayersActionBar(playersInFFA, "&a&lFight!");
					playPlayersSound(playersInFFA, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f, 1.2f);

					secondsTillGame = 0;
					startTimer(main);

					for (UUID uuid : playersInFFA) {
						Player p = Bukkit.getPlayer(uuid);
						if (p == null || !p.isOnline()) {
							continue;
						}

						PlayerInventory inventory = p.getInventory();
						inventory.clear();

						ItemStack shovel = new ItemStack(Material.GOLDEN_SHOVEL);
						ItemMeta meta = shovel.getItemMeta();
						if (meta != null) {
							meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
							meta.setUnbreakable(true);
							meta.setDisplayName(color("&9Splegg Gun"));
							meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
							shovel.setItemMeta(meta);
						}

						if (!inventory.contains(Material.GOLDEN_SHOVEL)) {
							inventory.addItem(shovel);
						}
					}

					gameStarting = false;
					cancel();
				}
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	public void endFFAGame() {
		gameEnding = true;

		Player winner = null;
		for (UUID uuid : playersAliveInGame.keySet()) {
			if (Boolean.TRUE.equals(playersAliveInGame.get(uuid))) {
				createPlayerStatsIfNotExists(main.getConnection(), uuid);
				addWin(main.getConnection(), uuid, 1);
				Player p = Bukkit.getPlayer(uuid);
				if (p != null) {
					winner = p;
					if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.CUSTOM)) {
						addPoints(main.getConnection(), uuid, 200);
						p.sendTitle(color("&6&lVICTORY!"), color("&7+200 shells"), 10, 50, 15);
					} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.POLAR)) {
						addPoints(main.getConnection(), uuid, 175);
						p.sendTitle(color("&6&lVICTORY!"), color("&7+175 shells"), 10, 50, 15);
					} else if (main.getDataLoader().hasPurchasableRank(uuid, PurchasableRank.GLACIER)) {
						addPoints(main.getConnection(), uuid, 150);
						p.sendTitle(color("&6&lVICTORY!"), color("&7+150 shells"), 10, 50, 15);
					} else {
						addPoints(main.getConnection(), uuid, 100);
						p.sendTitle(color("&6&lVICTORY!"), color("&7+100 shells"), 10, 50, 15);
					}
					p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
				}
			}
		}

		announceFFAWinner(winner);

		new BukkitRunnable() {
			int seconds = 0;

			@Override
			public void run() {
				seconds++;

				if (seconds == 5) {
					if (arena != null) {
						File schemFile = new File("plugins/FastAsyncWorldEdit/schematics", arena.getSchematic());
						main.getFAWEAPI().pasteSchematic(schemFile, arena.getPasteOrigin(), false);
					}

					for (UUID uuid : players.keySet()) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null && p.isOnline() && playersAliveInGame.containsKey(uuid)) {
							resetPlayerToLobby(p);
						}
					}

					for (UUID uuid : playersSpectating) {
						Player p = Bukkit.getPlayer(uuid);
						if (p != null && p.isOnline()) {
							resetPlayerToLobby(p);
						}
					}

					playersSpectating.clear();
					playersAliveInGame.clear();
					gameStarting = false;
					secondsTillGame = 0;
					stopTimer();
					arena = null;
					inGame = false;
					gameEnding = false;
					cancelCountdownTask();
					tryStartQueueCountdown();
					cancel();
				}
			}
		}.runTaskTimer(main, 0L, 20L);
	}

	private void resetPlayerToLobby(Player p) {
		p.setGameMode(GameMode.ADVENTURE);
		p.getInventory().clear();

		ItemStack slimeball = new ItemStack(Material.SLIME_BALL);
		ItemMeta leaveMeta = slimeball.getItemMeta();
		if (leaveMeta != null) {
			leaveMeta.addEnchant(Enchantment.EFFICIENCY, 5, true);
			leaveMeta.setUnbreakable(true);
			leaveMeta.setDisplayName(color("&cLeave"));
			leaveMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
			slimeball.setItemMeta(leaveMeta);
		}
		p.getInventory().setItem(8, slimeball);

		Location loc = new Location(Bukkit.getWorld("spleggffa"), 44, 66.5, 33);
		loc.setYaw(90f);
		p.teleport(loc);
		p.setBodyYaw(90f);

		addPlayer(p.getUniqueId(), false);

		int waiting = getWaitingPlayers();
		p.sendMessage(color("&a&lQueue Joined &8» &7You are now in the Splegg FFA queue. &8(&e" + waiting + " waiting&8)"));
		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
	}

	public void handleVote(UUID uuid, String arenaId) {
		if (!gameStarting || !inMapSelection) {
			return;
		}

		SpleggFFAArena arena = getArenaById(arenaId);
		if (arena == null) {
			return;
		}

		String oldVote = playerVotes.get(uuid);
		if (oldVote != null) {
			mapVotes.put(oldVote, Math.max(0, mapVotes.getOrDefault(oldVote, 0) - 1));
		}

		playerVotes.put(uuid, arenaId);
		mapVotes.put(arenaId, mapVotes.getOrDefault(arenaId, 0) + 1);

		Player player = Bukkit.getPlayer(uuid);
		if (player != null) {
			player.sendMessage(color("&a&lVote Registered &8» &7You voted for &e" + arenaId + "&7."));
			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.3f);
		}

		sendVoteMenu();
	}

	@SuppressWarnings("deprecation")
	public void sendVoteMenu() {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null) {
				continue;
			}

			p.sendMessage("");
			p.sendMessage(ChatColor.GREEN + "Vote for your favorite map! The game will begin in " + ChatColor.YELLOW
					+ secondsTillGame + ChatColor.GREEN + " seconds.");

			int arenaAmount = 0;
			List<SpleggFFAArena> arenas = new ArrayList<>();

			if (voteSmallArena != null) {
				arenas.add(voteSmallArena);
			}
			if (voteMediumArena != null) {
				arenas.add(voteMediumArena);
			}
			if (voteLargeArena != null) {
				arenas.add(voteLargeArena);
			}

			for (SpleggFFAArena arena : arenas) {
				arenaAmount++;

				int voteCount = mapVotes.getOrDefault(arena.getId(), 0);

				TextComponent line = null;
				switch (arenaAmount) {
				case 1: {
					line = new TextComponent(ChatColor.WHITE + "" + arenaAmount + ". " + ChatColor.GREEN + ChatColor.BOLD + "SMALL "
							+ ChatColor.WHITE + ChatColor.BOLD + "[" + ChatColor.YELLOW + ChatColor.BOLD + arena.getId()
							+ ChatColor.WHITE + ChatColor.BOLD + "]" + ChatColor.GRAY + " (" + voteCount + " votes)");
					break;
				}
				case 2: {
					line = new TextComponent(ChatColor.WHITE + "" + arenaAmount + ". " + ChatColor.YELLOW + ChatColor.BOLD + "MEDIUM "
							+ ChatColor.WHITE + ChatColor.BOLD + "[" + ChatColor.YELLOW + ChatColor.BOLD + arena.getId()
							+ ChatColor.WHITE + ChatColor.BOLD + "]" + ChatColor.GRAY + " (" + voteCount + " votes)");
					break;
				}
				case 3: {
					line = new TextComponent(ChatColor.WHITE + "" + arenaAmount + ". " + ChatColor.RED + ChatColor.BOLD + "LARGE "
							+ ChatColor.WHITE + ChatColor.BOLD + "[" + ChatColor.YELLOW + ChatColor.BOLD + arena.getId()
							+ ChatColor.WHITE + ChatColor.BOLD + "]" + ChatColor.GRAY + " (" + voteCount + " votes)");
					break;
				}
				}

				if (line == null) {
					continue;
				}

				line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote " + arena.getId()));
				line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						new ComponentBuilder(ChatColor.GREEN + "Click to vote for " + arena.getId()).create()));

				p.spigot().sendMessage(line);
			}

			p.sendMessage("");
		}
	}

	private void resetVotes() {
		mapVotes.clear();
		playerVotes.clear();

		List<SpleggFFAArena> smallArenas = new ArrayList<>();
		List<SpleggFFAArena> mediumArenas = new ArrayList<>();
		List<SpleggFFAArena> largeArenas = new ArrayList<>();

		for (SpleggFFAArena arena : main.getDataLoader().getSpleggFFAArenas()) {
			if (arena == null || arena.getSize() == null) {
				continue;
			}

			switch (arena.getSize().toLowerCase()) {
			case "small":
				smallArenas.add(arena);
				break;
			case "medium":
				mediumArenas.add(arena);
				break;
			case "large":
				largeArenas.add(arena);
				break;
			}
		}

		voteSmallArena = smallArenas.isEmpty() ? null : smallArenas.get((int) (Math.random() * smallArenas.size()));
		voteMediumArena = mediumArenas.isEmpty() ? null : mediumArenas.get((int) (Math.random() * mediumArenas.size()));
		voteLargeArena = largeArenas.isEmpty() ? null : largeArenas.get((int) (Math.random() * largeArenas.size()));

		if (voteSmallArena != null) {
			mapVotes.put(voteSmallArena.getId(), 0);
		}
		if (voteMediumArena != null) {
			mapVotes.put(voteMediumArena.getId(), 0);
		}
		if (voteLargeArena != null) {
			mapVotes.put(voteLargeArena.getId(), 0);
		}
	}

	public void announceFFAWinner(Player winner) {
		if (winner == null) {
			return;
		}

		int queueSize = getPlayerCount();

		String border = color("&8&m----------------------------------------");
		Bukkit.broadcastMessage(border);
		Bukkit.broadcastMessage(color("&6&lSplegg FFA"));
		Bukkit.broadcastMessage(color("&eWinner: &f" + winner.getName()));
		Bukkit.broadcastMessage(color("&ePlayers: &f" + queueSize));
		Bukkit.broadcastMessage(border);

		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
			}
		}
		for (UUID uuid : playersSpectating) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
			}
		}

		String discordMessage = "----------------------------------------\n" + "**SPLEGG FFA**\n" + "Winner: "
				+ winner.getName() + "\n" + "Players: " + queueSize + "\n" + "----------------------------------------";

		sendDiscordMessage(discordMessage);
	}

	private void sendDiscordMessage(String message) {
		DiscordSRV.getPlugin().getMainTextChannel().sendMessage(message).queue();
	}

	private SpleggFFAArena getArenaById(String arenaId) {
		for (SpleggFFAArena arena : main.getDataLoader().getSpleggFFAArenas()) {
			if (arena.getId().equalsIgnoreCase(arenaId)) {
				return arena;
			}
		}
		return null;
	}

	private SpleggFFAArena getWinningArena() {
		SpleggFFAArena winner = null;
		int highestVotes = -1;

		for (SpleggFFAArena arena : main.getDataLoader().getSpleggFFAArenas()) {
			if (arena.getSize() != null) {
				int votes = mapVotes.getOrDefault(arena.getId(), 0);
				if (votes > highestVotes) {
					highestVotes = votes;
					winner = arena;
				}
			}
		}

		if (winner == null) {
			for (SpleggFFAArena arena : main.getDataLoader().getSpleggFFAArenas()) {
				if (arena.getSize() != null) {
					return arena;
				}
			}
		}

		return winner;
	}

	public void startTimer(JavaPlugin plugin) {
		if (timerTask != null) {
			timerTask.cancel();
		}

		secondsElapsed = 0;

		timerTask = new BukkitRunnable() {
			@Override
			public void run() {
				secondsElapsed++;
			}
		};

		timerTask.runTaskTimer(plugin, 0L, 20L);
	}

	public void stopTimer() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	private void teleportPlayersToArena(SpleggFFAArena arena) {
		if (arena == null || arena.getSpawns() == null || arena.getSpawns().isEmpty()) {
			return;
		}

		List<Location> spawnList = new ArrayList<>(arena.getSpawns());
		Collections.shuffle(spawnList);

		int index = 0;

		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p == null) {
				continue;
			}

			if (index >= spawnList.size()) {
				index = 0;
			}

			Location spawn = spawnList.get(index).clone().add(-0.5, 0, -0.5);
			p.teleport(spawn);
			if(arena.getId().equalsIgnoreCase("cars") || arena.getId().equalsIgnoreCase("autumn")) {
				faceLocation(p, arena.getPasteOrigin());
			}
			setInFFA(uuid, true);
			p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.3f);
			index++;
		}
	}

	public void addPoints(Connection connection, UUID uuid, int amount) {
		String sql = "UPDATE splegg_ffa_points SET points = points + ? WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, amount);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addWin(Connection connection, UUID uuid, int amount) {
		String sql = "UPDATE splegg_ffa_wins SET wins = wins + ? WHERE uuid = ?";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, amount);
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createPlayerStatsIfNotExists(Connection connection, UUID uuid) {
		String uuidString = uuid.toString();

		String insertBlocks = "INSERT IGNORE INTO splegg_ffa_blocks_broken (uuid, blocks_broken) VALUES (?, 0)";
		String insertEggs = "INSERT IGNORE INTO splegg_ffa_eggs_fired (uuid, eggs_fired) VALUES (?, 0)";
		String insertWins = "INSERT IGNORE INTO splegg_ffa_wins (uuid, wins) VALUES (?, 0)";
		String insertPoints = "INSERT IGNORE INTO splegg_ffa_points (uuid, points) VALUES (?, 0)";

		try (PreparedStatement ps1 = connection.prepareStatement(insertBlocks);
				PreparedStatement ps2 = connection.prepareStatement(insertEggs);
				PreparedStatement ps3 = connection.prepareStatement(insertWins);
				PreparedStatement ps4 = connection.prepareStatement(insertPoints)) {

			ps1.setString(1, uuidString);
			ps2.setString(1, uuidString);
			ps3.setString(1, uuidString);
			ps4.setString(1, uuidString);

			ps1.executeUpdate();
			ps2.executeUpdate();
			ps3.executeUpdate();
			ps4.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	private void broadcastQueueMessage(String message) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendMessage(color(message));
			}
		}
	}

	private void broadcastGameMessage(String message) {
		for (UUID uuid : playersAliveInGame.keySet()) {
			if (!Boolean.TRUE.equals(playersAliveInGame.get(uuid))) {
				continue;
			}

			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendMessage(color(message));
			}
		}
	}

	private void broadcastAllParticipantsMessage(String message) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendMessage(color(message));
			}
		}
		for (UUID uuid : playersSpectating) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendMessage(color(message));
			}
		}
	}

	private void playQueueSound(Sound sound, float volume, float pitch) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
	}

	private void playPlayersSound(Set<UUID> uuids, Sound sound, float volume, float pitch) {
		for (UUID uuid : uuids) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
	}

	private void playAllParticipantsSound(Sound sound, float volume, float pitch) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
		for (UUID uuid : playersSpectating) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.playSound(p.getLocation(), sound, volume, pitch);
			}
		}
	}

	private void sendQueueActionBar(String message) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
			}
		}
	}

	private void sendPlayersActionBar(Set<UUID> uuids, String message) {
		for (UUID uuid : uuids) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
			}
		}
	}

	private void sendQueueTitle(String title, String subtitle) {
		for (UUID uuid : players.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendTitle(color(title), color(subtitle), 10, 40, 10);
			}
		}
	}

	private void sendGameTitle(String title, String subtitle) {
		for (UUID uuid : playersAliveInGame.keySet()) {
			if (!Boolean.TRUE.equals(playersAliveInGame.get(uuid))) {
				continue;
			}

			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendTitle(color(title), color(subtitle), 10, 40, 10);
			}
		}
	}

	private void sendPlayersTitle(Set<UUID> uuids, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		for (UUID uuid : uuids) {
			Player p = Bukkit.getPlayer(uuid);
			if (p != null) {
				p.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
			}
		}
	}

	private void sendCountdownState(int number) {
		String colorNumber;
		float pitch;

		switch (number) {
		case 5:
			colorNumber = "&a&l5";
			pitch = 1.0f;
			break;
		case 4:
			colorNumber = "&2&l4";
			pitch = 1.1f;
			break;
		case 3:
			colorNumber = "&e&l3";
			pitch = 1.2f;
			break;
		case 2:
			colorNumber = "&6&l2";
			pitch = 1.4f;
			break;
		default:
			colorNumber = "&c&l1";
			pitch = 1.6f;
			break;
		}

		sendQueueActionBar("&7Game starting in " + colorNumber);
		sendQueueTitle(colorNumber, "&7Get ready");
		playQueueSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
	}
}