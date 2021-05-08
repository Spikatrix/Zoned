package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapGridMissing;
import com.cg.zoned.maps.StartPositionsMissing;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;

public class ClientLobbyScreen extends ScreenObject implements ClientLobbyConnectionManager.ClientPlayerListener, InputProcessor {
    private ClientLobbyConnectionManager connectionManager;
    private String clientName;

    private com.cg.zoned.Map map;
    private MapManager mapManager;
    private Cell[][] mapGrid;
    private ExtendViewport mapViewport;
    private Color mapDarkOverlayColor;
    private Player[] players;

    private Table playerList;
    private TextButton readyButton;

    private Label mapLabel;
    private Array<String> startLocations;

    public ClientLobbyScreen(final Zoned game, Client client, String name) {
        super(game);
        game.discordRPCManager.updateRPC("In the Client lobby");

        animationManager = new AnimationManager(this.game, this);
        uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        startLocations = new Array<>();
        this.clientName = name;
        this.connectionManager = new ClientLobbyConnectionManager(client, this);
    }

    @Override
    public void show() {
        setUpClientLobbyStage();
        setUpMap();

        addPlayer(null, null, null, null, null);
        connectionManager.start(clientName);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                mapManager.loadExternalMaps(new MapManager.ExternalMapScanListener() {
                    @Override
                    public void onExternalMapScanComplete(Array<MapEntity> mapList, int externalMapStartIndex) {
                        connectionManager.sendClientNameToServer(clientName);
                    }
                });
                animationManager.setAnimationListener(null);
            }
        });
        animationManager.fadeInStage(screenStage);
    }

    private void setUpClientLobbyStage() {
        Table clientLobbyTable = new Table();
        clientLobbyTable.setFillParent(true);
        clientLobbyTable.center();
        //clientLobbyTable.setDebug(true);

        Label lobbyTitle = new Label("Lobby", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(lobbyTitle.getPrefHeight());
        clientLobbyTable.add(lobbyTitle).pad(headerPad);

        clientLobbyTable.row();

        Table scrollTable = new Table();
        ScrollPane playerListScrollPane = new ScrollPane(scrollTable, game.skin);
        playerListScrollPane.setOverscroll(false, true);

        playerList = new Table();
        scrollTable.add(playerList).expand();
        clientLobbyTable.add(playerListScrollPane).grow();
        clientLobbyTable.row();

        mapLabel = new Label("", game.skin);
        clientLobbyTable.add(mapLabel).pad(10f * game.getScaleFactor());
        clientLobbyTable.row();

        readyButton = new TextButton("Ready up", game.skin);
        readyButton.setTransform(true);
        readyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Table playerItem = (Table) playerList.getChild(0);
                Label readyLabel = playerItem.findActor("ready-label");
                DropDownMenu colorSelector = playerItem.findActor("color-selector");
                DropDownMenu startPosSelector = playerItem.findActor("startPos-selector");

                if (readyLabel.getText().toString().equals("Not ready")) {
                    readyLabel.setColor(Color.GREEN);
                    readyLabel.setText("Ready");
                    readyButton.setText("Unready");

                    colorSelector.setDisabled(true);
                    startPosSelector.setDisabled(true);
                    // TODO: Polish on this, perhaps?
                } else {
                    readyLabel.setColor(Color.RED);
                    readyLabel.setText("Not ready");
                    readyButton.setText("Ready up");

                    colorSelector.setDisabled(false);
                    startPosSelector.setDisabled(false);
                }

                connectionManager.broadcastClientInfo((Table) playerList.getChild(0));
            }
        });
        clientLobbyTable.add(readyButton).width(200 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        readyButton.setOrigin(200 * game.getScaleFactor() / 2f, readyButton.getHeight() / 2);

        screenStage.addFocusableActor(readyButton, 2);
        screenStage.row();

        screenStage.addActor(clientLobbyTable);
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
        this.mapManager = new MapManager();
        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
        this.mapViewport = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        this.mapDarkOverlayColor = new Color(0, 0, 0, .8f);
        this.players = new Player[0];
        // This array size is increased in playerConnected
        // I know I should use Arrays (libGDX's ArrayLists) instead, but Map works with regular 'ol arrays for now
    }

    @Override
    public void pause() {
        if (readyButton.getText().toString().equals("Unready")) {
            // Game was minimized in the mobile; so make the player unready
            readyButton.toggle();
        }
    }

    private void addPlayer(String name, String who, String ready, String color, String startPos) {
        Table playerItem = new Table();
        playerItem.pad(10 * game.getScaleFactor());

        Label nameLabel;
        Label whoLabel;
        if (name == null) {
            nameLabel = new Label(clientName, game.skin);
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
                    if (mapGrid == null) { // Did not receive map info from the server. Server likely died.
                        return;
                    }

                    players[0].color = PlayerColorHelper.getColorFromString(colorSelector.getSelected());
                    mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor = players[0].color;

                    connectionManager.broadcastClientInfo((Table) playerList.getChild(0));
                }
            });
            colorSelector.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (colorSelector.isDisabled()) {
                        nudgeReadyButton();
                    }
                }
            });
            playerItem.add(colorSelector);

            final DropDownMenu startPosSelector = new DropDownMenu(game.skin);
            startPosSelector.setName("startPos-selector");

            startPosSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updateMapColor(players[0], players[0].color, startPosSelector.getSelectedIndex());

                    connectionManager.broadcastClientInfo((Table) playerList.getChild(0));
                }
            });
            startPosSelector.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (startPosSelector.isDisabled()) {
                        nudgeReadyButton();
                    }
                }
            });
            playerItem.add(startPosSelector);

            screenStage.addFocusableActor(colorSelector);
            screenStage.addFocusableActor(startPosSelector);
            screenStage.row();
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
                screenStage.setFocusedActor(colorSelector);
            }
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
    }

    private void nudgeReadyButton() {
        if (!readyButton.hasActions()) {
            int nudgeAngle = 10;
            float nudgeTime = 0.05f;
            float scaleAmount = 1.2f;
            int nudgeRepeatCount = 2;
            Interpolation nudgeInterpolation = Interpolation.fastSlow;

            readyButton.addAction(Actions.sequence(
                    Actions.scaleTo(scaleAmount, scaleAmount, nudgeTime * 2, nudgeInterpolation),
                    Actions.repeat(nudgeRepeatCount, Actions.sequence(
                            Actions.rotateBy(nudgeAngle, nudgeTime, nudgeInterpolation),
                            Actions.rotateBy(-nudgeAngle * 2, nudgeTime * 2, nudgeInterpolation),
                            Actions.rotateBy(nudgeAngle, nudgeTime, nudgeInterpolation)
                    )),
                    Actions.scaleTo(1f, 1f, nudgeTime * 2, nudgeInterpolation)
            ));
        }
    }

    @Override
    public void updatePlayers(Array<String> playerNames, String[] nameStrings, String[] whoStrings, String[] readyStrings, String[] colorStrings, String[] startPosStrings) {
        for (int i = 0; i < nameStrings.length; i++) {
            if (nameStrings[i] == null || nameStrings[i].equals(clientName)) { // No need to update information for this client itself
                continue;
            }

            int index = playerNames.indexOf(nameStrings[i], false);
            if (whoStrings[i].equals("(DEL)")) {
                if (index != -1) { // Can be -1 too
                    playerNames.removeIndex(index);
                    removePlayer(index);
                }

                continue;
            }

            if (index != -1) {
                updatePlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i], startPosStrings[i], index);
            } else {
                addPlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i], startPosStrings[i]);
                playerNames.add(nameStrings[i]);
            }
        }
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
                name == null ? clientName : name);
        if (map != null) {
            updateMapColor(this.players[index], color, startPos);
        }
    }

    @Override
    public FileHandle getExternalMapDir() {
        return this.mapManager.getExternalMapDir();
    }

    @Override
    public void mapChanged(final String mapName, final int[] mapExtraParams, final int mapHash, boolean isNewMap) {
        if (readyButton.getText().toString().equals("Unready")) {
            readyButton.toggle();
        }

        if (isNewMap) {
            // If true, a new external map was just downloaded from the server
            this.mapManager.addNewExternalMap(mapName);
            this.mapLabel.setText("");
        }

        setMap(mapName, mapExtraParams, mapHash);
    }

    private void setMap(String mapName, int[] mapExtraParams, int mapHash) {
        startLocations.clear();

        MapManager mapManager = this.mapManager;
        if (!loadNewMap(mapManager, mapName, mapExtraParams, mapHash)) {
            return;
        }

        this.mapGrid = mapManager.getPreparedMapGrid();
        this.map = new com.cg.zoned.Map(this.mapGrid, 0, shapeDrawer);

        Array<StartPosition> startPositions = mapManager.getPreparedStartPositions();
        for (StartPosition startPosition : startPositions) {
            String startPosName = startPosition.getName();
            GridPoint2 startLocation = startPosition.getLocation();
            startPosName += (" (" + (mapGrid.length - startLocation.y - 1) + ", " + (startLocation.x) + ")");

            startLocations.add(startPosName);
        }

        for (Player player : players) {
            updateMapColor(player, player.color, 0);
        }

        resetStartPosLabels(startLocations);
        this.mapLabel.setText(mapManager.getPreparedMap().getName());
        readyButton.setDisabled(false);
    }

    private void resetStartPosLabels(Array<String> startLocations) {
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
    }

    private boolean loadNewMap(MapManager mapManager, String mapName, int[] mapExtraParams, int serverMapHash) {
        MapEntity map = mapManager.getMap(mapName);
        if (map == null) {
            // Server probably selected an external map which is unavailable in the client
            this.mapLabel.setText("Downloading map '" + mapName + "'");
            connectionManager.requestMap(mapName);
            readyButton.setDisabled(true);
            return false;
        }

        if (mapExtraParams != null) {
            map.getExtraParams().extraParams = mapExtraParams;
            map.applyExtraParams();
        }

        try {
            mapManager.loadMap(map);
        } catch (InvalidMapCharacter | StartPositionsMissing | InvalidMapDimensions | MapGridMissing |
                FileNotFoundException | IndexOutOfBoundsException e) {
            // Should never happen cause the server does this check before sending to all the clients
            e.printStackTrace();
            displayServerError("Error: " + e.getMessage());
            return false;
        }

        int clientMapHash = map.getMapData().hashCode();
        if (clientMapHash != serverMapHash) {
            // Map in the server and client have the same name, but different contents
            displayServerError("Server client map content mismatch!\n" +
                    "The map content for the map '" + mapName + "'\n is different for you and the server\n" +
                    "(Server: " + serverMapHash + ", Client: " + clientMapHash + ")");
            return false;
        }

        return true;
    }

    @Override
    public void displayServerError(String errorMsg) {
        screenStage.showOKDialog(errorMsg, false,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(FocusableStage.DialogButton button) {
                        disconnected();
                    }
                }, game.skin);
    }

    private void updatePlayer(String name, String who, String ready, String color, String startPos, int index) {
        Table playerItem = ((Table) this.playerList.getChild(index));
        ((Label) playerItem.findActor("name-label")).setText(name);

        if (who.endsWith("(Host, You)")) {
            who = who.substring(0, who.lastIndexOf(',')) + ')'; // Replace with "(Host)"
        }
        ((Label) playerItem.findActor("who-label")).setText(who);

        Label readyLabel = playerItem.findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) playerItem.findActor("color-label")).setText(color);
        ((Label) playerItem.findActor("startPos-label")).setText(startPos);

        updatePlayerDetails(index, color, startPos);
    }

    private Player[] inflatePlayerList() {
        int size = playerList.getChildren().size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            Table playerItem = ((Table) this.playerList.getChild(i));

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
            int startPosIndex = getStartPosIndex(mapManager.getPreparedStartPositions(), position);
            players[i].setStartPos(mapManager.getPreparedStartPositions().get(startPosIndex).getLocation());
        }

        return players;
    }

    @Override
    public void startGame() {
        final Player[] players = inflatePlayerList();
        clearMapGrid();
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(screenStage, ClientLobbyScreen.this, new GameScreen(game, mapManager, players, connectionManager));
            }
        });
    }

    private int getStartPosIndex(Array<StartPosition> startPositions, String startPos) {
        for (int i = 0; i < startPositions.size; i++) {
            if (startPos.equals(startPositions.get(i).getName())) {
                return i;
            }
        }

        return -1;
    }

    private void clearMapGrid() {
        for (Player player : players) {
            mapGrid[(int) player.position.y][(int) player.position.x].cellColor = null;
        }
    }

    private void updatePlayerDetails(int index, String color, String startPos) {
        try {
            updateMapColor(players[index], color, startPos);
        } catch (NullPointerException e) {
            // Map has not been loaded yet
            // Ignore it now, it'll probably get loaded and updated elsewhere
        }
    }

    private void updateMapColor(Player player, String color, String startPos) {
        Array<StartPosition> startPositions = mapManager.getPreparedStartPositions();
        startPos = startPos.substring(0, startPos.lastIndexOf('(')).trim();
        int startPosIndex = getStartPosIndex(startPositions, startPos);

        updateMapColor(player, PlayerColorHelper.getColorFromString(color), startPosIndex);
    }

    private void updateMapColor(Player player, Color color, int startPosIndex) {
        boolean outOfBounds = false;
        try {
            mapGrid[(int) player.position.y][(int) player.position.x].cellColor = null;
        } catch (ArrayIndexOutOfBoundsException e) {
            outOfBounds = true;
        }

        GridPoint2 prevLoc = new GridPoint2((int) player.position.x, (int) player.position.y);
        player.color = color;

        if (startPosIndex != -1) {
            player.setStartPos(
                    mapManager.getPreparedStartPositions().get(startPosIndex).getLocation());
            mapGrid[(int) player.position.y][(int) player.position.x].cellColor = player.color;
        }

        if (!outOfBounds) { // Huh? Excuse me, lint? Always true? Nope.
            for (Player p : players) {
                if ((int) p.position.x == prevLoc.x && (int) p.position.y == prevLoc.y && p.color != Color.BLACK) {
                    mapGrid[prevLoc.y][prevLoc.x].cellColor = p.color;
                    break;
                }
            }
        }
    }

    private void removePlayer(int index) {
        playerList.removeActor(playerList.getChild(index));

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
        playerList.clear();
        connectionManager.closeConnection();

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(screenStage, ClientLobbyScreen.this, new HostJoinScreen(game));
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (map != null) {
            renderMap(delta);
        }

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        drawDarkOverlay();

        screenStage.draw();
        screenStage.act(delta);

        displayFPS();
    }

    private void renderMap(float delta) {
        Viewport viewport = mapViewport;
        Player player = players[0];

        focusCameraOnPlayer(viewport, player, delta);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        map.render(players, shapeDrawer, (OrthographicCamera) viewport.getCamera(), delta);
        batch.end();
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
        super.resize(width, height);

        mapViewport.update(width, height);
        this.mapViewport.setScreenX(0);
    }

    private void drawDarkOverlay() {
        float height = screenStage.getViewport().getWorldHeight();
        float width = screenStage.getViewport().getWorldWidth();
        shapeDrawer.setColor(mapDarkOverlayColor);
        batch.begin();
        shapeDrawer.filledRectangle(0, 0, width, height);
        batch.end();
    }

    @Override
    public void dispose() {
        super.dispose();
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
        if (screenStage.dialogIsActive()) {
            return false;
        }

        disconnected();
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
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
