package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.listeners.ServerLobbyListener;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class ServerLobbyScreen extends ScreenAdapter implements InputProcessor {
    private final Zoned game;

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
        playerConnections = new Array<Connection>();
        playerItems = new Array<HorizontalGroup>();

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

        playerList = new VerticalGroup();
        //playerList.setDebug(true);
        serverLobbyTable.add(playerList).expand();

        serverLobbyTable.row();

        Table innerTable = new Table();
        Label gridSizeLabel = new Label("Grid size: ", game.skin, "themed");
        Label x = new Label("  x  ", game.skin);
        final int LOW_LIMIT = 3, HIGH_LIMIT = 100;
        int snapValue = 10;
        final Spinner rowSpinner = new Spinner(game.skin);
        final Spinner colSpinner = new Spinner(game.skin);
        rowSpinner.generateValueLabel(LOW_LIMIT, HIGH_LIMIT, game.skin);
        colSpinner.generateValueLabel(LOW_LIMIT, HIGH_LIMIT, game.skin);
        rowSpinner.getStepScrollPane().snapToStep(snapValue - LOW_LIMIT);
        colSpinner.getStepScrollPane().snapToStep(snapValue - LOW_LIMIT);

        innerTable.add(gridSizeLabel);
        innerTable.add(rowSpinner).width(rowSpinner.getPrefWidth() * game.getScaleFactor());
        innerTable.add(x);
        innerTable.add(colSpinner).width(colSpinner.getPrefWidth() * game.getScaleFactor());
        serverLobbyTable.add(innerTable).pad(10 * game.getScaleFactor());

        serverLobbyTable.row();

        TextButton startButton = new TextButton("Start Game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int rows = Math.round(rowSpinner.getScrollYPos() / rowSpinner.getScrollPaneHeight()) + LOW_LIMIT;
                int cols = Math.round(colSpinner.getScrollYPos() / colSpinner.getScrollPaneHeight()) + LOW_LIMIT;
                validateServerData(rows, cols);
            }
        });
        serverLobbyTable.add(startButton).width(200 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        stage.addActor(serverLobbyTable);
        stage.addFocusableActor(rowSpinner.getPlusButton());
        stage.addFocusableActor(rowSpinner.getMinusButton());
        stage.addFocusableActor(colSpinner.getPlusButton());
        stage.addFocusableActor(colSpinner.getMinusButton());
        stage.row();
        stage.addFocusableActor(startButton, 4);
        stage.row();
    }

    private void setUpBackButton() {
        Table table = new Table();
        table.setFillParent(true);
        table.left().top();
        Drawable backImage = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_back.png"))));
        final HoverImageButton backButton = new HoverImageButton(backImage);
        backButton.setNormalAlpha(1f);
        backButton.setHoverAlpha(.75f);
        backButton.setClickAlpha(.5f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
        table.add(backButton).padLeft(20f).padTop(35f);
        stage.addActor(table);
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
            colorSelector.setItems("Green", "Red", "Blue", "Yellow");
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    broadcastPlayerInfo(0);
                }
            });
            playerItem.addActor(colorSelector);

            stage.addFocusableActor(colorSelector, 4);
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

    private void validateServerData(int rows, int cols) {
        if (server.getConnections().length < 1) {
            stage.showInfoDialog("Insufficient players to start the match", game.getScaleFactor(), game.skin);
            return;
        }

        int size = playerItems.size;

        for (int i = 1; i < size; i++) {
            String ready = ((Label) playerItems.get(i).findActor("ready-label")).getText().toString();
            if (!ready.equals("Ready")) {
                stage.showInfoDialog("All players are not ready", game.getScaleFactor(), game.skin);
                return;
            }
        }

        Array<String> uniqueColors = new Array<String>();
        uniqueColors.add(((DropDownMenu) playerItems.get(0).findActor("color-selector")).getSelected());
        for (int i = 1; i < size; i++) {
            String color = ((Label) playerItems.get(i).findActor("color-label")).getText().toString();
            if (!uniqueColors.contains(color, false)) {
                uniqueColors.add(color);
            }
        }

        if (uniqueColors.size <= 1) {
            stage.showInfoDialog("Players must have at least two distinct colors", game.getScaleFactor(), game.skin);
            return;
        }

        BufferGameStart bgs = new BufferGameStart();
        bgs.playerNames = new String[size];
        bgs.startIndices = new int[size];
        bgs.rows = rows;
        bgs.cols = cols;
        for (int i = 0; i < size; i++) {
            bgs.playerNames[i] = ((Label) playerItems.get(i).findActor("name-label")).getText().toString();
            bgs.startIndices[i] = i;
        }

        server.sendToAllTCP(bgs);

        startGame(rows, cols);
    }

    private void startGame(final int rows, final int cols) {
        Vector2[] playerStartPositions = Map.getStartPositions(rows, cols);

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
            players[i].setStartPos(playerStartPositions[i]);
        }

        animationManager.fadeOutStage(stage, new GameScreen(game, rows, cols, players, server, null));
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
            FPSDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        removeListener(serverLobbyListener);
        stage.dispose();
    }

    private void onBackPressed() {
        server.close();
        playerList.clear();
        playerItems.clear();
        playerConnections.clear();

        animationManager.fadeOutStage(stage, new HostJoinScreen(game));
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