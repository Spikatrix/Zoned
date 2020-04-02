package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.listeners.ClientLobbyListener;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;

public class ClientLobbyScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Client client;
    private ClientLobbyListener clientLobbyListener;

    private FocusableStage stage;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private Viewport viewport;

    private VerticalGroup playerList;
    private Array<String> playerNames;
    private Array<HorizontalGroup> playerItems;

    public String name;

    public ClientLobbyScreen(final Zoned game, Client client, String name) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        this.name = name;
        playerItems = new Array<HorizontalGroup>();
        playerNames = new Array<String>();

        this.client = client;
        clientLobbyListener = new ClientLobbyListener(this);
        client.addListener(clientLobbyListener);
    }

    @Override
    public void show() {
        setUpClientLobbyStage();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        insertPlayer(null, null, null, null);
        playerNames.add(this.name);

        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                sendClientNameToServer();
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

        Label onlinePlayersTitle = new Label("Connected Players: ", game.skin, "themed");
        clientLobbyTable.add(onlinePlayersTitle).pad(10 * game.getScaleFactor());

        clientLobbyTable.row();

        playerList = new VerticalGroup();
        clientLobbyTable.add(playerList).expand();

        clientLobbyTable.row();

        final TextButton readyButton = new TextButton("Ready up", game.skin);
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Label readyLabel = playerItems.get(0).findActor("ready-label");

                if (readyLabel.getText().toString().equals("Not ready")) {
                    readyLabel.setColor(Color.GREEN);
                    readyLabel.setText("Ready");
                    readyButton.setText("Unready");
                } else {
                    readyLabel.setColor(1f, 0, 0, 1f);
                    readyLabel.setText("Not ready");
                    readyButton.setText("Ready up");
                }

                BufferPlayerData bpd = new BufferPlayerData();
                bpd.nameStrings = new String[]{
                        ((Label) playerItems.get(0).findActor("name-label")).getText().toString()
                };
                bpd.whoStrings = new String[]{
                        ((Label) playerItems.get(0).findActor("who-label")).getText().toString()
                };
                bpd.readyStrings = new String[]{
                        ((Label) playerItems.get(0).findActor("ready-label")).getText().toString()
                };
                bpd.colorStrings = new String[]{
                        ((DropDownMenu) playerItems.get(0).findActor("color-selector SelectBox")).getSelected()
                };

                if (client.isConnected()) {
                    client.sendTCP(bpd);
                }
            }
        });
        clientLobbyTable.add(readyButton).width(200 * game.getScaleFactor()).pad(10 * game.getScaleFactor());

        stage.addFocusableActor(readyButton);
        stage.row();

        stage.addActor(clientLobbyTable);
    }

    private void addSpace(HorizontalGroup playerItem) {
        playerItem.addActor(new Label("  ", game.skin));
    }

    private void sendClientNameToServer() {
        BufferClientConnect bcc = new BufferClientConnect();
        bcc.playerName = this.name;
        client.sendTCP(bcc);
    }

    public void receivePlayerData(String[] nameStrings, String[] whoStrings, String[] readyStrings, String[] colorStrings) {
        for (int i = 0; i < nameStrings.length; i++) {
            if (nameStrings[i].equals(this.name)) { // No need to update information for this client itself
                continue;
            }

            if (whoStrings[i].equals("(DEL)")) {
                int index = this.playerNames.indexOf(nameStrings[i], false);
                this.playerNames.removeIndex(index);
                playerList.removeActor(playerItems.get(index));
                this.playerItems.removeIndex(index);
                continue;
            }

            int index = playerNames.indexOf(nameStrings[i], false);
            if (index != -1) {
                updatePlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i], index);
            } else {
                insertPlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i]);
                playerNames.add(nameStrings[i]);
            }
        }
    }

    private void insertPlayer(String name, String who, String ready, String color) {
        HorizontalGroup playerItem = new HorizontalGroup();
        playerItem.pad(10 * game.getScaleFactor());

        Label nameLabel;
        Label whoLabel;
        if (name == null) {
            nameLabel = new Label(this.name, game.skin);
            whoLabel = new Label("(You)", game.skin, "themed");
        } else {
            nameLabel = new Label(name, game.skin);
            if (who.endsWith("(Host, You)")) {
                who = who.substring(0, who.lastIndexOf(',')) + ')'; // Replace with "(Host)"
            }
            whoLabel = new Label(who, game.skin, "themed");
        }
        nameLabel.setName("name-label");
        playerItem.addActor(nameLabel);

        whoLabel.setName("who-label");
        if (!whoLabel.getText().toString().isEmpty()) {
            addSpace(playerItem);
        }
        playerItem.addActor(whoLabel);

        if (name == null) { // SelectBox seems to need one more for proper alignment
            addSpace(playerItem);
        }
        addSpace(playerItem);

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
        playerItem.addActor(readyLabel);

        addSpace(playerItem);

        if (color == null) {
            //final SelectBox<String> colorSelector = new SelectBox<String>(game.skin);
            final DropDownMenu colorSelector = new DropDownMenu(game.skin);
            colorSelector.setName("color-selector SelectBox");
            colorSelector.setItems("Green", "Red", "Blue", "Yellow");
            colorSelector.getList().setAlignment(Align.center);
            colorSelector.setAlignment(Align.center);
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    BufferPlayerData bpd = new BufferPlayerData();
                    bpd.nameStrings = new String[]{
                            ((Label) playerItems.get(0).findActor("name-label")).getText().toString()
                    };
                    bpd.whoStrings = new String[]{
                            ((Label) playerItems.get(0).findActor("who-label")).getText().toString()
                    };
                    bpd.readyStrings = new String[]{
                            ((Label) playerItems.get(0).findActor("ready-label")).getText().toString()
                    };
                    bpd.colorStrings = new String[]{
                            colorSelector.getSelected()
                    };

                    if (client.isConnected()) {
                        client.sendTCP(bpd);
                    }
                }
            });
            playerItem.addActor(colorSelector);
            stage.addFocusableActor(colorSelector);
            stage.row();
            stage.setFocusedActor(colorSelector);
        } else {
            Label colorLabel = new Label(color, game.skin);
            colorLabel.setName("color-label");
            playerItem.addActor(colorLabel);
        }

        playerItems.add(playerItem);
        playerList.addActor(playerItem);
    }

    private void updatePlayer(String name, String who, String ready, String color, int index) {
        ((Label) this.playerItems.get(index).findActor("name-label")).setText(name);

        if (who.endsWith("(Host, You)")) {
            who = who.substring(0, who.lastIndexOf(',')) + ')'; // Replace with "(Host)"
        }
        ((Label) this.playerItems.get(index).findActor("who-label")).setText(who);

        Label readyLabel = this.playerItems.get(index).findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) this.playerItems.get(index).findActor("color-label")).setText(color);
    }

    public void displayError(String errorMsg) {
        showDialogAndCloseClient(errorMsg);
    }

    private void showDialogAndCloseClient(String msg) {
        Dialog dialog = new Dialog("", game.skin) {
            @Override
            protected void result(Object object) {
                if (client.isConnected()) {
                    client.close();
                }
            }
        };
        dialog.text(msg).pad(25f * game.getScaleFactor(), 25f * game.getScaleFactor(), 20f * game.getScaleFactor(), 25f * game.getScaleFactor());
        dialog.getColor().a = 0; // Gets rid of the dialog flicker issue during `show()`
        dialog.getButtonTable().defaults().width(200f * game.getScaleFactor());
        dialog.button("OK");
        dialog.show(stage);
    }

    public void removeListener(Listener listener) {
        client.removeListener(listener);
    }

    public void disconnect() {
        playerNames.clear();
        playerItems.clear();
        playerList.clear();

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(stage, new HostJoinScreen(game));
            }
        });
    }

    public void startGame(String[] names, int[] indices, final int rows, final int cols) {
        Vector2[] playerStartPositions = Map.getStartPositions(rows, cols);

        int size = this.playerItems.size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            String name = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
            String color;
            if (i == 0) {
                color = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector SelectBox")).getSelected();
            } else {
                color = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
            }
            players[i] = new Player(PlayerColorHelper.getColorFromString(color), name);
            int startPosIndex = -1;
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(name)) {
                    startPosIndex = j;
                    break;
                }
            }
            players[i].setStartPos(playerStartPositions[indices[startPosIndex]]);
        }

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                animationManager.fadeOutStage(stage, new GameScreen(game, rows, cols, players, null, client));
            }
        });
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
        removeListener(clientLobbyListener);
        stage.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            if (client.isConnected()) {
                client.close();
            }

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
            if (client.isConnected()) {
                client.close();
            }

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
