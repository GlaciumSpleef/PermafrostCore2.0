package io.paper.permafrost;


import java.io.File;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import com.comphenix.protocol.ProtocolLib;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import io.paper.permafrost.commands.*;
import io.paper.permafrost.core.game.tasks.TaskListener;
import io.paper.permafrost.core.game.tasks.TeamSpleefListener;
import io.paper.permafrost.gui.Board;
import io.paper.permafrost.gui.ShovelPowersManager;
import io.paper.permafrost.managers.CosmeticRankManager;
import io.paper.permafrost.managers.DuelMenuManager;
import io.paper.permafrost.managers.PlayerBubbleManager;
import io.paper.permafrost.managers.RankedSpleefManager;
import io.paper.permafrost.managers.RankedSpleggManager;
import io.paper.permafrost.managers.StatsManager;
import io.paper.permafrost.parkour.ParkourListener;
import io.paper.permafrost.parkour.RankedParkourManager;
//import io.paper.permafrost.gui.Board;
import io.paper.permafrost.server.FAWEAPI;
import io.paper.permafrost.server.VoidWorldGenerator;
import io.paper.permafrost.training.EndSessionCommand;
import io.paper.permafrost.training.InviteCommand;
import io.paper.permafrost.training.JoinCommand;
import io.paper.permafrost.training.LeaveCommand;
import io.paper.permafrost.training.PlotConfigManager;
import io.paper.permafrost.training.TrainingCommand;
import io.paper.permafrost.training.TrainingRoundListener;
import io.paper.permafrost.training.TrainingService;
import io.paper.permafrost.training.TrainingServiceImpl;
import io.paper.permafrost.utilities.Config;
import io.paper.permafrost.utilities.ShovelPowers;
import io.paper.permafrost.utilities.SpleefMode;
import io.paper.permafrost.utilities.important.CoreSettings;
import io.paper.permafrost.utilities.important.ProtocolLibHook;
import io.paper.permafrost.utilities.important.SQLManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import io.paper.permafrost.core.game.AmmoManager;
import io.paper.permafrost.core.game.DuelManager;
import io.paper.permafrost.core.game.GameManager;
import io.paper.permafrost.core.game.PrototypeSpleefFFA;
import io.paper.permafrost.core.game.SpleefGame;
import io.paper.permafrost.core.game.SpleefPlayer;
import io.paper.permafrost.core.game.SpleefQueueManager;
import io.paper.permafrost.core.game.SpleefType;
import io.paper.permafrost.core.game.SpleggQueueManager;
import io.paper.permafrost.core.game.tasks.CancelTask;
import io.paper.permafrost.core.game.tasks.CommandListener;
import io.paper.permafrost.core.game.tasks.CountdownTask;
import io.paper.permafrost.core.game.tasks.FFAListener;
import io.paper.permafrost.core.game.tasks.ItemListener;
import io.paper.permafrost.core.game.tasks.PowersListener;

@SuppressWarnings("unused")
public class Main extends JavaPlugin {
	private DataLoader dataLoader;
	public static Config arenas;
	public static Config banningAreas;
	public static Config spleggArenas;
	public static Config trainingArenas;
	public static Config trainingPlots;
	public static Config spleggFFAArenas;
	public static Config spleggRankedArenas;
	public static Config prototypeFFAArenas;
	public static Config parkourMaps;
	public static Config config;
	public static Location lobby;
	private FAWEAPI faweApi;
	SpleefGame spleef;
	boolean loaded;
	private GameManager gameManager;
	private SQLManager sqlManager;
	private AmmoManager ammoManager;
	private ShovelPowersManager powersMenuManager;
	private PlotConfigManager plotConfigManager;
	private DuelManager duelManager;
	private DuelMenuManager duelMenuManager;
	private SpleggQueueManager spleggQueueManager;
	private SpleefQueueManager spleefQueueManager;
	private StatsManager statsManager;
	private RankedSpleggManager rankedSpleggManager;
	private RankedSpleefManager rankedSpleefManager;
	private RankedParkourManager rankedParkourManager;
	private ShovelPowersManager shovelPowersManager;
	private CosmeticRankManager cosmeticRankManager;
	private PlayerBubbleManager playerBubbleManager;
	private HologramManager hologramManager;
	private Board board;
	private static Main instance;
	private int secondsElapsed = 0;
	private Connection connection;
	private TrainingService trainingService;
	private LuckPerms luckPerms;
	private boolean queuesOpen;

