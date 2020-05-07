package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;

import java.util.Arrays;
import java.util.Map;

public class ClientLobbyScreen extends ScreenAdapter implements ClientLobbyConnectionManager.ClientPlayerListener, InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ClientLobbyConnectionManager connectionManager;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ShapeRenderer renderer;
    private com.cg.zoned.Map map;
    private MapManager mapManager;
    private Cell[][] mapGrid;
    private ExtendViewport mapViewport;
    private Color mapDarkOverlayColor;
    private Player[] players;

    private Table playerList;
    private Label mapLabel;
    private Array<String> startLocations;

    public ClientLobbyScreen(final Zoned game, Client client, String name) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        startLocations = new Array<>();
        this.connectionManager = new ClientLobbyConnectionManager(client, this, name);
    }

    @Override
    public void show() {
        setUpClientLobbyStage();
        setUpMap();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        connectionManager.start();
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                connectionManager.sendClientNameToServer();
                animationManager.setAnimationListener(null);
            }
        });
        animationManager.fadeInStage(stage);
    }

    private void setUpClientLobbyStage() {
        Table clientLobbyTable = new Table();
        clientLobbyTable.setFillParent(true);
        //clientLobbyTable.setDebug(true);
        clientLobbyTable.center();

        Label onlinePlayersTitle = new Label("Connected Players", game.skin, "themed");
        clientLobbyTable.add(onlinePlayersTitle).pad(10 * game.getScaleFactor());

        clientLobbyTable.row();

        Table scrollTable = new Table();
        ScrollPane playerListScrollPane = new ScrollPane(scrollTable);
        playerListScrollPane.setOverscroll(false, true);

        playerList = new Table();
        scrollTable.add(playerList).expand();
        clientLobbyTable.add(playerListScrollPane).expand();
        clientLobbyTable.row();

        mapLabel = new Label("", game.skin);
        clientLobbyTable.add(mapLabel).pad(10f * game.getScaleFactor());
        clientLobbyTable.row();

        final TextButton readyButton = new TextButton("Ready up", game.skin);
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                connectionManager.toggleReadyState(readyButton);
            }
        });
        clientLobbyTable.add(readyButton).width(200 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        stage.addFocusableActor(readyButton);
        stage.row();

        stage.addActor(clientLobbyTable);
    }

    private void setUpMap() {
        this.mapManager = new MapManager();
        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.mapViewport = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        this.mapDarkOverlayColor = new Color(0, 0, 0, .8f);
        this.players = new Player[0];
        // This array size is increased in playerConnected
        // I know I should use Arrays (libGDX's ArrayLists) instead, but Map works with regular 'ol arrays for now
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
    public Table addPlayer(String name, String who, String ready, String color, String startPos) {
        Table playerItem = new Table();
        playerItem.pad(10 * game.getScaleFactor());

        Label nameLabel;
        Label whoLabel;
        if (name == null) {
            nameLabel = new Label(connectionManager.getCurrentUserName(), game.skin);
            whoLabel = new Label("(You)", game.skin, "themed");
        } else {
            nameLabel = new Label(name, game.skin);
            if (who.endsWith("(Host, You)")) {
                who = who.substring(0, who.lastIndexOf(',')) + ')'; // Replace with "(Host)"
            }
            whoLabel = new Label(who, game.skin, "themed");
        }
        nameLabel.setName("name-label");
        playerItem.add(nameLabel);

        whoLabel.setName("who-label");
        playerItem.add(whoLabel).space(20f * game.getScaleFactor());

        Label readyLabel = new Label(ready != null ? ready : "Not ready", game.skin);
        if (readyLabel.getText().toString().equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }

        readyLabel.setName("ready-label");
        playerItem.add(readyLabel).space(20f * game.getScaleFactor());

        if (color == null) {
            final DropDownMenu colorSelector = new DropDownMenu(game.skin);
            colorSelector.setName("color-selector");
            for (Map.Entry<String, Color> playerColorEntry : Constants.PLAYER_COLORS.entrySet()) {
                colorSelector.append(playerColorEntry.getKey());
            }
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    players[0].color = PlayerColorHelper.getColorFromString(colorSelector.getSelected());
                    mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor = players[0].color;

                    connectionManager.broadcastClientInfo();
                }
            });
            playerItem.add(colorSelector);

            final DropDownMenu startPosSelector = new DropDownMenu(game.skin);
            startPosSelector.setName("startPos-selector");

            startPosSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updateMapColor(players[0], players[0].color, startPosSelector.getSelectedIndex());

                    connectionManager.broadcastClientInfo();
                }
            });
            playerItem.add(startPosSelector);

            stage.addFocusableActor(colorSelector);
            stage.addFocusableActor(startPosSelector);
            stage.row();
            stage.setFocusedActor(colorSelector);
        } else {
            Label colorLabel = new Label(color, game.skin);
            colorLabel.setName("color-label");
            playerItem.add(colorLabel).space(20f * game.getScaleFactor());

            Label startPosLabel = new Label(startPos, game.skin);
            startPosLabel.setName("startPos-label");
            playerItem.add(startPosLabel).space(20f * game.getScaleFactor());
        }

        playerList.add(playerItem);
        playerList.row();

        addNewPlayerIntoMap(name, color, startPos);

        return playerItem;
    }

    private void addNewPlayerIntoMap(String name, String color, String startPos) {
        int index = this.players.length;
        this.players = Arrays.copyOf(this.players, this.players.length + 1);

        Color playerColor;
        if (color == null) {
            playerColor = PlayerColorHelper.getColorFromString(Constants.PLAYER_COLORS.keySet().iterator().next());
        } else {
            playerColor = PlayerColorHelper.getColorFromString(color);
        }
        this.players[index] = new Player(
                playerColor,
                name == null ? connectionManager.getCurrentUserName() : name);
        if (map != null) {
            updateMapColor(this.players[index], color, startPos);
        }
    }

    @Override
    public void mapChanged(MapManager mapManager) {
        startLocations.clear();
        this.mapManager = mapManager;

        if (map != null) {
            for (Player player : players) {
                mapGrid[(int) player.position.y][(int) player.position.x].cellColor = null;
            }
        }

        this.mapGrid = mapManager.getPreparedMapGrid();
        this.map = new com.cg.zoned.Map(this.mapGrid, 0);
        Array<GridPoint2> startPositions = mapManager.getPreparedStartPositions();
        Array<String> startPosNames = mapManager.getPreparedStartPosNames();
        for (int j = 0; j < startPositions.size; j++) {
            String startPosName;
            try {
                startPosName = startPosNames.get(j);
            } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                startPosName = Character.toString((char) (j + MapManager.VALID_START_POSITIONS.charAt(0)));
            }
            startPosName += (" (" + (mapGrid.length - startPositions.get(j).y - 1) + ", " + (startPositions.get(j).x) + ")");

            startLocations.add(startPosName);
        }

        for (Player player : players) {
            player.setStartPos(mapManager.getPreparedStartPositions().first());
            updateMapColor(player, player.color, 0);
        }

        connectionManager.mapChanged(startLocations);
        this.mapLabel.setText(mapManager.getPreparedMap().getName());
    }

    @Override
    public void displayError(String errorMsg) {
        final Array<String> dialogButtonTexts = new Array<>();
        dialogButtonTexts.add("OK");

        stage.showDialog(errorMsg, dialogButtonTexts,
                false,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        connectionManager.closeConnection();
                    }
                }, game.skin);
    }

    @Override
    public void startGame(final MapManager mapManager) {
        final Player[] players = connectionManager.inflatePlayerList(mapManager);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(stage, ClientLobbyScreen.this, new GameScreen(game, mapManager, players, connectionManager));
            }
        });
    }

    @Override
    public void updatePlayerDetails(int index, String color, String startPos) {
        updateMapColor(players[index], color, startPos);
    }

    private void updateMapColor(Player player, String color, String startPos) {
        Array<String> startPositionsNames = mapManager.getPreparedStartPosNames();
        startPos = startPos.substring(0, startPos.lastIndexOf('(')).trim();
        int startPosIndex = startPositionsNames.indexOf(startPos, false);

        updateMapColor(player, PlayerColorHelper.getColorFromString(color), startPosIndex);
    }

    private void updateMapColor(Player player, Color color, int startPosIndex) {
        mapGrid[(int) player.position.y][(int) player.position.x].cellColor = null;
        GridPoint2 prevLoc = new GridPoint2((int) player.position.x, (int) player.position.y);
        player.color = color;

        if (startPosIndex != -1) {
            player.setStartPos(
                    mapManager.getPreparedStartPositions().get(startPosIndex));
            mapGrid[(int) player.position.y][(int) player.position.x].cellColor = player.color;
        }

        for (Player p : players) {
            if ((int) p.position.x == prevLoc.x && (int) p.position.y == prevLoc.y && p.color != Color.BLACK) {
                mapGrid[prevLoc.y][prevLoc.x].cellColor = p.color;
                break;
            }
        }
    }

    @Override
    public void removePlayer(Table playerItem, int index) {
        playerList.removeActor(playerItem);

        updateMapColor(players[index], Color.BLACK, -1);

        Player[] ps = new Player[players.length - 1];
        int psIndex = 0;
        for (int i = 0; i < players.length; i++) {
            if (i == index) {
                continue;
            }
            ps[psIndex] = players[i];
            psIndex++;
        }

        this.players = ps;
    }

    @Override
    public void disconnected() {
        connectionManager.closeConnection();
        playerList.clear();

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(stage, ClientLobbyScreen.this, new HostJoinScreen(game));
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (map != null) {
            focusAndRenderViewport(mapViewport, players[0], delta);
        }

        this.viewport.apply(true);
        renderer.setProjectionMatrix(this.viewport.getCamera().combined);

        drawDarkOverlay();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    private void focusAndRenderViewport(Viewport viewport, Player player, float delta) {
        focusCameraOnPlayer(viewport, player, delta);
        viewport.apply();

        renderer.setProjectionMatrix(viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        map.render(players, renderer, (OrthographicCamera) viewport.getCamera(), delta);
        renderer.end();
    }

    private void focusCameraOnPlayer(Viewport viewport, Player player, float delta) {
        OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();

        float lerp = 2.5f;
        Vector3 position = camera.position;

        float posX = (player.position.x * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;

        position.x += (posX - position.x) * lerp * delta;
        position.y += (posY - position.y) * lerp * delta;
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);

        mapViewport.update(width, height);
        updateCamera(mapViewport.getCamera(), width, height);
        this.mapViewport.setScreenX(0);
    }

    private void updateCamera(Camera camera, int width, int height) {
        camera.viewportHeight = Constants.WORLD_SIZE;
        camera.viewportWidth = Constants.WORLD_SIZE * height / width;
        camera.update();
    }

    private void drawDarkOverlay() {
        float height = stage.getViewport().getWorldHeight();
        float width = stage.getViewport().getWorldWidth();
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(mapDarkOverlayColor);
        renderer.rect(0, 0, width, height);
        renderer.end();
    }

    @Override
    public void dispose() {
        stage.dispose();
        renderer.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        disconnected();
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
