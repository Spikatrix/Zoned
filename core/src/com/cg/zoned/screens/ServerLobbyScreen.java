package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.MapSelector;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.esotericsoftware.kryonet.Server;

import java.util.Arrays;
import java.util.Map;

public class ServerLobbyScreen extends ScreenObject implements ServerLobbyConnectionManager.ServerPlayerListener, InputProcessor {
    private ServerLobbyConnectionManager connectionManager;
    private String serverName;

    private com.cg.zoned.Map map;
    private Cell[][] mapGrid;
    private SplitViewportManager splitViewportManager;
    private Overlay mapOverlay;
    private Player[] players;

    private Table playerList;
    private MapSelector mapSelector;
    private FocusableStage mapSelectorStage;
    private Spinner mapSpinner;
    private boolean mapSelectorActive;
    private boolean extendedMapSelectorActive;
    private Array<String> startLocations;

    public ServerLobbyScreen(final Zoned game, Server server, String name) {
        super(game);
        game.discordRPCManager.updateRPC("In the Server Lobby");

        animationManager = new AnimationManager(this.game, this);
        uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        startLocations = new Array<>();
        this.serverName = name;
        this.connectionManager = new ServerLobbyConnectionManager(server, this);
    }

    @Override
    public void show() {
        setUpMapSelectorStage();
        setUpServerLobbyStage();
        setUpMap();

        playerConnected(null); // Add the host player (oneself) to the player list
        connectionManager.start();

        screenStage.getRoot().getColor().a = 0;

        // The screen is faded in once the maps and the extended selector is loaded
        mapSelector.loadExternalMaps((mapList, externalMapStartIndex) -> {
            // Set up the extended map selector once external maps have been loaded
            mapSelector.setUpExtendedSelector(mapSelectorStage, new MapSelector.ExtendedMapSelectionListener() {
                @Override
                public void onExtendedMapSelectorOpened() {
                    openExtendedMapSelector();
                }

                @Override
                public void onMapSelect(int mapIndex) {
                    if (mapIndex != -1) {
                        mapSpinner.snapToStep(mapIndex - mapSpinner.getPositionIndex());
                    }
                    animationManager.endExtendedMapSelectorAnimation(screenStage, mapSelectorStage);
                    extendedMapSelectorActive = false;
                }
            });

            animationManager.fadeInStage(screenStage);
        });
    }

    private void openExtendedMapSelector() {
        extendedMapSelectorActive = true;
        animationManager.startExtendedMapSelectorAnimation(screenStage, mapSelectorStage, 0);
        mapSpinner.snapToStep(0);
    }

    private void setUpMapSelectorStage() {
        mapSelectorStage = new FocusableStage(screenViewport);
        mapSelectorStage.getRoot().getColor().a = 0f;
    }

