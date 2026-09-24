package io.paper.permafrost.managers;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StatsManager implements CommandExecutor {

    private final Main main;

    private static final int MAX_ENTRIES = 10;

    private static final String FFA_HOLOGRAM_NAME = "SpleggFFAStats1";
    private static final String FFA_HOLOGRAM_NAME_2 = "SpleggFFAStats2";
    private static final String SPLEEF_FFA_HOLOGRAM_NAME = "SpleefFFAStats1";
    private static final String RANKED_SPLEGG_HOLOGRAM_NAME = "RankedSpleggStats1";
    private static final String RANKED_SPLEEF_HOLOGRAM_NAME = "RankedSpleefStats1";
    private static final String TEAMS_ELO_HOLOGRAM_NAME = "Teamselo";

    private LeaderboardType ffaCurrentType1 = LeaderboardType.SHELLS;
    private LeaderboardType ffaCurrentType2 = LeaderboardType.SHELLS;
    private LeaderboardType rankedSpleggCurrentType = LeaderboardType.SPLEGG_RANKED_ELO;
    private LeaderboardType rankedSpleefCurrentType = LeaderboardType.POWERSPLEEF_RANKED_ELO;
    private LeaderboardType teamsEloCurrentType = LeaderboardType.TEAMS_RANKED_ELO;
    private LeaderboardType spleefFfaCurrentType = LeaderboardType.SPLEEF_FFA_SHARDS;

    public StatsManager(Main main) {
        this.main = main;
    }

    public void init() {
        registerHolos();
        updateAllHolograms();
        startRefreshTask();
    }

    private void startRefreshTask() {
        Bukkit.getScheduler().runTaskTimer(main, this::updateAllHolograms, 20L, 20L * 60L);
    }

    public void registerHolos() {
        HologramManager manager = main.getHologramManager();

        World world = Bukkit.getWorld("lobby"); // CHANGE THIS
        if (world == null) {
            Bukkit.getLogger().warning("[SpleggStatsManager] Could not create holograms because world 'world' was not found.");
            return;
        }

        /*Location ffaLoc = new Location(world, 0.5, 80.0, 0.5); // CHANGE THIS
        Location ffaLoc2 = new Location(world, 0.5, 80.0, 5.0); // CHANGE THIS
        Location rankedSpleggLoc = new Location(world, 4.5, 80.0, 0.5); // CHANGE THIS
        Location rankedSpleefLoc = new Location(world, 8.5, 80.0, 0.5); // CHANGE THIS*/
        Location teamsEloLoc = new Location(world, 9007.5, 194.5, 8300.5); // CHANGE THIS

        /*registerTextHoloIfMissing(manager, FFA_HOLOGRAM_NAME, ffaLoc,
                cc("&e&l== SPLEGG FFA STATS =="),
                cc("&7(Click to change view) &8[&eShells&8]"),
                "&e");

        registerTextHoloIfMissing(manager, FFA_HOLOGRAM_NAME_2, ffaLoc2,
                cc("&e&l== SPLEGG FFA STATS =="),
                cc("&7(Click to change view) &8[&eShells&8]"),
                "&e");

        registerTextHoloIfMissing(manager, RANKED_SPLEGG_HOLOGRAM_NAME, rankedSpleggLoc,
                cc("&a&l== RANKED SPLEGG =="),
                cc("&7(Click to change view) &8[&aELO&8]"),
                "&a");

        registerTextHoloIfMissing(manager, RANKED_SPLEEF_HOLOGRAM_NAME, rankedSpleefLoc,
                cc("&2&l== RANKED SPLEEF =="),
                cc("&7(Click to change view) &8[&bPower ELO&8]"),
                "&2");*/
        
        registerTextHoloIfMissing(manager, TEAMS_ELO_HOLOGRAM_NAME, teamsEloLoc,
                cc("&b&l== RANKED TEAMS =="),
                cc("&7Ranked Team Games &8[&bELO&8]"),
                "&b");
    }

    private void registerTextHoloIfMissing(HologramManager manager, String hologramName, Location location, String title, String subtitle, String defaultColor) {
        if (manager.getHologram(hologramName).isPresent()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add(subtitle);

        for (int i = 1; i <= MAX_ENTRIES; i++) {
            lines.add(cc("&e#" + i + " &f--- &7>> " + defaultColor + "0"));
        }

        TextHologramData data = new TextHologramData(hologramName, location);
        data.setText(lines);
        data.setBillboard(Display.Billboard.CENTER);

        Hologram hologram = main.getHologramManager().create(data);
        main.getHologramManager().addHologram(hologram);
    }

    public void updateAllHolograms() {
        updateFfaHologram1();
        updateFfaHologram2();
        updateRankedSpleggHologram();
        updateRankedSpleefHologram();
        updateTeamsEloHologram();
        updateSpleefFfaHologram();
    }

    public void cycleBoard(BoardType boardType) {
        switch (boardType) {
            case FFA_1:
                ffaCurrentType1 = getNextFfaType(ffaCurrentType1);
                updateFfaHologram1();
                break;
            case FFA_2:
                ffaCurrentType2 = getNextFfaType(ffaCurrentType2);
                updateFfaHologram2();
                break;
            case RANKED_SPLEGG:
                rankedSpleggCurrentType = getNextRankedSpleggType(rankedSpleggCurrentType);
                updateRankedSpleggHologram();
                break;
            case RANKED_SPLEEF:
                rankedSpleefCurrentType = getNextRankedSpleefType(rankedSpleefCurrentType);
                updateRankedSpleefHologram();
                break;
            case SPLEEF_FFA:
            	spleefFfaCurrentType = getNextSpleefFfaType(spleefFfaCurrentType);
            	updateSpleefFfaHologram();
            	break;
        }
    }
    
    public void updateSpleefFfaHologram() {
    	List<DataLoader.LeaderboardEntry> entries = getEntriesForType(spleefFfaCurrentType);

    	List<String> lines = new ArrayList<>();
    	lines.add(cc("&b&l== SPLEEF FFA STATS =="));
    	lines.add(cc("&7(Click to change view) &8[" + getViewDisplayNameRaw(spleefFfaCurrentType) + "&8]"));

    	for (int i = 0; i < MAX_ENTRIES; i++) {
    		int place = i + 1;

    		if (i < entries.size()) {
    			DataLoader.LeaderboardEntry entry = entries.get(i);
    			String name = getPlayerName(entry.getUuid());
    			String value = formatValue(entry.getValue(), spleefFfaCurrentType);

    			lines.add(cc("&e#" + place + " &f" + name + " &7>> " + getStatColor(spleefFfaCurrentType) + value));
    		} else {
    			lines.add(cc("&e#" + place + " &f--- &7>> " + getStatColor(spleefFfaCurrentType) + getEmptyValue(spleefFfaCurrentType)));
    		}
    	}

    	applyLines(SPLEEF_FFA_HOLOGRAM_NAME, lines);
    }
    
    private LeaderboardType getNextSpleefFfaType(LeaderboardType current) {
    	switch (current) {
    	case SPLEEF_FFA_SHARDS:
    		return LeaderboardType.SPLEEF_FFA_WINS;
    	case SPLEEF_FFA_WINS:
    		return LeaderboardType.SPLEEF_FFA_BLOCKS_BROKEN;
    	case SPLEEF_FFA_BLOCKS_BROKEN:
    	default:
    		return LeaderboardType.SPLEEF_FFA_SHARDS;
    	}
    }

    public void updateFfaHologram1() {
        updateFfaHologram(FFA_HOLOGRAM_NAME, ffaCurrentType1);
    }

    public void updateFfaHologram2() {
        updateFfaHologram(FFA_HOLOGRAM_NAME_2, ffaCurrentType2);
    }

    private void updateFfaHologram(String hologramName, LeaderboardType currentType) {
        List<DataLoader.LeaderboardEntry> entries = getEntriesForType(currentType);

        List<String> lines = new ArrayList<>();
        lines.add(cc("&e&l== SPLEGG FFA STATS =="));
        lines.add(cc("&7(Click to change view) &8[" + getViewDisplayNameRaw(currentType) + "&8]"));

        for (int i = 0; i < MAX_ENTRIES; i++) {
            int place = i + 1;

            if (i < entries.size()) {
                DataLoader.LeaderboardEntry entry = entries.get(i);
                String name = getPlayerName(entry.getUuid());
                String value = formatValue(entry.getValue(), currentType);

                lines.add(cc("&e#" + place + " &f" + name + " &7>> " + getStatColor(currentType) + value));
            } else {
                lines.add(cc("&e#" + place + " &f--- &7>> " + getStatColor(currentType) + getEmptyValue(currentType)));
            }
        }

        applyLines(hologramName, lines);
    }

    public void updateRankedSpleggHologram() {
        List<DataLoader.LeaderboardEntry> entries = getEntriesForType(rankedSpleggCurrentType);

        List<String> lines = new ArrayList<>();
        lines.add(cc("&a&l== RANKED SPLEGG =="));
        lines.add(cc("&7(Click to change view) &8[" + getViewDisplayNameRaw(rankedSpleggCurrentType) + "&8]"));

        for (int i = 0; i < MAX_ENTRIES; i++) {
            int place = i + 1;

            if (i < entries.size()) {
                DataLoader.LeaderboardEntry entry = entries.get(i);
                String name = getPlayerName(entry.getUuid());
                String value = formatValue(entry.getValue(), rankedSpleggCurrentType);

                lines.add(cc("&e#" + place + " &f" + name + " &7>> " + getStatColor(rankedSpleggCurrentType) + value));
            } else {
                lines.add(cc("&e#" + place + " &f--- &7>> " + getStatColor(rankedSpleggCurrentType) + getEmptyValue(rankedSpleggCurrentType)));
            }
        }

        applyLines(RANKED_SPLEGG_HOLOGRAM_NAME, lines);
    }

    public void updateRankedSpleefHologram() {
        List<DataLoader.LeaderboardEntry> entries = getEntriesForType(rankedSpleefCurrentType);

        List<String> lines = new ArrayList<>();
        lines.add(cc("&2&l== RANKED SPLEEF =="));
        lines.add(cc("&7(Click to change view) &8[" + getViewDisplayNameRaw(rankedSpleefCurrentType) + "&8]"));

        for (int i = 0; i < MAX_ENTRIES; i++) {
            int place = i + 1;

            if (i < entries.size()) {
                DataLoader.LeaderboardEntry entry = entries.get(i);
                String name = getPlayerName(entry.getUuid());
                String value = formatValue(entry.getValue(), rankedSpleefCurrentType);

                lines.add(cc("&e#" + place + " &f" + name + " &7>> " + getStatColor(rankedSpleefCurrentType) + value));
            } else {
                lines.add(cc("&e#" + place + " &f--- &7>> " + getStatColor(rankedSpleefCurrentType) + getEmptyValue(rankedSpleefCurrentType)));
            }
        }

        applyLines(RANKED_SPLEEF_HOLOGRAM_NAME, lines);
    }
    
    public void updateTeamsEloHologram() {
        List<DataLoader.LeaderboardEntry> entries = main.getDataLoader().getTopTeamsElo(MAX_ENTRIES);

        List<String> lines = new ArrayList<>();
        lines.add(cc("&b&l== RANKED TEAMS =="));
        lines.add(cc("&7Ranked Team Games &8[&bELO&8]"));

        for (int i = 0; i < MAX_ENTRIES; i++) {
            int place = i + 1;

            if (i < entries.size()) {
                DataLoader.LeaderboardEntry entry = entries.get(i);
                String name = getPlayerName(entry.getUuid());
                int elo = (int) Math.round(entry.getValue());

                lines.add(cc("&e#" + place + " &f" + name + " &7>> &b" + elo));
            } else {
                lines.add(cc("&e#" + place + " &f--- &7>> &b0"));
            }
        }

        applyLines(TEAMS_ELO_HOLOGRAM_NAME, lines);
    }

    private void applyLines(String hologramName, List<String> lines) {
        HologramManager manager = main.getHologramManager();
        Optional<Hologram> optional = manager.getHologram(hologramName);

        if (optional.isEmpty()) {
            Bukkit.getLogger().warning("[SpleggStatsManager] Could not update hologram '" + hologramName + "' because it does not exist.");
            return;
        }

        Hologram hologram = optional.get();

        if (!(hologram.getData() instanceof TextHologramData data)) {
            Bukkit.getLogger().warning("[SpleggStatsManager] Hologram '" + hologramName + "' is not a text hologram.");
            return;
        }

        data.setText(lines);
        hologram.forceUpdate();
    }

    private List<DataLoader.LeaderboardEntry> getEntriesForType(LeaderboardType type) {
        switch (type) {
            case EGGS:
                return main.getDataLoader().getTopSpleggFfaEggs(MAX_ENTRIES);
            case WINS:
                return main.getDataLoader().getTopSpleggFfaWins(MAX_ENTRIES);
            case BLOCKS_BROKEN:
                return main.getDataLoader().getTopSpleggFfaBlocksBroken(MAX_ENTRIES);
            case SPLEGG_RANKED_ELO:
                return main.getDataLoader().getTopRankedSpleggElo(MAX_ENTRIES);
            case SPLEGG_RANKED_WINS:
                return main.getDataLoader().getTopRankedSpleggWins(MAX_ENTRIES);
            case SPLEGG_RANKED_WIN_RATE:
                return main.getDataLoader().getTopRankedSpleggWinRate(MAX_ENTRIES);
            case POWERSPLEEF_RANKED_ELO:
                return getTopEloFromTable("ranked_powerspleef_elo", MAX_ENTRIES);
            case AIRSPLEEF_RANKED_ELO:
                return getTopEloFromTable("ranked_airspleef_elo", MAX_ENTRIES);
            case MINISPLEEF_RANKED_ELO:
                return getTopEloFromTable("ranked_minispleef_elo", MAX_ENTRIES);
            case SPLEEF_RANKED_WINS:
                return main.getDataLoader().getTopRankedSpleefWins(MAX_ENTRIES);
            case SPLEEF_RANKED_WIN_RATE:
                return main.getDataLoader().getTopRankedSpleefWinRate(MAX_ENTRIES);
            case SPLEEF_FFA_SHARDS:
            	return main.getDataLoader().getTopSpleefFfaShards(MAX_ENTRIES);
            case SPLEEF_FFA_WINS:
            	return main.getDataLoader().getTopSpleefFfaWins(MAX_ENTRIES);
            case SPLEEF_FFA_BLOCKS_BROKEN:
            	return main.getDataLoader().getTopSpleefFfaBlocksBroken(MAX_ENTRIES);
            case SHELLS:
            default:
                return main.getDataLoader().getTopSpleggFfaShells(MAX_ENTRIES);
        }
    }

    private List<DataLoader.LeaderboardEntry> getTopEloFromTable(String tableName, int limit) {
        List<DataLoader.LeaderboardEntry> entries = new ArrayList<>();
        String sql = "SELECT uuid, elo FROM " + tableName + " ORDER BY elo DESC LIMIT ?";

        try (PreparedStatement ps = main.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int elo = rs.getInt("elo");
                    entries.add(new DataLoader.LeaderboardEntry(uuid, elo));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    private String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null) {
            return offline.getName();
        }

        return uuid.toString().substring(0, 8);
    }

    private LeaderboardType getNextFfaType(LeaderboardType current) {
        switch (current) {
            case SHELLS:
                return LeaderboardType.EGGS;
            case EGGS:
                return LeaderboardType.WINS;
            case WINS:
                return LeaderboardType.BLOCKS_BROKEN;
            case BLOCKS_BROKEN:
            default:
                return LeaderboardType.SHELLS;
        }
    }

    private LeaderboardType getNextRankedSpleggType(LeaderboardType current) {
        switch (current) {
            case SPLEGG_RANKED_ELO:
                return LeaderboardType.SPLEGG_RANKED_WINS;
            case SPLEGG_RANKED_WINS:
                return LeaderboardType.SPLEGG_RANKED_WIN_RATE;
            case SPLEGG_RANKED_WIN_RATE:
            default:
                return LeaderboardType.SPLEGG_RANKED_ELO;
        }
    }

    private LeaderboardType getNextRankedSpleefType(LeaderboardType current) {
        switch (current) {
            case POWERSPLEEF_RANKED_ELO:
                return LeaderboardType.AIRSPLEEF_RANKED_ELO;
            case AIRSPLEEF_RANKED_ELO:
                return LeaderboardType.MINISPLEEF_RANKED_ELO;
            case MINISPLEEF_RANKED_ELO:
                return LeaderboardType.SPLEEF_RANKED_WINS;
            case SPLEEF_RANKED_WINS:
                return LeaderboardType.SPLEEF_RANKED_WIN_RATE;
            case SPLEEF_RANKED_WIN_RATE:
            default:
                return LeaderboardType.POWERSPLEEF_RANKED_ELO;
        }
    }

    private String getViewDisplayNameRaw(LeaderboardType type) {
        switch (type) {
            case EGGS:
                return "&bEggs Fired";
            case WINS:
                return "&6Wins";
            case BLOCKS_BROKEN:
                return "&cBlocks Broken";
            case SPLEGG_RANKED_ELO:
                return "&aELO";
            case SPLEGG_RANKED_WINS:
                return "&eWins";
            case SPLEGG_RANKED_WIN_RATE:
                return "&dWin Rate";
            case POWERSPLEEF_RANKED_ELO:
                return "&bPower ELO";
            case AIRSPLEEF_RANKED_ELO:
                return "&3Air ELO";
            case MINISPLEEF_RANKED_ELO:
                return "&dMini ELO";
            case SPLEEF_RANKED_WINS:
                return "&6Wins";
            case SPLEEF_RANKED_WIN_RATE:
                return "&dWin Rate";
            case SPLEEF_FFA_SHARDS:
            	return "&bShards";
            case SPLEEF_FFA_WINS:
            	return "&6Wins";
            case SPLEEF_FFA_BLOCKS_BROKEN:
            	return "&cBlocks Broken";
            case SHELLS:
            default:
                return "&eShells";
        }
    }

    private String getStatColor(LeaderboardType type) {
        switch (type) {
            case EGGS:
                return "&b";
            case WINS:
                return "&6";
            case BLOCKS_BROKEN:
                return "&c";
            case SPLEGG_RANKED_ELO:
                return "&a";
            case SPLEGG_RANKED_WINS:
                return "&e";
            case SPLEGG_RANKED_WIN_RATE:
                return "&d";
            case POWERSPLEEF_RANKED_ELO:
                return "&b";
            case AIRSPLEEF_RANKED_ELO:
                return "&3";
            case MINISPLEEF_RANKED_ELO:
                return "&d";
            case SPLEEF_RANKED_WINS:
                return "&6";
            case SPLEEF_RANKED_WIN_RATE:
                return "&d";
            case SHELLS:
            default:
                return "&e";
        }
    }

    private String getEmptyValue(LeaderboardType type) {
        if (type == LeaderboardType.SPLEGG_RANKED_WIN_RATE || type == LeaderboardType.SPLEEF_RANKED_WIN_RATE) {
            return "0.0%";
        }
        return "0";
    }

    private String formatValue(double value, LeaderboardType type) {
        if (type == LeaderboardType.SPLEGG_RANKED_WIN_RATE || type == LeaderboardType.SPLEEF_RANKED_WIN_RATE) {
            return String.format("%.1f%%", value);
        }

        long rounded = Math.round(value);

        if (type == LeaderboardType.SPLEGG_RANKED_ELO
                || type == LeaderboardType.POWERSPLEEF_RANKED_ELO
                || type == LeaderboardType.AIRSPLEEF_RANKED_ELO
                || type == LeaderboardType.MINISPLEEF_RANKED_ELO) {
            return String.valueOf(rounded);
        }

        return formatNumber(rounded);
    }

    public String formatNumber(long value) {
        if (value >= 1_000_000_000L) {
            return String.format("%.1fb", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000L) {
            return String.format("%.1fm", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    private String cc(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cycleleaderboard")) {
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /cycleleaderboard <ffa1|ffa2|rankedsplegg|rankedspleef>");
            return true;
        }

        if (args[0].equalsIgnoreCase("ffa1")) {
            cycleBoard(BoardType.FFA_1);
            return true;
        }

        if (args[0].equalsIgnoreCase("ffa2")) {
            cycleBoard(BoardType.FFA_2);
            return true;
        }

        if (args[0].equalsIgnoreCase("rankedsplegg")) {
            cycleBoard(BoardType.RANKED_SPLEGG);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("spleefffa")) {
        	cycleBoard(BoardType.SPLEEF_FFA);
        	return true;
        }

        if (args[0].equalsIgnoreCase("rankedspleef")) {
            cycleBoard(BoardType.RANKED_SPLEEF);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /cycleleaderboard <ffa1|ffa2|rankedsplegg|rankedspleef>");
        return true;
    }

    public enum BoardType {
        FFA_1,
        FFA_2,
        RANKED_SPLEGG,
        RANKED_SPLEEF,
        SPLEEF_FFA
    }

    public enum LeaderboardType {
        SHELLS,
        EGGS,
        WINS,
        BLOCKS_BROKEN,
        SPLEGG_RANKED_ELO,
        SPLEGG_RANKED_WINS,
        SPLEGG_RANKED_WIN_RATE,
        POWERSPLEEF_RANKED_ELO,
        AIRSPLEEF_RANKED_ELO,
        MINISPLEEF_RANKED_ELO,
        SPLEEF_RANKED_WINS,
        SPLEEF_RANKED_WIN_RATE,
        TEAMS_RANKED_ELO,
        SPLEEF_FFA_SHARDS,
        SPLEEF_FFA_WINS,
        SPLEEF_FFA_BLOCKS_BROKEN
    }
}