package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScoreBar {
    public static final float BAR_HEIGHT = 10f;

    private float totalWidth;
    private float totalHeight;

    private float[] currentPos;

    public ScoreBar(Viewport viewport, int size) {
        currentPos = new float[size];

        totalWidth = viewport.getWorldWidth();
        totalHeight = viewport.getWorldHeight();
    }

    public void resize(int width, int height) {
        totalWidth = width;
        totalHeight = height;
    }

    public void render(ShapeRenderer renderer, SpriteBatch batch, BitmapFont font, Player[] players, float delta) {
        float lerpVal = 3.0f;

        float currentWidthPos = 0;
        float offsetY = totalHeight - BAR_HEIGHT;

        float totalScore = 0;
        for (Player player : players) {
            totalScore += player.score;
        }

        if (totalScore == 0) {
            return;
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < players.length; i++) {
            float barWidth = ((players[i].score / totalScore) * totalWidth);
            float drawWidth = currentPos[i] + (barWidth - currentPos[i]) * lerpVal * delta;

            renderer.setColor(players[i].color);

            if (i == 0) {                                                                 // First bar
                renderer.rect(currentWidthPos, offsetY, drawWidth, BAR_HEIGHT);

                font.setColor(Color.WHITE);
                font.draw(batch, players[i].name, drawWidth / 2, totalHeight - BAR_HEIGHT);
                // TODO: Try to make the text appear on top of renderer stuff

            } else if (i == players.length - 1) {                                         // Last bar
                renderer.rect(totalWidth - drawWidth, offsetY, drawWidth, BAR_HEIGHT);   // Can draw upto drawWidth or totalWidth. Should be the same
            } else {                                                                      // Mid bar(s)
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, (drawWidth / 2), BAR_HEIGHT);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, -drawWidth / 2, BAR_HEIGHT);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, drawWidth / 2, BAR_HEIGHT);
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, drawWidth, BAR_HEIGHT);
                //TODO: Need to polish this
                //TODO: Scorebar for teams
            }

            currentPos[i] = drawWidth;
            currentWidthPos += drawWidth;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // TODO: Should we fix the line being annoyingly seen even when the scorebar has not been drawn yet?
        renderer.rect(0, offsetY - BAR_HEIGHT, totalWidth, BAR_HEIGHT, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        renderer.end();
        batch.end();
    }
}
