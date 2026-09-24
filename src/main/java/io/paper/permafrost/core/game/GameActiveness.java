package io.paper.permafrost.core.game;

public class GameActiveness {
    private Arena arena;
    private boolean gameActive;

    public GameActiveness(Arena arena, boolean gameActive) {
        this.arena = arena;
        this.gameActive = gameActive;
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public void setGameActive(boolean gameActive) {
        this.gameActive = gameActive;
    }

    public Arena getArena() {
        return arena;
    }
}

