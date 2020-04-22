package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.KryoHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.net.InetAddress;

public class HostJoinScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<Texture>();

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private Array<String> dialogButtonTexts = new Array<String>();

    public HostJoinScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());

        dialogButtonTexts.add("OK");
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        Table table = new Table();
        table.setFillParent(true);
        //table.setDebug(true);
        table.center();

        Table infoTable = new Table();
        infoTable.setFillParent(true);
        infoTable.center().bottom();
        Texture infoIconTexture = new Texture(Gdx.files.internal("icons/ui_icons/ic_info.png"));
        usedTextures.add(infoIconTexture);
        Image infoImage = new Image(infoIconTexture);
        Label infoLabel = new Label("Make sure that all players\nare on the same local network", game.skin);
        infoLabel.setAlignment(Align.center);
        infoTable.add(infoImage).height(game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight())
                .width(game.skin.getFont(Constants.FONT_MANAGER.REGULAR.getName()).getLineHeight()).padRight(20f);
        infoTable.add(infoLabel).pad(10f);
        stage.addActor(infoTable);

        Label playerNameLabel = new Label("Player name: ", game.skin, "themed");
        final TextField playerNameField = new TextField("", game.skin);
        playerNameField.setText(game.preferences.getString(Constants.NAME_PREFERENCE, null));
        playerNameField.setCursorPosition(playerNameField.getText().length());
        table.add(playerNameLabel).right();
        table.add(playerNameField).width(playerNameField.getPrefWidth() * game.getScaleFactor()).left();

        table.row().pad(10 * game.getScaleFactor());
        stage.addFocusableActor(playerNameField, 2);
        stage.row();

        TextButton hostButton = new TextButton("Host", game.skin);
        TextButton joinButton = new TextButton("Join", game.skin);
        table.add(hostButton).width(200 * game.getScaleFactor());
        table.add(joinButton).width(200 * game.getScaleFactor());
        table.row();

        stage.addFocusableActor(hostButton);
        stage.addFocusableActor(joinButton);

        final Label searchingLabel = new Label("Searching for servers...", game.skin, "themed");
        searchingLabel.getColor().a = 0;
        table.add(searchingLabel).colspan(2);
        table.row();

        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                restoreScreen();

                String name = playerNameField.getText().trim();
                if (!name.isEmpty()) {
                    game.preferences.putString(Constants.NAME_PREFERENCE, name);
                    game.preferences.flush();

                    startServerLobby(playerNameField.getText().trim());
                } else {
                    stage.showDialog("Please enter the player name", dialogButtonTexts,
                            false,
                            game.getScaleFactor(), null, game.skin);
                }
            }
        });

        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                restoreScreen();

                String name = playerNameField.getText().trim();
                if (!name.isEmpty()) {
                    if (searchingLabel.getColor().a == 0) {
                        game.preferences.putString(Constants.NAME_PREFERENCE, name);
                        game.preferences.flush();

                        searchingLabel.setText("Searching for servers...");
                        startClientLobby(name, searchingLabel);
                    } else {
                        searchingLabel.setText("Already searching for servers...");
                    }
                } else {
                    stage.showDialog("Please enter the player name", dialogButtonTexts,
                            false,
                            game.getScaleFactor(), null, game.skin);
                }
            }
        });

        Table outTable = null;
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            outTable = createRestoreScreenPrompt();
            setMoveUpOnClick(playerNameField);
        }

        if (outTable != null) {
            stage.addActor(outTable);
        }
        stage.addActor(table);
        stage.setFocusedActor(playerNameField);
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

    private void startServerLobby(final String playerName) {
        final Server server = new Server();

        Kryo kryo = server.getKryo();
        KryoHelper.registerClasses(kryo);

        server.start();
        try {
            server.bind(Constants.SERVER_PORT, Constants.SERVER_PORT);
        } catch (IOException e) {
            stage.showDialog("Server bind error\n" + e.getMessage(), dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            e.printStackTrace();
            return;
        } catch (IllegalArgumentException e) {
            stage.showDialog("Server bind error\n" + e.getMessage(), dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            e.printStackTrace();
            return;
        }

        animationManager.fadeOutStage(stage, this, new ServerLobbyScreen(game, server, playerName));
    }

    private void startClientLobby(final String playerName, final Label searchingLabel) {
        final Client client = new Client();

        Kryo kryo = client.getKryo();
        KryoHelper.registerClasses(kryo);

        client.start();
        searchingLabel.addAction(Actions.fadeIn(.2f));

        new Thread(new Runnable() {
            @Override
            public void run() {
                final InetAddress addr = client.discoverHost(Constants.SERVER_PORT, 4000);
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        checkAndStartClientScreen(client, playerName, addr, searchingLabel);
                    }
                });
            }
        }).start();
    }

    private void checkAndStartClientScreen(Client client, String playerName, InetAddress addr, Label searchingLabel) {
        searchingLabel.clearActions();

        try {
            if (addr == null) {
                throw new IOException("Failed to find the host");
            }
            client.connect(4000, addr, Constants.SERVER_PORT, Constants.SERVER_PORT);
        } catch (IOException e) {
            stage.showDialog("Error connecting to the server\n" + e.getMessage(), dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            searchingLabel.addAction(Actions.fadeOut(.2f));
            return;
        }

        if (!client.isConnected()) {
            stage.showDialog("Failed to connect to the server", dialogButtonTexts,
                    false,
                    game.getScaleFactor(), null, game.skin);
            searchingLabel.addAction(Actions.fadeOut(.2f));
            return;
        }

        animationManager.fadeOutStage(stage, this, new ClientLobbyScreen(game, client, playerName));
    }

    private void restoreScreen() {
        Gdx.input.setOnscreenKeyboardVisible(false);

        stage.addAction(Actions.moveTo(0, 0, .5f, Interpolation.fastSlow));
        stage.unfocusAll();
    }

    private Table createRestoreScreenPrompt() {
        Table outTable = new Table();
        outTable.setTouchable(Touchable.enabled);
        Label restoreLabel = new Label("Tap anywhere else to restore the screen", game.skin);
        outTable.setHeight(stage.getHeight() * 2);
        outTable.setWidth(stage.getWidth());
        outTable.setPosition(outTable.getPrefWidth() / 2, -stage.getHeight());
        outTable.add(restoreLabel).padTop(restoreLabel.getHeight() * game.getScaleFactor()).align(Align.center);
        outTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                restoreScreen();
            }
        });

        return outTable;
    }

    private void setMoveUpOnClick(final TextField textField) {
        textField.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                if (textField.getY() != 0) {
                    stage.addAction(Actions.moveTo(0, stage.getHeight() - textField.getY() - textField.getHeight() - 10f, .5f, Interpolation.fastSlow));
                } else {
                    stage.addAction(Actions.moveTo(0, stage.getHeight() - textField.getParent().getY() - textField.getParent().getHeight() - 10f, .5f, Interpolation.fastSlow));
                }
            }
        });
    }


    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
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
    public void dispose() {
        stage.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        animationManager.fadeOutStage(stage, this, new MainMenuScreen(game));
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
