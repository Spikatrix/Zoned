package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.KryoHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.UITextDisplayer;
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

public class HostJoinScreen extends ScreenObject implements InputProcessor {
    // Bigger connection buffer size as map preview images, which are bigger than text data, are sent
    public static final int CONNECTION_BUFFER_SIZE = 131072; // 2^17
    public static final int SERVER_OBJECT_BUFFER_SIZE = 2048;
    public static final int CLIENT_WRITE_BUFFER_SIZE = 8192;

    public HostJoinScreen(final Zoned game) {
        super(game);

        game.discordRPCManager.updateRPC("Setting up local multiplayer");

        this.screenViewport = new ScreenViewport();
        this.screenStage = new FocusableStage(this.screenViewport);
        this.animationManager = new AnimationManager(this.game, this);
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();
        animationManager.fadeInStage(screenStage);
    }

    private void setUpStage() {
        Table table = new Table();
        table.setFillParent(true);
        //table.setDebug(true);
        table.center();

        Table infoTable = new Table();
        infoTable.setFillParent(true);
        infoTable.center().bottom();
        Texture infoIconTexture = new Texture(Gdx.files.internal("images/ui_icons/ic_info.png"));
        usedTextures.add(infoIconTexture);
        Image infoImage = new Image(infoIconTexture);
        Label infoLabel = new Label("Make sure that all players\nare on the same local network", game.skin);
        infoLabel.setAlignment(Align.center);
        infoTable.add(infoImage).height(game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight())
                .width(game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight()).padRight(20f);
        infoTable.add(infoLabel).pad(10f);
        screenStage.addActor(infoTable);

        Label playerNameLabel = new Label("Player name: ", game.skin, "themed");
        final TextField playerNameField = new TextField("", game.skin);
        playerNameField.setText(game.preferences.getString(Preferences.NAME_PREFERENCE, null));
        playerNameField.setCursorPosition(playerNameField.getText().length());
        table.add(playerNameLabel).right();
        table.add(playerNameField).width(playerNameField.getPrefWidth() * game.getScaleFactor()).left();

        table.row().pad(10 * game.getScaleFactor());
        screenStage.addFocusableActor(playerNameField, 2);
        screenStage.row();

        TextButton hostButton = new TextButton("Host", game.skin);
        TextButton joinButton = new TextButton("Join", game.skin);
        table.add(hostButton).width(200 * game.getScaleFactor());
        table.add(joinButton).width(200 * game.getScaleFactor());
        table.row();

        screenStage.addFocusableActor(hostButton);
        screenStage.addFocusableActor(joinButton);

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
                    game.preferences.putString(Preferences.NAME_PREFERENCE, name);
                    game.preferences.flush();

                    startServerLobby(playerNameField.getText().trim());
                } else {
                    screenStage.showOKDialog("Please enter the player name", false,
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
                        game.preferences.putString(Preferences.NAME_PREFERENCE, name);
                        game.preferences.flush();

                        searchingLabel.setText("Searching for servers...");
                        startClientLobby(name, searchingLabel);
                    } else {
                        searchingLabel.setText("Already searching for servers...");
                    }
                } else {
                    screenStage.showOKDialog("Please enter the player name", false,
                            game.getScaleFactor(), null, game.skin);
                }
            }
        });

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            Table outTable = createRestoreScreenPrompt();
            setMoveUpOnClick(playerNameField);
            screenStage.addActor(outTable);
        }

        screenStage.addActor(table);
        screenStage.setFocusedActor(playerNameField);
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    private void startServerLobby(final String playerName) {
        final Server server = new Server(CONNECTION_BUFFER_SIZE, SERVER_OBJECT_BUFFER_SIZE);

        Kryo kryo = server.getKryo();
        KryoHelper.registerClasses(kryo);

        server.start();
        try {
            server.bind(Constants.SERVER_PORT, Constants.SERVER_PORT);
        } catch (IOException | IllegalArgumentException e) {
            screenStage.showOKDialog("Server bind error\n" + e.getMessage(), false,
                    game.getScaleFactor(), null, game.skin);
            e.printStackTrace();
            return;
        }

        animationManager.fadeOutStage(screenStage, this, new ServerLobbyScreen(game, server, playerName));
    }

    private void startClientLobby(final String playerName, final Label searchingLabel) {
        final Client client = new Client(CLIENT_WRITE_BUFFER_SIZE, CONNECTION_BUFFER_SIZE);

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
            screenStage.showOKDialog("Error connecting to the server\n" + e.getMessage(), false,
                    game.getScaleFactor(), null, game.skin);
            searchingLabel.addAction(Actions.fadeOut(.2f));
            return;
        }

        if (!client.isConnected()) {
            screenStage.showOKDialog("Failed to connect to the server", false,
                    game.getScaleFactor(), null, game.skin);
            searchingLabel.addAction(Actions.fadeOut(.2f));
            return;
        }

        animationManager.fadeOutStage(screenStage, this, new ClientLobbyScreen(game, client, playerName));
    }

    private void restoreScreen() {
        Gdx.input.setOnscreenKeyboardVisible(false);

        screenStage.addAction(Actions.moveTo(0, 0, .5f, Interpolation.fastSlow));
        screenStage.unfocusAll();
    }

    private Table createRestoreScreenPrompt() {
        Table outTable = new Table();
        outTable.setTouchable(Touchable.enabled);
        Label restoreLabel = new Label("Tap anywhere else to restore the screen", game.skin, "themed");
        outTable.setHeight(screenStage.getHeight() * 2);
        outTable.setWidth(screenStage.getWidth());
        outTable.setPosition(outTable.getPrefWidth() / 2, -screenStage.getHeight());
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
                    screenStage.addAction(Actions.moveTo(0, screenStage.getHeight() - textField.getY() - textField.getHeight() - 10f, .5f, Interpolation.fastSlow));
                } else {
                    screenStage.addAction(Actions.moveTo(0, screenStage.getHeight() - textField.getParent().getY() - textField.getParent().getHeight() - 10f, .5f, Interpolation.fastSlow));
                }
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        if (game.showFPSCounter()) {
            UITextDisplayer.displayFPS(screenViewport, screenStage.getBatch(), game.getSmallFont());
        }

        screenStage.draw();
        screenStage.act(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
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

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
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
