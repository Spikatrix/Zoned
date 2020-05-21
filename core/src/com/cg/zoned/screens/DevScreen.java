package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class DevScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ScreenViewport viewport;
    private FocusableStage stage;
    private AnimationManager animationManager;
    private BitmapFont font;
    private boolean showFPSCounter;

    private Spinner splitscreenPlayerCountSpinner;
    private int minPlayerCount = 1;
    private Spinner mapStartPosSplitscreenCountSpinner;
    private int minSplitScreenCount = 1;

    public DevScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        Table table = new Table();
        table.center();
        table.setFillParent(true);

        table.add(new Label("Developer Options", game.skin, "themed")).expandX().pad(10f);
        table.row();

        Table innerTable = new Table();
        innerTable.center();
        innerTable.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(innerTable);
        screenScrollPane.setOverscroll(false, true);

        int maxPlayerCount = 100;
        int currentPlayerCount = game.preferences.getInteger(Constants.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);
        Label splitscreenPlayerCountLabel = new Label("Splitscreen player count", game.skin);
        splitscreenPlayerCountSpinner = new Spinner(game.skin,
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight(),
                64f * game.getScaleFactor(),
                true);
        splitscreenPlayerCountSpinner.generateValueRange(minPlayerCount, maxPlayerCount, game.skin);
        splitscreenPlayerCountSpinner.snapToStep(currentPlayerCount - minPlayerCount);

        int maxSplitScreenCount = 100;
        int currentSplitScreenCount = game.preferences.getInteger(Constants.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);
        Label mapStartPosSplitscreenCountLabel = new Label("Map start position splitscreen count", game.skin);
        mapStartPosSplitscreenCountSpinner = new Spinner(game.skin,
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight(),
                64f * game.getScaleFactor(),
                true);
        mapStartPosSplitscreenCountSpinner.generateValueRange(minSplitScreenCount, maxSplitScreenCount, game.skin);
        mapStartPosSplitscreenCountSpinner.snapToStep(currentSplitScreenCount - minSplitScreenCount);

        innerTable.add(splitscreenPlayerCountLabel);
        innerTable.row();
        innerTable.add(splitscreenPlayerCountSpinner);
        innerTable.row();

        innerTable.add(mapStartPosSplitscreenCountLabel).padTop(20f);
        innerTable.row();
        innerTable.add(mapStartPosSplitscreenCountSpinner);

        table.add(screenScrollPane).expand();

        stage.addActor(table);
        stage.setScrollFocus(screenScrollPane);
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage();
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

        this.viewport.apply(true);

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.act(delta);
        stage.draw();
    }

    private void saveData() {
        game.preferences.putInteger(Constants.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, splitscreenPlayerCountSpinner.getPositionIndex() + minPlayerCount);
        game.preferences.putInteger(Constants.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, mapStartPosSplitscreenCountSpinner.getPositionIndex() + minSplitScreenCount);
        game.preferences.flush();
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        saveData();
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
