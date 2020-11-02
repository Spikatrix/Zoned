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
import com.cg.zoned.Assets;
import com.cg.zoned.Preferences;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.controls.ControlType;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ControlManager;
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
        this.font = game.skin.getFont(Assets.FontManager.SMALL.getFontName());
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

        ControlType[] controlTypes = ControlManager.CONTROL_TYPES;
        int currentControl = game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0);

        Label controlSchemeLabel = new Label("Control scheme", game.skin, "themed");
        table.add(controlSchemeLabel).colspan(controlTypes.length).padBottom(10f);
        table.row();

        final HoverImageButton[] controlButtons = new HoverImageButton[controlTypes.length];
        final Label[] controlLabels = new Label[controlTypes.length];
        for (int i = 0; i < controlTypes.length; i++) {
            ControlType controlType = controlTypes[i];

            Texture controlOffTexture = new Texture(Gdx.files.internal(controlType.controlOffTexturePath));
            Texture controlOnTexture = new Texture(Gdx.files.internal(controlType.controlOnTexturePath));
            usedTextures.add(controlOffTexture);
            usedTextures.add(controlOnTexture);

            Drawable controlOff = new TextureRegionDrawable(controlOffTexture);
            Drawable controlOn = new TextureRegionDrawable(controlOnTexture);

            controlButtons[i] = new HoverImageButton(controlOff, controlOn);
            controlLabels[i] = new Label(controlType.controlName, game.skin);

            controlButtons[i].setHoverAlpha(.7f);
            controlButtons[i].setClickAlpha(.4f);

            if (i == currentControl) {
                controlButtons[i].setChecked(true);
            }

            final int controlIndex = i;
            controlButtons[i].addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    int currentControl = game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0);
                    if (controlButtons[currentControl].isChecked()) {
                        game.preferences.putInteger(Preferences.CONTROL_PREFERENCE, controlIndex);
                        game.preferences.flush();
                        controlButtons[currentControl].toggle();
                    } else {
                        controlButtons[controlIndex].toggle();
                    }
                }
            });
        }

        for (HoverImageButton controlButton : controlButtons) {
            table.add(controlButton).space(5f);
            stage.addFocusableActor(controlButton);
        }
        table.row();
        stage.row();
        for (Label controlLabel : controlLabels) {
            table.add(controlLabel).space(5f);
        }
        table.row();


        final HoverCheckBox showFPS = new HoverCheckBox("Show FPS counter", game.skin);
        showFPS.getImageCell().width(showFPS.getLabel().getPrefHeight()).height(showFPS.getLabel().getPrefHeight());
        showFPS.getImage().setScaling(Scaling.fill);
        showFPS.setChecked(game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false));
        showFPSCounter = showFPS.isChecked();
        showFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.preferences.putBoolean(Preferences.FPS_PREFERENCE, showFPS.isChecked());
                game.preferences.flush();
                showFPSCounter = showFPS.isChecked();
            }
        });

        table.add(showFPS).colspan(controlTypes.length).padTop(30f);
        table.row();

        HoverCheckBox discordRPCSwitch = null;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            discordRPCSwitch = new HoverCheckBox("Enable Discord Rich Presence", game.skin);
            discordRPCSwitch.getImageCell().width(discordRPCSwitch.getLabel().getPrefHeight()).height(discordRPCSwitch.getLabel().getPrefHeight());
            discordRPCSwitch.getImage().setScaling(Scaling.fill);
            discordRPCSwitch.setChecked(game.preferences.getBoolean(Preferences.DISCORD_RPC_PREFERENCE, true));
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

                    game.preferences.putBoolean(Preferences.DISCORD_RPC_PREFERENCE, discordRPCSwitch.isChecked());
                    game.preferences.flush();
                }
            });

            table.add(discordRPCSwitch).colspan(controlTypes.length).padTop(30f);
        }

        masterTable.add(screenScrollPane).grow();
        stage.addActor(masterTable);
        stage.addFocusableActor(showFPS, controlTypes.length);
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            stage.row();
            stage.addFocusableActor(discordRPCSwitch, controlTypes.length);
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
