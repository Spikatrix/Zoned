package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.ScoreBar;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.GameManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

public class GameScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private GameManager gameManager;

    private Map map;

    private ExtendViewport[] playerViewports; // Two viewports in split-screen mode; else one
    private ShapeRenderer renderer;

    private boolean gameComplete = false;
    private Color fadeOutOverlay = new Color(0, 0, 0, 0);
    private boolean gameCompleteFadeOutDone = false;

    private FocusableStage fullScreenStage;
    private BitmapFont font;
    private boolean showFPSCounter;
    private ScoreBar scoreBars;

    private float targetZoom = Constants.ZOOM_MIN_VALUE;

    public GameScreen(final Zoned game, int rows, int cols, Player[] players, Server server, Client client) {
        this.game = game;

        this.fullScreenStage = new FocusableStage(new ScreenViewport());

        this.gameManager = new GameManager(this, server, client, players, fullScreenStage, game.preferences.getInteger(Constants.CONTROL_PREFERENCE, Constants.PIE_MENU_CONTROL), game.skin);

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.map = new Map(rows, cols);
        Array<GridPoint2> startPositions = this.map.getStartPositions();
        for (Player player : players) {
            player.setStartPos(startPositions.get(0)); // TODO: Fix/Modify this later
            // Was `get(i % startPositions.size);`
        }

        initViewports();

        this.scoreBars = new ScoreBar(fullScreenStage.getViewport(), players.length);
    }

    public GameScreen(final Zoned game, Cell[][] mapGrid, Array<GridPoint2> startPositions, int wallCount, Player[] players, Server server, Client client) {
        this.game = game;

        this.fullScreenStage = new FocusableStage(new ScreenViewport());

        this.gameManager = new GameManager(this, server, client, players, fullScreenStage, game.preferences.getInteger(Constants.CONTROL_PREFERENCE, Constants.PIE_MENU_CONTROL), game.skin);

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.map = new Map(mapGrid, startPositions, wallCount);
        for (Player player : players) {
            player.setStartPos(this.map.getStartPositions().get(0)); // TODO: Fix/Modify this later
            // Was `get(i % startPositions.size);`
        }

        initViewports();

        this.scoreBars = new ScoreBar(fullScreenStage.getViewport(), players.length);
    }

    private void initViewports() {
        int viewportCount = isSplitscreenMultiplayer() ? gameManager.playerManager.getPlayers().length : 1;

        this.playerViewports = new ExtendViewport[viewportCount];
        for (int i = 0; i < this.playerViewports.length; i++) {
            this.playerViewports[i] = new ExtendViewport(Constants.WORLD_SIZE / viewportCount, Constants.WORLD_SIZE);
        }

        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpInputProcessors();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        setUpPauseButton();
        setUpZoomButton();
        setUpUI();
    }

    private void setUpUI() {
        HoverImageButton pauseButton = setUpPauseButton();
        HoverImageButton zoomButton = setUpZoomButton();

        Table table = new Table();
        table.setFillParent(true);
        table.top();

        table.add(pauseButton).padTop(ScoreBar.BAR_HEIGHT)
                .width(pauseButton.getWidth() * game.getScaleFactor()).height(pauseButton.getHeight() * game.getScaleFactor());
        table.row();
        table.add(zoomButton).padTop(5f)
                .width(zoomButton.getWidth() * game.getScaleFactor()).height(zoomButton.getHeight() * game.getScaleFactor());

        fullScreenStage.addActor(table);
    }

    private HoverImageButton setUpPauseButton() {
        Drawable pauseImageDrawable = new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/ui_icons/ic_pause.png")));
        final HoverImageButton pauseButton = new HoverImageButton(pauseImageDrawable);
        Image pauseImage = pauseButton.getImage();
        pauseImage.setOrigin(pauseImage.getPrefWidth() / 2, pauseImage.getPrefHeight() / 2);
        pauseImage.setScale(game.getScaleFactor());
        pauseButton.setNormalAlpha(.8f);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseDialog();
            }
        });
        return pauseButton;
    }

    private HoverImageButton setUpZoomButton() {
        Drawable zoomInImage = new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/ui_icons/ic_zoom_in.png")));
        Drawable zoomOutImage = new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/ui_icons/ic_zoom_out.png")));
        final HoverImageButton zoomButton = new HoverImageButton(zoomOutImage, zoomInImage);
        Image zoomImage = zoomButton.getImage();
        zoomImage.setOrigin(zoomImage.getPrefWidth() / 2, zoomImage.getPrefHeight() / 2);
        zoomImage.setScale(game.getScaleFactor());
        zoomButton.setNormalAlpha(.8f);
        zoomButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (targetZoom == Constants.ZOOM_MIN_VALUE) {
                    targetZoom = Constants.ZOOM_MAX_VALUE;
                } else if (targetZoom == Constants.ZOOM_MAX_VALUE) {
                    targetZoom = Constants.ZOOM_MIN_VALUE;
                }
            }
        });
        return zoomButton;
    }

    private void setUpInputProcessors() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(fullScreenStage);
        inputMultiplexer.addProcessor(gameManager.playerManager);
        inputMultiplexer.addProcessor(this); // ESC key, Back button etc
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void resize(int width, int height) {
        for (int i = 0; i < playerViewports.length; i++) {
            playerViewports[i].update(width / playerViewports.length, height);
            updateCamera(playerViewports[i].getCamera(), width / playerViewports.length, height);
            this.playerViewports[i].setScreenX(i * width / playerViewports.length);
        }

        scoreBars.resize(width, height);
        fullScreenStage.getViewport().update(width, height, true);
    }

    private void updateCamera(Camera camera, int width, int height) {
        camera.viewportHeight = Constants.WORLD_SIZE;
        camera.viewportWidth = Constants.WORLD_SIZE * height / width;
        camera.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //TODO: Draw cool background, bloom and particle effects

        if (!gameComplete) {
            if (!isSplitscreenMultiplayer()) {      // We're playing on multiple devices (Server-client)
                gameManager.connectionManager.serverClientCommunicate();
            } else {                                // We're playing on the same device (Splitscreen)
                gameManager.playerManager.updatePlayerDirections();
            }
        }

        map.update(gameManager.playerManager, delta);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < this.playerViewports.length; i++) {
            focusAndRenderViewport(playerViewports[i], gameManager.playerManager.getPlayer(i), delta);
        }

        fullScreenStage.getViewport().apply(true);
        renderer.setProjectionMatrix(fullScreenStage.getCamera().combined);

        if (isSplitscreenMultiplayer()) { // Draw the viewport divider only when playing on the same device
            drawViewportDividers();
        }

        if (!gameComplete) {
            gameManager.playerManager.renderPlayerControlPrompt(renderer, delta);
        }

        scoreBars.render(renderer, gameManager.playerManager.getPlayers(), delta);

        if (!gameComplete && map.gameComplete(gameManager.playerManager.getPlayers())) {
            gameManager.directionBufferManager.clearBuffer();
            gameManager.playerManager.stopPlayers();
            gameComplete = true;
        }

        if (gameComplete) {
            fadeOutScreen(delta);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (showFPSCounter) {
            FPSDisplayer.displayFPS(fullScreenStage.getViewport(), fullScreenStage.getBatch(), font, 0, 7);
        }
        fullScreenStage.act(delta);
        fullScreenStage.draw();
    }

    private void fadeOutScreen(float delta) {
        fadeOutOverlay.a += delta * 2f * (2f - fadeOutOverlay.a);
        fadeOutOverlay.a = Math.min(fadeOutOverlay.a, 1f);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(fadeOutOverlay);
        renderer.rect(0, 0, fullScreenStage.getWidth(), fullScreenStage.getHeight());
        renderer.end();

        if (fadeOutOverlay.a >= 1f && !gameCompleteFadeOutDone) {
            gameCompleteFadeOutDone = true;
            if (gameManager.connectionManager.isActive) {
                gameManager.connectionManager.close();
            }
            game.setScreen(new VictoryScreen(game, gameManager.playerManager, map.rows, map.cols, map.wallCount));
        }
    }

    private void drawViewportDividers() {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        int lineCount = gameManager.playerManager.getPlayers().length;

        float height = fullScreenStage.getViewport().getWorldHeight();
        for (int i = 1; i < lineCount; i++) {
            float startX = (fullScreenStage.getViewport().getWorldWidth() / (float) lineCount) * i;

            renderer.rect(startX, 0,
                    Constants.VIEWPORT_DIVIDER_TOTAL_WIDTH / 2, height,
                    Color.BLACK, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK);
            renderer.rect(startX, 0,
                    -Constants.VIEWPORT_DIVIDER_TOTAL_WIDTH / 2, height,
                    Color.BLACK, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK);
        }
        renderer.end();
    }

    private void focusAndRenderViewport(Viewport viewport, Player player, float delta) {
        focusCameraOnPlayer(viewport, player, delta);
        viewport.apply();

        renderer.setProjectionMatrix(viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        map.render(gameManager.playerManager.getPlayers(), renderer, (OrthographicCamera) viewport.getCamera(), delta);
        renderer.end();
    }

    private void focusCameraOnPlayer(Viewport viewport, Player player, float delta) {
        OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();

        float lerp = 1.8f;
        Vector3 position = camera.position;

        float posX = (player.position.x * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;

        position.x += (posX - position.x) * lerp * delta;
        position.y += (posY - position.y) * lerp * delta;

        camera.zoom += (targetZoom - camera.zoom) * lerp * 3 * delta;
    }

    private void showDisconnectionDialog() {
        final Array<String> dialogButtonTexts = new Array<String>();
        dialogButtonTexts.add("OK");
        fullScreenStage.showDialog("Disconnected", dialogButtonTexts,
                false,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        gameManager.connectionManager.close();
                        game.setScreen(new MainMenuScreen(game));
                    }
                }, game.skin);
    }

    private void showPauseDialog() {
        gameManager.playerManager.stopPlayers();
        if (!gameManager.connectionManager.isActive) {
            gameManager.directionBufferManager.clearBuffer();
        }

        final Array<String> dialogButtonTexts = new Array<String>();
        dialogButtonTexts.add("Resume");
        //dialogButtonTexts.add("Restart");
        dialogButtonTexts.add("Main Menu");
        fullScreenStage.showDialog("Game Paused", dialogButtonTexts,
                true,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        if (buttonText.equals(dialogButtonTexts.get(1))) {
                            if (gameManager.connectionManager.isActive) {
                                gameManager.connectionManager.close();
                            } else {
                                game.setScreen(new MainMenuScreen(game));
                            }
                        }
                    }
                }, game.skin);
    }

    private boolean isSplitscreenMultiplayer() {
        return !gameManager.connectionManager.isActive;
    }

    public void disconnected() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (!gameComplete) {
                    gameManager.playerManager.stopPlayers();
                    gameManager.directionBufferManager.clearBuffer();
                    showDisconnectionDialog();
                    Gdx.input.setInputProcessor(fullScreenStage);
                }
            }
        });
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            showPauseDialog();

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
            showPauseDialog();

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