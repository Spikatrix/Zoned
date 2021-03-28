package com.cg.zoned;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.managers.DiscordRPCBridge;
import com.cg.zoned.managers.DiscordRPCManager;
import com.cg.zoned.screens.LoadingScreen;

public class Zoned extends Game {
    public Skin skin;
    public Preferences preferences;

    public Assets assets;
    public DiscordRPCManager discordRPCManager;

    private boolean showFPSCounter;
    private BitmapFont smallFont; // Used for showing the FPS

    private static float SCALE_FACTOR = 1.0f;

    public Zoned(DiscordRPCBridge discordRPCBridge) {
        super();
        this.discordRPCManager = new DiscordRPCManager(discordRPCBridge);
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        setScaleFactor();
        assets = new Assets();

        this.setScreen(new LoadingScreen(this));
    }

    private void setScaleFactor() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // Values from https://developer.android.com/training/multiscreen/screendensities
            float screenDensity = Gdx.graphics.getDensity();
            if (screenDensity <= 1.2f) {
                SCALE_FACTOR = Constants.ANDROID_LDPI_UI_SCALE_FACTOR;
            } else if (screenDensity <= 1.6f) {
                SCALE_FACTOR = Constants.ANDROID_MDPI_UI_SCALE_FACTOR;
            } else if (screenDensity <= 2.4f) {
                SCALE_FACTOR = Constants.ANDROID_HDPI_UI_SCALE_FACTOR;
            } else if (screenDensity <= 3.2f) {
                SCALE_FACTOR = Constants.ANDROID_XHDPI_UI_SCALE_FACTOR;
            } else if (screenDensity <= 4.8f) {
                SCALE_FACTOR = Constants.ANDROID_XXHDPI_UI_SCALE_FACTOR;
            } else {
                SCALE_FACTOR = Constants.ANDROID_XXXHDPI_UI_SCALE_FACTOR;
            }
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            SCALE_FACTOR = Constants.DESKTOP_UI_SCALE_FACTOR;
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
        assets.setAssetManager(assetManager);
    }

    // Called from LoadingScreen when the skin and preferences have been loaded to load FPS font and preference
    public void initFPSUtils() {
        this.smallFont = skin.getFont(Assets.FontManager.SMALL.getFontName());
        this.showFPSCounter = preferences.getBoolean(com.cg.zoned.Preferences.FPS_PREFERENCE, false);
    }

    public void toggleFPSCounter() {
        this.showFPSCounter = !this.showFPSCounter;
        preferences.putBoolean(com.cg.zoned.Preferences.FPS_PREFERENCE, this.showFPSCounter);
        preferences.flush();
    }

    public Boolean showFPSCounter() {
        return this.showFPSCounter;
    }

    public BitmapFont getSmallFont() {
        return this.smallFont;
    }

    @Override
    public void dispose() {
        discordRPCManager.shutdownRPC();

        try {
            assets.dispose();
        } catch (GdxRuntimeException ignored) {
            // "Pixmap already disposed!" error
            // idk why this happens but ok ¯\_(ツ)_/¯
        }

        try {
            skin.dispose();
        } catch (GdxRuntimeException ignored) {
            // "Pixmap already disposed!" error
            // idk why this happens but ok ¯\_(ツ)_/¯
        } catch (NullPointerException ignored) {
            // Application was closed before the skin was loaded
        }
    }
}