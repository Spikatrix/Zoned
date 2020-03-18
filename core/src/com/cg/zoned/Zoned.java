package com.cg.zoned;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.cg.zoned.screens.LoadingScreen;

public class Zoned extends Game {
    public Skin skin;

    private static float SCALE_FACTOR = 1.0f;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            SCALE_FACTOR = Constants.ANDROID_FONT_SCALE_FACTOR;
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            SCALE_FACTOR = Constants.DESKTOP_FONT_SCALE_FACTOR;
        }

        this.setScreen(new LoadingScreen(this));
    }

    public float getScaleFactor() {
        return SCALE_FACTOR;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        skin.dispose();
    }
}