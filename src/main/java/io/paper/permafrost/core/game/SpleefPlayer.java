package io.paper.permafrost.core.game;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.sk89q.worldedit.internal.util.AbstractAdapter;
import io.paper.permafrost.Main;
import io.paper.permafrost.parkour.ParkourGame;
import io.paper.permafrost.utilities.ShovelPowers;

public class SpleefPlayer {
	private UUID uuid;

	private ParkourGame parkourGame;
	
	private boolean isInParkour;
	
	private int onlinetime;

	private int totalonlinetime;

	private Date lastlogin;

	private Date registerdate;

	private boolean fly;

	private boolean resetStarting;

	private String ip;

	boolean inGame;

	boolean inTeamGame;
	
	boolean available;

	int points;

	boolean dead;

	boolean onSwapCooldown;
	
	boolean isSilenced;

	int timeLeft;
	
	private SpleggGame spleggGame;
	
	private ShovelPowers randomPower;
	private ShovelPowers randomPower2;
	
	private SpleefSpectator spectator;

	private boolean acceptedReset;
	private boolean acceptedEndgame;
	private boolean acceptedPlayto;

	TeamSpleefTeam team;

	TeamSpleefTeam permaTeam;

	private boolean roundStarting;

	private String country;

	private Location location;

	private long lastActivationTick = -1;

	private boolean isHidingSpectators;

	private ShovelPowers selectedPower;
	private ShovelPowers deselectedPower;
	private ShovelPowers cooldown1;
	private ShovelPowers cooldown2;
	
	private boolean dashCooldown;
	
	private int dashTimeLeft;

	private List<SpleefDuel> duels = new ArrayList<>();

	private int spleef_elo;
	private int splegg_elo;
	private int tntrun_elo;

	boolean winner;

	private SpleefGame game;

	private boolean isSpectating;

	private boolean hasBeenDuelRequested;

	private Main main;

	public SpleefPlayer(Main main, UUID uuid) {
		this.main = main;
		this.uuid = uuid;
		main.getDataLoader().addPlayer(uuid, this);
		inGame = false;
		inTeamGame = false;
		points = 0;
		winner = false;
		roundStarting = false;
	}

	public long getLastActivationTick() {
		return lastActivationTick;
	}

	public void setLastActivationTick(long tick) {
		this.lastActivationTick = tick;
	}

	public boolean isAvailable() {
		return available;
	}
	
	public void setAvailable(boolean available) {
		this.available = available;
	}
	
	public boolean isSilenced() {
		return isSilenced;
	}
	
	public void setSilenced(boolean isSilenced) {
		this.isSilenced = isSilenced;
	}
	
	public SpleefSpectator getSpectator() {
		return this.spectator;
	}

	public void setSpectator(SpleefSpectator spectator) {
		this.spectator = spectator;
	}

	public boolean getHasBeenRequested() {
		return hasBeenDuelRequested;
	}

	public void setHasBeenRequested(boolean hasBeenDuelRequested) {
		this.hasBeenDuelRequested = hasBeenDuelRequested;
	}

	public ShovelPowers getSelectedPower() {
		return selectedPower;
	}

	public void setSelectedPower(ShovelPowers selectedPower) {
		this.selectedPower = selectedPower;
	}

	public ShovelPowers getDeselectedPower() {
		return deselectedPower;
	}

	public void setDeselectedPower(ShovelPowers deselectedPower) {
		this.deselectedPower = deselectedPower;
	}

	public String getFormattedSelectedPower() {
		String str = "";
		switch (selectedPower) {
		case ShovelPowers.SPEED: {
			str = "SPEED";
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			str = "WIND";
			break;
		}
		case ShovelPowers.BLACKGOO: {
			str = "GOO";
			break;
		}
		case ShovelPowers.LEVITATION: {
			str = "LEVITATE";
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			str = "ICE";
			break;
		}
		case ShovelPowers.BUILDER: {
			str = "BUILD";
			break;
		}
		case ShovelPowers.TNTRUN: {
			str = "TNTRUN";
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			str = "DECAY";
			break;
		}
		case ShovelPowers.SWAP: {
			str = "SWAP";
			break;
		}
		case ShovelPowers.SILENCER: {
			str = "SILENCE";
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			str = "DOUBLE";
			break;
		}
		case ShovelPowers.SNARE: {
			str = "SNARE";
			break;
		}
		case ShovelPowers.RECALL: {
			str = "RECALL";
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			str = "SMOKE";
			break;
		}
		case ShovelPowers.ROULETTE: {
			str = "ROULETTE";
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			str = "VORTEX";
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			str = "SHIELD";
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			str = "PEARL";
			break;
		}
		}
		return str;
	}
	
