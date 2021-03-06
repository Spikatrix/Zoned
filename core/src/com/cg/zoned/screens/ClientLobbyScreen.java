package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Assets;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.PlayerItemAttributes;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapGridMissing;
import com.cg.zoned.maps.StartPositionsMissing;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryonet.Client;

import java.io.FileNotFoundException;

public class ClientLobbyScreen extends LobbyScreenHelper implements ClientLobbyConnectionManager.ClientPlayerListener, InputProcessor {
    private ClientLobbyConnectionManager connectionManager;

    private MapManager mapManager;

    private HoverImageButton readyButton;
    private Label mapLabel;

    public ClientLobbyScreen(final Zoned game, Client client, String name) {
        super(game, name);
        game.discordRPCManager.updateRPC("In the Client lobby");

        animationManager = new AnimationManager(game, this);
        connectionManager = new ClientLobbyConnectionManager(client, this);
    }

    @Override
    public void show() {
        setUpClientLobbyStage();
        initMap();

        addPlayer(null, false, 0, 0);
        connectionManager.start(name);
        animationManager.setAnimationListener(stage -> {
            mapManager.loadExternalMaps((mapList, externalMapStartIndex) -> connectionManager.sendClientNameToServer(this.name));
            animationManager.setAnimationListener(null);
        });
        animationManager.fadeInStage(screenStage);
    }

