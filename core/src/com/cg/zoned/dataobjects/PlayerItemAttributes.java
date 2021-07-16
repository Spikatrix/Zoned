package com.cg.zoned.dataobjects;

public class PlayerItemAttributes {
    private String name;
    private boolean ready;
    private boolean inGame;
    private int colorIndex;
    private int startPosIndex;

    public PlayerItemAttributes(String name, boolean ready, boolean inGame, int colorIndex, int startPosIndex) {
        this.name = name;
        this.ready = ready;
        this.inGame = inGame;
        this.colorIndex = colorIndex;
        this.startPosIndex = startPosIndex;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    public void setStartPosIndex(int startPosIndex) {
        this.startPosIndex = startPosIndex;
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isInGame() {
        return inGame;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public int getStartPosIndex() {
        return startPosIndex;
    }
}
