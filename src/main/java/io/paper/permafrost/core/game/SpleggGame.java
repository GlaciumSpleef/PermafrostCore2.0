package io.paper.permafrost.core.game;

import static net.kyori.adventure.text.Component.text;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.base.Function;
import com.sk89q.worldedit.math.BlockVector3;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import io.paper.permafrost.DataLoader;
import io.paper.permafrost.Main;
import io.paper.permafrost.core.game.SpleggArena.SpawnPoint;
import io.paper.permafrost.core.game.tasks.CancelTask;
import io.paper.permafrost.core.game.tasks.CountdownTask;
import io.paper.permafrost.core.game.tasks.GameTask;
import io.paper.permafrost.utilities.ShovelPowers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;


import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.kyori.adventure.text.Component.text;

public class SpleggGame implements Listener {

    // ============================================================
    // # Fields (grouped by concern)
    // ============================================================

    // Core/context
    private final Main main;
    private SpleggArena arena;
    private final DataLoader dataLoader;

    // Game state
    private GameState gameState;
    private boolean teamGame;
    private boolean startingGame;

    private int totalTime;
    private int roundTime;
    private int secondsElapsed;
    private BukkitRunnable timerTask;

    private int round;
    private int roundCount;
    private int teamCount;
    private int playToInt = 5; 
    private final Set<SpleefTeam> teams = new HashSet<>();
    private final Map<String, TeamSpleefTeam> teamSpleefTeams = new HashMap<>();
    private Map<Integer, List<UUID>> playerTeams = new HashMap<>();

    private SpleefType spleefType;
    private SpleefType type; 

    private GameTask gameTask;
    private CancelTask cancel;
    private String winner, loser, wpoints, lpoints;

    private Location arenaCorner1;
    private Location arenaCorner2;

    public SpleggGame(Main main, SpleggArena arena, SpleefType spleefType) {
        this.main = main;
        this.arena = arena;
        this.spleefType = spleefType;
        this.type = spleefType;
        this.dataLoader = new DataLoader(main);
        this.roundCount = 0;
    }

