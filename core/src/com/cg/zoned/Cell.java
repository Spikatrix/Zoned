package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

public class Cell {
    public Color cellColor;
    public int noOfPlayers;
    public boolean isMovable;

    public Cell() {
        this.cellColor = null; // No color for the cell
        this.noOfPlayers = 0;  // No player on the cell
        this.isMovable = true; // Cell isn't a wall
    }
}
