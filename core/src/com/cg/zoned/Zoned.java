package com.cg.zoned;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.screens.LoadingScreen;

public class Zoned extends Game {
    public Skin skin;
    public Preferences preferences;

    private AssetManager assetManager;

    private static float SCALE_FACTOR = 1.0f;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        setScaleFactor();

        this.setScreen(new LoadingScreen(this));
    }

    private void setScaleFactor() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // Values from https://developer.android.com/training/multiscreen/screendensities
            float screenDensity = Gdx.graphics.getDensity();
            if (screenDensity <= 1.2f) {
                SCALE_FACTOR = Constants.ANDROID_LDPI_FONT_SCALE_FACTOR;
            } else if (screenDensity <= 1.6f) {
                SCALE_FACTOR = Constants.ANDROID_MDPI_FONT_SCALE_FACTOR;
            } else if (screenDensity <= 2.4f) {
                SCALE_FACTOR = Constants.ANDROID_HDPI_FONT_SCALE_FACTOR;
            } else if (screenDensity <= 3.2f) {
                SCALE_FACTOR = Constants.ANDROID_XHDPI_FONT_SCALE_FACTOR;
            } else if (screenDensity <= 4.8f) {
                SCALE_FACTOR = Constants.ANDROID_XXHDPI_FONT_SCALE_FACTOR;
            } else {
                SCALE_FACTOR = Constants.ANDROID_XXXHDPI_FONT_SCALE_FACTOR;
            }
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            SCALE_FACTOR = Constants.DESKTOP_FONT_SCALE_FACTOR;
        }
    }

    public float getScaleFactor() {
        return SCALE_FACTOR;
    }

    @Override
    public void render() {
        super.render();
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void dispose() {
        try {
            assetManager.dispose();
        } catch (GdxRuntimeException ignored) {
            // "Pixmap already disposed!" error
            // idk why this happens but ok ¯\_(ツ)_/¯
        }

        try {
            skin.dispose();
        } catch (GdxRuntimeException ignored) {
            // "Pixmap already disposed!" error
            // idk why this happens but ok ¯\_(ツ)_/¯
        }
    }
}