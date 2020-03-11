package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.Spinner;
import com.cg.zoned.ui.StepScrollPane;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class TestScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private static final String reallyLongString = "This\nIs\nA\nReally\nLong\nString\nThat\nHas\nLots\nOf\nLines\nAnd\nRepeats.\n"
            + "This\nIs\nA\nReally\nLong\nString\nThat\nHas\nLots\nOf\nLines\nAnd\nRepeats.\n"
            + "This\nIs\nA\nReally\nLong\nString\nThat\nHas\nLots\nOf\nLines\nAnd\nRepeats.\n";

    private AnimationManager animationManager;
    private ScreenViewport viewport;
    private Stage stage;
    private ScrollPane scrollPane;

    public TestScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);

        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void show() {
        Table parentTable = new Table();
        parentTable.setFillParent(true);

        final int LOW_LIMIT = 3, HIGH_LIMIT = 100;

        Label value = new Label("", game.skin);
        value.setAlignment(Align.center);
        value.setWrap(true);
        StringBuilder sb = new StringBuilder();
        for (int i = LOW_LIMIT; i <= HIGH_LIMIT; i++) {
            sb.append(i);
            if (i != HIGH_LIMIT) {
                sb.append('\n');
            }
        }
        value.setText(sb.toString());

        final float scrollPaneHeight = (value.getPrefHeight() / (HIGH_LIMIT - LOW_LIMIT + 1));
        final float itemWidth = 60f;

        Spinner spinner = new Spinner(game.skin);
        spinner.generateValueLabel(LOW_LIMIT, HIGH_LIMIT, game.skin);
        //spinner.getStepScrollPane().add(value);

        parentTable.add(spinner).width(itemWidth * 3);

        stage.addActor(parentTable);
        animationManager.fadeInStage(stage);
    }

    @Override
    public void resize(int width, int height) {
        this.viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.viewport.apply(true);

        this.stage.draw();
        this.stage.act(delta);
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