	public String getFormattedRandomPower() {
		String str = "";
		switch (randomPower) {
		case ShovelPowers.SPEED: {
			str = "SPEED";
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			str = "WIND";
			break;
		}
		case ShovelPowers.BLACKGOO: {
			str = "GOO";
			break;
		}
		case ShovelPowers.LEVITATION: {
			str = "LEVITATE";
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			str = "ICE";
			break;
		}
		case ShovelPowers.BUILDER: {
			str = "BUILD";
			break;
		}
		case ShovelPowers.TNTRUN: {
			str = "TNTRUN";
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			str = "DECAY";
			break;
		}
		case ShovelPowers.SWAP: {
			str = "SWAP";
			break;
		}
		case ShovelPowers.SILENCER: {
			str = "SILENCE";
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			str = "DOUBLE";
			break;
		}
		case ShovelPowers.SNARE: {
			str = "SNARE";
			break;
		}
		case ShovelPowers.RECALL: {
			str = "RECALL";
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			str = "SMOKE";
			break;
		}
		case ShovelPowers.ROULETTE: {
			str = "ROULETTE";
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			str = "VORTEX";
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			str = "SHIELD";
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			str = "PEARL";
			break;
		}
		}
		return str;
	}
	
	public String getFormattedRandomPower2() {
		String str = "";
		switch (randomPower2) {
		case ShovelPowers.SPEED: {
			str = "SPEED";
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			str = "WIND";
			break;
		}
		case ShovelPowers.BLACKGOO: {
			str = "GOO";
			break;
		}
		case ShovelPowers.LEVITATION: {
			str = "LEVITATE";
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			str = "ICE";
			break;
		}
		case ShovelPowers.BUILDER: {
			str = "BUILD";
			break;
		}
		case ShovelPowers.TNTRUN: {
			str = "TNTRUN";
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			str = "DECAY";
			break;
		}
		case ShovelPowers.SWAP: {
			str = "SWAP";
			break;
		}
		case ShovelPowers.SILENCER: {
			str = "SILENCE";
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			str = "DOUBLE";
			break;
		}
		case ShovelPowers.SNARE: {
			str = "SNARE";
			break;
		}
		case ShovelPowers.RECALL: {
			str = "RECALL";
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			str = "SMOKE";
			break;
		}
		case ShovelPowers.ROULETTE: {
			str = "ROULETTE";
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			str = "VORTEX";
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			str = "SHIELD";
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			str = "PEARL";
			break;
		}
		}
		return str;
	}
	
	public ShovelPowers getRandomPower() {
		return randomPower;
	}
	
	public void setRandomPower(ShovelPowers power) {
		this.randomPower = power;
	}
	
	public ShovelPowers getRandomPower2() {
		return randomPower2;
	}
	
	public void setRandomPower2(ShovelPowers power) {
		this.randomPower2 = power;
	}

	public String getFormattedDeselectedPower() {
		String str = "";
		switch (deselectedPower) {
		case ShovelPowers.SPEED: {
			str = "SPEED";
			break;
		}
		case ShovelPowers.WINDCHARGE: {
			str = "WIND";
			break;
		}
		case ShovelPowers.BLACKGOO: {
			str = "GOO";
			break;
		}
		case ShovelPowers.LEVITATION: {
			str = "LEVITATE";
			break;
		}
		case ShovelPowers.ICEGRENADE: {
			str = "ICE";
			break;
		}
		case ShovelPowers.BUILDER: {
			str = "BUILD";
			break;
		}
		case ShovelPowers.TNTRUN: {
			str = "TNTRUN";
			break;
		}
		case ShovelPowers.DECAYGRENADE: {
			str = "DECAY";
			break;
		}
		case ShovelPowers.SWAP: {
			str = "SWAP";
			break;
		}
		case ShovelPowers.SILENCER: {
			str = "SILENCE";
			break;
		}
		case ShovelPowers.DOUBLEJUMP: {
			str = "DOUBLE";
			break;
		}
		case ShovelPowers.SNARE: {
			str = "SNARE";
			break;
		}
		case ShovelPowers.RECALL: {
			str = "RECALL";
			break;
		}
		case ShovelPowers.SMOKEGRENADE: {
			str = "SMOKE";
			break;
		}
		case ShovelPowers.ROULETTE: {
			str = "ROULETTE";
			break;
		}
		case ShovelPowers.VORTEXGRENADE: {
			str = "VORTEX";
			break;
		}
		case ShovelPowers.PURGINGSHIELD: {
			str = "SHIELD";
			break;
		}
		case ShovelPowers.ENDERPEARL: {
			str = "PEARL";
			break;
		}
		}
		return str;
	}

