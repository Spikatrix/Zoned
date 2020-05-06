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
import com.cg.zoned.MapSelector;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.esotericsoftware.kryonet.Server;

import java.util.Arrays;
import java.util.Map;

public class ServerLobbyScreen extends ScreenAdapter implements ServerLobbyConnectionManager.ServerPlayerListener, InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ServerLobbyConnectionManager connectionManager;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ShapeRenderer renderer;
    private com.cg.zoned.Map map;
    private Cell[][] mapGrid;
    private ExtendViewport mapViewport;
    private Color mapDarkOverlayColor;
    private Player[] players;

    private Table playerList;
    private MapSelector mapSelector;
    private Array<String> startLocations;

    public ServerLobbyScreen(final Zoned game, Server server, String name) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        startLocations = new Array<>();
        this.connectionManager = new ServerLobbyConnectionManager(server, this, name);
    }

    @Override
    public void show() {
        setUpServerLobbyStage();
        setUpMap();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        connectionManager.start(mapSelector.getMapManager().getMapList().first().getName());
        animationManager.fadeInStage(stage);
    }

    private void setUpServerLobbyStage() {
        Table serverLobbyTable = new Table();
        serverLobbyTable.setFillParent(true);
        //serverLobbyTable.setDebug(true);
        serverLobbyTable.center();

        Label onlinePlayersTitle = new Label("Connected Players", game.skin, "themed");
        serverLobbyTable.add(onlinePlayersTitle).pad(10 * game.getScaleFactor());

        serverLobbyTable.row();

        Table scrollTable = new Table();
        ScrollPane playerListScrollPane = new ScrollPane(scrollTable);
        playerListScrollPane.setOverscroll(false, true);

        playerList = new Table();
        //playerList.setDebug(true);
        scrollTable.add(playerList).expand();
        serverLobbyTable.add(playerListScrollPane).expand();
        serverLobbyTable.row();

        mapSelector = new MapSelector(stage, game.getScaleFactor(), game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        final Spinner mapSpinner = mapSelector.loadMapSelectorSpinner(150 * game.getScaleFactor(),
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight() * 3);
        final Table mapSelectorTable = new Table();
        mapSelectorTable.add(mapSpinner).pad(10f);
        final TextButton mapButton = new TextButton(mapSelector.getMapManager().getMapList().get(mapSpinner.getPositionIndex()).getName(), game.skin);
        mapButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final int prevIndex = mapSpinner.getPositionIndex();
                Array<String> buttonTexts = new Array<>();
                buttonTexts.add("Cancel");
                buttonTexts.add("Set Map");

                stage.showDialog(mapSelectorTable, buttonTexts,
                        false, game.getScaleFactor(),
                        new FocusableStage.DialogResultListener() {
                            @Override
                            public void dialogResult(String buttonText) {
                                if (buttonText.equals("Set Map")) {
                                    repopulateMapStartPosLocations();
                                    mapButton.setText(mapSelector.getMapManager().getMapList().get(mapSpinner.getPositionIndex()).getName());

                                    mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor = null;
                                    mapGrid = mapSelector.getMapManager().getPreparedMapGrid();
                                    map = new com.cg.zoned.Map(mapGrid, 0);
                                    mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor = players[0].color;
                                } else {
                                    // Cancel was pressed; restore the spinner pos
                                    mapSpinner.snapToStep(mapSpinner.getPositionIndex() - prevIndex);
                                }
                            }
                        }, game.skin);
            }
        });
        // mapSelector.loadExternalMaps(); Not yet

        serverLobbyTable.add(mapButton).width(200f * game.getScaleFactor());
        serverLobbyTable.row();

        TextButton startButton = new TextButton("Start Game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mapSelector.loadSelectedMap()) {
                    String errorMsg = connectionManager.validateServerData();
                    if (errorMsg != null) {
                        Array<String> dialogButtonTexts = new Array<>();
                        dialogButtonTexts.add("OK");

                        stage.showDialog(errorMsg, dialogButtonTexts,
                                false,
                                game.getScaleFactor(), null, game.skin);
                        return;
                    }

                    MapManager mapManager = mapSelector.getMapManager();
                    connectionManager.broadcastGameStart(mapManager);
                    startGame(mapManager);
                }
            }
        });
        serverLobbyTable.add(startButton).width(200 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        stage.addActor(serverLobbyTable);
        stage.addFocusableActor(mapSelector.getLeftButton());
        stage.addFocusableActor(mapSelector.getRightButton());
        stage.row();
        stage.addFocusableActor(startButton, 2);
        stage.row();
    }

    private void setUpMap() {
        mapSelector.loadSelectedMap();
        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.mapViewport = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        this.mapDarkOverlayColor = new Color(0, 0, 0, .85f);
        this.mapGrid = mapSelector.getMapManager().getPreparedMapGrid();
        this.map = new com.cg.zoned.Map(this.mapGrid, 0);
        this.players = new Player[0]; // This array size is increased in playerConnected
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
    public Table playerConnected(String ipAddress) {
        Table playerItem = new Table();
        playerItem.pad(10 * game.getScaleFactor());

        Label name;
        Label who;
        if (ipAddress == null) {
            name = new Label(connectionManager.getCurrentUserName(), game.skin);
            who = new Label("(Host, You)", game.skin, "themed");
        } else {
            name = new Label(ipAddress, game.skin);
            who = new Label("", game.skin, "themed");
        }
        name.setName("name-label");
        playerItem.add(name).space(20f * game.getScaleFactor());

        who.setName("who-label");
        playerItem.add(who).space(20f * game.getScaleFactor());

        Label ready = new Label("Not Ready", game.skin);
        ready.setColor(Color.RED);
        ready.setName("ready-label");
        playerItem.add(ready).space(20f * game.getScaleFactor());

        if (ipAddress == null) {
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

                    connectionManager.broadcastPlayerInfo(0);
                }
            });
            playerItem.add(colorSelector);

            final DropDownMenu startPosSelector = new DropDownMenu(game.skin);
            startPosSelector.setName("startPos-selector");
            if (mapSelector.loadSelectedMap()) {
                repopulateMapStartPosLocations();

                startPosSelector.setItems(startLocations);
                startPosSelector.setSelectedIndex(0);

                startPosSelector.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        updateMapColor(players[0], players[0].color, startPosSelector.getSelectedIndex());

                        connectionManager.broadcastPlayerInfo(0);
                    }
                });
                playerItem.add(startPosSelector); // idk why .expandX on stuff added to playerItem doesn't do anything
            }

            stage.addFocusableActor(colorSelector);
            stage.addFocusableActor(startPosSelector);
            stage.row();
            stage.setFocusedActor(colorSelector);
        } else {
            Label colorLabel = new Label(Constants.PLAYER_COLORS.keySet().iterator().next(), game.skin);
            colorLabel.setName("color-label");
            playerItem.add(colorLabel).space(20f * game.getScaleFactor()).expandX();

            Label startPosLabel = new Label(startLocations.first(), game.skin);
            startPosLabel.setName("startPos-label");
            playerItem.add(startPosLabel).space(20f * game.getScaleFactor()).expandX();
        }

        playerList.add(playerItem).expand();
        playerList.row();

        addNewPlayerIntoMap(ipAddress);

        return playerItem;
    }

    private void addNewPlayerIntoMap(String ipAddress) {
        int index = this.players.length;
        this.players = Arrays.copyOf(this.players, this.players.length + 1);

        this.players[index] = new Player(
                PlayerColorHelper.getColorFromString(Constants.PLAYER_COLORS.keySet().iterator().next()),
                ipAddress == null ? connectionManager.getCurrentUserName() : ipAddress);
        this.players[index].setStartPos(mapSelector.getMapManager().getPreparedStartPositions().first());
        this.mapGrid[(int) players[index].position.y][(int) players[index].position.x].cellColor = players[index].color;
    }

    private void repopulateMapStartPosLocations() {
        mapSelector.loadSelectedMap();
        MapManager mapManager = mapSelector.getMapManager();
        startLocations.clear();

        Cell[][] mapGrid = mapManager.getPreparedMapGrid();
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

        connectionManager.mapChanged(mapManager.getPreparedMap().getName(), startLocations);
    }

    private void startGame(MapManager mapManager) {
        Player[] players = connectionManager.inflatePlayerList(mapManager);
        animationManager.fadeOutStage(stage, this, new GameScreen(game, mapManager, players, connectionManager));
    }

    @Override
    public void updatePlayerDetails(int index, String color, String startPos) {
        updateMapColor(players[index], color, startPos);
    }

    private void updateMapColor(Player player, String color, String startPos) {
        Array<String> startPositionsNames = mapSelector.getMapManager().getPreparedStartPosNames();
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
                    mapSelector.getMapManager().getPreparedStartPositions().get(startPosIndex));
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
    public void playerDisconnected(Table playerItem, int index) {
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
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        focusAndRenderViewport(mapViewport, players[0], delta);

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
        playerList.clear();
        connectionManager.closeConnection();

        animationManager.fadeOutStage(stage, this, new HostJoinScreen(game));
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