	public void onEnable() {
		queuesOpen = true;
		AmmoManager ammoManager = new AmmoManager(this);
		luckPerms = LuckPermsProvider.get();
		getServer().getPluginManager().registerEvents(ammoManager, this);
		for (Player p : Bukkit.getOnlinePlayers()) {
			ammoManager.seedAmmoFor(p, List.of(ShovelPowers.BLACKGOO, ShovelPowers.LEVITATION));
		}

		instance = this;
		CoreSettings.getInstance().load();
		File playerDataFolder = new File(getDataFolder(), "players");
		if (!playerDataFolder.exists()) {
			playerDataFolder.mkdirs();
		}
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Team team = scoreboard.getTeam("nocollision");
		if (team == null)
			team = scoreboard.registerNewTeam("nocollision");
		team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		for (Player player : Bukkit.getOnlinePlayers()) {
			team.addEntry(player.getName());
		}
		faweApi = new FAWEAPI();
		dataLoader = new DataLoader(this);
		getServer().getPluginManager().registerEvents(new TaskListener(this), this);
		getServer().getPluginManager().registerEvents(new ItemListener(this), this);
		board = Board.getInstance();
		getServer().getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				board.updateScoreboards();
			}
		}, 0, 20);
		getLogger().info("Permafrost plugin enabled!");
		arenas = new Config("arenas.yml", (Plugin) this);
		banningAreas = new Config("banning_areas.yml", (Plugin) this);
		spleggArenas = new Config("splegg_arenas.yml", (Plugin) this);
		trainingArenas = new Config("training_arenas.yml", (Plugin) this);
		trainingPlots = new Config("training_plots.yml", (Plugin) this);
		spleggFFAArenas = new Config("splegg_ffa_arenas.yml", (Plugin) this);
		spleggRankedArenas = new Config("splegg_ranked_arenas.yml", (Plugin) this);
		prototypeFFAArenas = new Config("prototype_arenas.yml", (Plugin) this);
		parkourMaps = new Config("parkour_maps.yml", (Plugin) this);

		this.trainingService = new TrainingServiceImpl(this);

		getCommand("invite").setExecutor(new InviteCommand(trainingService));
		getCommand("join").setExecutor(new JoinCommand(trainingService));
		getCommand("leave").setExecutor(new LeaveCommand(this, trainingService));
		loadWorlds();
		initTasks();
		initManagers();
		registerEvents();
		registerCommands();
		new BukkitRunnable() {
			public void run() {
				lobby = Bukkit.getWorld("world").getSpawnLocation();
				dataLoader.loadArenas();
				dataLoader.loadBanningAreas();
				dataLoader.loadSpleggArenas();
				dataLoader.loadRankedSpleggArenas();
				dataLoader.loadTrainingArenas();
				dataLoader.loadTrainingPlots();
				dataLoader.loadSpleggFFAArenas();
				dataLoader.loadPrototypeFFAArenas();
				dataLoader.loadParkourMaps();
			}
		}.runTaskAsynchronously(this);

		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			saveDefaultConfig();
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		saveDefaultConfig();
		try {
			connect();
			try (Statement stmt = connection.createStatement()) {
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_powers (" + "uuid VARCHAR(36) PRIMARY KEY, "
						+ "power1 VARCHAR(32), " + "power2 VARCHAR(32), " + ");");
				getLogger().info("Table player_powers checked/created.");
			} catch (SQLException e) {
				getLogger().severe("Error creating player_powers table: " + e.getMessage());
			}
			String blocksTable = "CREATE TABLE IF NOT EXISTS splegg_ffa_blocks_broken (" + "uuid VARCHAR(36) NOT NULL,"
					+ "blocks_broken INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)" + ");";

			String eggsTable = "CREATE TABLE IF NOT EXISTS splegg_ffa_eggs_fired (" + "uuid VARCHAR(36) NOT NULL,"
					+ "eggs_fired INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)" + ");";

			String winsTable = "CREATE TABLE IF NOT EXISTS splegg_ffa_wins (" + "uuid VARCHAR(36) NOT NULL,"
					+ "wins INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)" + ");";

			String pointsTable = "CREATE TABLE IF NOT EXISTS splegg_ffa_points (" + "uuid VARCHAR(36) NOT NULL,"
					+ "points INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)" + ");";
			String rankStateTable = "CREATE TABLE IF NOT EXISTS splegg_rank_state (" + "uuid VARCHAR(36) NOT NULL,"
					+ "selected_rank INT NOT NULL DEFAULT 0," + "ranks_enabled BOOLEAN NOT NULL DEFAULT TRUE,"
					+ "PRIMARY KEY (uuid)" + ");";
			String rankedSpleggStats = "CREATE TABLE IF NOT EXISTS ranked_splegg_stats (" + "uuid VARCHAR(36) NOT NULL,"
					+ "wins INT NOT NULL DEFAULT 0," + "games_played INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)"
					+ ");";
			String rankedPowerElo = "CREATE TABLE IF NOT EXISTS ranked_powerspleef_elo ("
					+ "uuid VARCHAR(36) PRIMARY KEY, " + "elo INT NOT NULL DEFAULT 1000" + ")";
			
			String rankedAirElo = "CREATE TABLE IF NOT EXISTS ranked_airspleef_elo (" + "uuid VARCHAR(36) PRIMARY KEY, "
					+ "elo INT NOT NULL DEFAULT 1000" + ")";

			String rankedMiniElo = "CREATE TABLE IF NOT EXISTS ranked_minispleef_elo ("
					+ "uuid VARCHAR(36) PRIMARY KEY, " + "elo INT NOT NULL DEFAULT 1000" + ")";
			String rankedSpleefStats = "CREATE TABLE IF NOT EXISTS ranked_spleef_stats (" + "uuid VARCHAR(36) NOT NULL,"
					+ "wins INT NOT NULL DEFAULT 0," + "games_played INT NOT NULL DEFAULT 0," + "PRIMARY KEY (uuid)"
					+ ");";

			String favorites = "CREATE TABLE IF NOT EXISTS ranked_favorite_maps (" + "player_uuid VARCHAR(36) NOT NULL,"
					+ "arena_id VARCHAR(64) NOT NULL," + "PRIMARY KEY (player_uuid, arena_id)" + ")";

			try (Statement stmt = connection.createStatement()) {
				stmt.executeUpdate(
						"INSERT IGNORE INTO ranked_powerspleef_elo (uuid, elo) " +
						"SELECT uuid, elo FROM ranked_spleef_elo");
				stmt.executeUpdate(blocksTable);
				stmt.executeUpdate(eggsTable);
				stmt.executeUpdate(winsTable);
				stmt.executeUpdate(pointsTable);
				stmt.executeUpdate(rankStateTable);
				stmt.executeUpdate(rankedSpleggStats);
				stmt.executeUpdate(rankedSpleefStats);
				stmt.executeUpdate(favorites);
				stmt.executeUpdate(rankedPowerElo);
				stmt.executeUpdate(rankedAirElo);
				stmt.executeUpdate(rankedMiniElo);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			getLogger().info("MySQL connection established!");
		} catch (

		Exception e) {
			getLogger().severe("Could not connect to MySQL: " + e.getMessage());
			e.printStackTrace();
			// Optionally: getServer().getPluginManager().disablePlugin(this);
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"hologram edit &6Splegg setline 4 &fPlayers: &e" + this.getSpleggQueueManager().getPlayerCount());

		ProtocolLibHook.register(this);
		
		createSpleefEloTable();

		createSpleggEloTable();

		getLogger().info("Permafrost plugin enabled!");

	}

	public void onDisable() {
		if (connection != null) {
			try {
				connection.close();
				getLogger().info("MySQL connection closed.");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		getLogger().info("Permafrost plugin disabled!");
	}

	public static Main getInstance() {
		return instance;
	}

	private void registerCommands() {
		getCommand("spawn").setExecutor((CommandExecutor) new SpawnCommand(this));
		getCommand("endgame").setExecutor((CommandExecutor) new EndGameCommand(this));
		getCommand("spectate").setExecutor((CommandExecutor) new SpectateCommand(this));
		getCommand("teams").setExecutor((CommandExecutor) new TeamsCommand(this));
		getCommand("hover").setExecutor((CommandExecutor) new HoverCommand(this));
		getCommand("teams").setExecutor((CommandExecutor) new TeamsCommand(this));
		getCommand("playto").setExecutor((CommandExecutor) new PlaytoCommand(this));
		getCommand("powers").setExecutor((CommandExecutor) new PowersCommand(this));
		getCommand("bans").setExecutor((CommandExecutor) new BansCommand(this));
		getCommand("powerslist").setExecutor((CommandExecutor) new PowersListCommand(this));
		getCommand("matches").setExecutor((CommandExecutor) new MatchesCommand(this));
		getCommand("duelaccept").setExecutor(new DuelAcceptCommand(duelManager));
		getCommand("dueldeny").setExecutor(new DuelDenyCommand(duelManager));
		getCommand("duelcancel").setExecutor(new DuelCancelCommand(duelManager));
		getCommand("duel").setExecutor((CommandExecutor) new DuelCommand2(this));
		getCommand("endsession").setExecutor((CommandExecutor) new EndSessionCommand(this));
		getCommand("admin").setExecutor((CommandExecutor) new AdminCommand(plotConfigManager, this));
		getCommand("spleggqueue").setExecutor((CommandExecutor) new SpleggQueueCommand(this));
		getCommand("spleefqueue").setExecutor((CommandExecutor) new SpleefQueueCommand(this));
		getCommand("vote").setExecutor((CommandExecutor) new VoteCommand(this));
		getCommand("nvn").setExecutor((CommandExecutor) new nvnCommand(this));
		getCommand("fendgame").setExecutor(new ForceEndGameCommand(this));
		getCommand("fplayto").setExecutor(new ForcePlayToCommand(this));
		getCommand("teamlist").setExecutor((CommandExecutor) new TeamListCommand(this));
		getCommand("spectateffa").setExecutor((CommandExecutor) new SpectateFFACommand(this));
		getCommand("stats").setExecutor(new StatsCommand(this));
		getCommand("rankedqueue").setExecutor(new RankedQueueCommand(this));
		getCommand("cycleleaderboard").setExecutor(statsManager);
		getCommand("ping").setExecutor(new PingCommand(this));
		getCommand("shop").setExecutor(new ShopCommand(this));
		getCommand("donationsholo").setExecutor(new DonationsHoloCommand(this));
		getCommand("ranks").setExecutor(new RanksCommand(this));
		getCommand("shovels").setExecutor(new ShovelsCommand(this));
		getCommand("favorites").setExecutor(new FavoritesCommand(this));
		getCommand("customrank").setExecutor(new CustomRankCommand(this));
		getCommand("fstartgame").setExecutor(new ForceStartCommand(this));
		getCommand("spawnall").setExecutor(new SpawnAllCommand(this));
		getCommand("queues").setExecutor(new QueuesCommand(this));
		getCommand("msg").setExecutor(new MsgCommand(this));
		getCommand("fly").setExecutor(new FlyCommand(this));
		getCommand("foundshovel").setExecutor(new FoundShovelCommand(this));
		getCommand("ffaevent").setExecutor(new FFAEventCommand(this));
		getCommand("staffchat").setExecutor(new StaffChatCommand(this));
	}

	private void loadWorlds() {
		new WorldCreator("voidworld").createWorld();
	}

	private void registerEvents() {
		getServer().getPluginManager().registerEvents((Listener) new ItemListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new TaskListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new CommandListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new DataLoader(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new TeamSpleefListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new PowersListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new AmmoManager(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new TrainingRoundListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new FFAListener(this), (Plugin) this);
		getServer().getPluginManager().registerEvents((Listener) new ParkourListener(this), (Plugin) this);
		Bukkit.getPluginManager().registerEvents(duelMenuManager, this);
	}

	private void initTasks() {
	}

	private void initManagers() {
		this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
		this.dataLoader = new DataLoader(this);
		this.gameManager = new GameManager(this);
		this.sqlManager = new SQLManager(this);
		this.powersMenuManager = new ShovelPowersManager(this);
		this.duelManager = new DuelManager(this);
		this.duelMenuManager = new DuelMenuManager(this);
		this.ammoManager = new AmmoManager(this);
		this.plotConfigManager = new PlotConfigManager(this);
		this.spleggQueueManager = new SpleggQueueManager(this);
		this.spleefQueueManager = new SpleefQueueManager(this);
		this.statsManager = new StatsManager(this);
		statsManager.init();
		getCommand("spleggstatsholo").setExecutor(statsManager);
		this.rankedSpleggManager = new RankedSpleggManager(this);
		this.rankedSpleefManager = new RankedSpleefManager(this);
		this.rankedParkourManager = new RankedParkourManager(this);
		this.shovelPowersManager = new ShovelPowersManager(this);
		this.cosmeticRankManager = new CosmeticRankManager(this, luckPerms);
		this.playerBubbleManager = new PlayerBubbleManager(this);
	}

	public FAWEAPI getFAWEAPI() {
		return this.faweApi;
	}

	public DataLoader getDataLoader() {
		return this.dataLoader;
	}

	public GameManager getGameManager() {
		return this.gameManager;
	}

	public SQLManager getSQLManager() {
		return this.sqlManager;
	}

	public DuelManager getDuelManager() {
		return this.duelManager;
	}

	public DuelMenuManager getDuelMenuManager() {
		return this.duelMenuManager;
	}

	public CosmeticRankManager getCosmeticRankManager() {
		return this.cosmeticRankManager;
	}

	public AmmoManager getAmmoManger() {
		return this.ammoManager;
	}

	public ShovelPowersManager getShovelPowersManager() {
		return this.shovelPowersManager;
	}

	public HologramManager getHologramManager() {
		return this.hologramManager;
	}

	public StatsManager getSpleggStatsManager() {
		return statsManager;
	}

	public SpleggQueueManager getSpleggQueueManager() {
		return this.spleggQueueManager;
	}
	
	public SpleefQueueManager getSpleefQueueManager() {
		return this.spleefQueueManager;
	}

	public RankedSpleggManager getRankedSpleggManager() {
		return this.rankedSpleggManager;
	}

	public RankedSpleefManager getRankedSpleefManager() {
		return this.rankedSpleefManager;
	}
	
	public RankedParkourManager getRankedParkourManager() {
		return this.rankedParkourManager;
	}
	
	public PlayerBubbleManager getPlayerBubbleManager() {
		return this.playerBubbleManager;
	}

	public TrainingService getTrainingService() {
		return trainingService;
	}

	public void setQueuesOpen(boolean queuesOpen) {
		this.queuesOpen = queuesOpen;
	}

	public boolean getQueuesOpen() {
		return queuesOpen;
	}
	
	public boolean isPlayerInAnyQueue(UUID uuid) {
		if (uuid == null) {
			return false;
		}

		// FFA / Splegg queue
		if (getSpleggQueueManager() != null && getSpleggQueueManager().getPlayers().containsKey(uuid)) {
			return true;
		}

		// Ranked Splegg queue
		if (getRankedSpleggManager() != null && getRankedSpleggManager().getPlayersInQueue().contains(uuid)) {
			return true;
		}

		// Ranked Spleef queues
		if (getRankedSpleefManager() != null) {
			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(uuid)) {
				return true;
			}

			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(uuid)) {
				return true;
			}

			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(uuid)) {
				return true;
			}
		}

		// Ranked Parkour queue
		if (getRankedParkourManager() != null && getRankedParkourManager().getPlayersInQueue().contains(uuid)) {
			return true;
		}

		return false;
	}
	
	public void removePlayerFromAllRankedQueues(UUID uuid) {
		if (uuid == null) {
			return;
		}

		if (getRankedSpleggManager() != null && getRankedSpleggManager().getPlayersInQueue().contains(uuid)) {
			getRankedSpleggManager().removePlayerFromQueue(uuid);
		}

		if (getRankedSpleefManager() != null) {
			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.POWERSPLEEF).contains(uuid)) {
				getRankedSpleefManager().removePlayerFromQueue(uuid, SpleefMode.POWERSPLEEF);
			}

			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.AIRSPLEEF).contains(uuid)) {
				getRankedSpleefManager().removePlayerFromQueue(uuid, SpleefMode.AIRSPLEEF);
			}

			if (getRankedSpleefManager().getPlayersInQueue(SpleefMode.MINISPLEEF).contains(uuid)) {
				getRankedSpleefManager().removePlayerFromQueue(uuid, SpleefMode.MINISPLEEF);
			}
		}

		if (getRankedParkourManager() != null && getRankedParkourManager().getPlayersInQueue().contains(uuid)) {
			getRankedParkourManager().removePlayerFromQueue(uuid);
		}
	}

	public void registerHolos() {

		if (hologramManager.getHologram("SpleggFFAStats1").isPresent()) {
			return;
		}

		World world = Bukkit.getWorld("lobby"); // change if needed
		if (world == null)
			return;

		Location ffaLoc = new Location(world, 9004.5, 194.5, 8273.5);
		Location rankedSpleggLoc = new Location(world, 9001.5, 194.5, 8275.5);
		Location rankedSpleefLoc = new Location(world, 9023.5, 194.5, 8275.5);

		List<String> lines = new ArrayList<>();

		lines.add("&e&l== SPLEGG FFA STATS ==");

		lines.add("&7(Click to change view) &8[&eShells&8]");

		for (int i = 1; i <= 10; i++) {
			lines.add("&e#" + i + " &f--- &7>> &e0");
		}

		TextHologramData data = new TextHologramData("SpleggFFAStats1", ffaLoc);
		data.setText(lines);

		Hologram hologram = hologramManager.create(data);

		hologramManager.addHologram(hologram);
	}

	public void createSpleefEloTable() {
		String sql = "CREATE TABLE IF NOT EXISTS ranked_spleef_elo (" + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
				+ "elo INT NOT NULL DEFAULT 1000" + ");";

		try (Connection conn = this.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.executeUpdate();
			Bukkit.getLogger().info("ranked_spleef_elo table is ready.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createSpleggEloTable() {
		String sql = "CREATE TABLE IF NOT EXISTS ranked_splegg_elo (" + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
				+ "elo INT NOT NULL DEFAULT 1000" + ");";

		try (Connection conn = this.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.executeUpdate();
			Bukkit.getLogger().info("ranked_splegg_elo table is ready.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				connect();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

//	private void initManagers() {
//		  this.dataManager = new DataManager(this);
//	  } 
//	public DataManager getDataManager() {
//		return this.dataManager;
	public void connect() throws SQLException {
		FileConfiguration config = getConfig();
		String host = config.getString("mysql.host");
		int port = config.getInt("mysql.port");
		String database = config.getString("mysql.database");
		String username = config.getString("mysql.username");
		String password = config.getString("mysql.password");
		boolean useSSL = config.getBoolean("mysql.useSSL");

		// JDBC URL for MySQL 8+
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&autoReconnect=true";

		// Load MySQL JDBC driver (optional for modern Java, but good practice)
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			getLogger().severe("MySQL driver not found! Did you add it to your plugin's dependencies?");
			return;
		}

		connection = DriverManager.getConnection(url, username, password);
	}
	
	public int getPlayerShovelModelData(Player player) {
	    String sql = "SELECT custom_model_data FROM player_shovel_model_data WHERE uuid = ?";

	    try (Connection connection = getConnection();
	         PreparedStatement ps = connection.prepareStatement(sql)) {

	        ps.setString(1, player.getUniqueId().toString());

	        try (ResultSet rs = ps.executeQuery()) {
	            if (rs.next()) {
	                return rs.getInt("custom_model_data");
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	        player.sendMessage("§cThere was an error loading your shovel model data.");
	    }

	    return 0; // none/default
	}

	public ShovelPowersManager getShovelPowersMenu() {
		return this.powersMenuManager;

	}
}
