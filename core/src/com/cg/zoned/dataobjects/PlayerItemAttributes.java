package com.cg.zoned.dataobjects;

public class PlayerItemAttributes {
    private String name;
    private boolean ready;
    private int colorIndex;
    private int startPosIndex;

    public PlayerItemAttributes(String name, boolean ready, int colorIndex, int startPosIndex) {
        this.name = name;
        this.ready = ready;
        this.colorIndex = colorIndex;
        this.startPosIndex = startPosIndex;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
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

    public int getColorIndex() {
        return colorIndex;
    }

    public int getStartPosIndex() {
        return startPosIndex;
    }
}
