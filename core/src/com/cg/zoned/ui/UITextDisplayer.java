package com.cg.zoned.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.viewport.Viewport;

public class UITextDisplayer {
    public static final float padding = 6f;

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font, float xOffset, float yOffset) {
        int fps = Gdx.graphics.getFramesPerSecond();

        if (fps >= 55) {
            font.setColor(Color.GREEN);
        } else if (fps >= 30) {
            font.setColor(Color.YELLOW);
        } else {
            font.setColor(Color.RED);
        }

        drawString(batch, "FPS: " + fps, font, viewport, xOffset, yOffset);
    }

    public static void displayFPS(Viewport viewport, Batch batch, BitmapFont font) {
        displayFPS(viewport, batch, font, padding, padding);
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping, float xOffset, float yOffset) {
        if (ping < 50) {
            font.setColor(Color.GREEN);
        } else if (ping < 100) {
            font.setColor(Color.YELLOW);
        } else {
            font.setColor(Color.RED);
        }

        drawString(batch, "Ping: " + ping + " ms", font, viewport, xOffset, yOffset);
    }

    public static void displayPing(Viewport viewport, Batch batch, BitmapFont font, int ping) {
        displayPing(viewport, batch, font, ping, padding, padding);
    }

    public static void displayExtendedGLStatistics(Viewport viewport, Batch batch, BitmapFont font, GLProfiler profiler, float xOffset, float yOffset) {
        font.setColor(Color.CORAL);

        StringBuilder sb = new StringBuilder();
        sb.append("GL Calls: ").append(profiler.getCalls()).append("\n");
        sb.append("Draw calls: ").append(profiler.getDrawCalls()).append("\n");
        sb.append("Shader switches: ").append(profiler.getShaderSwitches()).append("\n");
        sb.append("Texture bindings: ").append(profiler.getTextureBindings()).append("\n");
        sb.append("Vertex calls: ").append(profiler.getVertexCount().total).append("\n");
        long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        sb.append("Used memory: ").append(memory).append(" MB");

        drawString(batch, sb.toString(), font, viewport, xOffset, yOffset);
    }

    private static void drawString(Batch batch, String string, BitmapFont font, Viewport viewport, float xOffset, float yOffset) {
        batch.begin();
        font.draw(batch, string, xOffset,
                viewport.getWorldHeight() - yOffset);
        batch.end();
    }
}
