package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverCheckBox;
import com.cg.zoned.ui.HoverImageButton;

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

        Label controlLabel = new Label("Control scheme", game.skin, "themed");
        Drawable controlFlingOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_control_fling_off.png"))));
        Drawable controlFlingOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_control_fling_on.png"))));
        Drawable controlPiemenuOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_control_piemenu_on.png"))));
        Drawable controlPiemenuOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_control_piemenu_off.png"))));
        final HoverImageButton flingControl = new HoverImageButton(controlFlingOff, controlFlingOff, controlFlingOn);
        final HoverImageButton piemenuControl = new HoverImageButton(controlPiemenuOff, controlPiemenuOff, controlPiemenuOn);
        final Label flingControlLabel = new Label("Fling", game.skin);
        Label piemenuControlLabel = new Label("Piemenu", game.skin);
        flingControl.setHoverAlpha(.7f);
        piemenuControl.setHoverAlpha(.7f);
        flingControl.setClickAlpha(.4f);
        piemenuControl.setClickAlpha(.4f);
        int currentControl = game.preferences.getInteger(Constants.CONTROL_PREFERENCE, Constants.PIE_MENU_CONTROL);
        if (currentControl == Constants.PIE_MENU_CONTROL) {
            piemenuControl.setChecked(true);
        } else if (currentControl == Constants.FLING_CONTROL) {
            flingControl.setChecked(true);
        }
        flingControl.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (piemenuControl.isChecked()) {
                    game.preferences.putInteger(Constants.CONTROL_PREFERENCE, Constants.FLING_CONTROL);
                    game.preferences.flush();
                    piemenuControl.toggle();
                } else {
                    flingControl.toggle();
                }
            }
        });
        piemenuControl.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (flingControl.isChecked()) {
                    game.preferences.putInteger(Constants.CONTROL_PREFERENCE, Constants.PIE_MENU_CONTROL);
                    game.preferences.flush();
                    flingControl.toggle();
                } else {
                    piemenuControl.toggle();
                }
            }
        });

        table.add(controlLabel).colspan(2).padBottom(10f);
        table.row();
        table.add(piemenuControl).padLeft(5f);
        table.add(flingControl).padRight(5f);
        table.row();
        table.add(piemenuControlLabel).padLeft(5f);
        table.add(flingControlLabel).padRight(5f);
        table.row();

        final HoverCheckBox showFPS = new HoverCheckBox("Show FPS counter", game.skin);
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

        table.add(showFPS).colspan(2).padTop(30f);

        stage.addActor(table);
        stage.addFocusableActor(piemenuControl);
        stage.addFocusableActor(flingControl);
        stage.row();
        stage.addFocusableActor(showFPS, 2);
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
            FPSDisplayer.displayFPS(viewport, stage.getBatch(), font);
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
        if (button == Input.Buttons.BACK) {
            animationManager.fadeOutStage(stage, new MainMenuScreen(game));
            return true;
        }

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
