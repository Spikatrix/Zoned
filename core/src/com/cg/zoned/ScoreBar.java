package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
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

    public void render(ShapeDrawer shapeDrawer, BitmapFont font, Player[] players, float delta) {
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

        float shadowHeight = Math.min(scoreBarHeight, 10f);
        shapeDrawer.filledRectangle(0, totalHeight, totalWidth, -(shadowHeight + scoreBarHeight),
                Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        for (int i = 0; i < players.length; i++) {
            float barWidth = ((players[i].score / totalScore) * totalWidth);
            float drawWidth = currentPos[i] + (barWidth - currentPos[i]) * lerpVal * delta;

            shapeDrawer.setColor(players[i].color);

            if (i == 0) {                                                                 // First bar
                shapeDrawer.filledRectangle(currentWidthPos, offsetY, drawWidth, scoreBarHeight);
                scoreBarStartX[i] = currentWidthPos;
            } else if (i == players.length - 1) {                                         // Last bar
                shapeDrawer.filledRectangle(totalWidth - drawWidth, offsetY, drawWidth, scoreBarHeight);   // Can draw upto drawWidth or totalWidth. Should be the same
                scoreBarStartX[i] = totalWidth - drawWidth;
            } else {                                                                      // Mid bar(s)
                //shapeDrawer.filledRectangle(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, (drawWidth / 2), BAR_HEIGHT);
                shapeDrawer.filledRectangle(currentWidthPos + (barWidth / 2), offsetY, -drawWidth / 2, scoreBarHeight);
                shapeDrawer.filledRectangle(currentWidthPos + (barWidth / 2), offsetY, drawWidth / 2, scoreBarHeight);
                //shapeDrawer.filledRectangle(currentWidthPos + (barWidth / 2) - (drawWidth / 2), offsetY, drawWidth, BAR_HEIGHT);
                //TODO: Need to polish this
                //TODO: Scorebar for teams
                scoreBarStartX[i] = currentWidthPos;
            }
            scoreBarWidths[i] = drawWidth;

            currentPos[i] = drawWidth;
            currentWidthPos += drawWidth;
        }

        for (int i = 0; i < scoreBarWidths.length; i++) {
            font.setColor(getGoodTextColor(players[i].color));
            font.draw(shapeDrawer.getBatch(), String.valueOf(players[i].score),
                    scoreBarStartX[i], totalHeight - (scoreBarHeight / 2) + (font.getLineHeight() / 4),
                    scoreBarWidths[i], Align.center, false);
        }
    }

    private Color getGoodTextColor(Color bgColor) {
        // I forgot where I got this expression from...
        int o = MathUtils.round(((bgColor.r * 299) +
                (bgColor.g * 587) +
                (bgColor.b * 114)) / 1000);

        if (o > 125) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }
}
