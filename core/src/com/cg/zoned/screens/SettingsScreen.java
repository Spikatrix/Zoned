package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;

public class SettingsScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private BitmapFont font;

    private boolean showFPSCounter;

    public SettingsScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpStage();
        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        Table table = new Table();
        //table.setDebug(true);
        table.setFillParent(true);
        table.center();

        final CheckBox showFPS = new CheckBox("Show FPS counter", game.skin);
        showFPS.getImageCell().width(showFPS.getLabel().getPrefHeight()).height(showFPS.getLabel().getPrefHeight());
        showFPS.getImage().setScaling(Scaling.fill);
        showFPS.setChecked(game.preferences.getBoolean(Constants.FPS_PREFERENCE, false));
        showFPSCounter = showFPS.isChecked();
        showFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.preferences.putBoolean(Constants.FPS_PREFERENCE, showFPS.isChecked());
                game.preferences.flush();
                showFPSCounter = showFPS.isChecked();
            }
        });

        table.add(showFPS);

        stage.addActor(table);
        stage.addFocusableActor(showFPS);
        stage.setFocusedActor(showFPS);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        if (showFPSCounter) {
            FPSDisplayer.displayFPS(stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            animationManager.fadeOutStage(stage, new MainMenuScreen(game));
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
