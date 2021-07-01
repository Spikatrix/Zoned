package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Assets;
import com.cg.zoned.MapSelector;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.PlayerItemAttributes;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.PlayerManager;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;
import com.esotericsoftware.kryonet.Server;

public class ServerLobbyScreen extends LobbyScreenHelper implements ServerLobbyConnectionManager.ServerPlayerListener, InputProcessor {
    private ServerLobbyConnectionManager connectionManager;

    private MapSelector mapSelector;
    private FocusableStage mapSelectorStage;
    private Spinner mapSpinner;
    private MapSelector.ExtendedMapSelectionListener extendedMapSelectionListener;
    private boolean mapSelectorActive;
    private boolean extendedMapSelectorActive;

    public ServerLobbyScreen(final Zoned game, Server server, String name) {
        super(game, name);
        game.discordRPCManager.updateRPC("In the Server Lobby");

        this.animationManager = new AnimationManager(game, this);
        this.connectionManager = new ServerLobbyConnectionManager(server, this);
    }

    @Override
    public void show() {
        setUpServerLobbyStage();
        setUpMapSelectorStage();
        initMap();

        playerConnected(null); // Add the host player (oneself) to the player list
        connectionManager.start();

        // Required as the screen is shown only after external maps and selector is loaded
        screenStage.getRoot().getColor().a = 0;

        // The screen is faded in once the maps and the extended selector is loaded
        mapSelector.loadExternalMaps(false, (mapList, externalMapStartIndex) -> {
            // Set up the extended map selector once external maps have been loaded
            setUpExtendedSelectorListener();
            mapSelector.setUpExtendedSelector(mapSelectorStage, extendedMapSelectionListener);

            animationManager.fadeInStage(screenStage);
        });
    }

    private void setUpExtendedSelectorListener() {
        this.extendedMapSelectionListener = new MapSelector.ExtendedMapSelectionListener() {
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
        };
    }

    private void openExtendedMapSelector() {
        extendedMapSelectorActive = true;
        animationManager.startExtendedMapSelectorAnimation(screenStage, mapSelectorStage, 0);
        mapSpinner.snapToStep(0);
    }

    private void setUpMapSelectorStage() {
        mapSelectorStage = new FocusableStage(screenViewport, this.game.getScaleFactor(), this.game.skin);
        mapSelectorStage.getRoot().getColor().a = 0f;
    }

