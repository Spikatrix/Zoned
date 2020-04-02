package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;

public class FPSDisplayer {
    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font, float xOffset, float yOffset) {
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
                5 + xOffset, viewport.getWorldHeight() - 8 - yOffset);
        batch.end();
    }

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font) {
        displayFPS(viewport, batch, font, 0, 0);
    }
}