    public SpleggGame(Main main) {
        this.main = main;
        this.dataLoader = new DataLoader(main);
        this.roundCount = 0;
    }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }

    public int getTotalTime() { return totalTime; }
    public void setTotalTime(int time) { this.totalTime = time; }

    public int getRoundTime() { return roundTime; }
    public void setRoundTime(int i) { this.roundTime = i; }

    public int getRound() { return round; }

    public boolean getTeamGame() { return teamGame; }
    public void setTeamGame(boolean teamGame) { this.teamGame = teamGame; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public boolean getStartingGame() { return startingGame; }
    public void setStartingGame(boolean startingGame) { this.startingGame = startingGame; }

    public SpleggArena getArena() { return arena; }

    public Set<SpleefTeam> getTeams() { return teams; }

    public void setPlaytoScore(int playToInt) { this.playToInt = playToInt; }
    public int getPlaytoScore() { return this.playToInt; }

    public SpleefTeam getTeam(SpleefPlayer sp) {
        for (SpleefTeam team : this.teams) {
            for (UUID u : team.getPlayers()) {
                if (u.equals(sp.getUUID())) return team;
            }
        }
        return null;
    }

    public void saveTeams(Map<Integer, List<UUID>> playerTeams) {
        this.playerTeams = new HashMap<>();
        for (Map.Entry<Integer, List<UUID>> entry : playerTeams.entrySet()) {
            this.playerTeams.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    public Set<SpleefPlayer> getPlayers() {
        Set<SpleefPlayer> set = new HashSet<>();
        if (teamGame) {
            for (List<UUID> teamPlayers : playerTeams.values()) {
                for (UUID uuid : teamPlayers) {
                    SpleefPlayer sp = main.getDataLoader().getPlayer(uuid);
                    if (sp != null) set.add(sp);
                }
            }
        } else {
            for (SpleefTeam team : this.teams) {
                for (UUID u : team.getPlayers()) {
                    SpleefPlayer sp = main.getDataLoader().getPlayer(u);
                    if (sp != null) set.add(sp);
                }
            }
        }
        return set;
    }

    public List<TeamSpleefTeam> getTeamSpleefTeams() {
        // Build unique list by color from players' current TeamSpleefTeam
        Set<TeamSpleefTeam> unique = new HashSet<>();
        Set<String> colors = new HashSet<>();
        for (SpleefPlayer p : this.getPlayers()) {
            TeamSpleefTeam t = p.getTeam();
            if (t != null && colors.add(t.getColor())) unique.add(t);
        }
        return new ArrayList<>(unique);
    }

    public TeamSpleefTeam getTeamSpleefTeam(String color) {
        return teamSpleefTeams.get(color);
    }

    public void setTeamSpleefTeam(String color, List<UUID> players) {
        TeamSpleefTeam team = new TeamSpleefTeam(main, color, players);
        teamSpleefTeams.put(color, team);
    }

    public void setTeamSpleefTeamDead(String color, boolean dead) {
        TeamSpleefTeam team = teamSpleefTeams.get(color);
        if (team != null) team.setDead(dead);
    }

    public boolean isTeamSpleefTeamDead(String color) {
        TeamSpleefTeam team = teamSpleefTeams.get(color);
        return team != null && team.isDead();
    }

    // ============================================================
    // # Arena selection / setup
    // ============================================================

    public void setArena() {
        List<SpleggArena> arenas = new ArrayList<>(main.getDataLoader().getSpleggArenas());
        if (arenas.isEmpty()) return;

        int teamNeeded = teamSpleefTeams.size();

        // Try random first
        SpleggArena random = arenas.get(new Random().nextInt(arenas.size()));
        if (!random.isInGame() && random.getTeamAmount() >= teamNeeded) {
            this.arena = random;
        } else {
            // Fallback: first suitable
            for (SpleggArena a : arenas) {
                if (!a.isInGame() && a.getTeamAmount() >= teamNeeded) {
                    this.arena = a;
                    break;
                }
            }
        }

        if (this.arena != null) this.arena.setInGame(true);
    }

    // ============================================================
    // # Game lifecycle: start / round / end
    // ============================================================

    // ---------- TEAM SPLEEF START ----------
    public void startTeamGame() {
        startingGame = true;
        this.playToInt = (this.playToInt <= 0) ? 5 : this.playToInt;

        // Build nice "Started a game..." messages (Discord + in-game)
        Function<TeamSpleefTeam, String> teamWithPlayers = (team) -> {
            String teamName = capitalize(team.getColor()) + " team";
            String players = team.getPlayers().stream()
                    .map(uuid -> {
                        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                        String name = (p != null && p.getName() != null) ? p.getName() : "Unknown";
                        return name;
                    })
                    .collect(Collectors.joining(", "));
            return players.isEmpty() ? teamName : (teamName + " (" + players + ")");
        };

        BiFunction<List<String>, String, String> joinHuman = (items, lastSep) -> {
            if (items.isEmpty()) return "";
            if (items.size() == 1) return items.get(0);
            if (items.size() == 2) return items.get(0) + " " + lastSep + " " + items.get(1);
            String middle = String.join(", ", items.subList(0, items.size() - 1));
            return middle + ", " + lastSep + " " + items.get(items.size() - 1);
        };

        List<TeamSpleefTeam> teamsForDiscord = new ArrayList<>(teamSpleefTeams.values());
        String discordMsg = "Started a game of **SPLEGG** between "
                + joinHuman.apply(teamsForDiscord.stream().map(teamWithPlayers).collect(Collectors.toList()), "and");
        sendMessage(discordMsg);

        // In-game fancy message with hover
        Component message = Component.text("Started a game of ").color(NamedTextColor.AQUA)
                .append(text("SPLEGG ").color(NamedTextColor.WHITE)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(text("between ").color(NamedTextColor.AQUA));

        List<Component> teamComponents = new ArrayList<>();
        for (TeamSpleefTeam team : teamSpleefTeams.values()) {
            String colorName = capitalize(team.getColor());
            NamedTextColor textColor = getAdventureColor(team.getColor());

            StringBuilder hover = new StringBuilder();
            hover.append("§l").append(getChatColor(colorName)).append("§l").append(colorName)
                    .append(" §lTeam\n").append("§r§f§lPlayers:\n§r");

            int count = team.getPlayers().size();
            for (UUID uuid : team.getPlayers()) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                String name = (p != null && p.getName() != null) ? p.getName() : "Unknown";
                count--;
                if (count == 0) hover.append("§r- §r§f").append(name);
                else hover.append("§r- §r§f").append(name).append("\n");
            }

            Component teamComponent = text(colorName).color(textColor)
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(text(hover.toString())));
            teamComponents.add(teamComponent);
        }

        for (int i = 0; i < teamComponents.size(); i++) {
            if (i > 0 && teamComponents.size() > 2) {
                message = message.append(text(", ").color(NamedTextColor.WHITE));
            }
            if (i == teamComponents.size() - 1 && teamComponents.size() > 1) {
                message = message.append(text(teamComponents.size() == 2 ? " and " : "and ").color(NamedTextColor.AQUA));
            }
            message = message.append(teamComponents.get(i));
        }
        message = message.append(text(" team").color(NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text().append(message).build());

        setArena();
        teamGame = true;
        startTeamRound();
        startTimer(main);
    }

    public void startTeamRound() {
        this.roundTime = 0;

        // Prime player flags
        for (SpleefPlayer sp : this.getPlayers()) {
            sp.setRoundStarting(true);
            sp.setSpleggGame(this);
            sp.setInTeamGame(true);
        }

        // Mark offline players dead
        for (TeamSpleefTeam team : teamSpleefTeams.values()) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(offlinePlayer);
                    if (sp != null) sp.setDead(true);
                }
            }
        }

        // Paste arena schematic
        if (arena != null) {
            File schemFile = new File("plugins/PermafrostCore/schematics", arena.getSchematicPath());
            main.getFAWEAPI().pasteSchematic(schemFile, arena.getPasteOrigin(), false);
        }

        // Teleport players to spawn points by matching team colors
        if (arena != null && arena.getPlayerSpawns() != null) {
            for (SpawnPoint point : arena.getPlayerSpawns()) {
                String spawnColor = point.getTeamColor();
                if (spawnColor == null) continue;

                for (TeamSpleefTeam team : teamSpleefTeams.values()) {
                    if (spawnColor.equalsIgnoreCase(team.getColor())) {
                        for (UUID uuid : team.getPlayers()) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) player.teleport(point.toLocation());
                        }
                    }
                }
            }
        }

        // Give items, armor, and start per-player cases & countdowns
        for (List<UUID> teamPlayers : playerTeams.values()) {
            for (UUID uuid : teamPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                // Give shovel
                PlayerInventory inv = player.getInventory();
                boolean hasShovel = inv.contains(Material.DIAMOND_SHOVEL);
                if (!hasShovel) {
                    ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
                    ItemMeta meta = shovel.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                        meta.setUnbreakable(true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
                        shovel.setItemMeta(meta);
                    }
                    inv.addItem(shovel);
                }

                // Team armor
                SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(player);
                if (sp != null && sp.getTeam() != null) {
                    for (UUID mate : sp.getTeam().getPlayers()) {
                        Player p = Bukkit.getPlayer(mate);
                        if (p != null) giveColoredLeatherTunic(p, sp.getTeam().getColor());
                    }
                }

                // Powers & flags
                String[] powers = getPlayerPowers(uuid);
                SpleefPlayer splayer = main.getDataLoader().getSpleefPlayer(player);
                if (splayer != null) {
                    splayer.setAvailable(true);
                    if (powers[0] != null) splayer.setSelectedPower(ShovelPowers.valueOf(powers[0].toUpperCase()));
                    if (powers[1] != null) splayer.setDeselectedPower(ShovelPowers.valueOf(powers[1].toUpperCase()));
                }

                // Player prep
                player.setGameMode(GameMode.SURVIVAL);
                createGlassCase(player);
                new CountdownTask(main, this);

                // Break glass after countdown
                new BukkitRunnable() {
                    final Location loc = player.getLocation();
                    final int px = loc.getBlockX();
                    final int py = loc.getBlockY();
                    final int pz = loc.getBlockZ();

                    @Override
                    public void run() {
                        for (int x = -1; x <= 1; x++) {
                            for (int y = 0; y < 3; y++) {
                                for (int z = -1; z <= 1; z++) {
                                    if (x == 0 && z == 0) continue;
                                    new Location(player.getWorld(), px + x, py + y, pz + z).getBlock().setType(Material.AIR);
                                }
                            }
                        }
                        new Location(player.getWorld(), px, py + 2, pz).getBlock().setType(Material.AIR);
                    }
                }.runTaskLater(main, 70L);
            }
        }
    }

    public void endTeamGame() {
        // Clean any spawned blocks
        for (SpleefPlayer sp : this.getPlayers()) {
            if (sp == null) continue;
            if (main.getDataLoader().getSnowSpawned(sp.getUUID()) != null) {
                for (Location snow : main.getDataLoader().getSnowSpawned(sp.getUUID())) {
                    snow.getBlock().setType(Material.AIR);
                }
                main.getDataLoader().removeAllSnowSpawned(sp.getUUID());
            }
        }

        // Compute standings
        List<TeamSpleefTeam> standings = new ArrayList<>(this.getTeamSpleefTeams());
        standings.sort(Comparator.comparingInt(TeamSpleefTeam::getPoints).reversed());

        boolean draw = false;
        if (!standings.isEmpty()) {
            int topScore = standings.get(0).getPoints();
            List<TeamSpleefTeam> top = standings.stream().filter(t -> t.getPoints() == topScore).collect(Collectors.toList());
            if (top.size() == 1) top.get(0).setWinner(true);
            else draw = true;
        }

        // Build components
        List<Component> drawTeams = new ArrayList<>();
        List<Component> losingTeams = new ArrayList<>();
        Component winnerComponent = Component.text("Unknown").color(NamedTextColor.GRAY);
        int winnerPoints = 0;
        List<Integer> allPoints = new ArrayList<>();

        for (TeamSpleefTeam team : this.getTeamSpleefTeams()) {
            String colorName = capitalize(team.getColor());
            NamedTextColor textColor = getAdventureColor(team.getColor());

            StringBuilder hover = new StringBuilder();
            hover.append("§l").append(getChatColor(colorName)).append("§l").append(colorName)
                    .append(" §lTeam\n").append("§r§f§lPlayers:\n§r");
            List<UUID> plist = team.getPlayers();
            for (int i = 0; i < plist.size(); i++) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(plist.get(i));
                hover.append("§r- §r§f").append(p != null ? p.getName() : "Unknown");
                if (i < plist.size() - 1) hover.append("\n");
            }

            Component comp = Component.text(colorName).color(textColor)
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text(hover.toString())));

            if (draw && team.getPoints() == standings.get(0).getPoints()) {
                drawTeams.add(comp);
            } else if (team.isWinner()) {
                winnerComponent = comp;
                winnerPoints = team.getPoints();
                allPoints.add(winnerPoints);
            } else {
                losingTeams.add(comp);
                allPoints.add(team.getPoints());
            }
        }

        String scoreString = "[" + allPoints.stream().map(String::valueOf).collect(Collectors.joining("-")) + "]";

        Component endMessage = Component.text("SPLEEF ", NamedTextColor.WHITE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);

        if (draw) {
            endMessage = endMessage.append(Component.text("game ended in a draw between ", NamedTextColor.AQUA));
            for (int i = 0; i < drawTeams.size(); i++) {
                if (i > 0 && drawTeams.size() > 2) endMessage = endMessage.append(Component.text(",", NamedTextColor.WHITE));
                if (i == drawTeams.size() - 1 && drawTeams.size() > 1) {
                    endMessage = endMessage.append(Component.text(" and ", NamedTextColor.AQUA));
                }
                endMessage = endMessage.append(drawTeams.get(i));
            }
        } else {
            endMessage = endMessage.append(Component.text("game won by ", NamedTextColor.AQUA))
                    .append(winnerComponent)
                    .append(Component.text(" team against ", NamedTextColor.AQUA));
            for (int i = 0; i < losingTeams.size(); i++) {
                if (i > 0 && losingTeams.size() > 2) endMessage = endMessage.append(Component.text(",", NamedTextColor.WHITE));
                if (i == losingTeams.size() - 1 && losingTeams.size() > 1) {
                    endMessage = endMessage.append(Component.text(" and ", NamedTextColor.AQUA));
                }
                endMessage = endMessage.append(losingTeams.get(i));
            }
        }

        endMessage = endMessage.append(Component.text(" team - " + scoreString, NamedTextColor.WHITE));
        Bukkit.broadcast(endMessage);

        // Discord message
        Function<TeamSpleefTeam, String> teamWithPlayers = (team) -> {
            String teamName = capitalize(team.getColor()) + " Team";
            String players = team.getPlayers().stream()
                    .map(uuid -> {
                        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                        return (p != null && p.getName() != null) ? p.getName() : "Unknown";
                    })
                    .collect(Collectors.joining(", "));
            return teamName + " (" + players + ")";
        };

        BiFunction<List<String>, String, String> joinHuman = (items, lastSep) -> {
            if (items.isEmpty()) return "";
            if (items.size() == 1) return items.get(0);
            if (items.size() == 2) return items.get(0) + " " + lastSep + " " + items.get(1);
            String middle = String.join(", ", items.subList(0, items.size() - 1));
            return middle + ", " + lastSep + " " + items.get(items.size() - 1);
        };

        StringBuilder discordMessage = new StringBuilder("SPLEEF ");
        if (draw) {
            List<TeamSpleefTeam> drawList = standings.stream()
                    .filter(t -> t.getPoints() == standings.get(0).getPoints())
                    .collect(Collectors.toList());
            discordMessage.append("game tied between ")
                    .append(joinHuman.apply(drawList.stream().map(teamWithPlayers).collect(Collectors.toList()), "and"))
                    .append(" [game ended prematurely]");
        } else {
            TeamSpleefTeam winnerTeam = standings.stream().filter(TeamSpleefTeam::isWinner).findFirst().orElse(null);
            List<TeamSpleefTeam> losers = standings.stream().filter(t -> !t.isWinner()).collect(Collectors.toList());
            discordMessage.append("game won by ")
                    .append(winnerTeam != null ? teamWithPlayers.apply(winnerTeam) : "Unknown Team")
                    .append(" against ")
                    .append(joinHuman.apply(losers.stream().map(teamWithPlayers).collect(Collectors.toList()), "and"))
                    .append(" ").append(scoreString);
        }
        sendMessage(discordMessage.toString());

        // Cleanup players
        Location spawn = new Location(Bukkit.getWorld("lobby"), -1114.5, 103, -1061.5);
        spawn.setYaw(90f);

        for (SpleefPlayer sp : this.getPlayers()) {
            if (sp != null && sp.getPlayer() != null && sp.getPlayer().isOnline()) {
                sp.setHasBeenRequested(false);
                sp.setPoints(0);
                sp.getPlayer().getInventory().clear();
                sp.getPlayer().teleport(spawn);
                sp.setInTeamGame(false);
                sp.setSpectating(false);
                sp.setGame(null);
                sp.getPlayer().setGameMode(GameMode.SURVIVAL);
            }
            if (main.getDataLoader().getIceSpawned(sp.getUUID()) != null) {
                main.getDataLoader().removeAllIceSpawned(sp.getUUID());
            }
            if (main.getDataLoader().getDecaySpawned(sp.getUUID()) != null) {
                main.getDataLoader().removeAllDecaySpawned(sp.getUUID());
            }
        }

        // (Spectator cleanup was commented out in original; keep as-is if managed elsewhere)

        if (arena != null) arena.setInGame(false);
        teamGame = false;
        main.getGameManager().getGames().remove(this);

        resetTimer();
    }
    // ---------- TEAM SPLEEF END ----------

    // ============================================================
    // # Timer
    // ============================================================

    public void startTimer(JavaPlugin plugin) {
        if (timerTask != null) timerTask.cancel();
        secondsElapsed = 0;
        roundTime = 0;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsElapsed++;
                roundTime++;
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

    public String getSecondsElapsed() {
        int minutes = secondsElapsed / 60;
        int seconds = secondsElapsed % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void resetTimer() {
        stopTimer();
        secondsElapsed = 0;
        roundTime = 0;
    }

    // ============================================================
    // # Utilities
    // ============================================================

    public Location getSpawnLocation(Location center, double angleDegrees, double distance, float yawf, float pitchf) {
        double angleRadians = Math.toRadians(angleDegrees);
        double xOffset = distance * Math.cos(angleRadians);
        double zOffset = distance * Math.sin(angleRadians);
        Location cloneCenter = center.clone();
        cloneCenter.setYaw(yawf);
        cloneCenter.setPitch(pitchf);
        return cloneCenter.add(xOffset, 1, zOffset);
    }

    public void giveColoredLeatherTunic(Player player, String colorName) {
        if (player == null) return;
        ItemStack tunic = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta) tunic.getItemMeta();
        if (meta != null) {
            meta.setColor(getBukkitColor(colorName));
            tunic.setItemMeta(meta);
        }
        player.getInventory().setChestplate(tunic);
    }

    public String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private String stripColor(String input) {
        return org.bukkit.ChatColor.stripColor(input);
    }

    public void faceLocation(Player player, Location target) {
        if (player == null || target == null) return;
        Location playerLoc = player.getLocation();
        Location targetLoc = target.clone().add(0.5, 0.5, 0.5);

        double dx = targetLoc.getX() - playerLoc.getX();
        double dz = targetLoc.getZ() - playerLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        playerLoc.setYaw(yaw);
        player.teleport(playerLoc);
    }

    public void createGlassCase(Player player) {
        if (player == null) return;
        Location loc = player.getLocation();
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        Material glass = Material.GLASS;
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    Location blockLoc = new Location(player.getWorld(), px + x, py + y, pz + z);
                    Block block = blockLoc.getBlock();
                    block.setType(glass);
                }
            }
        }
        new Location(player.getWorld(), px, py + 2, pz).getBlock().setType(glass);
    }

    public String[] getPlayerPowers(UUID uuid) {
        String[] powers = new String[2];
        String sql = "SELECT power1, power2 FROM player_powers WHERE uuid = ?";
        try (Connection conn = main.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    powers[0] = rs.getString("power1");
                    powers[1] = rs.getString("power2");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return powers;
    }

    public void createCylinder(Location center, Material material, int radius, int height) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        world.getBlockAt(new Location(world, cx + x, cy + y, cz + z)).setType(material);
                    }
                }
            }
        }
    }

    public void startParticles(Location centerf, int radius, int height) {
        if (centerf == null || centerf.getWorld() == null) return;
        final World world = centerf.getWorld();
        final Location center = centerf.clone().add(0.5, 0.0, 0.5);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 72; i++) {
                    double theta = (2 * Math.PI) * i / 72;
                    double x = center.getX() + Math.cos(theta) * radius;
                    double z = center.getZ() + Math.sin(theta) * radius;
                    Location p = new Location(world, x, center.getY() + 1, z);
                    BlockData iceBlock = Material.ICE.createBlockData();
                    world.spawnParticle(Particle.BLOCK, p, 1, 0.02, 0.02, 0.02, 0.0, iceBlock);
                }
                ticks += 5;
            }
        }.runTaskTimer(main, 0L, 5L);
    }

    // ============================================================
    // # Chat / Color helpers
    // ============================================================

    public NamedTextColor getAdventureColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return NamedTextColor.RED;
            case "blue": return NamedTextColor.BLUE;
            case "green": return NamedTextColor.GREEN;
            case "yellow": return NamedTextColor.YELLOW;
            case "orange": return NamedTextColor.GOLD;
            case "purple": return NamedTextColor.LIGHT_PURPLE;
            case "pink": return NamedTextColor.LIGHT_PURPLE;
            case "light blue": return NamedTextColor.AQUA;
            case "magenta": return NamedTextColor.LIGHT_PURPLE;
            case "brown": return NamedTextColor.DARK_RED;
            case "cyan": return NamedTextColor.DARK_AQUA;
            case "light gray": return NamedTextColor.GRAY;
            case "gray": return NamedTextColor.DARK_GRAY;
            case "dark gray": return NamedTextColor.DARK_GRAY;
            case "white": return NamedTextColor.WHITE;
            case "black": return NamedTextColor.BLACK;
            default: return NamedTextColor.WHITE;
        }
    }

    public String getChatColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return "§c";
            case "blue": return "§9";
            case "green": return "§a";
            case "yellow": return "§e";
            case "orange": return "§6";
            case "purple": return "§5";
            case "pink": return "§d";
            case "light blue": return "§b";
            case "magenta": return "§d";
            case "brown": return "§6";
            case "cyan": return "§3";
            case "light gray": return "§7";
            case "gray": return "§8";
            case "dark gray": return "§8";
            case "white": return "§f";
            case "black": return "§0";
            default: return "§f";
        }
    }

    private Color getBukkitColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return Color.fromRGB(151, 52, 49);
            case "blue": return Color.fromRGB(60, 68, 170);
            case "green": return Color.fromRGB(72, 179, 58);
            case "yellow": return Color.fromRGB(249, 236, 78);
            case "orange": return Color.fromRGB(249, 128, 29);
            case "purple": return Color.fromRGB(137, 50, 183);
            case "pink": return Color.fromRGB(243, 139, 170);
            case "light blue": return Color.fromRGB(58, 179, 218);
            case "magenta": return Color.fromRGB(200, 78, 189);
            case "brown": return Color.fromRGB(131, 84, 50);
            case "cyan": return Color.fromRGB(22, 156, 156);
            case "light gray": return Color.fromRGB(157, 157, 151);
            case "gray": return Color.fromRGB(71, 79, 82);
            case "dark gray": return Color.fromRGB(41, 49, 51);
            case "white": return Color.fromRGB(255, 255, 255);
            case "black": return Color.fromRGB(29, 29, 33);
            default: return Color.WHITE;
        }
    }

    // ============================================================
    // # Events
    // ============================================================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setDropItems(false);
        SpleefPlayer sp = main.getDataLoader().getSpleefPlayer(event.getPlayer());
        if (sp != null && sp.isRoundStarting()) {
            event.setCancelled(true);
        }
    }

    // ============================================================
    // # Discord
    // ============================================================

    /** NOTE: original method was spelled sendMessaage; renamed and fixed. */
    public static void sendMessage(String message) {
        TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById("1401653713889788065");
        if (channel != null) channel.sendMessage(message).queue();
    }

    // ============================================================
    // # Formatting helpers (kept; used by message builders)
    // ============================================================

    private String formatTeamListColoredAnd(List<String> teams, String accentColor) {
        if (teams.isEmpty()) return "Unknown";
        if (teams.size() == 1) return teams.get(0);
        if (teams.size() == 2) return teams.get(0) + accentColor + " and " + teams.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            if (i == teams.size() - 1) sb.append(accentColor).append("and ").append(teams.get(i));
            else sb.append(teams.get(i)).append(", ");
        }
        return sb.toString();
    }

    private String formatTeamListPlain(List<String> teams) {
        List<String> stripped = teams.stream().map(this::stripColor).collect(Collectors.toList());
        if (stripped.isEmpty()) return "Unknown";
        if (stripped.size() == 1) return stripped.get(0);
        if (stripped.size() == 2) return stripped.get(0) + " and " + stripped.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stripped.size(); i++) {
            if (i == stripped.size() - 1) sb.append("and ").append(stripped.get(i));
            else sb.append(stripped.get(i)).append(", ");
        }
        return sb.toString();
    }
}

