package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.GridPoint2;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.Player;
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

        Label readyLabel;
        if (ready == null) {
            readyLabel = new Label("Not ready", game.skin);
            readyLabel.setColor(Color.RED);
        } else {
            readyLabel = new Label(ready, game.skin);
            if (ready.equals("Ready")) {
                readyLabel.setColor(Color.GREEN);
            } else {
                readyLabel.setColor(Color.RED);
            }
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
                    connectionManager.broadcastClientInfo();
                }
            });
            playerItem.add(colorSelector);

            DropDownMenu startPosSelector = new DropDownMenu(game.skin);
            startPosSelector.setName("startPos-selector");

            startPosSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
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

        return playerItem;
    }

    @Override
    public void mapChanged(MapManager mapManager) {
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
    public void removePlayer(Table playerItem) {
        playerList.removeActor(playerItem);
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
        stage.dispose();
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
