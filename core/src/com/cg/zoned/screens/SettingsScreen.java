package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverCheckBox;
import com.cg.zoned.ui.HoverImageButton;

public class SettingsScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private BitmapFont font;

    private boolean showFPSCounter;

    public SettingsScreen(final Zoned game) {
        this.game = game;
        game.discordRPCManager.updateRPC("Configuring Settings");

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getFontName());
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        Table masterTable = new Table();
        //masterTable.setDebug(true);
        masterTable.setFillParent(true);
        masterTable.center();

        Table table = new Table();
        table.center();
        table.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);

        Label controlLabel = new Label("Control scheme", game.skin, "themed");
        Texture controlFlingOffTexture = new Texture(Gdx.files.internal("icons/control_icons/ic_control_fling_off.png"));
        Texture controlFlingOnTexture = new Texture(Gdx.files.internal("icons/control_icons/ic_control_fling_on.png"));
        Texture controlPiemenuOffTexture = new Texture(Gdx.files.internal("icons/control_icons/ic_control_piemenu_off.png"));
        Texture controlPiemenuOnTexture = new Texture(Gdx.files.internal("icons/control_icons/ic_control_piemenu_on.png"));
        usedTextures.add(controlFlingOffTexture);
        usedTextures.add(controlFlingOnTexture);
        usedTextures.add(controlPiemenuOffTexture);
        usedTextures.add(controlPiemenuOnTexture);
        Drawable controlFlingOff = new TextureRegionDrawable(controlFlingOffTexture);
        Drawable controlFlingOn = new TextureRegionDrawable(controlFlingOnTexture);
        Drawable controlPiemenuOff = new TextureRegionDrawable(controlPiemenuOffTexture);
        Drawable controlPiemenuOn = new TextureRegionDrawable(controlPiemenuOnTexture);
        final HoverImageButton flingControl = new HoverImageButton(controlFlingOff, controlFlingOn);
        final HoverImageButton piemenuControl = new HoverImageButton(controlPiemenuOff, controlPiemenuOn);
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
        table.row();

        HoverCheckBox discordRPCSwitch = null;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            discordRPCSwitch = new HoverCheckBox("Enable Discord Rich Presence", game.skin);
            discordRPCSwitch.getImageCell().width(discordRPCSwitch.getLabel().getPrefHeight()).height(discordRPCSwitch.getLabel().getPrefHeight());
            discordRPCSwitch.getImage().setScaling(Scaling.fill);
            discordRPCSwitch.setChecked(game.preferences.getBoolean(Constants.DISCORD_RPC_PREFERENCE, true));
            discordRPCSwitch.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    HoverCheckBox discordRPCSwitch = (HoverCheckBox) actor;
                    if (discordRPCSwitch.isChecked()) {
                        game.discordRPCManager.initRPC();
                        game.discordRPCManager.updateRPC("Configuring Settings");
                    } else {
                        game.discordRPCManager.shutdownRPC();
                    }

                    game.preferences.putBoolean(Constants.DISCORD_RPC_PREFERENCE, discordRPCSwitch.isChecked());
                    game.preferences.flush();
                }
            });

            table.add(discordRPCSwitch).colspan(2).padTop(30f);
        }

        masterTable.add(screenScrollPane).grow();
        stage.addActor(masterTable);
        stage.addFocusableActor(piemenuControl);
        stage.addFocusableActor(flingControl);
        stage.row();
        stage.addFocusableActor(showFPS, 2);
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            stage.row();
            stage.addFocusableActor(discordRPCSwitch, 2);
        }
        stage.setScrollFocus(screenScrollPane);
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getBackButtonTexture());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        animationManager.fadeOutStage(stage, this, new MainMenuScreen(game));
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            onBackPressed();
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
            onBackPressed();
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
