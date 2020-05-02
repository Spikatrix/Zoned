package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.MapSelector;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.listeners.ServerLobbyListener;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.Map;

public class ServerLobbyScreen extends ScreenAdapter implements InputProcessor {
    private final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private Server server;
    private ServerLobbyListener serverLobbyListener;

    private FocusableStage stage;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private Viewport viewport;

    private VerticalGroup playerList;
    private Array<Connection> playerConnections;
    private Array<HorizontalGroup> playerItems;

    private String name;

    public ServerLobbyScreen(final Zoned game, Server server, String name) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        this.name = name;
        playerConnections = new Array<>();
        playerItems = new Array<>();

        this.server = server;
        serverLobbyListener = new ServerLobbyListener(this);
        server.addListener(serverLobbyListener);
    }

    @Override
    public void show() {
        setUpServerLobbyStage();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        playerConnections.add(null);
        insertPlayer(null);

        animationManager.fadeInStage(stage);
    }

    private void setUpServerLobbyStage() {
        Table serverLobbyTable = new Table();
        serverLobbyTable.setFillParent(true);
        //serverLobbyTable.setDebug(true);
        serverLobbyTable.center();

        Label onlinePlayersTitle = new Label("Connected Players: ", game.skin, "themed");
        serverLobbyTable.add(onlinePlayersTitle).pad(10 * game.getScaleFactor());

        serverLobbyTable.row();

        Table scrollTable = new Table();
        ScrollPane playerListScrollPane = new ScrollPane(scrollTable);
        playerListScrollPane.setOverscroll(false, true);

        playerList = new VerticalGroup();
        //playerList.setDebug(true);
        scrollTable.add(playerList).expand();
        serverLobbyTable.add(playerListScrollPane).expand();
        serverLobbyTable.row();

        final MapSelector mapSelector = new MapSelector(stage, game.getScaleFactor(), game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        Spinner mapSpinner = mapSelector.loadMapSelectorSpinner(150 * game.getScaleFactor(),
                game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight() * 3);
        // mapSelector.loadExternalMaps(); Not yet

        serverLobbyTable.add(mapSpinner).pad(10 * game.getScaleFactor());
        serverLobbyTable.row();

        TextButton startButton = new TextButton("Start Game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mapSelector.loadSelectedMap()) {
                    validateServerData(mapSelector.getMapManager());
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

    private void insertPlayer(String address) {
        HorizontalGroup playerItem = new HorizontalGroup();
        playerItem.pad(10 * game.getScaleFactor());

        Label name;
        Label who;
        if (address == null) {
            name = new Label(this.name, game.skin);
            who = new Label("(Host, You)", game.skin, "themed");
        } else {
            name = new Label(address, game.skin);
            who = new Label("", game.skin, "themed");
        }
        name.setName("name-label");
        playerItem.addActor(name);

        who.setName("who-label");
        if (address == null) {
            addSpace(playerItem);
        }
        playerItem.addActor(who);

        if (address == null) { // SelectBox seems to need one more for proper alignment
            addSpace(playerItem);
        }
        addSpace(playerItem);

        Label ready = new Label("Not Ready", game.skin);
        ready.setColor(Color.RED);
        ready.setName("ready-label");
        playerItem.addActor(ready);

        addSpace(playerItem);

        if (address == null) {
            DropDownMenu colorSelector = new DropDownMenu(game.skin);
            colorSelector.setName("color-selector");
            Array<String> colors = new Array<>();
            for (Map.Entry<String, Color> playerColorEntry : Constants.PLAYER_COLORS.entrySet()) {
                colors.add(playerColorEntry.getKey());
            }
            colorSelector.setItems(colors);
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    broadcastPlayerInfo(0);
                }
            });
            playerItem.addActor(colorSelector);

            stage.addFocusableActor(colorSelector, 2);
            stage.row();
            stage.setFocusedActor(colorSelector);
        } else {
            Label colorLabel = new Label("Green", game.skin);
            colorLabel.setName("color-label");
            playerItem.addActor(colorLabel);
        }

        playerItems.add(playerItem);
        //playerItem.setDebug(true);
        playerList.addActor(playerItem);
    }

    private void addSpace(HorizontalGroup playerItem) {
        playerItem.addActor(new Label("  ", game.skin));
    }

    public void connect(Connection connection) {
        String clientIpAddress = connection.getRemoteAddressTCP().getAddress().getHostAddress();
        insertPlayer(clientIpAddress);
        playerConnections.add(connection);
    }

    public void disconnect(Connection connection) {
        int index = playerConnections.indexOf(connection, true);

        ((Label) playerItems.get(index).findActor("who-label")).setText("(DEL)"); // Notify all clients to delete this player
        broadcastPlayerInfo(index);

        playerList.removeActor(playerItems.get(index));
        playerItems.removeIndex(index);
        playerConnections.removeIndex(index);
    }

    private void validateServerData(MapManager mapManager) {
        Array<String> dialogButtonTexts = new Array<>();
        dialogButtonTexts.add("OK");

        if (server.getConnections().length < 1) {
            stage.showDialog("Insufficient players to start the match", dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            return;
        }

        int size = playerItems.size;

        for (int i = 1; i < size; i++) {
            String ready = ((Label) playerItems.get(i).findActor("ready-label")).getText().toString();
            if (!ready.equals("Ready")) {
                stage.showDialog("All players are not ready", dialogButtonTexts,
                        false,
                        game.getScaleFactor(), null, game.skin);
                return;
            }
        }

        Array<String> uniqueColors = new Array<>();
        uniqueColors.add(((DropDownMenu) playerItems.get(0).findActor("color-selector")).getSelected());
        for (int i = 1; i < size; i++) {
            String color = ((Label) playerItems.get(i).findActor("color-label")).getText().toString();
            if (!uniqueColors.contains(color, false)) {
                uniqueColors.add(color);
            }
        }

        if (uniqueColors.size <= 1) {
            stage.showDialog("Players must have at least two distinct colors", dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            return;
        }

        MapEntity preparedMap = mapManager.getPreparedMap();

        BufferGameStart bgs = new BufferGameStart();
        bgs.playerNames = new String[size];
        bgs.startIndices = new int[size];
        bgs.mapName = preparedMap.getName();
        if (preparedMap.getExtraParams() != null) {
            bgs.mapExtraParams = preparedMap.getExtraParams().extraParams;
        } else {
            bgs.mapExtraParams = null;
        }
        for (int i = 0; i < size; i++) {
            bgs.playerNames[i] = ((Label) playerItems.get(i).findActor("name-label")).getText().toString();
            bgs.startIndices[i] = i;
        }

        server.sendToAllTCP(bgs);

        startGame(mapManager);
    }

    private void startGame(MapManager mapManager) {
        int size = this.playerItems.size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            String name = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
            String color;
            if (i == 0) {
                color = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
            } else {
                color = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
            }
            players[i] = new Player(PlayerColorHelper.getColorFromString(color), name);
        }

        animationManager.fadeOutStage(stage, this, new GameScreen(game, mapManager, players, server));
    }

    public void receiveClientName(Connection connection, String name) {
        for (HorizontalGroup hg : playerItems) {
            String n = ((Label) hg.findActor("name-label")).getText().toString();

            if (n.equals(name)) {
                BufferServerRejectedConnection bsrc = new BufferServerRejectedConnection();
                bsrc.errorMsg = "Another player is using the same name\nPlease use a different name";
                connection.sendTCP(bsrc);
                return;
            }
        }

        int index = playerConnections.indexOf(connection, true);
        HorizontalGroup hg = playerItems.get(index);
        Label nameLabel = hg.findActor("name-label");
        nameLabel.setText(name);

        broadcastPlayerInfo(-1);
    }

    private void broadcastPlayerInfo(int index) {
        int size;
        if (index == -1) { // Broadcast all info
            size = this.playerItems.size;
        } else {
            size = 1;
        }

        BufferPlayerData bpd = new BufferPlayerData();
        bpd.nameStrings = new String[size];
        bpd.whoStrings = new String[size];
        bpd.readyStrings = new String[size];
        bpd.colorStrings = new String[size];

        for (int i = 0; i < size; i++) {
            if (index == -1) {
                bpd.nameStrings[i] = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
                bpd.whoStrings[i] = ((Label) this.playerItems.get(i).findActor("who-label")).getText().toString();
                bpd.readyStrings[i] = ((Label) this.playerItems.get(i).findActor("ready-label")).getText().toString();

                if (i != 0) {
                    bpd.colorStrings[i] = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
                } else {
                    bpd.colorStrings[i] = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
                }
            } else {
                bpd.nameStrings[0] = ((Label) this.playerItems.get(index).findActor("name-label")).getText().toString();
                bpd.whoStrings[0] = ((Label) this.playerItems.get(index).findActor("who-label")).getText().toString();
                bpd.readyStrings[0] = ((Label) this.playerItems.get(index).findActor("ready-label")).getText().toString();

                if (index != 0) {
                    bpd.colorStrings[0] = ((Label) this.playerItems.get(index).findActor("color-label")).getText().toString();
                } else {
                    bpd.colorStrings[0] = ((DropDownMenu) this.playerItems.get(index).findActor("color-selector")).getSelected();
                }
            }
        }

        for (Connection connection : server.getConnections()) {
            connection.sendTCP(bpd);
        }
    }

    public void receiveClientData(Connection connection, String name, String who, String ready, String color) { // TODO: Unused params?
        int index = playerConnections.indexOf(connection, true);
        HorizontalGroup hg = playerItems.get(index);

        // Only needs to set the ready and color labels as others will not be changed
        Label readyLabel = hg.findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) hg.findActor("color-label")).setText(color);

        broadcastPlayerInfo(index);
    }

    private void removeListener(Listener listener) {
        server.removeListener(listener);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void dispose() {
        removeListener(serverLobbyListener);
        stage.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        server.close();
        playerList.clear();
        playerItems.clear();
        playerConnections.clear();

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