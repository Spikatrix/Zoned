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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.ScoreBar;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.GameManager;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

public class GameScreen extends ScreenAdapter implements InputProcessor, GestureDetector.GestureListener {
    final Zoned game;

    private GameManager gameManager;

    private Map map;

    private ExtendViewport[] playerViewports; // Two viewports in split-screen mode; else one
    private ShapeRenderer renderer;

    private boolean gameComplete = false;
    private Color fadeOutOverlay = new Color(0, 0, 0, 0);
    private boolean gameCompleteFadeOutDone = false;

    private Stage fullScreenStage;
    private BitmapFont font;
    private boolean showFPSCounter;
    private ScoreBar scoreBars;

    private Vector2 touchStartPos;
    private Vector2 touchStartPos2;
    private float targetZoom = Constants.ZOOM_MIN_VALUE;

    public GameScreen(final Zoned game, int rows, int cols, Player[] players, Server server, Client client) {
        this.game = game;

        this.fullScreenStage = new Stage(new ScreenViewport());

        this.gameManager = new GameManager(this, server, client, players, fullScreenStage);

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.map = new Map(rows, cols);

        initViewports();

        this.scoreBars = new ScoreBar(players.length);

        touchStartPos = new Vector2();
        touchStartPos2 = new Vector2();
    }

    private void initViewports() {
        int noOfViewports = isSplitscreenMultiplayer() ? gameManager.playerManager.getPlayers().length : 1;

        this.playerViewports = new ExtendViewport[noOfViewports];
        for (int i = 0; i < this.playerViewports.length; i++) {
            this.playerViewports[i] = new ExtendViewport(Constants.WORLD_SIZE / noOfViewports, Constants.WORLD_SIZE);
        }

        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpInputProcessors();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        setUpZoomButton();
    }

    private void setUpInputProcessors() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(fullScreenStage);
        inputMultiplexer.addProcessor(new GestureDetector(this));
        inputMultiplexer.addProcessor(gameManager.playerManager);
        inputMultiplexer.addProcessor(this); // ESC key, Back button etc
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpZoomButton() {
        Table table = new Table();
        table.setFillParent(true);
        table.top();
        Drawable zoomInImage = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_zoom_in.png"))));
        Drawable zoomOutImage = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_zoom_out.png"))));
        final HoverImageButton zoomButton = new HoverImageButton(zoomOutImage, zoomOutImage, zoomInImage);
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
        table.add(zoomButton).padTop(ScoreBar.BAR_HEIGHT);
        fullScreenStage.addActor(table);
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

        scoreBars.render(renderer, gameManager.playerManager.getPlayers(), delta);

        if (!gameComplete && map.gameComplete(gameManager.playerManager.getPlayers())) {
            gameManager.playerManager.stopPlayers();
            gameComplete = true;
        }

        if (gameComplete) {
            fadeOutScreen(delta);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (this.showFPSCounter) {
            FPSDisplayer.displayFPS(fullScreenStage.getBatch(), font, 0, 7);
        }
        fullScreenStage.act(delta);
        fullScreenStage.draw();
    }

    private void fadeOutScreen(float delta) {
        fadeOutOverlay.a += delta * 2f * (2f - fadeOutOverlay.a);
        fadeOutOverlay.a = Math.min(fadeOutOverlay.a, 1f);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(fadeOutOverlay);
        renderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer.end();

        if (fadeOutOverlay.a >= 1f && !gameCompleteFadeOutDone) {
            gameCompleteFadeOutDone = true;
            if (gameManager.connectionManager.isActive) {
                gameManager.connectionManager.close();
            }
            game.setScreen(new VictoryScreen(game, gameManager.playerManager, map.rows, map.cols));
        }
    }

    private void drawViewportDividers() {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        int lineCount = gameManager.playerManager.getPlayers().length;

        float height = fullScreenStage.getViewport().getScreenHeight();
        for (int i = 1; i < lineCount; i++) {
            float startX = (fullScreenStage.getViewport().getScreenWidth() / (float) lineCount) * i;

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
        Dialog disconnectionDialog = new Dialog("", game.skin) {
            @Override
            public void result(Object obj) {
                gameManager.connectionManager.close();
                game.setScreen(new MainMenuScreen(game));
            }
        };
        disconnectionDialog.getButtonTable().defaults().width(200f);
        disconnectionDialog.button("OK");
        disconnectionDialog.getColor().a = 0; // Gets rid of the dialog flicker issue during `show()`
        disconnectionDialog.text("Disconnected").pad(25f * game.getScaleFactor(), 25f * game.getScaleFactor(), 20f * game.getScaleFactor(), 25f * game.getScaleFactor());
        Label label = (Label) disconnectionDialog.getContentTable().getChild(0);
        label.setAlignment(Align.center);
        disconnectionDialog.show(fullScreenStage);
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
            if (gameManager.connectionManager.isActive) {
                gameManager.connectionManager.close();
            } else {
                game.setScreen(new MainMenuScreen(game));
            }

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

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {
    }
}