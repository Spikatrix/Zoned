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

    private VerticalGroup playerList;

    public ServerLobbyScreen(final Zoned game, Server server, String name) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        this.connectionManager = new ServerLobbyConnectionManager(server, this, name);
    }

    @Override
    public void show() {
        setUpServerLobbyStage();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        connectionManager.start();
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
    public HorizontalGroup playerConnected(String ipAddress) {
        HorizontalGroup playerItem = new HorizontalGroup();
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
        playerItem.addActor(name);

        who.setName("who-label");
        if (ipAddress == null) {
            addSpace(playerItem);
        }
        playerItem.addActor(who);

        if (ipAddress == null) { // SelectBox seems to need one more for proper alignment
            addSpace(playerItem);
        }
        addSpace(playerItem);

        Label ready = new Label("Not Ready", game.skin);
        ready.setColor(Color.RED);
        ready.setName("ready-label");
        playerItem.addActor(ready);

        addSpace(playerItem);

        if (ipAddress == null) {
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
                    connectionManager.broadcastPlayerInfo(0);
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

        //playerItem.setDebug(true);
        playerList.addActor(playerItem);

        return playerItem;
    }

    private void addSpace(HorizontalGroup playerItem) {
        playerItem.addActor(new Label("  ", game.skin));
    }

    @Override
    public void playerDisconnected(HorizontalGroup playerItem) {
        playerList.removeActor(playerItem);
    }

    private void startGame(MapManager mapManager) {
        Player[] players = connectionManager.inflatePlayerList();
        animationManager.fadeOutStage(stage, this, new GameScreen(game, mapManager, players, connectionManager.getServer()));
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