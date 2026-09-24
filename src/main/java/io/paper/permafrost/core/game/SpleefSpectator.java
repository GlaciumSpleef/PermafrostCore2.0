package io.paper.permafrost.core.game;

import java.util.Set;
import java.util.UUID;

import io.paper.permafrost.Main;

public class SpleefSpectator {
	private Main main;
	private UUID uuid;
	private SpleefGame game;
	private boolean inGame;
	private boolean inTeamGame;
	
	public SpleefSpectator(Main main, UUID uuid) {
		this.main = main;
		this.uuid = uuid;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public SpleefGame getGame() {
		return this.game;
	}
	
	public void setGame(SpleefGame game) {
		this.game = game;
	}
	
	public boolean isInGame() {
		return inGame;
	}
	
	public boolean isInTeamGame() {
		return inTeamGame;
	}
	
	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}
	
	public void setInTeamGame(boolean inTeamGame) {
		this.inTeamGame = inTeamGame;
	}
}
