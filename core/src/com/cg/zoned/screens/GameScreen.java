package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.Preferences;
import com.cg.zoned.ScoreBar;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.ViewportDividers;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.GameManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.UITextDisplayer;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class GameScreen extends ScreenObject implements InputProcessor {
    private GameManager gameManager;
    private GLProfiler profiler;

    private Map map;
    private SplitViewportManager splitViewportManager;
    private ViewportDividers viewportDividers;

    private Overlay backgroundColorOverlay;
    private float bgAlpha = .25f;

    private Overlay screenOverlay;
    private boolean gameCompleteFadeOutDone;

    private ScoreBar scoreBars;
    private boolean gamePaused;
    private boolean gameDisconnected;

    private GridPoint2[] playerStartPositions;

    public GameScreen(final Zoned game, PreparedMapData mapData, Player[] players) {
        this(game, mapData, players, null, null);
    }

    public GameScreen(final Zoned game, PreparedMapData mapData, Player[] players, ServerLobbyConnectionManager connectionManager) {
        this(game, mapData, players, connectionManager.getServer(), null);
    }

    public GameScreen(final Zoned game, PreparedMapData mapData, Player[] players, ClientLobbyConnectionManager connectionManager) {
        this(game, mapData, players, null, connectionManager.getClient());
    }

    private GameScreen(final Zoned game, PreparedMapData mapData, Player[] players, Server server, Client client) {
        super(game);
        game.discordRPCManager.updateRPC("Playing a match", mapData.map.getName(), players.length - 1);

        this.gameManager = new GameManager(this);
        this.gameManager.setUpConnectionManager(server, client);
        this.gameManager.setUpDirectionAndPlayerBuffer(players, screenStage,
                game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0),
                game.skin, game.getScaleFactor(), usedTextures);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
        this.map = new Map(mapData.mapGrid, mapData.wallCount, this.shapeDrawer);
        this.map.initFloodFillVars();

        this.backgroundColorOverlay = new Overlay(new Color(0, 0, 0, bgAlpha), 5.0f);
        this.screenOverlay = new Overlay(new Color(Color.CLEAR), new Color(Color.BLACK), 6.0f);
        this.screenOverlay.drawOverlay(false);
        this.scoreBars = new ScoreBar(screenStage.getViewport(), this.gameManager.playerManager.getTeamData().size, game.getScaleFactor());

        playerStartPositions = new GridPoint2[players.length];
        for (int i = 0; i < players.length; i++) {
            players[i].resetPrevPosition();
            playerStartPositions[i] = players[i].getRoundedPosition();
        }

        if (Constants.DISPLAY_EXTENDED_GL_STATS) {
            profiler = new GLProfiler(Gdx.graphics);
            profiler.enable();
        }

        BitmapFont playerLabelFont = game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName());
        initViewports(players, map);
        map.createPlayerLabelTextures(players, shapeDrawer, playerLabelFont);
        map.updateMap(gameManager.playerManager);
    }

    private void initViewports(Player[] players, Map map) {
        int viewportCount = isSplitscreenMultiplayer() ? players.length : 1;
        float stretchAspectRatio = 16f / 9;

        float centerX = (map.cols * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        float centerY = (map.rows * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;

        if (isSplitscreenMultiplayer()) {
            // Uses extend viewports for each player so that all screen space is used for the game area
            this.splitViewportManager = new SplitViewportManager(viewportCount, Constants.WORLD_SIZE, new Vector2(centerX, centerY));
        } else {
            // Uses stretch viewports for all players on all devices so that all of them see the same game area regardless of
            // their screen/game window size. Play on a 16:9 aspect ratio for the best experience
            this.splitViewportManager = new SplitViewportManager(viewportCount, Constants.WORLD_SIZE, stretchAspectRatio, new Vector2(centerX, centerY));
        }
        splitViewportManager.setCameraPosLerpVal(1.8f);

        viewportDividers = new ViewportDividers(viewportCount, players);
    }

    @Override
    public void show() {
        setUpInputProcessors();
        setUpUI();
    }

    private void setUpUI() {
        uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        Texture pauseTexture = game.assets.getTexture(Assets.TextureObject.PAUSE_TEXTURE);
        uiButtonManager.addPauseButtonToStage(pauseTexture).addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseDialog();
            }
        });

        Texture zoomInTexture = game.assets.getTexture(Assets.TextureObject.ZOOM_IN_TEXTURE);
        Texture zoomOutTexture = game.assets.getTexture(Assets.TextureObject.ZOOM_OUT_TEXTURE);
        uiButtonManager.addZoomButtonToStage(zoomOutTexture, zoomInTexture).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleZoom();
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
        super.resize(width, height);
        scoreBars.resize(width, height);
        splitViewportManager.resize(width, height);
    }

    @Override
    public void render(float delta) {
        if (profiler != null) {
            profiler.reset();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!gameManager.gameOver) {
            if (!isSplitscreenMultiplayer()) {
                // We're playing on multiple devices (Server-client)
                gameManager.gameConnectionManager.serverClientCommunicate(map);
            } else {
                // We're playing on the same device (Splitscreen)
                gameManager.playerManager.updatePlayerDirections();
            }

            if (!gameDisconnected) {
                map.update(gameManager.playerManager, delta);
            }
        }

        drawGameBG(delta);

        // Renders each split player viewport
        splitViewportManager.render(shapeDrawer, batch, map, gameManager.playerManager.getPlayers(), delta);

        screenStage.getViewport().apply(true);
        batch.setProjectionMatrix(screenStage.getCamera().combined);
        batch.begin();

        viewportDividers.render(shapeDrawer, screenStage);

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

        float textXOffset = UITextDisplayer.padding * 3 * game.getScaleFactor();
        float textYOffset = scoreBars.scoreBarHeight + textXOffset;
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
        // TODO: Could optimize, no need to compute leading team every FRAME. Instead compute it every TURN. Minor optimization tho
        Color highscoreTeamColor = gameManager.playerManager.getLeadingTeamColor();
        backgroundColorOverlay.drawOverlay(highscoreTeamColor != null);
        if (highscoreTeamColor != null) {
            backgroundColorOverlay.setTargetColor(highscoreTeamColor.r, highscoreTeamColor.g, highscoreTeamColor.b, bgAlpha);
        }

        batch.setProjectionMatrix(screenStage.getCamera().combined);
        batch.begin();
        backgroundColorOverlay.render(shapeDrawer, screenStage, delta);
        batch.end();
    }

    private void fadeOutScreen(float delta) {
        screenOverlay.drawOverlay(true);
        screenOverlay.render(shapeDrawer, screenStage, delta);

        if (screenOverlay.getOverlayAlpha() >= 0.96f && !gameCompleteFadeOutDone) {
            gameCompleteFadeOutDone = true;
            if (gameManager.gameConnectionManager.isActive) {
                gameManager.gameConnectionManager.close();
            }

            // Transition to VictoryScreen after completing rendering the current frame to avoid SIGSEGV crashes
            Gdx.app.postRunnable(() -> {
                dispose();
                game.setScreen(new VictoryScreen(game, gameManager.playerManager.getTeamData()));
            });
        }
    }

    private void showPauseDialog() {
        gamePaused = true;

        FocusableStage.DialogButton[] dialogButtons;

        if (isSplitscreenMultiplayer()) {
            // Stop players when in splitscreen multiplayer only
            gameManager.playerManager.stopPlayers(false);
            gameManager.directionBufferManager.clearBuffer();

            dialogButtons = new FocusableStage.DialogButton[] {
                    FocusableStage.DialogButton.Resume,
                    FocusableStage.DialogButton.Restart,
                    FocusableStage.DialogButton.MainMenu,
            };
        } else {
            dialogButtons = new FocusableStage.DialogButton[] {
                    FocusableStage.DialogButton.Resume,
                    FocusableStage.DialogButton.MainMenu,
            };
        }

        screenStage.showDialog("Pause Menu", dialogButtons,
                true, button -> {
                    if (button == FocusableStage.DialogButton.MainMenu) {
                        endGame();
                    } else if (button == FocusableStage.DialogButton.Restart) {
                        restartGame();
                    }

                    gamePaused = false;
                });
    }

    /**
     * Restarts the match (Supported only in splitscreen multiplayer mode for now)
     */
    private void restartGame() {
        gameManager.playerManager.stopPlayers(true);
        gameManager.directionBufferManager.clearBuffer();

        Player[] players = gameManager.playerManager.getPlayers();
        for (int i = 0; i < players.length; i++) {
            players[i].setPosition(playerStartPositions[i]);
            players[i].resetPrevPosition();
        }

        gameManager.playerManager.resetScores();
        scoreBars.reset();
        map.clearGrid();
        map.updateMap(gameManager.playerManager);
    }

    private boolean isSplitscreenMultiplayer() {
        return !gameManager.gameConnectionManager.isActive;
    }

    public void serverPlayerDisconnected(final Connection connection) {
        Gdx.app.postRunnable(() -> {
            if (gameManager.gameOver) {
                return;
            }

            // TODO: Connection ID is not always the connIndex, don't use getID for the index
            int connIndex = connection.getID();
            String playerName = "A player"; // A generic name for now xD

            try {
                playerName = gameManager.playerManager.getPlayer(connIndex).name;
            } catch (ArrayIndexOutOfBoundsException e) {
                Gdx.app.error(Constants.LOG_TAG, "Failed to fetch the name of the disconnected client");
            }

            gameManager.playerManager.stopPlayers(false);

            gameManager.directionBufferManager.ignorePlayer();
            gameManager.gameConnectionManager.sendPlayerDisconnectedBroadcast(playerName);
            showPlayerDisconnectedDialog(playerName);
        });
    }

    public void clientPlayerDisconnected(String playerName) {
        gameManager.playerManager.stopPlayers(false);
        gameManager.directionBufferManager.ignorePlayer();
        showPlayerDisconnectedDialog(playerName);
    }

    private void showPlayerDisconnectedDialog(String playerName) {
        screenStage.showOKDialog(playerName + " got disconnected", false, null);
    }

    public void disconnected() {
        Gdx.app.postRunnable(() -> {
            gameDisconnected = true;
            if (!gameManager.gameOver) {
                gameManager.directionBufferManager.clearBuffer();
                screenStage.showOKDialog("Disconnected", false, button -> endGame());
                Gdx.input.setInputProcessor(screenStage);
            }
        });
    }

    private void endGame() {
        if (gameManager.gameConnectionManager.isActive) {
            gameManager.gameConnectionManager.close();
        } else {
            dispose();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void toggleZoom() {
        splitViewportManager.toggleZoom();
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
            toggleZoom();
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