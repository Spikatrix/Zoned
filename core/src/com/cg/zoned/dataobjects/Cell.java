package com.cg.zoned.dataobjects;

import com.badlogic.gdx.graphics.Color;

public class Cell {
    public Color cellColor;
    public int playerCount;
    public boolean isMovable;

    public Cell() {
        this.cellColor = null; // No color for the cell
        this.playerCount = 0;  // No player on the cell
        this.isMovable = true; // Cell isn't a wall
    }
}