    private void setUpServerLobbyStage() {
        Table serverLobbyTable = new Table();
        serverLobbyTable.setFillParent(true);
        serverLobbyTable.center();
        //serverLobbyTable.setDebug(true);

        Label lobbyTitle = new Label("Lobby", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(lobbyTitle.getPrefHeight());
        serverLobbyTable.add(lobbyTitle).pad(headerPad);
        serverLobbyTable.row();

        Table scrollTable = new Table();
        ScrollPane playerListScrollPane = new ScrollPane(scrollTable, game.skin);
        playerListScrollPane.setOverscroll(false, true);

        playerList = new Table();
        //playerList.setDebug(true);
        scrollTable.add(playerList).expand();
        serverLobbyTable.add(playerListScrollPane).grow();
        serverLobbyTable.row();

        mapSelector = new MapSelector(screenStage, game.getScaleFactor(), game.assets, game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        float spinnerHeight = game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight() * 3;
        mapSpinner = mapSelector.loadMapSelectorSpinner(spinnerHeight * 1.5f, spinnerHeight);
        final Table mapSelectorTable = new Table();
        mapSelectorTable.add(mapSpinner).pad(10f);

        final TextButton mapButton = new TextButton(mapSelector.getMapManager().getMapList().get(mapSpinner.getPositionIndex()).getName(), game.skin);

        final Array<Actor> focusableDialogButtons = new Array<>();
        focusableDialogButtons.add(mapSpinner.getLeftButton());
        focusableDialogButtons.add(mapSpinner.getRightButton());

        mapButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final int prevIndex = mapSpinner.getPositionIndex();
                mapSelectorActive = true;

                screenStage.showDialog(mapSelectorTable, focusableDialogButtons,
                        new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.SetMap },
                        false, game.getScaleFactor(),
                        button -> {
                            if (button == FocusableStage.DialogButton.SetMap && mapSelector.loadSelectedMap()) {
                                PreparedMapData preparedMapData = mapSelector.getMapManager().getPreparedMapData();
                                mapButton.setText(preparedMapData.map.getName());
                                mapGrid = preparedMapData.mapGrid;
                                map = new com.cg.zoned.Map(mapGrid, 0, shapeDrawer);
                                setCameraPosition();

                                repopulateMapStartPosLocations();
                                updateMapColor(players[0], players[0].color, 0);
                            } else {
                                // Cancelled; restore the spinner pos
                                mapSpinner.snapToStep(prevIndex - mapSpinner.getPositionIndex());
                            }

                            mapSelectorActive = false;
                        }, game.skin);
            }
        });

        serverLobbyTable.add(mapButton).width(200f * game.getScaleFactor());
        serverLobbyTable.row();

        TextButton startButton = new TextButton("Start Game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mapSelector.loadSelectedMap()) {
                    String errorMsg = connectionManager.validateServerData(playerList.getChildren());
                    if (errorMsg != null) {
                        screenStage.showOKDialog(errorMsg, false,
                                game.getScaleFactor(), null, game.skin);
                        return;
                    }

                    MapManager mapManager = mapSelector.getMapManager();
                    connectionManager.broadcastGameStart();
                    startGame(mapManager);
                }
            }
        });
        serverLobbyTable.add(startButton).width(250 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        screenStage.addActor(serverLobbyTable);
        screenStage.addFocusableActor(mapButton, 2);
        screenStage.row();
        screenStage.addFocusableActor(startButton, 2);
        screenStage.row();
        screenStage.setScrollFocus(playerListScrollPane);

        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    private void setUpMap() {
        mapSelector.loadSelectedMap();
        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
        this.mapOverlay = new Overlay(new Color(0, 0, 0, .8f));
        this.mapGrid = mapSelector.getMapManager().getPreparedMapData().mapGrid;
        this.map = new com.cg.zoned.Map(this.mapGrid, 0, shapeDrawer);
        this.splitViewportManager = new SplitViewportManager(1, Constants.WORLD_SIZE, null);
        this.splitViewportManager.setUpDragOffset(screenStage);
        setCameraPosition();

        // This array size is increased in playerConnected
        // I know I should use Arrays (libGDX's ArrayLists) instead but Map works with regular 'ol arrays for now
        this.players = new Player[0];
    }

    private void setCameraPosition() {
        float centerX = (map.cols * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        float centerY = (map.rows * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        splitViewportManager.setViewportCameraPosition(new Vector2(centerX, centerY));
    }

    @Override
    public void playerConnected(String ipAddress) {
        Table playerItem = new Table();
        playerItem.pad(10 * game.getScaleFactor());

        Label name;
        Label who;
        if (ipAddress == null) {
            name = new Label(serverName, game.skin);
            who = new Label("(Host, You)", game.skin, "themed");
        } else {
            name = new Label(ipAddress, game.skin);
            who = new Label("", game.skin, "themed");
        }
        name.setName("name-label");
        playerItem.add(name).space(20f * game.getScaleFactor());

        who.setName("who-label");
        playerItem.add(who).space(20f * game.getScaleFactor());

        Label ready = new Label("Not ready", game.skin);
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
                    mapGrid[players[0].roundedPosition.y][players[0].roundedPosition.x].cellColor = players[0].color;

                    connectionManager.broadcastPlayerInfo(playerList.getChildren(), 0);
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

                        connectionManager.broadcastPlayerInfo(playerList.getChildren(), 0);
                    }
                });
                playerItem.add(startPosSelector); // idk why .expandX on stuff added to playerItem doesn't do anything
            }

            screenStage.addFocusableActor(colorSelector);
            screenStage.addFocusableActor(startPosSelector);
            screenStage.row();
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
                screenStage.setFocusedActor(colorSelector);
            }
        } else {
            Label colorLabel = new Label(Constants.PLAYER_COLORS.keySet().iterator().next(), game.skin);
            colorLabel.setName("color-label");
            playerItem.add(colorLabel).space(20f * game.getScaleFactor());

            Label startPosLabel = new Label(startLocations.first(), game.skin);
            startPosLabel.setName("startPos-label");
            playerItem.add(startPosLabel).space(20f * game.getScaleFactor());
        }

        playerList.add(playerItem).expand();
        playerList.row();

        addNewPlayerIntoMap(ipAddress);
    }

    private void addNewPlayerIntoMap(String ipAddress) {
        int index = this.players.length;
        this.players = Arrays.copyOf(this.players, this.players.length + 1);

        this.players[index] = new Player(
                PlayerColorHelper.getColorFromString(Constants.PLAYER_COLORS.keySet().iterator().next()),
                ipAddress == null ? serverName : ipAddress);
        this.players[index].setPosition(mapSelector.getMapManager().getPreparedMapData().startPositions.first().getLocation());
        this.mapGrid[players[index].roundedPosition.y][players[index].roundedPosition.x].cellColor = players[index].color;
    }

    public void updatePlayerDetails(int playerIndex, String clientName) {
        for (Actor playerItemActor : playerList.getChildren()) {
            Table playerItem = (Table) playerItemActor;

            String name = ((Label) playerItem.findActor("name-label")).getText().toString();

            if (clientName.equals(name)) {
                connectionManager.rejectConnection(playerIndex, "Another player is using the same name\nPlease use a different name");
                return;
            }
        }

        Table playerItem = (Table) playerList.getChild(playerIndex);
        Label nameLabel = playerItem.findActor("name-label");
        nameLabel.setText(clientName);

        connectionManager.acceptPlayer(playerIndex);
        connectionManager.sendMapDetails(playerIndex, mapSelector.getMapManager());
        connectionManager.broadcastPlayerInfo(playerList.getChildren(), -1); // Send info about the new player to other clients and vice-versa
    }

    @Override
    public void updatePlayerDetails(int playerIndex, String name, String who, String ready, String color, String startPos) {
        Table playerItem = (Table) playerList.getChild(playerIndex);

        // Only needs to set the ready and color labels as others will not be changed
        Label readyLabel = playerItem.findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) playerItem.findActor("color-label")).setText(color);
        ((Label) playerItem.findActor("startPos-label")).setText(startPos);

        connectionManager.broadcastPlayerInfo(playerList.getChildren(), playerIndex);

        updateMapColor(players[playerIndex], color, startPos);
    }

    private void repopulateMapStartPosLocations() {
        startLocations.clear();
        PreparedMapData mapData = mapSelector.getMapManager().getPreparedMapData();

        Cell[][] mapGrid = mapData.mapGrid;
        for (StartPosition startPosition : mapData.startPositions) {
            String startPosName = startPosition.getName();
            GridPoint2 startLocation = startPosition.getLocation();
            startPosName += (" (" + (mapGrid.length - startLocation.y - 1) + ", " + (startLocation.x) + ")");

            startLocations.add(startPosName);
        }

        mapChanged(startLocations);
    }

    private void mapChanged(Array<String> startLocations) {
        for (Actor playerItemActor : playerList.getChildren()) {
            Table playerItem = (Table) playerItemActor;

            Label startPosLabel = playerItem.findActor("startPos-label");
            if (startPosLabel != null) {
                startPosLabel.setText(startLocations.first());
            } else {
                DropDownMenu startPosSelector = playerItem.findActor("startPos-selector");
                startPosSelector.setItems(startLocations);
                startPosSelector.setSelectedIndex(0);
            }
        }

        connectionManager.sendMapDetails(-1, mapSelector.getMapManager());
    }

    @Override
    public FileHandle getExternalMapDir() {
        return this.mapSelector.getMapManager().getExternalMapDir();
    }

    @Override
    public MapEntity fetchMap(String mapName) {
        return this.mapSelector.getMapManager().getMap(mapName);
    }

    private void startGame(MapManager mapManager) {
        Player[] players = inflatePlayerList(mapManager);
        map.clearGrid();
        animationManager.fadeOutStage(screenStage, this, new GameScreen(game, mapManager.getPreparedMapData(), players, connectionManager));
    }

    private Player[] inflatePlayerList(MapManager mapManager) {
        SnapshotArray<Actor> playerItemArray = playerList.getChildren();
        int size = playerItemArray.size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            Table playerItem = (Table) playerItemArray.get(i);

            String name = ((Label) playerItem.findActor("name-label")).getText().toString();
            String position;
            String color;
            if (i == 0) {
                color = ((DropDownMenu) playerItem.findActor("color-selector")).getSelected();
                position = ((DropDownMenu) playerItem.findActor("startPos-selector")).getSelected();
            } else {
                color = ((Label) playerItem.findActor("color-label")).getText().toString();
                position = ((Label) playerItem.findActor("startPos-label")).getText().toString();
            }
            players[i] = new Player(PlayerColorHelper.getColorFromString(color), name);

            position = position.substring(0, position.lastIndexOf('(')).trim();
            int startPosIndex = getStartPosIndex(mapManager.getPreparedMapData().startPositions, position);
            players[i].setPosition(mapManager.getPreparedMapData().startPositions.get(startPosIndex).getLocation());
        }

        return players;
    }

    private int getStartPosIndex(Array<StartPosition> startPositions, String startPos) {
        for (int i = 0; i < startPositions.size; i++) {
            if (startPos.equals(startPositions.get(i).getName())) {
                return i;
            }
        }

        return -1;
    }

    private void updateMapColor(Player player, String color, String startPos) {
        Array<StartPosition> startPositionsNames = mapSelector.getMapManager().getPreparedMapData().startPositions;
        startPos = startPos.substring(0, startPos.lastIndexOf('(')).trim();
        int startPosIndex = getStartPosIndex(startPositionsNames, startPos);

        updateMapColor(player, PlayerColorHelper.getColorFromString(color), startPosIndex);
    }

    private void updateMapColor(Player player, Color color, int startPosIndex) {
        boolean outOfBounds = false;
        try {
            mapGrid[player.roundedPosition.y][player.roundedPosition.x].cellColor = null;
        } catch (ArrayIndexOutOfBoundsException e) {
            outOfBounds = true; // Probably the map changed
        }

        GridPoint2 prevLoc = new GridPoint2(player.roundedPosition.x, player.roundedPosition.y);
        player.color = color;

        if (startPosIndex != -1) {
            player.setPosition(
                    mapSelector.getMapManager().getPreparedMapData().startPositions.get(startPosIndex).getLocation());
            mapGrid[player.roundedPosition.y][player.roundedPosition.x].cellColor = player.color;
        }

        if (!outOfBounds) { // Huh? Excuse me, lint? Always true? Nope.
            for (Player p : players) {
                if (p.roundedPosition.x == prevLoc.x && p.roundedPosition.y == prevLoc.y && p.color != Color.BLACK) {
                    mapGrid[prevLoc.y][prevLoc.x].cellColor = p.color;
                    break;
                }
            }
        }
    }

    @Override
    public void playerDisconnected(int index) {
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

        Table playerItem = (Table) playerList.getChild(index);
        ((Label) playerItem.findActor("who-label")).setText("(DEL)"); // Notify all clients to delete this player
        connectionManager.broadcastPlayerInfo(playerList.getChildren(), index);

        playerList.removeActor(playerItem);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Renders the map in the background of the lobby
        splitViewportManager.render(shapeDrawer, batch, map, players, delta);

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        batch.begin();
        mapOverlay.render(shapeDrawer, screenStage, delta);
        batch.end();

        screenStage.act(delta);
        if (screenStage.getRoot().getColor().a > 0) {
            screenStage.draw();
        }

        mapSelectorStage.act(delta);
        if (mapSelectorStage.getRoot().getColor().a > 0) {
            mapSelectorStage.draw();
        }

        displayFPS();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        splitViewportManager.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        mapSelectorStage.dispose();
        if (map != null) {
            map.dispose();
            map = null;
        }
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (extendedMapSelectorActive) {
            animationManager.endExtendedMapSelectorAnimation(screenStage, mapSelectorStage);
            extendedMapSelectorActive = false;
            return true;
        } else if (screenStage.dialogIsActive()) {
            return false;
        }

        playerList.clear();
        connectionManager.closeConnection();

        animationManager.fadeOutStage(screenStage, this, new HostJoinScreen(game));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
        } else if (mapSelectorActive && !extendedMapSelectorActive && !mapSelector.extraParamsDialogActive()) {
            if (keycode == Input.Keys.S) {
                return mapSelector.extraParamShortcutPressed();
            } else if (keycode == Input.Keys.E) {
                openExtendedMapSelector();
                return true;
            }
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
            return onBackPressed();
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
