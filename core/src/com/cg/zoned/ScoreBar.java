package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScoreBar {
    public float scoreBarHeight;

    private float totalWidth;
    private float totalHeight;

    private float[] currentPos;
    private float[] scoreBarStartX;
    private float[] scoreBarWidths;

    public ScoreBar(Viewport viewport, int size, float scaleFactor) {
        currentPos = new float[size];
        scoreBarStartX = new float[size];
        scoreBarWidths = new float[size];

        totalWidth = viewport.getWorldWidth();
        totalHeight = viewport.getWorldHeight();

        scoreBarHeight = 16f * scaleFactor;
    }

    public void resize(int width, int height) {
        totalWidth = width;
        totalHeight = height;
    }

    public void render(ShapeRenderer renderer, SpriteBatch batch, BitmapFont font, Player[] players, float delta) {
        float lerpVal = 3.0f;

        float currentWidthPos = 0;
        float offsetY = totalHeight - scoreBarHeight;

        float totalScore = 0;
        for (Player player : players) {
            totalScore += player.score;
        }

        if (totalScore == 0) {
            return;
        }

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < players.length; i++) {
            float barWidth = ((players[i].score / totalScore) * totalWidth);
            float drawWidth = currentPos[i] + (barWidth - currentPos[i]) * lerpVal * delta;

            renderer.setColor(players[i].color);

            if (i == 0) {                                                                 // First bar
                renderer.rect(currentWidthPos, offsetY, drawWidth, scoreBarHeight);
                scoreBarStartX[i] = currentWidthPos;
            } else if (i == players.length - 1) {                                         // Last bar
                renderer.rect(totalWidth - drawWidth, offsetY, drawWidth, scoreBarHeight);   // Can draw upto drawWidth or totalWidth. Should be the same
                scoreBarStartX[i] = totalWidth - drawWidth;
            } else {                                                                      // Mid bar(s)
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, (drawWidth / 2), BAR_HEIGHT);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, -drawWidth / 2, scoreBarHeight);
                renderer.rect(currentWidthPos + (barWidth / 2), offsetY, drawWidth / 2, scoreBarHeight);
                //renderer.rect(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, drawWidth, BAR_HEIGHT);
                //TODO: Need to polish this
                //TODO: Scorebar for teams
                scoreBarStartX[i] = currentWidthPos;
            }
            scoreBarWidths[i] = drawWidth;

            currentPos[i] = drawWidth;
            currentWidthPos += drawWidth;
        }

        // TODO: Should we fix the line being annoyingly seen even when the scorebar has not been drawn yet?
        renderer.rect(0, offsetY - scoreBarHeight, totalWidth, scoreBarHeight, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        renderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        for (int i = 0; i < scoreBarWidths.length; i++) {
            font.setColor(Color.WHITE);

            font.draw(batch, String.valueOf(players[i].score),
                    scoreBarStartX[i], totalHeight - (scoreBarHeight / 4),
                    scoreBarWidths[i], Align.center, false);
        }
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
