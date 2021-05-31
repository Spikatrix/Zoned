package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.PlayerItemAttributes;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.DropDownMenu;
import com.cg.zoned.ui.HoverImageButton;

import java.util.Arrays;

public abstract class LobbyScreenHelper extends ScreenObject {
    String name;

    com.cg.zoned.Map map;
    Cell[][] mapGrid;
    SplitViewportManager splitViewportManager;
    Overlay mapOverlay;
    PreparedMapData preparedMapData;

    Player[] players;
    Array<PlayerItemAttributes> playerItemAttributes;
    Table playerList;

    LobbyScreenHelper(Zoned game, String name) {
        super(game);

        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
        this.name = name;
    }

    Table setUpLobbyUI() {
        Table lobbyTable = new Table();
        lobbyTable.setFillParent(true);
        lobbyTable.center();
        //lobbyTable.setDebug(true);

        Label lobbyTitle = new Label("Lobby", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(lobbyTitle.getPrefHeight());
        lobbyTable.add(lobbyTitle).pad(headerPad);
        lobbyTable.row();

        playerList = new Table();
        playerList.center();
        //playerList.setFillParent(true);
        ScrollPane playerListScrollPane = new ScrollPane(playerList, game.skin);
        playerListScrollPane.setOverscroll(false, true);

        lobbyTable.add(playerListScrollPane).grow();
        lobbyTable.row();

        screenStage.setScrollFocus(playerListScrollPane);

        return lobbyTable;
    }

    void initMap() {
        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
        this.splitViewportManager = new SplitViewportManager(1, Constants.WORLD_SIZE, null);
        this.splitViewportManager.setUpDragOffset(screenStage);
        this.mapOverlay = new Overlay(new Color(0, 0, 0, .8f));

        // This array size is increased in playerConnected
        // I know I should use ArrayLists instead, but Map works with regular 'ol arrays for now
        this.players = new Player[0];
        this.playerItemAttributes = new Array<>();
    }

    void setCameraPosition() {
        if (map == null) {
            return;
        }

        float centerX = (map.cols * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        float centerY = (map.rows * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        splitViewportManager.setViewportCameraPosition(new Vector2(centerX, centerY));
    }

    private void addNewPlayerIntoMap(String playerName, int colorIndex, int startPosIndex) {
        int playerIndex = players.length;
        players = Arrays.copyOf(players, playerIndex + 1);

        PlayerItemAttributes playerItemAttributes = new PlayerItemAttributes(playerName == null ? name : playerName,
                false, colorIndex, startPosIndex);
        this.playerItemAttributes.add(playerItemAttributes);

        if (preparedMapData != null) {
            players[playerIndex] = createPlayerFromAttributes(playerItemAttributes, preparedMapData.startPositions);
        } else {
            players[playerIndex] = createPlayerFromAttributes(playerItemAttributes, null);
        }

        updateMapColor(playerIndex);
    }

    void updateMapColor(int playerIndex) {
        updateMapColor(playerIndex, playerItemAttributes.get(playerIndex).getStartPosIndex());
    }

    void updateMapColor(int playerIndex, int startPosIndex) {
        Player player = players[playerIndex];
        Color color = player.color;

        updateMapColor(playerIndex, player, color, startPosIndex);
    }

    private void updateMapColor(int playerIndex, Player player, Color color, int startPosIndex) {
        if (mapGrid == null) {
            // Skip updating colors as the map hasn't been loaded yet
            return;
        }

        boolean outOfBounds = false;
        try {
            mapGrid[player.roundedPosition.y][player.roundedPosition.x].cellColor = null;
        } catch (ArrayIndexOutOfBoundsException e) {
            outOfBounds = true; // Probably the map changed
        }

        GridPoint2 prevLoc = new GridPoint2(player.roundedPosition);
        player.color = color;

        if (startPosIndex != -1) {
            //player.setPosition(preparedMapData.startPositions.get(startPosIndex).getLocation());
            updatePlayerStartPosAttr(playerIndex, startPosIndex);
            mapGrid[player.roundedPosition.y][player.roundedPosition.x].cellColor = player.color;
        }

        if (!outOfBounds) { // Huh? Excuse me, lint? Always true? Nope.
            for (Player p : players) {
                if (p.roundedPosition.equals(prevLoc) && p.color != Color.BLACK) {
                    mapGrid[prevLoc.y][prevLoc.x].cellColor = p.color;
                    break;
                }
            }
        }
    }

    void removePlayer(int playerIndex) {
        // Remove player colors from the map
        updateMapColor(playerIndex, players[playerIndex], Color.BLACK, 0);

        // Remove player object
        Player[] ps = new Player[players.length - 1];
        int psIndex = 0;
        for (int i = 0; i < players.length; i++) {
            if (i == playerIndex) {
                continue;
            }
            ps[psIndex] = players[i];
            psIndex++;
        }

        // Remove player item attributes object
        this.playerItemAttributes.removeIndex(playerIndex);
        this.players = ps;

        // Remove player from the player list
        playerList.removeActorAt(playerIndex, true);
    }

    Table newPlayerItem(String name, ScreenObject screen) {
        return newPlayerItem(name, false, 0, 0, screen);
    }

    Table newPlayerItem(String name, boolean ready, int colorIndex, int startPosIndex, ScreenObject screen) {
        Table playerItem = new Table();
        playerItem.pad(10 * game.getScaleFactor());

        boolean isServer = false;
        if (screen instanceof ServerLobbyScreen) {
            isServer = true;
        }

        for (PlayerItemType playerItemType : PlayerItemType.values()) {
            if (playerItemType == PlayerItemType.READY) {
                addReadyField(playerItem, ready, isServer);
            } else if (playerItemType == PlayerItemType.NAME) {
                addNameField(playerItem, name);
            } else if (playerItemType == PlayerItemType.COLOR) {
                addColorField(playerItem, colorIndex, name);
            } else if (playerItemType == PlayerItemType.STARTPOS) {
                addStartPosField(playerItem, startPosIndex, name);
            } else if (playerItemType == PlayerItemType.KICK) {
                if (isServer) {
                    addKickField(playerItem);
                }
            } else {
                throw new IllegalStateException("Unknown player item type " + playerItemType);
            }
        }

        playerList.add(playerItem).uniformX().fillX();
        playerList.row();

        screenStage.row();

        addNewPlayerIntoMap(name, colorIndex, startPosIndex);

        return playerItem;
    }

    private void addReadyField(Table playerItem, boolean ready, boolean isServer) {
        boolean isHost = false;
        if (isServer && players.length == 0) {
            // First player in the server is the host
            isHost = true;
        } else if (!isServer && players.length == 1) {
            // Second player in the client screen will be the host
            isHost = true;
        }

        TextureRegionDrawable hostTexture = new TextureRegionDrawable(
                game.assets.getTexture(Assets.TextureObject.HOST_TEXTURE));
        TextureRegionDrawable readyTextureDrawable = new TextureRegionDrawable(
                game.assets.getTexture(Assets.TextureObject.READY_TEXTURE));
        TextureRegionDrawable notReadyTextureDrawable = new TextureRegionDrawable(
                game.assets.getTexture(Assets.TextureObject.NOT_READY_TEXTURE));

        ImageButton readyImage;
        if (isHost) {
            readyImage = new ImageButton(hostTexture);
        } else {
            readyImage = new ImageButton(notReadyTextureDrawable, null, readyTextureDrawable);
        }

        readyImage.setChecked(ready);
        readyImage.setDisabled(true);
        playerItem.add(readyImage).space(20f * game.getScaleFactor()).size(48f * game.getScaleFactor());
    }

    private void addNameField(Table playerItem, String name) {
        Label nameLabel;
        if (name == null) {
            nameLabel = new Label(this.name, game.skin); // this.name != name
        } else {
            nameLabel = new Label(name, game.skin);
        }
        nameLabel.setAlignment(Align.center);
        nameLabel.setEllipsis(true);
        playerItem.add(nameLabel).space(20f * game.getScaleFactor()).width(180f * game.getScaleFactor());
    }

    private void addColorField(Table playerItem, int colorIndex, String name) {
        if (name != null) {
            Label colorLabel = new Label(PlayerColorHelper.getStringFromIndex(colorIndex), game.skin);
            colorLabel.setAlignment(Align.center);
            playerItem.add(colorLabel).space(20f * game.getScaleFactor()).expandX().uniformX().fillX();
        } else {
            DropDownMenu colorSelector = new DropDownMenu(game.skin, game.getScaleFactor());
            colorSelector.setItems(PlayerColorHelper.getNameList());
            playerItem.add(colorSelector).space(20f * game.getScaleFactor()).expandX().uniformX().fillX();

            screenStage.addFocusableActor(colorSelector);
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
                screenStage.setFocusedActor(colorSelector);
            }
        }
    }

    private void addStartPosField(Table playerItem, int startPosIndex, String name) {
        if (name != null) {
            Label startPosLabel = new Label(preparedMapData.startPositions.get(startPosIndex).getViewName(), game.skin);
            startPosLabel.setAlignment(Align.center);
            playerItem.add(startPosLabel).space(20f * game.getScaleFactor()).expandX().uniformX().fillX();
        } else {
            final DropDownMenu startPosSelector = new DropDownMenu(game.skin, game.getScaleFactor());
            startPosSelector.setItems(getStartPosViewNameList());
            playerItem.add(startPosSelector).space(20f * game.getScaleFactor()).expandX().uniformX().fillX();

            screenStage.addFocusableActor(startPosSelector);
        }
    }

    private void addKickField(Table playerItem) {
        TextureRegionDrawable kickTextureDrawable = new TextureRegionDrawable(game.assets.getTexture(Assets.TextureObject.KICK_TEXTURE));
        HoverImageButton kickButton = new HoverImageButton(kickTextureDrawable);
        kickButton.setClickAlpha(.6f);
        kickButton.setHoverAlpha(.7f);
        playerItem.add(kickButton).space(20f * game.getScaleFactor()).size(48f * game.getScaleFactor());

        if (players.length == 0) {
            screenStage.addFocusableActor(kickButton);
        } else {
            screenStage.addFocusableActor(kickButton, 3);
        }
    }

    Player createPlayerFromAttributes(PlayerItemAttributes playerItemAttributes, Array<StartPosition> startPositions) {
        Player player = new Player(PlayerColorHelper.getColorFromIndex(playerItemAttributes.getColorIndex()), playerItemAttributes.getName());
        if (startPositions != null) {
            player.setPosition(startPositions.get(playerItemAttributes.getStartPosIndex()).getLocation());
        }
        return player;
    }

    void updatePlayerAttrs(int playerIndex, boolean readyStatus, int colorIndex, int startPosIndex) {
        // Not called anywhere as updating the start pos here means we would lose the player previous position needed to update map colors
        updatePlayerReadyAttr(playerIndex, readyStatus);
        updatePlayerColorAttr(playerIndex, colorIndex);
        updatePlayerStartPosAttr(playerIndex, startPosIndex);
    }

    void updatePlayerReadyAttr(int playerIndex, boolean readyStatus) {
        PlayerItemAttributes playerAttribute = playerItemAttributes.get(playerIndex);
        playerAttribute.setReady(readyStatus);

        Table playerItem = ((Table) playerList.getChild(playerIndex));
        ImageButton readyImage = ((ImageButton) playerItem.getChild(PlayerItemType.READY.ordinal()));
        readyImage.setChecked(readyStatus);
    }

    void updatePlayerColorAttr(int playerIndex, int colorIndex) {
        PlayerItemAttributes playerAttribute = playerItemAttributes.get(playerIndex);
        playerAttribute.setColorIndex(colorIndex);

        Player player = players[playerIndex];
        player.color = PlayerColorHelper.getColorFromIndex(colorIndex);

        Table playerItem = ((Table) playerList.getChild(playerIndex));
        if (playerIndex != 0) {
            Label colorLabel = ((Label) playerItem.getChild(PlayerItemType.COLOR.ordinal()));
            colorLabel.setText(PlayerColorHelper.getStringFromIndex(colorIndex));
        } else {
            DropDownMenu colorSelector = ((DropDownMenu) playerItem.getChild(PlayerItemType.COLOR.ordinal()));
            colorSelector.setSelectedIndex(colorIndex);
        }
    }

    void updatePlayerStartPosAttr(int playerIndex, int startPosIndex) {
        PlayerItemAttributes playerAttribute = playerItemAttributes.get(playerIndex);
        playerAttribute.setStartPosIndex(startPosIndex);

        StartPosition startPosition = preparedMapData.startPositions.get(startPosIndex);

        Player player = players[playerIndex];
        player.setPosition(startPosition.getLocation());

        Table playerItem = ((Table) playerList.getChild(playerIndex));
        if (playerIndex != 0) {
            Label startPosLabel = ((Label) playerItem.getChild(PlayerItemType.STARTPOS.ordinal()));
            startPosLabel.setText(startPosition.getViewName());
        } else {
            DropDownMenu startPosSelector = ((DropDownMenu) playerItem.getChild(PlayerItemType.STARTPOS.ordinal()));
            startPosSelector.setSelectedIndex(startPosIndex);
        }
    }

    void updatePlayerName(int playerIndex, String name) {
        playerItemAttributes.get(playerIndex).setName(name);
        players[playerIndex].name = name;

        Table playerItem = ((Table) playerList.getChild(playerIndex));
        ((Label) playerItem.getChild(PlayerItemType.NAME.ordinal())).setText(name);
    }

    void resetStartPositions() {
        if (players.length == 0) {
            return;
        }

        DropDownMenu startPosSelector = (DropDownMenu) ((Table) playerList.getChild(0)).getChild(PlayerItemType.STARTPOS.ordinal());
        startPosSelector.setItems(getStartPosViewNameList());

        for (int i = 0; i < playerItemAttributes.size; i++) {
            updatePlayerStartPosAttr(i, 0);
        }
    }

    Array<String> getStartPosViewNameList() {
        Array<String> startPosList = new Array<>();
        if (preparedMapData != null) {
            for (StartPosition startPosition : preparedMapData.startPositions) {
                startPosList.add(startPosition.getViewName());
            }
        }
        return startPosList;
    }

    // The element order here determines how they're laid out in the UI
    enum PlayerItemType {
        READY, NAME, COLOR, STARTPOS, KICK
    }
}
