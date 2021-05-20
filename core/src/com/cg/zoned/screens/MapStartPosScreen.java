package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.ViewportDividers;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.PlayerSetUpParams;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.ButtonGroup;
import com.cg.zoned.ui.CheckBox;
import com.cg.zoned.ui.HoverImageButton;

public class MapStartPosScreen extends ScreenObject implements InputProcessor {
    private PreparedMapData mapData;
    private Map map;
    private Cell[][] mapGrid;
    private Array<StartPosition> startPositions;

    private Overlay mapOverlay;
    private SplitViewportManager splitViewportManager;
    private ViewportDividers viewportDividers;
    private int splitScreenCount;

    private Player[] players;
    private CheckBox[][] radioButtons;
    private ButtonGroup[] buttonGroup;
    private Label[] playerLabels;
    private int playerStartIndex;

    public MapStartPosScreen(final Zoned game, PreparedMapData preparedMapData, Player[] players) {
        super(game);
        game.discordRPCManager.updateRPC("Choosing start positions");

        this.mapData = preparedMapData;
        this.mapGrid = preparedMapData.mapGrid;
        this.startPositions = preparedMapData.startPositions;

        this.players = players;
        this.splitScreenCount = game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);

        this.animationManager = new AnimationManager(this.game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);
    }

    @Override
    public void show() {
        setUpMap();
        setUpStage();
        animationManager.fadeInStage(screenStage);
    }

    private void setUpMap() {
        map = new Map(mapGrid, 0, shapeDrawer); // Wall count is unnecessary in this case so 0
        map.createPlayerLabelTextures(players, shapeDrawer, game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName()));

        mapOverlay = new Overlay(new Color(0, 0, 0, 0.8f));

        float centerX = (map.cols * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        float centerY = (map.rows * (Constants.CELL_SIZE + Constants.MAP_GRID_LINE_WIDTH)) / 2;
        splitViewportManager = new SplitViewportManager(splitScreenCount, Constants.WORLD_SIZE, new Vector2(centerX, centerY));

        for (int i = 0; i < players.length; i++) {
            players[i].setPosition(startPositions.get(i % startPositions.size).getLocation());
            mapGrid[players[i].roundedPosition.y][players[i].roundedPosition.x].cellColor = players[i].color;
        }

        playerStartIndex = 0;
        viewportDividers = new ViewportDividers(splitScreenCount, players);
    }

    private void setUpStage() {
        final Table masterTable = new Table();
        masterTable.center();
        masterTable.setFillParent(true);

        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });

        Label title = new Label("Choose start positions", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(title.getPrefHeight());
        masterTable.add(title).expandX().padTop(headerPad).colspan(splitScreenCount);
        masterTable.row();

        playerLabels = new Label[splitScreenCount];
        radioButtons = new CheckBox[splitScreenCount][];
        buttonGroup = new ButtonGroup[splitScreenCount];
        for (int i = 0; i < splitScreenCount; i++) {
            Table table = new Table();
            ScrollPane startPosScrollPane = null;

            final boolean alignLeft = (i < (splitScreenCount / 2));
            if (i < players.length) {
                playerLabels[i] = new Label("Player " + (i + 1), game.skin);
                Color labelColor = new Color(players[i].color);
                playerLabels[i].setColor(labelColor);
                if (alignLeft) {
                    table.add(playerLabels[i]).padBottom(10f * game.getScaleFactor()).left().expandX();
                } else {
                    table.add(playerLabels[i]).padBottom(10f * game.getScaleFactor()).right().expandX();
                }
                table.row();

                Table scrollTable = new Table();
                startPosScrollPane = new ScrollPane(scrollTable);
                startPosScrollPane.setOverscroll(false, true);

                radioButtons[i] = new CheckBox[startPositions.size];
                buttonGroup[i] = new ButtonGroup();
                buttonGroup[i].setMinCheckCount(1);
                buttonGroup[i].setMaxCheckCount(1);
                for (int j = 0; j < startPositions.size; j++) {
                    String startPosName = startPositions.get(j).getName();
                    GridPoint2 startLocation = startPositions.get(j).getLocation();
                    startPosName += (" (" + (mapGrid.length - startLocation.y - 1) + ", " + (startLocation.x) + ")");

                    radioButtons[i][j] = new CheckBox(startPosName, game.skin, "radio", !alignLeft);
                    radioButtons[i][j].getImageCell().width(radioButtons[i][j].getLabel().getPrefHeight()).height(radioButtons[i][j].getLabel().getPrefHeight());
                    radioButtons[i][j].getImage().setScaling(Scaling.fill);
                    if (alignLeft) {
                        scrollTable.add(radioButtons[i][j]).left().expandX();
                    } else {
                        scrollTable.add(radioButtons[i][j]).right().expandX();
                    }
                    scrollTable.row();

                    buttonGroup[i].add(radioButtons[i][j]);

                    if (j == i % startPositions.size) {
                        radioButtons[i][j].setChecked(true);
                    }
                }

                table.add(startPosScrollPane).grow();

                final int splitScreenIndex = i;
                buttonGroup[i].setOnCheckChangeListener(button -> updatePlayerPosition(splitScreenIndex));
            }

            com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> cell = masterTable.add(table)
                    .expand().uniformX().padLeft(20f * game.getScaleFactor()).padRight(20f * game.getScaleFactor());
            if (alignLeft) {
                cell.left();
            } else {
                cell.right();

                if (startPosScrollPane != null) {
                    startPosScrollPane.layout();
                    startPosScrollPane.setScrollPercentX(1f); // I think this doesn't have width info by now which is why it's kinda buggy?
                    startPosScrollPane.updateVisualScroll();
                }
            }
        }
        masterTable.row();

        splitViewportManager.setUpDragOffset(screenStage);

        final TextButton doneButton = new TextButton("Next", game.skin);
        setDoneButtonText(doneButton);

        doneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                proceedToNext(masterTable, doneButton);
            }
        });
        masterTable.add(doneButton).expandX().colspan(splitScreenCount).width(200f * game.getScaleFactor()).pad(20f * game.getScaleFactor());

        setUpFocus(doneButton, false);

        screenStage.addActor(masterTable);
    }

    private void updatePlayerPosition(int splitScreenIndex) {
        int startPosIndex = buttonGroup[splitScreenIndex].getCheckedIndex();

        int index = splitScreenIndex + playerStartIndex;
        if (index >= players.length) {
            return;
        }

        GridPoint2 oldPos = new GridPoint2(players[index].roundedPosition);
        players[index].setPosition(startPositions.get(startPosIndex).getLocation());
        GridPoint2 newPos = players[index].roundedPosition;

        mapGrid[oldPos.y][oldPos.x].cellColor = null;
        for (Player player : players) {
            if (player.position.x == oldPos.x && player.position.y == oldPos.y) {
                mapGrid[oldPos.y][oldPos.x].cellColor = player.color;
                break;
            }
        }

        mapGrid[newPos.y][newPos.x].cellColor = players[index].color;
    }

    private void setUpFocus(TextButton doneButton, boolean focusDoneButton) {
        int excessSplitCount = Math.max(splitScreenCount + playerStartIndex - players.length, 0);

        screenStage.clearFocusableArray();

        for (int i = 0; i < radioButtons[0].length; i++) {
            for (int j = 0; j < radioButtons.length; j++) {
                if (excessSplitCount > 0 && j >= splitScreenCount - 1) {
                    break;
                }
                screenStage.addFocusableActor(radioButtons[j][i]);
            }
            screenStage.row();
        }

        screenStage.addFocusableActor(doneButton, splitScreenCount - excessSplitCount);
        if (focusDoneButton) {
            screenStage.setFocusedActor(doneButton);
        }
    }

    private void proceedToNext(Table masterTable, TextButton doneButton) {
        for (int i = 0; i < splitScreenCount; i++) {
            if (i + playerStartIndex >= players.length) {
                break;
            }
            players[i + playerStartIndex].setPosition(startPositions.get(buttonGroup[i].getCheckedIndex()).getLocation());
        }

        if (playerStartIndex >= players.length - splitScreenCount) {
            // Done with all players
            map.clearGrid(players);
            animationManager.fadeOutStage(screenStage, this, new GameScreen(game, mapData, players));
        } else {
            // Some more players are remaining
            updateNextPlayers(masterTable);
            setUpFocus(doneButton, true);
        }

        setDoneButtonText(doneButton);
    }

    private void updateNextPlayers(Table masterTable) {
        playerStartIndex += splitScreenCount;
        if (playerStartIndex >= players.length) {
            playerStartIndex = players.length - 1;
        }
        viewportDividers.updateDividerColors(players, playerStartIndex);

        for (int i = 0; i < splitScreenCount; i++) {
            if (i + playerStartIndex >= players.length) {
                // Excess splitscreens
                masterTable.removeActor(masterTable.getChild(masterTable.getChildren().size - 2));
                continue;
            }

            playerLabels[i].setText("Player " + (i + playerStartIndex + 1));
            Color labelColor = new Color(players[i + playerStartIndex].color);
            playerLabels[i].setColor(labelColor);

            radioButtons[i][(i + playerStartIndex) % radioButtons[i].length].setChecked(true);
        }
    }

    private void setDoneButtonText(TextButton doneButton) {
        if (playerStartIndex >= players.length - splitScreenCount) {
            doneButton.setText("Start Game");
        }
        // By default it's "Next"
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        splitViewportManager.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Renders each split map viewport
        splitViewportManager.render(shapeDrawer, batch, map, players, playerStartIndex, delta);

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        batch.begin();
        viewportDividers.render(shapeDrawer, screenStage);
        mapOverlay.render(shapeDrawer, screenStage, delta);
        batch.end();

        screenStage.act(delta);
        screenStage.draw();

        displayFPS();
    }

    @Override
    public void dispose() {
        super.dispose();
        map.dispose();
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

        MapEntity map = mapData.map;
        Color[] playerColors = new Color[players.length];
        for (int i = 0; i < players.length; i++) {
            playerColors[i] = new Color(players[i].color);
        }
        PlayerSetUpParams playerSetUpParams = new PlayerSetUpParams(map.getName(), map.getExtraParams(), playerColors);

        animationManager.fadeOutStage(screenStage, this, new PlayerSetUpScreen(game, playerSetUpParams));
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

