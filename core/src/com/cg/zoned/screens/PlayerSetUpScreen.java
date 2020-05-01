package com.cg.zoned.screens;

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
import com.cg.zoned.MapSelector;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.CustomButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class PlayerSetUpScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ShapeRenderer renderer;
    private float bgAlpha = .25f;
    private float bgAnimSpeed = 1.8f;
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
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage();
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

        final MapSelector mapSelector = new MapSelector(stage, game.getScaleFactor(), game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        Spinner mapSpinner = mapSelector.loadMapSelectorSpinner(150 * game.getScaleFactor(),
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight() * 3);
        mapSelector.loadExternalMaps();
        table.add(mapSpinner).colspan(NO_OF_COLORS + 1).pad(20 * game.getScaleFactor());
        table.row();

        stage.addFocusableActor(mapSelector.getLeftButton(), 1);
        stage.addFocusableActor(mapSelector.getRightButton(), NO_OF_COLORS - 1);
        stage.row();

        if (playerCount <= 2) {
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
        }

        TextButton startButton = new TextButton("Next", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Array<Color> playerColors = new Array<>();
                for (ButtonGroup buttonGroup : colorButtonGroups) {
                    playerColors.add(buttonGroup.getChecked().getColor());
                }

                if (mapSelector.loadSelectedMap()) {
                    startGame(playerColors, mapSelector.getMapManager());
                }
            }

        });
        table.add(startButton).width(200 * game.getScaleFactor()).colspan(NO_OF_COLORS + 1);
        stage.addFocusableActor(startButton, NO_OF_COLORS);
        masterTable.add(screenScrollPane);
        stage.addActor(masterTable);
    }

    private void startGame(Array<Color> playerColors, MapManager mapManager) {
        final Player[] players = new Player[playerColors.size];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(playerColors.get(i), PlayerColorHelper.getStringFromColor(playerColors.get(i)));
        }

        for (int i = 0; i < players.length; i++) {
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
        }

        //animationManager.fadeOutStage(stage, this, new GameScreen(game, mapManager, players, null, null));
        animationManager.fadeOutStage(stage, this, new MapStartPosScreen(game, mapManager, players, 2, false));
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < currentBgColors.length; i++) {
            currentBgColors[i].lerp(targetBgColors[i], bgAnimSpeed * delta);
            currentBgColors[i].a = Math.min(targetBgColors[i].a, stage.getRoot().getColor().a / 2.5f);
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
