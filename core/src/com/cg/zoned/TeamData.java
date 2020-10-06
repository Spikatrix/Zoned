package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

public class TeamData {
    private Color color;
    private int score;
    private double capturePercentage;

    public TeamData(Color color) {
        this.color = color;
        this.score = 0;
        this.capturePercentage = 0f;
    }

    public void incrementScore() {
        score++;
    }

    public void incrementScore(int amount) {
        score += amount;
    }

    public void resetScore() {
        score = 0;
    }

    public void setCapturePercentage(int total) {
        capturePercentage = 100 * ((double) score / total);
    }

    public void roundCapturePercentage(DoubleFormatter df) {
        capturePercentage = df.formatDouble(capturePercentage);
    }

    public double getCapturePercentage() {
        return capturePercentage;
    }

    public Color getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }
}