	public ShovelPowers getCooldown1() {
		return cooldown1;
	}

	public void setCooldown1(ShovelPowers cooldown1) {
		this.cooldown1 = cooldown1;
	}

	public ShovelPowers getCooldown2() {
		return cooldown2;
	}

	public void setCooldown2(ShovelPowers cooldown2) {
		this.cooldown2 = cooldown2;
	}

	public boolean isOnCooldown() {
		return cooldown1 != null || cooldown2 != null;
	}
	
	public boolean isOnDashCooldown() {
		return dashCooldown;
	}
	
	public void setDashCooldown(boolean dashCooldown) {
		this.dashCooldown = dashCooldown;
	}

	public boolean hasCooldown(ShovelPowers p) {
		if (p == null)
			return false;
		return p.equals(cooldown1) || p.equals(cooldown2);
	}

	public boolean isOnSwapCooldown() {
		return onSwapCooldown;
	}

	public void setOnSwapCooldown(boolean onSwapCooldown) {
		this.onSwapCooldown = onSwapCooldown;
	}

	public int getTimeLeft() {
		return timeLeft;
	}

	public void setTimeLeft(int timeLeft) {
		this.timeLeft = timeLeft;
	}
	
	public int getDashTimeLeft() {
		return dashTimeLeft;
	}

	public void setDashTimeLeft(int dashTimeLeft) {
		this.dashTimeLeft = dashTimeLeft;
	}

	public boolean getInGame() {
		return inGame;
	}

	public void setInGame(boolean b) {
		inGame = b;
	}
	
	public void setParkourGame(ParkourGame game) {
		this.parkourGame = game;
	}
	
	public ParkourGame getParkourGame() {
		return parkourGame;
	}
	
	public void setInParkourGame(boolean isInParkour) {
		this.isInParkour = isInParkour;
	}
	
	public boolean getInParkourGame() {
		return isInParkour;
	}

	public boolean getInTeamGame() {
		return inTeamGame;
	}

	public void setInTeamGame(boolean b) {
		inTeamGame = b;
	}

	public void addPoint() {
		points++;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int i) {
		points = i;
	}

	public int getELO(SpleefType spleefType) {
		switch (spleefType) {
		case SPLEEF:
			return this.spleef_elo;
		case SPLEGG:
			return this.splegg_elo;
		case TNTRUN:
			return this.tntrun_elo;
		}

		return 0;
	}

	public void setELO(SpleefType spleefType, int newELO) {
		switch (spleefType) {
		case SPLEEF:
			this.spleef_elo = newELO;
			break;
		case SPLEGG:
			this.splegg_elo = newELO;
			break;
		case TNTRUN:
			this.tntrun_elo = newELO;
			break;
		}
	}

	public int getSpleefELO() {
		return spleef_elo;
	}

	public void setSpleefELO(int spleef_elo) {
		this.spleef_elo = spleef_elo;
	}

	public int getSpleggELO() {
		return splegg_elo;
	}

	public void setSpleggELO(int splegg_elo) {
		this.splegg_elo = splegg_elo;
	}

	public int getTntrunELO() {
		return tntrun_elo;
	}