    private void setUpClientLobbyStage() {
        Table clientLobbyTable = super.setUpLobbyUI();

        mapLabel = new Label("", game.skin);
        float mapLabelPadding = uiButtonManager.getHeaderPad(mapLabel.getPrefHeight());
        mapLabel.setAlignment(Align.center);
        clientLobbyTable.add(mapLabel).pad(mapLabelPadding).expandX();
        clientLobbyTable.row();

        screenStage.addActor(clientLobbyTable); // Needs to be placed here before uiButtonManager stuff

        Texture readyTexture = game.assets.getTexture(Assets.TextureObject.READY_UP_TEXTURE);
        Texture unreadyTexture = game.assets.getTexture(Assets.TextureObject.UNREADY_UP_TEXTURE);
        readyButton = uiButtonManager.addReadyButtonToStage(readyTexture, unreadyTexture);
        readyButton.setTransform(true);
        readyButton.setOrigin(uiButtonManager.buttonSize * game.getScaleFactor() / 2,
                uiButtonManager.buttonSize * game.getScaleFactor() / 2);
        readyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                PlayerItemAttributes pAttr = playerItemAttributes.first();
                boolean playerIsNotReady = !pAttr.isReady();

                /*super.*/updatePlayerReadyAttr(0, playerIsNotReady);

                Table playerItem = (Table) playerList.getChild(0);
                DropDownMenu<?> colorSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.COLOR.ordinal());
                DropDownMenu<?> startPosSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.STARTPOS.ordinal());

                colorSelector.setDisabled(playerIsNotReady);
                startPosSelector.setDisabled(playerIsNotReady);

                connectionManager.broadcastClientInfo(pAttr);
            }
        });

        screenStage.addFocusableActor(readyButton, 2);
        screenStage.row();

        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    void initMap() {
        super.initMap();
        this.mapManager = new MapManager();
    }

    @Override
    public void pause() {
        if (playerItemAttributes.first().isReady()) {
            // Game was minimized in the mobile; so make the player unready
            readyButton.toggle();
        }
    }

    private void addPlayer(String name, boolean ready, int colorIndex, int startPosIndex) {
        Table playerItem = super.newPlayerItem(name, ready, colorIndex, startPosIndex, this);

        if (name == null) {
            DropDownMenu<?> colorSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.COLOR.ordinal());
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (mapGrid == null) { // Did not receive map info from the server yet. Slow connection?
                        return;
                    }

                    /*super.*/updatePlayerColorAttr(0, colorSelector.getSelectedIndex());
                    updateMapColor(0);

                    connectionManager.broadcastClientInfo(playerItemAttributes.first());
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

            DropDownMenu<?> startPosSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.STARTPOS.ordinal());
            startPosSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (mapGrid == null) { // Did not receive map info from the server yet. Slow connection?
                        return;
                    }

                    /*super.*/updateMapColor(0, startPosSelector.getSelectedIndex());
                    connectionManager.broadcastClientInfo(playerItemAttributes.first());
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
        }
    }

    private void nudgeReadyButton() {
        if (!readyButton.hasActions()) {
            int nudgeAngle = 25;
            float nudgeTime = 0.06f;
            float scaleAmount = 1.35f;
            int nudgeRepeatCount = 2;
            Interpolation nudgeInterpolation = Interpolation.smooth;

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
    public void updatePlayers(Array<String> playerNames, String[] nameStrings, boolean[] ready, int[] colorIndices, int[] startPosIndices) {
        for (int i = 0; i < nameStrings.length; i++) {
            int playerIndex = playerNames.indexOf(nameStrings[i], false);
            if (playerIndex != -1) {
                super.updatePlayerAttrsAndMap(playerIndex, nameStrings[i], ready[i], colorIndices[i], startPosIndices[i]);
            } else {
                addPlayer(nameStrings[i], ready[i], colorIndices[i], startPosIndices[i]);
                playerNames.add(nameStrings[i]);
            }
        }
    }

    @Override
    public void playerDisconnected(int playerIndex) {
        super.removePlayer(playerIndex);
    }

    @Override
    public FileHandle getExternalMapDir() {
        return this.mapManager.getExternalMapDir();
    }

    @Override
    public void mapChanged(final String mapName, final int[] mapExtraParams, final int mapHash, boolean isNewMap) {
        if (playerItemAttributes.first().isReady()) {
            // Make the client unready as the map has been changed
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
        if (!loadNewMap(mapName, mapExtraParams, mapHash)) {
            return;
        }

        this.preparedMapData = mapManager.getPreparedMapData();
        this.mapGrid = preparedMapData.mapGrid;
        this.map = new com.cg.zoned.Map(this.mapGrid, shapeDrawer);
        setCameraPosition();
        resetStartPosLabels();

        this.mapLabel.setText(preparedMapData.map.getName());
        readyButton.setDisabled(false);
    }

    private void resetStartPosLabels() {
        super.resetStartPositions();
    }

    private boolean loadNewMap(String mapName, int[] mapExtraParams, int serverMapHash) {
        MapEntity map = mapManager.getMap(mapName);
        if (map == null) {
            // Server selected an external map which is unavailable in the client
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
            disconnectWithMessage("Error: " + e.getMessage());
            return false;
        }

        int clientMapHash = map.getMapData().hashCode();
        if (clientMapHash != serverMapHash) {
            // Map in the server and client have the same name, but different contents
            disconnectWithMessage("Server client map content mismatch!\n" +
                    "The map content for the map '" + mapName + "'\n is different for you and the server\n" +
                    "(Server: " + serverMapHash + ", Client: " + clientMapHash + ")");
            return false;
        }

        return true;
    }

    @Override
    public void disconnectWithMessage(String errorMsg) {
        disconnectClient();
        screenStage.showOKDialog(errorMsg, false, button -> exitScreen());
    }

    @Override
    public void startGame() {
        map.clearGrid(players);
        Gdx.app.postRunnable(() -> animationManager.fadeOutStage(screenStage, ClientLobbyScreen.this,
                new GameScreen(game, preparedMapData, players, connectionManager)));
    }

    @Override
    public void disconnectClient() {
        playerList.clear();
        connectionManager.closeConnection();
    }

    private void exitScreen() {
        animationManager.fadeOutStage(screenStage, ClientLobbyScreen.this, new HostJoinScreen(game));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (map != null) {
            splitViewportManager.render(shapeDrawer, batch, map, players, delta);
        }

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        batch.begin();
        mapOverlay.render(shapeDrawer, screenStage, delta);
        batch.end();

        screenStage.draw();
        screenStage.act(delta);

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

        disconnectClient();
        exitScreen();

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
