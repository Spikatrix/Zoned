package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;

public class UITextDisplayer {
    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font, float xOffset, float yOffset) {
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
                xOffset, viewport.getWorldHeight() - yOffset);
        batch.end();
    }

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font) {
        displayFPS(viewport, batch, font, 6, 6);
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping, float xOffset, float yOffset) {
        batch.begin();
        font.draw(batch, "Ping: " + ping + " ms",
                xOffset, viewport.getWorldHeight() - font.getLineHeight() - yOffset);
        batch.end();
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping) {
        displayPing(viewport, batch, font, ping, 6, 6);
    }
}