	public void setTntrunELO(int tntrun_elo) {
		this.tntrun_elo = tntrun_elo;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public boolean isHidingSpectators() {
		return this.isHidingSpectators;
	}

	public void hideSpectators(boolean b) {
		this.isHidingSpectators = b;
	}

	public Date getRegisterDate() {
		return this.registerdate;
	}

	public void setRegisterDate(Date d) {
		this.registerdate = d;
	}

	public Date getLastLogin() {
		return this.lastlogin;
	}

	public void setLastLogin(Date d) {
		this.lastlogin = d;
	}

	public SpleefGame getGame() {
		return this.game;
	}

	public void setGame(SpleefGame game) {
		this.game = game;
	}
	
	public SpleggGame getSpleggGame() {
		return this.spleggGame;
	}

	public void setSpleggGame(SpleggGame spleggGame) {
		this.spleggGame = spleggGame;
	}

	public void setSpectating(boolean b) {
		this.isSpectating = b;
	}

	public boolean isSpectating() {
		return this.isSpectating;
	}

	public void setRoundStarting(boolean roundStarting) {
		this.roundStarting = roundStarting;
	}

	public boolean isRoundStarting() {
		return roundStarting;
	}

	public void setResetStarting(boolean resetStarting) {
		this.resetStarting = resetStarting;
	}

	public boolean isResetStarting() {
		return resetStarting;
	}

	public void setAcceptedReset(boolean acceptedReset) {
		this.acceptedReset = acceptedReset;
	}

	public boolean hasAcceptedReset() {
		return acceptedReset;
	}

	public void setAcceptedEndgame(boolean acceptedEndgame) {
		this.acceptedEndgame = acceptedEndgame;
	}

	public boolean hasAcceptedEndgame() {
		return acceptedEndgame;
	}

	public void setAcceptedPlayto(boolean acceptedPlayto) {
		this.acceptedPlayto = acceptedPlayto;
	}

	public boolean hasAcceptedPlayto() {
		return acceptedPlayto;
	}

	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(this.uuid);
	}

	public List<SpleefDuel> getDuels() {
		return this.duels;
	}

	public void setWinner(boolean b) {
		winner = b;
	}

	public SpleefPlayer getWinner(SpleefPlayer sp1, SpleefPlayer sp2) {
		if (sp1.getPoints() > sp2.getPoints()) {
			sp1.setWinner(true);
			return sp1;
		} else if (sp1.getPoints() < sp2.getPoints()) {
			sp2.setWinner(true);
			return sp2;
		}
		return null;
	}

	public boolean isWinner() {
		return winner;
	}

	public SpleefDuel getDuelByUUID(UUID uuid) {
		for (SpleefDuel d : this.duels) {
			if (d.getUUID().equals(uuid))
				return d;
		}
		return null;
	}

	public boolean hasDueled(SpleefPlayer sp) {
		for (SpleefDuel sd : this.duels) {
			if (sd.getDueledPlayers().contains(sp))
				return true;
		}
		return false;
	}

	public SpleefDuel getDuelByDueledPlayer(SpleefPlayer sp) {
		for (SpleefDuel sd : this.duels) {
			if (sd.getDueledPlayers().contains(sp))
				return sd;
		}
		return null;
	}

	public String getCountry() {
		return this.country;
	}

	public void setCountry(String s) {
		this.country = s;
	}

	public int getTotalOnlineTime() {
		return this.totalonlinetime;
	}

	public void setTotalOnlineTime(int i) {
		this.totalonlinetime = i;
	}

	public int getOnlineTime() {
		return this.onlinetime;
	}

	public void addOnlineTime() {
		this.onlinetime++;
		this.totalonlinetime++;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public String getIP() {
		return this.ip;
	}

	public void setIP(String ip) {
		this.ip = ip;
	}

	public void fly() {
		this.fly = true;
		(new BukkitRunnable() {
			public void run() {
				SpleefPlayer.this.getPlayer().setAllowFlight(true);
				SpleefPlayer.this.getPlayer().setFlying(true);
			}
		}).runTask(main);
	}

	public void stopfly() {
		this.fly = false;
		(new BukkitRunnable() {
			public void run() {
				SpleefPlayer.this.getPlayer().setAllowFlight(false);
				SpleefPlayer.this.getPlayer().setFlying(false);
			}
		}).runTask(main);
	}

	public boolean isFlying() {
		return this.fly;
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(this.uuid);
	}

	public String getName() {
		return getOfflinePlayer().getName();
	}

	public void sendMessage(final String message) {
		if (getOfflinePlayer().isOnline())
			if (Bukkit.isPrimaryThread()) {
				getPlayer().sendMessage(message);
			} else {
				(new BukkitRunnable() {
					public void run() {
						if (SpleefPlayer.this.getOfflinePlayer().isOnline())
							SpleefPlayer.this.getPlayer().sendMessage(message);
					}
				}).runTask(main);
			}
	}

	public TeamSpleefTeam getTeam() {
		return this.team;
	}

	public void setTeam(TeamSpleefTeam team) {
		this.team = team;
	}

	public boolean isDead() {
		return dead;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}

	public boolean isOnline() {
		return getOfflinePlayer().isOnline();
	}

	@Override
	public String toString() {
		return "SpleefPlayer [uuid=" + uuid + ", onlinetime=" + onlinetime + ", totalonlinetime=" + totalonlinetime
				+ ", country=" + country + ", location=" + location + "]";
	}

}
