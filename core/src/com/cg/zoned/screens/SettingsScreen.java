package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Assets;
import com.cg.zoned.Preferences;
import com.cg.zoned.Zoned;
import com.cg.zoned.controls.ControlType;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ControlManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.HoverCheckBox;
import com.cg.zoned.ui.HoverImageButton;

public class SettingsScreen extends ScreenObject implements InputProcessor {
    public SettingsScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Configuring Settings");

        this.animationManager = new AnimationManager(this.game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        animationManager.fadeInStage(screenStage);
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

        screenStage.setScrollpane(screenScrollPane);

        ControlType[] controlTypes = ControlManager.CONTROL_TYPES;
        int currentControl = game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0);

        Label controlSchemeLabel = new Label("Control setting", game.skin, "themed-rounded-background");
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
            screenStage.addFocusableActor(controlButton);
        }
        table.row();
        screenStage.row();
        for (Label controlLabel : controlLabels) {
            table.add(controlLabel).space(5f);
        }
        table.row();

        Label miscLabel = new Label("Misc", game.skin, "themed-rounded-background");
        table.add(miscLabel).colspan(controlTypes.length).padTop(40f);
        table.row();

        final HoverCheckBox showFPS = new HoverCheckBox("Show FPS counter", game.skin);
        showFPS.getImageCell().width(showFPS.getLabel().getPrefHeight()).height(showFPS.getLabel().getPrefHeight());
        showFPS.getImage().setScaling(Scaling.fill);
        showFPS.setChecked(game.showFPSCounter());
        showFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.toggleFPSCounter();
            }
        });

        table.add(showFPS).colspan(controlTypes.length).padTop(20f);
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

            table.add(discordRPCSwitch).colspan(controlTypes.length).padTop(20f);
        }

        masterTable.add(screenScrollPane).grow();
        screenStage.addActor(masterTable);
        screenStage.addFocusableActor(showFPS, controlTypes.length);
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            screenStage.row();
            screenStage.addFocusableActor(discordRPCSwitch, controlTypes.length);
        }
        screenStage.setScrollFocus(screenScrollPane);
    }

    private void setUpBackButton() {
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        screenStage.draw();
        screenStage.act(delta);

        displayFPS();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (screenStage.dialogIsActive()) {
            return false;
        }

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
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
            return onBackPressed();
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
