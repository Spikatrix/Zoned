package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;

public class UITextDisplayer {
    public static final float padding = 6f;

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font, float xOffset, float yOffset) {
        int fps = Gdx.graphics.getFramesPerSecond();

        Color origColor = font.getColor();
        if (fps >= 55) {
            font.setColor(Color.GREEN);
        } else if (fps >= 30) {
            font.setColor(Color.YELLOW);
        } else {
            font.setColor(Color.RED);
        }

        batch.begin();
        font.draw(batch, "FPS: " + fps,
                xOffset, viewport.getWorldHeight() - yOffset);
        batch.end();

        font.setColor(origColor);
    }

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font) {
        displayFPS(viewport, batch, font, padding, padding);
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping, float xOffset, float yOffset) {
        Color origColor = font.getColor();
        if (ping < 50) {
            font.setColor(Color.GREEN);
        } else if (ping < 100) {
            font.setColor(Color.YELLOW);
        } else {
            font.setColor(Color.RED);
        }

        batch.begin();
        font.draw(batch, "Ping: " + ping + " ms",
                xOffset, viewport.getWorldHeight() - font.getLineHeight() - yOffset);
        batch.end();

        font.setColor(origColor);
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping) {
        displayPing(viewport, batch, font, ping, padding, padding);
    }
}
