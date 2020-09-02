package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

public class TeamData {
    private Color color;
    private int score;

    public TeamData(Color color) {
        this.color = color;
        this.score = 0;
    }

    public void incrementScore() {
        score++;
    }

    public void resetScore() {
        score = 0;
    }

    public Color getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }
}
