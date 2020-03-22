package com.cg.zoned;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class FPSDisplayer {
    public static void displayFPS(Batch batch, BitmapFont font, float xOffset, float yOffset) {
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
                5 + xOffset, Gdx.graphics.getHeight() - 8 - yOffset);
        batch.end();
    }

    public static void displayFPS(Batch batch, BitmapFont font) {
        displayFPS(batch, font, 0, 0);
    }
}
