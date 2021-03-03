package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.dataobjects.TeamData;

public class ScoreBar {
    private static final float SCOREBAR_LERP_VALUE = 3.0f;

    public float scoreBarHeight;

    private float totalWidth;
    private float totalHeight;

    private float[] scoreBarCurrentWidth;
    private float[] scoreBarTargetWidth;
    private float[] scoreBarDrawStartPos;

    public ScoreBar(Viewport viewport, int size, float scaleFactor) {
        scoreBarCurrentWidth = new float[size];
        scoreBarTargetWidth = new float[size];
        scoreBarDrawStartPos = new float[size];

        totalWidth = viewport.getWorldWidth();
        totalHeight = viewport.getWorldHeight();

        computeDrawStartPos();

        scoreBarHeight = 16f * scaleFactor;
    }

    private void computeDrawStartPos() {
        int size = scoreBarDrawStartPos.length;

        float splitScreenWidth = totalWidth / size;
        for (int i = 1; i < size - 1; i++) {
            scoreBarDrawStartPos[i] = splitScreenWidth * i;
            scoreBarDrawStartPos[i] += (splitScreenWidth / 2);
        }
        if (size != 1) {
            scoreBarDrawStartPos[size - 1] = totalWidth;
        } else {
            scoreBarDrawStartPos[size - 1] = totalWidth / 2;
        }
    }

    public void resize(int width, int height) {
        totalWidth = width;
        totalHeight = height;
    }

    public void render(ShapeDrawer shapeDrawer, BitmapFont font, Array<TeamData> teamData, float delta) {
        float offsetY = totalHeight - scoreBarHeight;

        float totalScore = 0;
        for (TeamData td : teamData) {
            totalScore += td.getScore();
        }

        if (totalScore == 0) {
            return;
        }

        float shadowHeight = Math.min(scoreBarHeight, 10f);
        shapeDrawer.filledRectangle(0, totalHeight, totalWidth, -(shadowHeight + scoreBarHeight),
                Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK, Color.BLACK);

        for (int i = 0; i < teamData.size; i++) {
            float barWidth = ((teamData.get(i).getScore() / totalScore) * totalWidth);
            scoreBarTargetWidth[i] = barWidth;

            float drawWidth = scoreBarCurrentWidth[i] + ((scoreBarTargetWidth[i] - scoreBarCurrentWidth[i]) * SCOREBAR_LERP_VALUE * delta);
            scoreBarCurrentWidth[i] = drawWidth;

            shapeDrawer.setColor(teamData.get(i).getColor());
            font.setColor(getGoodTextColor(teamData.get(i).getColor()));

            float barStartX = 0;
            for (int j = 0; j < i; j++) {
                barStartX += scoreBarTargetWidth[j];
            }

            if (i == 0) {
                /* ScoreBar at the left */
                scoreBarDrawStartPos[i] = scoreBarDrawStartPos[i] + ((barStartX - scoreBarDrawStartPos[i]) * SCOREBAR_LERP_VALUE * delta);

                shapeDrawer.filledRectangle(scoreBarDrawStartPos[i], offsetY, drawWidth, scoreBarHeight);
                font.draw(shapeDrawer.getBatch(), String.valueOf(teamData.get(i).getScore()),
                        scoreBarDrawStartPos[i], totalHeight - (scoreBarHeight / 2) + (font.getLineHeight() / 4),
                        drawWidth, Align.center, false);
            } else if (i == teamData.size - 1) {
                /* ScoreBar at the right */
                scoreBarDrawStartPos[i] = scoreBarDrawStartPos[i] + ((barStartX + scoreBarTargetWidth[i] - scoreBarDrawStartPos[i]) * SCOREBAR_LERP_VALUE * delta);

                shapeDrawer.filledRectangle(scoreBarDrawStartPos[i], offsetY, -drawWidth, scoreBarHeight);
                font.draw(shapeDrawer.getBatch(), String.valueOf(teamData.get(i).getScore()),
                        scoreBarDrawStartPos[i] - drawWidth, totalHeight - (scoreBarHeight / 2) + (font.getLineHeight() / 4),
                        drawWidth, Align.center, false);
            } else {
                /* ScoreBars at the middle */
                scoreBarDrawStartPos[i] = scoreBarDrawStartPos[i] + ((barStartX + (scoreBarTargetWidth[i] / 2) - scoreBarDrawStartPos[i]) * SCOREBAR_LERP_VALUE * delta);

                shapeDrawer.filledRectangle(scoreBarDrawStartPos[i], offsetY, drawWidth / 2, scoreBarHeight);
                shapeDrawer.filledRectangle(scoreBarDrawStartPos[i], offsetY, -drawWidth / 2, scoreBarHeight);
                font.draw(shapeDrawer.getBatch(), String.valueOf(teamData.get(i).getScore()),
                        scoreBarDrawStartPos[i] - (drawWidth / 2), totalHeight - (scoreBarHeight / 2) + (font.getLineHeight() / 4),
                        drawWidth, Align.center, false);
            }
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
