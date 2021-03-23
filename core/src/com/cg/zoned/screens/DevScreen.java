package com.cg.zoned.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Preferences;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class DevScreen extends ScreenObject implements InputProcessor {
    private Spinner splitscreenSpinner;
    private final int minPlayerCount = 1;
    private Spinner mapSplitScreenSpinner;
    private final int minSplitScreenCount = 1;

    public DevScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Exploring secrets");

        this.screenViewport = new ScreenViewport();
        this.screenStage = new FocusableStage(this.screenViewport);
        this.animationManager = new AnimationManager(game, this);
    }

    @Override
    public void show() {
        setUpStage();

        animationManager.fadeInStage(screenStage);
    }

    private void setUpStage() {
        Table table = new Table();
        table.center();
        table.setFillParent(true);

        UIButtonManager uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        Label devOptions = new Label("Developer Options", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(devOptions.getPrefHeight());
        table.add(devOptions).expandX().pad(headerPad);
        table.row();

        Table innerTable = new Table();
        innerTable.center();
        innerTable.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(innerTable);
        screenScrollPane.setOverscroll(false, true);

        int maxPlayerCount = 100;
        int currentPlayerCount = game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);
        Label splitscreenPlayerCountLabel = new Label("Splitscreen player count", game.skin);
        splitscreenSpinner = new Spinner(game.skin,
                game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight(),
                64f * game.getScaleFactor(),
                true);
        splitscreenSpinner.generateValueRange(minPlayerCount, maxPlayerCount, game.skin);
        splitscreenSpinner.snapToStep(currentPlayerCount - minPlayerCount);

        int maxSplitScreenCount = 100;
        int currentSplitScreenCount = game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);
        Label mapStartPosSplitscreenCountLabel = new Label("Map start position splitscreen count", game.skin);
        mapSplitScreenSpinner = new Spinner(game.skin,
                game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight(),
                64f * game.getScaleFactor(),
                true);
        mapSplitScreenSpinner.generateValueRange(minSplitScreenCount, maxSplitScreenCount, game.skin);
        mapSplitScreenSpinner.snapToStep(currentSplitScreenCount - minSplitScreenCount);

        innerTable.add(splitscreenPlayerCountLabel);
        innerTable.row();
        innerTable.add(splitscreenSpinner);
        innerTable.row();

        screenStage.addFocusableActor(splitscreenSpinner.getLeftButton());
        screenStage.addFocusableActor(splitscreenSpinner.getRightButton());
        screenStage.row();

        innerTable.add(mapStartPosSplitscreenCountLabel).padTop(20f);
        innerTable.row();
        innerTable.add(mapSplitScreenSpinner);

        screenStage.addFocusableActor(mapSplitScreenSpinner.getLeftButton());
        screenStage.addFocusableActor(mapSplitScreenSpinner.getRightButton());
        screenStage.row();

        table.add(screenScrollPane).grow();

        screenStage.addActor(table);
        screenStage.setScrollFocus(screenScrollPane);

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

        if (game.showFPSCounter()) {
            UITextDisplayer.displayFPS(screenViewport, screenStage.getBatch(), game.getSmallFont());
        }

        screenStage.act(delta);
        screenStage.draw();
    }

    private void saveData() {
        boolean prefUpdated = false;

        int splitScreenVal = splitscreenSpinner.getPositionIndex() + minPlayerCount;
        int mapSplitScreenVal = mapSplitScreenSpinner.getPositionIndex() + minSplitScreenCount;

        // These conditions are there for performance reasons (I think saving is a bit slow, haven't actually benchmarked this)
        if (splitScreenVal != game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, splitScreenVal)) {
            game.preferences.putInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, splitScreenVal);
            prefUpdated = true;
        }
        if (mapSplitScreenVal != game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, mapSplitScreenVal)) {
            game.preferences.putInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, mapSplitScreenVal);
            prefUpdated = true;
        }

        if (prefUpdated) {
            game.preferences.flush();
        }
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

        saveData();
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
