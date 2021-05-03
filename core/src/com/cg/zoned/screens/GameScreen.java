package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.Preferences;
import com.cg.zoned.ScoreBar;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.GameManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class GameScreen extends ScreenObject implements InputProcessor {
    private GameManager gameManager;
    private GLProfiler profiler;

    private Map map;
    private Viewport[] playerViewports;
    private Color[] dividerLeftColor, dividerRightColor;

    private Color fadeOutOverlay = new Color(0, 0, 0, 0);
    private boolean gameCompleteFadeOutDone = false;

    private ScoreBar scoreBars;
    private boolean gamePaused = false;

    private Color currentBgColor, targetBgColor;
    private float bgAnimSpeed = 1.8f;
    private float bgAlpha = .25f;

    private HoverImageButton zoomButton;
    private float targetZoom = Constants.ZOOM_MIN_VALUE;

    public GameScreen(final Zoned game, MapManager mapManager, Player[] players) {
        this(game, mapManager, players, null, null);
    }

    public GameScreen(final Zoned game, MapManager mapManager, Player[] players, ServerLobbyConnectionManager connectionManager) {
        this(game, mapManager, players, connectionManager.getServer(), null);
    }

    public GameScreen(final Zoned game, MapManager mapManager, Player[] players, ClientLobbyConnectionManager connectionManager) {
        this(game, mapManager, players, null, connectionManager.getClient());
    }

    private GameScreen(final Zoned game, MapManager mapManager, Player[] players, Server server, Client client) {
        super(game);
        game.discordRPCManager.updateRPC("Playing a match", mapManager.getPreparedMap().getName(), players.length - 1);

        this.gameManager = new GameManager(this);
        this.gameManager.setUpConnectionManager(server, client);
        this.gameManager.setUpDirectionAndPlayerBuffer(players, screenStage,
                game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0),
                game.skin, game.getScaleFactor(), usedTextures);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
        this.map = new Map(mapManager.getPreparedMapGrid(), mapManager.getWallCount(), this.shapeDrawer);
        this.map.initFloodFillVars();

        if (Constants.DISPLAY_EXTENDED_GL_STATS) {
            profiler = new GLProfiler(Gdx.graphics);
            profiler.enable();
        }

        currentBgColor = new Color(0, 0, 0, bgAlpha);
        targetBgColor = new Color(0, 0, 0, bgAlpha);

        BitmapFont playerLabelFont = game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName());
        initViewports(players);

        map.createPlayerLabelTextures(players, shapeDrawer, playerLabelFont);

        this.scoreBars = new ScoreBar(screenStage.getViewport(), this.gameManager.playerManager.getTeamData().size, game.getScaleFactor());
    }

    private void initViewports(Player[] players) {
        int viewportCount = isSplitscreenMultiplayer() ? players.length : 1;
        float stretchAspectRatio = 16f / 9;

        if (isSplitscreenMultiplayer()) {
            // Extend viewport so that all screen space is used for the game area
            this.playerViewports = new ExtendViewport[viewportCount];
        } else {
            // Stretch viewport so that all players see the same game area regardless of
            // their screen/game window size. Play on a 16:9 aspect ratio for the best experience
            this.playerViewports = new StretchViewport[viewportCount];
        }
        if (viewportCount > 1) {
            this.dividerRightColor = new Color[viewportCount - 1];
            this.dividerLeftColor = new Color[viewportCount - 1];
        }
        for (int i = 0; i < this.playerViewports.length; i++) {
            if (isSplitscreenMultiplayer()) {
                this.playerViewports[i] = new ExtendViewport(Constants.WORLD_SIZE / viewportCount, Constants.WORLD_SIZE);
            } else {
                this.playerViewports[i] = new StretchViewport(Constants.WORLD_SIZE * stretchAspectRatio, Constants.WORLD_SIZE);
            }

            if (i < viewportCount - 1) {
                this.dividerLeftColor[i] = new Color(players[i].color);
                this.dividerRightColor[i] = new Color(players[i + 1].color);
            }
        }
    }

    @Override
    public void show() {
        setUpInputProcessors();
        setUpUI();
    }

    private void setUpUI() {
        uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
        setUpPauseButton(uiButtonManager);
        setUpZoomButton(uiButtonManager);
    }

    private void setUpPauseButton(UIButtonManager uiButtonManager) {
        final HoverImageButton pauseButton = uiButtonManager.addPauseButtonToStage();
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseDialog();
            }
        });
    }

    private void setUpZoomButton(UIButtonManager uiButtonManager) {
        zoomButton = uiButtonManager.addZoomButtonToStage();
        zoomButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (targetZoom == Constants.ZOOM_MIN_VALUE) {
                    targetZoom = Constants.ZOOM_MAX_VALUE;
                } else if (targetZoom == Constants.ZOOM_MAX_VALUE) {
                    targetZoom = Constants.ZOOM_MIN_VALUE;
                }
            }
        });
    }

    private void setUpInputProcessors() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(screenStage);
        inputMultiplexer.addProcessor(gameManager.playerManager);
        inputMultiplexer.addProcessor(this); // ESC key, Back button etc
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void resize(int width, int height) {
        for (int i = 0; i < playerViewports.length; i++) {
            playerViewports[i].update(width / playerViewports.length, height);
            this.playerViewports[i].setScreenX(i * width / playerViewports.length);
        }

        scoreBars.resize(width, height);
        super.resize(width, height);
    }

    @Override
    public void render(float delta) {
        if (profiler != null) {
            profiler.reset();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawGameBG(delta);

        if (!gameManager.gameOver) {
            if (!isSplitscreenMultiplayer()) {      // We're playing on multiple devices (Server-client)
                gameManager.gameConnectionManager.serverClientCommunicate();
            } else {                                // We're playing on the same device (Splitscreen)
                gameManager.playerManager.updatePlayerDirections();
            }
        }

        map.update(gameManager.playerManager, delta);

        batch.setProjectionMatrix(screenStage.getCamera().combined);
        batch.begin();
        shapeDrawer.setColor(currentBgColor);
        shapeDrawer.filledRectangle(0, 0, screenStage.getWidth(), screenStage.getHeight());
        batch.end();

        for (int i = 0; i < this.playerViewports.length; i++) {
            // Render everything in the i-th player viewport
            renderMap(i, delta);
        }

        screenStage.getViewport().apply(true);
        batch.setProjectionMatrix(screenStage.getCamera().combined);
        batch.begin();

        if (isSplitscreenMultiplayer() && playerViewports.length >= 2) {
            // Draw the viewport divider only when playing on the same device with at least 2 splitscreens
            drawViewportDividers();
        }

        if (!gameManager.gameOver) {
            gameManager.playerManager.renderPlayerControlPrompt(shapeDrawer, delta);
        }


        scoreBars.render(shapeDrawer, game.getSmallFont(), gameManager.playerManager.getTeamData(), delta);

        if (!gameManager.gameOver && map.gameComplete(gameManager.playerManager.getTeamData())) {
            gameManager.directionBufferManager.clearBuffer();
            gameManager.playerManager.stopPlayers(true);
            gameManager.gameOver = true;
        }

        if (gameManager.gameOver) {
            fadeOutScreen(delta);
        }

        batch.end();

        screenStage.act(delta);
        screenStage.draw();

        displayUIText();
    }

    private void displayUIText() {
        BitmapFont smallFont = game.getSmallFont();

        float textYOffset = scoreBars.scoreBarHeight + (UITextDisplayer.padding * 2);
        float textXOffset = UITextDisplayer.padding * 3 * game.getScaleFactor();
        float fontHeight = smallFont.getLineHeight();
        int uiTextLineIndex = 0;

        // Display FPS if enabled from the game settings
        if (game.showFPSCounter()) {
            UITextDisplayer.displayFPS(screenStage.getViewport(), screenStage.getBatch(), smallFont,
                    textXOffset, textYOffset);
            uiTextLineIndex++;
        }

        // Display ping if in networked multiplayer mode
        if (gameManager.gameConnectionManager.isActive) {
            UITextDisplayer.displayPing(screenStage.getViewport(), screenStage.getBatch(), smallFont,
                    gameManager.gameConnectionManager.getPing(), textXOffset, textYOffset + (fontHeight * uiTextLineIndex));
            uiTextLineIndex++;
        }

        // Display GL profiler stats if enabled when building the game
        if (profiler != null) {
            UITextDisplayer.displayExtendedGLStatistics(screenStage.getViewport(), screenStage.getBatch(),
                    smallFont, profiler, textXOffset, textYOffset + (fontHeight * uiTextLineIndex));
            uiTextLineIndex++;
        }
    }

    private void drawGameBG(float delta) {
        int highscore = 0;
        for (TeamData teamData : gameManager.playerManager.getTeamData()) {
            if (teamData.getScore() > highscore) {
                highscore = teamData.getScore();
                targetBgColor.set(teamData.getColor());
                targetBgColor.a = bgAlpha;
            } else if (teamData.getScore() == highscore) {
                targetBgColor.set(0, 0, 0, bgAlpha);
            }
        }
        currentBgColor.lerp(targetBgColor, bgAnimSpeed * delta);
        currentBgColor.a = Math.min(targetBgColor.a, 1 - fadeOutOverlay.a);
    }

    private void fadeOutScreen(float delta) {
        fadeOutOverlay.a += delta * 2f * (2f - fadeOutOverlay.a);
        fadeOutOverlay.a = Math.min(fadeOutOverlay.a, 1f);

        shapeDrawer.setColor(fadeOutOverlay);
        shapeDrawer.filledRectangle(0, 0, screenStage.getWidth(), screenStage.getHeight());

        if (fadeOutOverlay.a >= 1f && !gameCompleteFadeOutDone) {
            gameCompleteFadeOutDone = true;
            if (gameManager.gameConnectionManager.isActive) {
                gameManager.gameConnectionManager.close();
            }

            // Transition to VictoryScreen after completing rendering the current frame to avoid SIGSEGV crashes
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    dispose();
                    game.setScreen(new VictoryScreen(game, gameManager.playerManager.getTeamData()));
                }
            });
        }
    }

    private void drawViewportDividers() {
        int lineCount = gameManager.playerManager.getPlayers().length - 1;
        float height = screenStage.getViewport().getWorldHeight();
        float dividerFadeWidth = Math.max(Constants.VIEWPORT_DIVIDER_FADE_WIDTH / (playerViewports.length - 1), 3f);
        float dividerSolidWidth = Math.max(Constants.VIEWPORT_DIVIDER_SOLID_WIDTH / (playerViewports.length - 1), 1f);
        for (int i = 0; i < lineCount; i++) {
            float startX = (screenStage.getViewport().getWorldWidth() / (float) (lineCount + 1)) * (i + 1);

            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    dividerSolidWidth * 2, height,
                    dividerRightColor[i], dividerLeftColor[i], dividerLeftColor[i], dividerRightColor[i]);
            shapeDrawer.filledRectangle(startX + dividerSolidWidth, 0,
                    dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, dividerRightColor[i], dividerRightColor[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    -dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, dividerLeftColor[i], dividerLeftColor[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
        }
    }

    private void renderMap(int index, float delta) {
        Viewport viewport = playerViewports[index];
        Player[] players = gameManager.playerManager.getPlayers();

        focusCameraOnPlayer(viewport, players[index], delta);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        map.render(players, index, shapeDrawer, (OrthographicCamera) viewport.getCamera(), delta);
        batch.end();
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
        screenStage.showOKDialog("Disconnected", false,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(FocusableStage.DialogButton button) {
                        gameManager.gameConnectionManager.close();
                        dispose();
                        game.setScreen(new MainMenuScreen(game));
                    }
                }, game.skin);
    }

    private void showPauseDialog() {
        gamePaused = true;

        if (isSplitscreenMultiplayer()) {
            // Stop players when in splitscreen multiplayer only
            gameManager.playerManager.stopPlayers(false);
            gameManager.directionBufferManager.clearBuffer();
        }

        //dialogButtonTexts.add("Restart");

        screenStage.showDialog("Pause Menu",
                new FocusableStage.DialogButton[]{FocusableStage.DialogButton.Resume, FocusableStage.DialogButton.MainMenu},
                true,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(FocusableStage.DialogButton button) {
                        if (button == FocusableStage.DialogButton.MainMenu) {
                            if (gameManager.gameConnectionManager.isActive) {
                                gameManager.gameConnectionManager.close();
                            } else {
                                dispose();
                                game.setScreen(new MainMenuScreen(game));
                            }
                        }

                        gamePaused = false;
                    }
                }, game.skin);
    }

    private boolean isSplitscreenMultiplayer() {
        return !gameManager.gameConnectionManager.isActive;
    }

    public void serverPlayerDisconnected(final Connection connection) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (gameManager.gameOver) {
                    return;
                }

                int connIndex = connection.getID();
                String playerName = gameManager.playerManager.getPlayer(connIndex).name;

                gameManager.playerManager.stopPlayers(false);

                gameManager.directionBufferManager.ignorePlayer();
                gameManager.gameConnectionManager.sendPlayerDisconnectedBroadcast(playerName);
                showPlayerDisconnectedDialog(playerName);
            }
        });
    }

    public void clientPlayerDisconnected(String playerName) {
        gameManager.playerManager.stopPlayers(false);
        gameManager.directionBufferManager.ignorePlayer();
        showPlayerDisconnectedDialog(playerName);
    }

    private void showPlayerDisconnectedDialog(String playerName) {
        screenStage.showOKDialog(playerName + " got disconnected",
                false, game.getScaleFactor(),
                null, game.skin);
    }

    public void disconnected() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (!gameManager.gameOver) {
                    gameManager.playerManager.stopPlayers(false);
                    gameManager.directionBufferManager.clearBuffer();
                    showDisconnectionDialog();
                    Gdx.input.setInputProcessor(screenStage);
                }
            }
        });
    }

    @Override
    public void pause() {
        if (!gamePaused) {
            showPauseDialog();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        map.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.P) {
            showPauseDialog();
            return true;
        } else if (keycode == Input.Keys.Z) {
            zoomButton.toggle();
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}