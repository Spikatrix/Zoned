package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

public class TeamData {
    public Color color;
    public int score;

    public TeamData(Color color, int score) {
        this.color = color;
        this.score = score;
    }

    public TeamData(Color color) {
        this.color = color;
        this.score = 0;
    }
}
