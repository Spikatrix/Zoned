package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.UITextDisplayer;

public abstract class ScreenObject extends ScreenAdapter {
    protected final Zoned game;

    // These variables are used in almost every screen
    protected Array<Texture> usedTextures;
    protected ScreenViewport screenViewport;
    protected FocusableStage screenStage;
    protected ShapeDrawer shapeDrawer;
    protected SpriteBatch batch;
    protected AnimationManager animationManager;
    protected UIButtonManager uiButtonManager;

    public ScreenObject(final Zoned game) {
        this.game = game;
        this.usedTextures = new Array<>();
        this.screenViewport = new ScreenViewport();
        this.screenStage = new FocusableStage(this.screenViewport);
    }

    @Override
    public void resize(int width, int height) {
        screenStage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        screenViewport.apply(true);

        if (batch != null) {
            batch.setProjectionMatrix(screenViewport.getCamera().combined);
        }
    }

    public void displayFPS() {
        if (!game.showFPSCounter()) {
            // Don't do anything if the fps setting is switched off
            return;
        }

        if (uiButtonManager != null) {
            // Display FPS on the right side of the top left button
            float xOffset = ((2 * UITextDisplayer.padding) +
                    uiButtonManager.buttonSize + uiButtonManager.buttonPadding) * game.getScaleFactor();
            float yOffset = (UITextDisplayer.padding * game.getScaleFactor());

            UITextDisplayer.displayFPS(screenViewport, screenStage.getBatch(), game.getSmallFont(), xOffset, yOffset);
        } else {
            // Fallback to top left corner
            UITextDisplayer.displayFPS(screenViewport, screenStage.getBatch(), game.getSmallFont());
        }
    }

    public void dispose() {
        screenStage.dispose();
        screenStage = null;

        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }
}
