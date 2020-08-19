package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScoreBar {
    private static final float SCOREBAR_LERP_VALUE = 3.0f;

    public float scoreBarHeight;

    private float totalWidth;
    private float totalHeight;

    private float[] scoreBarWidths;

    public ScoreBar(Viewport viewport, int size, float scaleFactor) {
        scoreBarWidths = new float[size];

        totalWidth = viewport.getWorldWidth();
        totalHeight = viewport.getWorldHeight();

        scoreBarHeight = 16f * scaleFactor;
    }

    public void resize(int width, int height) {
        totalWidth = width;
        totalHeight = height;
    }

    public void render(ShapeDrawer shapeDrawer, BitmapFont font, Array<TeamData> teamData, float delta) {
        float currentWidthPos = 0;
        float offsetY = totalHeight - scoreBarHeight;

        float totalScore = 0;
        for (TeamData td : teamData) {
            totalScore += td.score;
        }

        if (totalScore == 0) {
            return;
        }

        float shadowHeight = Math.min(scoreBarHeight, 10f);
        shapeDrawer.filledRectangle(0, totalHeight, totalWidth, -(shadowHeight + scoreBarHeight),
                Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        for (int i = 0; i < teamData.size; i++) {
            float barWidth = ((teamData.get(i).score / totalScore) * totalWidth);
            float drawWidth = scoreBarWidths[i] + (barWidth - scoreBarWidths[i]) * SCOREBAR_LERP_VALUE * delta;

            shapeDrawer.setColor(teamData.get(i).color);
            font.setColor(getGoodTextColor(teamData.get(i).color));

            float barStartX;
            if (i == 0) {                                                         // First bar
                barStartX = currentWidthPos;
                shapeDrawer.filledRectangle(barStartX, offsetY, drawWidth, scoreBarHeight);
            } else if (i == teamData.size - 1) {                                  // Last bar
                barStartX = totalWidth - drawWidth;
                shapeDrawer.filledRectangle(barStartX, offsetY, drawWidth, scoreBarHeight);   // Can draw upto drawWidth or totalWidth. Should be the same
            } else {                                                              // Mid bar(s)
                barStartX = currentWidthPos;
                shapeDrawer.filledRectangle(barStartX + (barWidth / 2), offsetY, -drawWidth / 2, scoreBarHeight);
                shapeDrawer.filledRectangle(barStartX + (barWidth / 2), offsetY, drawWidth / 2, scoreBarHeight);
                //TODO: Need to polish this
            }
            scoreBarWidths[i] = drawWidth;

            font.draw(shapeDrawer.getBatch(), String.valueOf(teamData.get(i).score),
                    barStartX, totalHeight - (scoreBarHeight / 2) + (font.getLineHeight() / 4),
                    scoreBarWidths[i], Align.center, false);

            currentWidthPos += drawWidth;
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