    private void setUpServerLobbyStage() {
        Table serverLobbyTable = super.setUpLobbyUI();

        mapSelector = new MapSelector(screenStage, game.getScaleFactor(), game.assets, game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        float spinnerHeight = game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight() * 3;
        mapSpinner = mapSelector.loadMapSelectorSpinner(spinnerHeight * 1.5f, spinnerHeight);
        final Table mapSelectorTable = new Table();
        mapSelectorTable.add(mapSpinner).pad(10f);

        final Array<Actor> focusableDialogButtons = new Array<>();
        focusableDialogButtons.add(mapSpinner.getLeftButton());
        focusableDialogButtons.add(mapSpinner.getRightButton());

        final TextButton mapButton = new TextButton(mapSelector.getMapManager().getMapList().get(mapSpinner.getPositionIndex()).getName(), game.skin);
        mapButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openMapSelectorDialog(mapSelectorTable, focusableDialogButtons, mapButton);
            }
        });

        TextButton startButton = new TextButton("Start Game", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String errorMsg = connectionManager.validateServerData(playerItemAttributes);
                if (errorMsg != null) {
                    screenStage.showOKDialog(errorMsg, false, null);
                    return;
                }

                connectionManager.broadcastGameStart();
                startGame();
            }
        });

        Table footerTable = new Table();
        footerTable.add(mapButton).width(250f * game.getScaleFactor()).pad(10 * game.getScaleFactor()).expandX();
        footerTable.add(startButton).width(250f * game.getScaleFactor()).pad(10 * game.getScaleFactor()).expandX();

        screenStage.addFocusableActor(mapButton);
        screenStage.addFocusableActor(startButton, 2);
        screenStage.row();

        serverLobbyTable.add(footerTable).growX();
        screenStage.addActor(serverLobbyTable);

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
        mapSelector.loadSelectedMap(); // Loads the first map (Rectangle)
        updateMap();
    }

    private void updateMap() {
        this.preparedMapData = mapSelector.getMapManager().getPreparedMapData();
        this.mapGrid = this.preparedMapData.mapGrid;
        this.map = new com.cg.zoned.Map(this.mapGrid, shapeDrawer);
        super.setCameraPosition();
        mapChanged();
    }

    private void openMapSelectorDialog(Table mapSelectorTable, Array<Actor> focusableDialogButtons, TextButton mapButton) {
        final int prevIndex = mapSpinner.getPositionIndex();
        mapSelectorActive = true;

        screenStage.showDialog(mapSelectorTable, focusableDialogButtons,
                new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.SetMap },
                false, button -> {
                    if (button == FocusableStage.DialogButton.SetMap && mapSelector.loadSelectedMap()) {
                        updateMap();
                        mapButton.setText(preparedMapData.map.getName());
                        super.updateMapColor(0);
                    } else {
                        // Cancelled; restore the spinner pos
                        mapSpinner.snapToStep(prevIndex - mapSpinner.getPositionIndex());
                    }

                    mapSelectorActive = false;
                });
    }

    @Override
    public void playerConnected(String ipAddress) {
        Table playerItem = super.newPlayerItem(ipAddress, this);

        if (ipAddress == null) {
            DropDownMenu<?> colorSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.COLOR.ordinal());
            colorSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    /*super.*/updatePlayerColorAttr(0, colorSelector.getSelectedIndex());
                    updateMapColor(0);

                    connectionManager.broadcastPlayerInfo(playerItemAttributes, 0);
                }
            });

            DropDownMenu<?> startPosSelector = (DropDownMenu<?>) playerItem.getChild(PlayerItemType.STARTPOS.ordinal());
            startPosSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    /*super.*/updateMapColor(0, startPosSelector.getSelectedIndex());
                    connectionManager.broadcastPlayerInfo(playerItemAttributes, 0);
                }
            });
        }

        HoverImageButton kickButton = (HoverImageButton) playerItem.getChild(PlayerItemType.KICK.ordinal());
        kickButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String playerName = ((Label) playerItem.getChild(PlayerItemType.NAME.ordinal())).getText().toString();
                confirmKickPlayer(playerName);
            }
        });
    }

    private void confirmKickPlayer(String playerName) {
        final String finalPlayerName = playerName;

        if (playerName.equals(this.name)) {
            // Server wishes to kick ... themselves
            playerName = "... yourself?\nWait, you can do that";
        }

        screenStage.showDialog("Are you sure you want to kick " + playerName + "?",
                new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.Kick },
                false, button -> {
                    if (button == FocusableStage.DialogButton.Kick) {
                        int playerIndex = PlayerManager.getPlayerIndex(players, finalPlayerName);

                        if (playerIndex == 0) {
                            // Close the server and go back because the server kicked itself
                            exitScreen();
                        } else if (playerIndex != -1) {
                            // Kick the client out of the lobby
                            connectionManager.kickPlayer(playerIndex, "You were kicked from the lobby");
                        }
                    }
                });
    }

    public void updatePlayerDetails(int playerIndex, String clientName) {
        for (PlayerItemAttributes playerItemAttribute : playerItemAttributes) {
            String name = playerItemAttribute.getName();

            if (clientName.equals(name)) {
                connectionManager.kickPlayer(playerIndex, "Another player is using the same name\nPlease use a different name");
                return;
            }
        }

        super.updatePlayerName(playerIndex, clientName);

        connectionManager.acceptPlayer(playerIndex);
        connectionManager.sendMapDetails(playerIndex, mapSelector.getMapManager());
        connectionManager.broadcastPlayerInfo(playerItemAttributes, -1); // Send info about the new player to other clients and vice-versa
    }

    @Override
    public void updatePlayerDetails(int playerIndex, String name, boolean ready, int colorIndex, int startPosIndex) {
        super.updatePlayerName(playerIndex, name);
        super.updatePlayerColorAttr(playerIndex, colorIndex);
        super.updatePlayerReadyAttr(playerIndex, ready);
        super.updateMapColor(playerIndex, startPosIndex);

        connectionManager.broadcastPlayerInfo(playerItemAttributes, playerIndex);
    }

    private void mapChanged() {
        super.resetStartPositions();
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

    private void startGame() {
        map.clearGrid(players);
        animationManager.fadeOutStage(screenStage, this, new GameScreen(game, preparedMapData, players, connectionManager));
    }

    @Override
    public void playerDisconnected(int playerIndex) {
        String playerName = playerItemAttributes.get(playerIndex).getName();
        super.removePlayer(playerIndex);
        connectionManager.broadcastPlayerDisconnected(playerIndex, playerName);
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

    private void exitScreen() {
        playerList.clear();
        connectionManager.closeConnection();

        animationManager.fadeOutStage(screenStage, this, new HostJoinScreen(game));
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

        exitScreen();

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
