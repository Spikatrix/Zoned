package com.cg.zoned.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;

public abstract class ScreenObject extends ScreenAdapter {
    protected final Zoned game;

    // These variables are used in almost every screen
    protected Array<Texture> usedTextures;
    protected ScreenViewport screenViewport;
    protected FocusableStage screenStage;
    protected ShapeDrawer shapeDrawer;
    protected SpriteBatch batch;
    protected BitmapFont smallFont;
    protected boolean showFPSCounter;
    protected AnimationManager animationManager;

    public ScreenObject(final Zoned game) {
        this.game = game;
        this.usedTextures = new Array<>();

        init();
    }

    // preset = true is called from the Loading screen where the pref and skin aren't yet loaded
    public ScreenObject(final Zoned game, boolean preset) {
        this.game = game;
        this.usedTextures = new Array<>();

        if (preset) {
            init();
        }
    }

    private void init() {
        smallFont = game.skin.getFont(Assets.FontManager.SMALL.getFontName());
        showFPSCounter = game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false);
    }

    public void dispose() {
        if (screenStage != null) {
            screenStage.dispose();
            screenStage = null;
        }

        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }
}
