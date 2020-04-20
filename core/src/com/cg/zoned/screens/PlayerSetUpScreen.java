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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.CustomButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class PlayerSetUpScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<Texture>();

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ShapeRenderer renderer;
    private float bgAlpha = .2f;
    private float bgAnimSpeed = 1.4f;
    private Color[] currentBgColors;
    private Color[] targetBgColors;

    private int playerCount;
    private Table playerList;

    public PlayerSetUpScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        this.renderer = new ShapeRenderer();

        this.playerCount = Constants.NO_OF_PLAYERS;
        this.playerList = new Table();

        this.currentBgColors = new Color[this.playerCount];
        this.targetBgColors = new Color[this.playerCount];
        for (int i = 0; i < this.playerCount; i++) {
            this.currentBgColors[i] = new Color(0, 0, 0, bgAlpha);
            this.targetBgColors[i] = new Color(0, 0, 0, bgAlpha);
        }
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        animationManager.fadeInStage(stage);
    }

    private void setUpBackButton() {
        HoverImageButton backButton = UIButtonManager.addBackButtonToStage(stage, game.getScaleFactor(), usedTextures);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    private void setUpStage() {
        final int NO_OF_COLORS = Constants.PLAYER_COLORS.size();
        final float COLOR_BUTTON_DIMENSIONS = 60f * game.getScaleFactor();

        Table masterTable = new Table();
        masterTable.setFillParent(true);
        //masterTable.setDebug(true);
        masterTable.center();

        Table table = new Table();
        table.center();
        table.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);
        // Have to set overscrollX to false since on Android, it seems to overscroll even when there is space
        // But on Desktop it works perfectly well.

        Label[] promptLabels = new Label[playerCount];
        Button[][] colorButtons = new Button[playerCount][];
        final CustomButtonGroup[] colorButtonGroups = new CustomButtonGroup[playerCount];
        for (int i = 0; i < playerCount; i++) {
            Table playerItem = new Table();
            playerItem.center();
            promptLabels[i] = new Label("Player " + (i + 1) + " color: ", game.skin, "themed");
            playerItem.add(promptLabels[i]);

            colorButtons[i] = new Button[NO_OF_COLORS];
            colorButtonGroups[i] = new CustomButtonGroup();
            colorButtonGroups[i].setMinCheckCount(1);
            colorButtonGroups[i].setMaxCheckCount(1);

            for (int j = 0; j < NO_OF_COLORS; j++) {
                colorButtons[i][j] = new Button(game.skin, "color-button");
                colorButtons[i][j].setColor(PlayerColorHelper.getColorFromIndex(j));
                colorButtonGroups[i].add(colorButtons[i][j]);

                playerItem.add(colorButtons[i][j]).width(COLOR_BUTTON_DIMENSIONS).height(COLOR_BUTTON_DIMENSIONS);

                stage.addFocusableActor(colorButtons[i][j]);
            }

            final int finalI = i;
            colorButtonGroups[i].setOnCheckChangeListener(new CustomButtonGroup.OnCheckChangeListener() {
                @Override
                public void buttonPressed(Button button) {
                    targetBgColors[finalI].set(button.getColor());
                    targetBgColors[finalI].a = bgAlpha;
                }
            });

            colorButtonGroups[i].uncheckAll();
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
        final Spinner rowSpinner = new Spinner(game.skin,
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight(),
                64f * game.getScaleFactor(), true);
        final Spinner colSpinner = new Spinner(game.skin,
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight(),
                64f * game.getScaleFactor(), true);
        rowSpinner.generateValueRange(LOW_LIMIT, HIGH_LIMIT, game.skin);
        colSpinner.generateValueRange(LOW_LIMIT, HIGH_LIMIT, game.skin);
        rowSpinner.snapToStep(snapValue - LOW_LIMIT);
        colSpinner.snapToStep(snapValue - LOW_LIMIT);

        innerTable.add(gridSizeLabel);
        innerTable.add(rowSpinner);
        innerTable.add(x);
        innerTable.add(colSpinner);
        table.add(innerTable).colspan(NO_OF_COLORS + 1).pad(20 * game.getScaleFactor());
        table.row();

        stage.addFocusableActor(rowSpinner.getLeftButton());
        stage.addFocusableActor(rowSpinner.getRightButton());
        stage.addFocusableActor(colSpinner.getLeftButton(), 2);
        stage.addFocusableActor(colSpinner.getRightButton());
        stage.row();

        Table infoTable = new Table();
        infoTable.center();
        Texture infoIconTexture = new Texture(Gdx.files.internal("icons/ui_icons/ic_info.png"));
        usedTextures.add(infoIconTexture);
        Image infoImage = new Image(infoIconTexture);
        Label infoLabel = new Label("First to capture more than 50% of the grid wins", game.skin);
        infoTable.add(infoImage).height(infoLabel.getPrefHeight()).width(infoLabel.getPrefHeight()).padRight(20f);
        infoTable.add(infoLabel);
        table.add(infoTable).colspan(NO_OF_COLORS + 1).padBottom(20f * game.getScaleFactor());
        table.row();

        TextButton startButton = new TextButton("Start game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int rows = rowSpinner.getPositionIndex() + LOW_LIMIT;
                int cols = colSpinner.getPositionIndex() + LOW_LIMIT;

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
        masterTable.add(screenScrollPane);
        stage.addActor(masterTable);
    }

    private void startGame(Array<Color> playerColors, final int rows, final int cols) {
        final Player[] players = new Player[playerColors.size];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(playerColors.get(i), PlayerColorHelper.getStringFromColor(playerColors.get(i)));
        }

        for (int i = 0; i < players.length; i++) {
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
        }

        animationManager.fadeOutStage(stage, this, new GameScreen(game, rows, cols, players, null, null));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < currentBgColors.length; i++) {
            currentBgColors[i].lerp(targetBgColors[i], bgAnimSpeed * delta);
        }

        viewport.apply(true);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        renderer.setProjectionMatrix(viewport.getCamera().combined);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < currentBgColors.length; i++) {
            renderer.setColor(currentBgColors[i]);
            renderer.rect(i * stage.getWidth() / currentBgColors.length, 0,
                    stage.getWidth() / currentBgColors.length, stage.getHeight());
        }
        renderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (showFPSCounter) {
            FPSDisplayer.displayFPS(viewport, stage.getBatch(), font);
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
