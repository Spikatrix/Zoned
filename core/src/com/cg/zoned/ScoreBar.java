package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ScoreBar {
    public static final float BAR_HEIGHT = 10f;

    private float totalWidth;
    private float totalHeight;

    private float[] currentPos;

    public ScoreBar(int size) {
        currentPos = new float[size];

        totalWidth = Gdx.graphics.getWidth();
        totalHeight = Gdx.graphics.getHeight();
    }

    public void resize(int width, int height) {
        totalWidth = width;
        totalHeight = height;
    }

    public void render(ShapeRenderer renderer, Player[] players, float delta) {
        float lerpVal = 3.0f;

        float currentWidthPos = 0;
        float offsetY = totalHeight - BAR_HEIGHT;

        float totalScore = 0;
        for (Player player : players) {
            totalScore += player.score;
        }

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < players.length; i++) {
            float barWidth = ((players[i].score / totalScore) * totalWidth);
            float drawWidth = currentPos[i] + (barWidth - currentPos[i]) * lerpVal * delta;

            renderer.setColor(players[i].color);

            if (i == 0) {                                                                 // First bar
                renderer.rect(currentWidthPos, offsetY, drawWidth, BAR_HEIGHT);
            } else if (i == players.length - 1) {                                    // Last bar
                renderer.rect(totalWidth - drawWidth, offsetY, drawWidth, BAR_HEIGHT);   // Can draw upto drawWidth or totalWidth. Should be the same
            } else {                                                                      // Mid bar(s)
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, (drawWidth / 2), BAR_HEIGHT);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, -drawWidth / 2, BAR_HEIGHT);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, drawWidth / 2, BAR_HEIGHT);
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, drawWidth, BAR_HEIGHT);
                //TODO: Need to polish this I guess?
                //TODO: Scorebar for teams
            }

            currentPos[i] = drawWidth;
            currentWidthPos += drawWidth;
        }

        // Should we fix the line being annoyingly seen even when the scorebar has not been drawn yet? TODO
        renderer.rect(0, offsetY - BAR_HEIGHT, totalWidth, BAR_HEIGHT, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        renderer.end();
    }
}
