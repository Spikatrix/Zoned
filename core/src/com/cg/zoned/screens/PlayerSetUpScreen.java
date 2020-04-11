package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class PlayerSetUpScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private int playerCount;
    private Table playerList;

    public PlayerSetUpScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        this.playerCount = Constants.NO_OF_PLAYERS;
        this.playerList = new Table();
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        animationManager.fadeInStage(stage);
    }

    private void setUpBackButton() {
        Table table = new Table();
        table.setFillParent(true);
        table.left().top();
        Drawable backImage = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_back.png"))));
        final HoverImageButton backButton = new HoverImageButton(backImage);
        backButton.setNormalAlpha(1f);
        backButton.setHoverAlpha(.75f);
        backButton.setClickAlpha(.5f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
        table.add(backButton).padLeft(20f).padTop(35f);
        stage.addActor(table);
    }

    private void setUpStage() {
        final int NO_OF_COLORS = Constants.PLAYER_COLORS.size();
        final float COLOR_BUTTON_DIMENSIONS = 60f;

        Table table = new Table();
        table.setFillParent(true);
        //table.setDebug(true);
        table.center();

        Label[] promptLabels = new Label[playerCount];
        Button[][] colorButtons = new Button[playerCount][];
        final ButtonGroup[] colorButtonGroups = new ButtonGroup[playerCount]; // TODO: Modify to list to get rid of warning?
        for (int i = 0; i < playerCount; i++) {
            Table playerItem = new Table();
            playerItem.center();
            promptLabels[i] = new Label("Player " + (i + 1) + " color: ", game.skin, "themed");
            playerItem.add(promptLabels[i]);

            colorButtons[i] = new Button[NO_OF_COLORS];
            colorButtonGroups[i] = new ButtonGroup<Button>();
            colorButtonGroups[i].setMinCheckCount(1);
            colorButtonGroups[i].setMaxCheckCount(1);

            for (int j = 0; j < NO_OF_COLORS; j++) {
                colorButtons[i][j] = new Button(game.skin, "color-button");
                colorButtons[i][j].setColor(PlayerColorHelper.getColorFromIndex(j));
                colorButtonGroups[i].add(colorButtons[i][j]);

                playerItem.add(colorButtons[i][j]).width(COLOR_BUTTON_DIMENSIONS * game.getScaleFactor()).height(COLOR_BUTTON_DIMENSIONS * game.getScaleFactor());

                stage.addFocusableActor(colorButtons[i][j]);
            }

            colorButtons[i][i % NO_OF_COLORS].setChecked(true);
            stage.row();
            playerList.add(playerItem).right();
            playerList.row();
        }
        table.add(playerList).colspan(NO_OF_COLORS + 1);
        table.row();

        Table innerTable = new Table();
        Label gridSizeLabel = new Label("Grid size: ", game.skin, "themed");
        Label x = new Label("  x  ", game.skin);
        final int LOW_LIMIT = 3, HIGH_LIMIT = 100;
        int snapValue = 10;
        final Spinner rowSpinner = new Spinner(game.skin, game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight());
        final Spinner colSpinner = new Spinner(game.skin, game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight());
        rowSpinner.generateValueLabel(LOW_LIMIT, HIGH_LIMIT, game.skin);
        colSpinner.generateValueLabel(LOW_LIMIT, HIGH_LIMIT, game.skin);
        rowSpinner.snapToStep(snapValue - LOW_LIMIT);
        colSpinner.snapToStep(snapValue - LOW_LIMIT);

        innerTable.add(gridSizeLabel);
        innerTable.add(rowSpinner).width(rowSpinner.getPrefWidth() * game.getScaleFactor());
        innerTable.add(x);
        innerTable.add(colSpinner).width(colSpinner.getPrefWidth() * game.getScaleFactor());
        table.add(innerTable).colspan(NO_OF_COLORS + 1).pad(20 * game.getScaleFactor());

        table.row();

        stage.addFocusableActor(rowSpinner.getLeftButton());
        stage.addFocusableActor(rowSpinner.getRightButton());
        stage.addFocusableActor(colSpinner.getLeftButton(), 2);
        stage.addFocusableActor(colSpinner.getRightButton());
        stage.row();

        Table infoTable = new Table();
        infoTable.center();
        Image infoImage = new Image(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_info.png")))));
        Label infoLabel = new Label("First to capture more than 50% of the grid wins", game.skin);
        infoTable.add(infoImage).height(infoLabel.getPrefHeight()).width(infoLabel.getPrefHeight()).padRight(20f);
        infoTable.add(infoLabel);
        table.add(infoTable).colspan(NO_OF_COLORS + 1).padBottom(20f * game.getScaleFactor());
        table.row();

        TextButton startButton = new TextButton("Start game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int rows = Math.round(rowSpinner.getScrollYPos() / rowSpinner.getScrollPaneHeight()) + LOW_LIMIT;
                int cols = Math.round(colSpinner.getScrollYPos() / colSpinner.getScrollPaneHeight()) + LOW_LIMIT;

                if (Gdx.app.getType() == Application.ApplicationType.Android) { // Swap because splitscreen; phone will be rotated (hopefully xD)
                    int temp = rows;
                    rows = cols;
                    cols = temp;
                }

                Array<Color> playerColors = new Array<Color>();
                for (ButtonGroup buttonGroup : colorButtonGroups) {
                    playerColors.add(buttonGroup.getChecked().getColor());
                }

                startGame(playerColors, rows, cols);
            }

        });
        table.add(startButton).width(200 * game.getScaleFactor()).colspan(NO_OF_COLORS + 1);
        stage.addFocusableActor(startButton, NO_OF_COLORS);
        stage.addActor(table);
    }

    private void startGame(Array<Color> playerColors, final int rows, final int cols) {
        final Player[] players = new Player[playerColors.size];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(playerColors.get(i), PlayerColorHelper.getStringFromColor(playerColors.get(i)));
        }

        Vector2[] playerStartPositions = Map.getStartPositions(rows, cols);

        for (int i = 0; i < players.length; i++) {
            players[i].setStartPos(playerStartPositions[i % playerStartPositions.length]);
            players[i].setControlsIndex(i % Constants.PLAYER_CONTROLS.length);
        }

        animationManager.fadeOutStage(stage, new GameScreen(game, rows, cols, players, null, null));
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

    private void onBackPressed() {
        animationManager.fadeOutStage(stage, new MainMenuScreen(game));
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
