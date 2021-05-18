package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
import com.cg.zoned.Player;
import com.cg.zoned.Preferences;
import com.cg.zoned.ScoreBar;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.ViewportDividers;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.TeamData;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.GameManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
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

    private Color fadeOutOverlay = new Color(0, 0, 0, 0);
    private boolean gameCompleteFadeOutDone = false;

    private ScoreBar scoreBars;
    private boolean gamePaused = false;

    private Color currentBgColor, targetBgColor;
    private float bgAnimSpeed = 2.0f;
    private float bgAlpha = .25f;

    private HoverImageButton zoomButton;

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

        if (Constants.DISPLAY_EXTENDED_GL_STATS) {
            profiler = new GLProfiler(Gdx.graphics);
            profiler.enable();
        }

        currentBgColor = new Color(0, 0, 0, bgAlpha);
        targetBgColor = new Color(0, 0, 0, bgAlpha);

        BitmapFont playerLabelFont = game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName());
        initViewports(players, map);
        map.createPlayerLabelTextures(players, shapeDrawer, playerLabelFont);

        playerStartPositions = new GridPoint2[players.length];
        for (int i = 0; i < players.length; i++) {
            playerStartPositions[i] = new GridPoint2(players[i].roundedPosition.x, players[i].roundedPosition.y);
        }

        this.scoreBars = new ScoreBar(screenStage.getViewport(), this.gameManager.playerManager.getTeamData().size, game.getScaleFactor());
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
                splitViewportManager.toggleZoom();
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
            if (!isSplitscreenMultiplayer()) {      // We're playing on multiple devices (Server-client)
                gameManager.gameConnectionManager.serverClientCommunicate(map);
            } else {                                // We're playing on the same device (Splitscreen)
                gameManager.playerManager.updatePlayerDirections();
            }
        }

        map.update(gameManager.playerManager, delta);

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

        batch.setProjectionMatrix(screenStage.getCamera().combined);
        batch.begin();
        shapeDrawer.setColor(currentBgColor);
        shapeDrawer.filledRectangle(0, 0, screenStage.getWidth(), screenStage.getHeight());
        batch.end();
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
            Gdx.app.postRunnable(() -> {
                dispose();
                game.setScreen(new VictoryScreen(game, gameManager.playerManager.getTeamData()));
            });
        }
    }

    private void showDisconnectionDialog() {
        screenStage.showOKDialog("Disconnected", false,
                game.getScaleFactor(), button -> endGame(), game.skin);
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

        screenStage.showDialog("Pause Menu",
                dialogButtons, true,
                game.getScaleFactor(), button -> {
                    if (button == FocusableStage.DialogButton.MainMenu) {
                        endGame();
                    } else if (button == FocusableStage.DialogButton.Restart) {
                        restartGame();
                    }

                    gamePaused = false;
                }, game.skin);
    }

    /**
     * Restarts the match (Supported only in splitscreen multiplayer mode for now)
     */
    private void restartGame() {
        gameManager.playerManager.stopPlayers(true);
        gameManager.directionBufferManager.clearBuffer();

        Player[] players = gameManager.playerManager.getPlayers();
        for (int i = 0; i < players.length; i++) {
            players[i].prevPosition = null;
            players[i].setPosition(playerStartPositions[i]);
        }

        gameManager.playerManager.resetScores();
        map.clearGrid();
        map.updateMap(players, gameManager.playerManager);
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
        screenStage.showOKDialog(playerName + " got disconnected",
                false, game.getScaleFactor(),
                null, game.skin);
    }

    public void disconnected() {
        Gdx.app.postRunnable(() -> {
            if (!gameManager.gameOver) {
                gameManager.playerManager.stopPlayers(false);
                gameManager.directionBufferManager.clearBuffer();
                showDisconnectionDialog();
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