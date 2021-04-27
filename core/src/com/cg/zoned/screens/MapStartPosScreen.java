package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.GameTouchPoint;
import com.cg.zoned.dataobjects.PlayerSetUpParams;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.ui.ButtonGroup;
import com.cg.zoned.ui.CheckBox;
import com.cg.zoned.ui.HoverImageButton;

public class MapStartPosScreen extends ScreenObject implements InputProcessor {
    private Cell[][] mapGrid;
    private Array<GridPoint2> startPositions;
    private Array<String> startPosNames;

    private MapManager mapManager;
    private Map map;
    private Vector2[] dragOffset;
    private Color mapDarkOverlayColor;
    private ExtendViewport[] mapViewports;
    private Color[] dividerLeftColor, dividerRightColor;
    private int splitScreenCount;

    private Player[] players;
    private CheckBox[][] radioButtons;
    private ButtonGroup[] buttonGroup;
    private Label[] playerLabels;
    private int playerIndex;

    public MapStartPosScreen(final Zoned game, MapManager mapManager, Player[] players, int splitScreenCount) {
        super(game);
        game.discordRPCManager.updateRPC("Choosing start positions");

        this.mapManager = mapManager;
        this.mapGrid = mapManager.getPreparedMapGrid();
        this.startPositions = mapManager.getPreparedStartPositions();
        this.startPosNames = mapManager.getPreparedStartPosNames();
        this.players = players;
        this.splitScreenCount = splitScreenCount;

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
        mapDarkOverlayColor = new Color(0, 0, 0, 0.8f);
        mapViewports = new ExtendViewport[splitScreenCount];
        for (int i = 0; i < players.length; i++) {
            players[i].setStartPos(startPositions.get(i % startPositions.size));
            mapGrid[(int) players[i].position.y][(int) players[i].position.x].cellColor = players[i].color;
        }
        for (int i = 0; i < splitScreenCount; i++) {
            mapViewports[i] = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        }
        playerIndex = 0;
        if (splitScreenCount > 1) {
            dividerLeftColor = new Color[splitScreenCount - 1];
            dividerRightColor = new Color[splitScreenCount - 1];
            updateDividerColors(playerIndex);
        }
    }

    private void updateDividerColors(int playerIndex) {
        for (int i = 0; i < splitScreenCount - 1; i++) {
            if (i + playerIndex < players.length) {
                dividerLeftColor[i] = new Color(players[i + playerIndex].color);
            } else {
                dividerLeftColor[i] = Color.BLACK;
            }

            if (i + playerIndex + 1 < players.length) {
                dividerRightColor[i] = new Color(players[i + playerIndex + 1].color);
            } else {
                dividerRightColor[i] = Color.BLACK;
            }
        }
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
                    String startPosName;
                    try {
                        startPosName = startPosNames.get(j);
                    } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                        startPosName = Character.toString((char) (j + MapManager.VALID_START_POSITIONS.charAt(0)));
                    }
                    startPosName += (" (" + (mapGrid.length - startPositions.get(j).y - 1) + ", " + (startPositions.get(j).x) + ")");

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

                final int finalI = i;
                buttonGroup[i].setOnCheckChangeListener(new ButtonGroup.OnCheckChangeListener() {
                    @Override
                    public void buttonPressed(Button button) {
                        int startPosIndex = buttonGroup[finalI].getCheckedIndex();

                        int index = finalI + playerIndex;
                        if (index >= players.length) {
                            return;
                        }

                        int oldPosX = (int) players[index].position.x;
                        int oldPosY = (int) players[index].position.y;

                        players[index].position.y = startPositions.get(startPosIndex).y;
                        players[index].position.x = startPositions.get(startPosIndex).x;

                        mapGrid[oldPosY][oldPosX].cellColor = null;
                        for (Player player : players) {
                            if (player.position.x == oldPosX && player.position.y == oldPosY) {
                                mapGrid[oldPosY][oldPosX].cellColor = player.color;
                                break;
                            }
                        }

                        mapGrid[(int) players[index].position.y][(int) players[index].position.x].cellColor = players[index].color;
                    }
                });
            }

            com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> cell = masterTable.add(table)
                    .expand().uniformX().padLeft(20f * game.getScaleFactor()).padRight(10f * game.getScaleFactor());
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

        final GameTouchPoint[] touchPoint = new GameTouchPoint[splitScreenCount];
        dragOffset = new Vector2[splitScreenCount];
        for (int i = 0; i < splitScreenCount; i++) {
            dragOffset[i] = new Vector2(0, 0);
            touchPoint[i] = new GameTouchPoint(0, 0, -1, null, -1);
        }
        screenStage.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int splitPaneIndex = 0;
                float width = screenStage.getViewport().getWorldWidth();
                for (int i = 1; i < splitScreenCount; i++) {
                    if (x > ((width / splitScreenCount) * i)) {
                        splitPaneIndex++;
                    } else {
                        break;
                    }
                }

                if (touchPoint[splitPaneIndex].pointer == -1) {
                    touchPoint[splitPaneIndex].pointer = pointer;
                    touchPoint[splitPaneIndex].point.x = (int) x;
                    touchPoint[splitPaneIndex].point.y = (int) y;
                }

                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                for (int i = 0; i < splitScreenCount; i++) {
                    if (touchPoint[i].pointer == pointer) {
                        dragOffset[i].x = touchPoint[i].point.x - x;
                        dragOffset[i].y = touchPoint[i].point.y - y;
                        break;
                    }
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                for (int i = 0; i < splitScreenCount; i++) {
                    if (touchPoint[i].pointer == pointer) {
                        dragOffset[i].set(0, 0);
                        touchPoint[i].pointer = -1;
                        break;
                    }
                }
            }
        });

        final TextButton doneButton = new TextButton("Next", game.skin);
        setDoneButtonText(doneButton);

        final Screen thisScreen = this;
        doneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                for (int i = 0; i < splitScreenCount; i++) {
                    if (i + playerIndex < players.length) {
                        players[i + playerIndex].setStartPos(startPositions.get(buttonGroup[i].getCheckedIndex()));
                    }
                }

                if (playerIndex >= players.length - splitScreenCount) {
                    // Done with all players
                    for (Player player : players) {
                        mapGrid[(int) player.position.y][(int) player.position.x].cellColor = null;
                    }
                    animationManager.fadeOutStage(screenStage, thisScreen, new GameScreen(game, mapManager, players));
                } else {
                    // Some more players are remaining
                    playerIndex += splitScreenCount;
                    if (playerIndex >= players.length) {
                        playerIndex = players.length - 1;
                    }
                    updateDividerColors(playerIndex);
                    int excessCount = 0;
                    for (int i = 0; i < splitScreenCount; i++) {
                        if (i + playerIndex >= players.length) {
                            // Excess splitscreens
                            excessCount++;
                            masterTable.removeActor(masterTable.getChild(masterTable.getChildren().size - 2));
                            continue;
                        }

                        playerLabels[i].setText("Player " + (i + playerIndex + 1));
                        Color labelColor = new Color(players[i + playerIndex].color);
                        playerLabels[i].setColor(labelColor);

                        radioButtons[i][(i + playerIndex) % radioButtons[i].length].setChecked(true);
                    }

                    if (excessCount > 0) {
                        screenStage.clearFocusableArray();
                        for (int i = 0; i < radioButtons[0].length; i++) {
                            for (int j = 0; j < radioButtons.length; j++) {
                                if (j >= excessCount) {
                                    break;
                                }
                                screenStage.addFocusableActor(radioButtons[j][i]);
                            }
                            screenStage.row();
                        }
                        screenStage.addFocusableActor(doneButton, splitScreenCount - excessCount);
                    }
                }

                setDoneButtonText(doneButton);
            }
        });
        masterTable.add(doneButton).expandX().colspan(splitScreenCount).width(200f * game.getScaleFactor()).pad(20f * game.getScaleFactor());

        int excess = 0;
        for (int i = 0; i < radioButtons[0].length; i++) {
            for (int j = 0; j < radioButtons.length; j++) {
                if (radioButtons[j] == null) {
                    excess = j - radioButtons.length;
                    break;
                }
                screenStage.addFocusableActor(radioButtons[j][i]);
            }
            screenStage.row();
        }
        screenStage.addFocusableActor(doneButton, splitScreenCount - excess);

        screenStage.addActor(masterTable);
    }

    private void setDoneButtonText(TextButton doneButton) {
        if (playerIndex >= players.length - splitScreenCount) {
            doneButton.setText("Start Game");
        }
        // By default it's "Next"
    }

    private void renderMap(int playerIndex, float delta) {
        int mapViewportIndex = playerIndex - this.playerIndex;
        Viewport viewport = mapViewports[mapViewportIndex];

        focusCameraOnPlayer(viewport, players[playerIndex], dragOffset[mapViewportIndex], delta);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        map.render(players, playerIndex, shapeDrawer, (OrthographicCamera) viewport.getCamera(), delta);
        batch.end();
    }

    private void focusCameraOnPlayer(Viewport viewport, Player player, Vector2 vel, float delta) {
        OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();

        float lerp = 2.5f;
        Vector3 position = camera.position;

        float posX = (player.position.x * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;

        posX += vel.x;
        posY += vel.y;

        position.x += (posX - position.x) * lerp * delta;
        position.y += (posY - position.y) * lerp * delta;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        for (int i = 0; i < mapViewports.length; i++) {
            mapViewports[i].update(width / mapViewports.length, height);
            this.mapViewports[i].setScreenX(i * width / mapViewports.length);
        }
    }

    private void drawViewportDividers() {
        batch.begin();
        int lineCount = splitScreenCount - 1;
        float dividerFadeWidth = Math.max(Constants.VIEWPORT_DIVIDER_FADE_WIDTH / (mapViewports.length - 1), 3f);
        float dividerSolidWidth = Math.max(Constants.VIEWPORT_DIVIDER_SOLID_WIDTH / (mapViewports.length - 1), 1f);
        float height = screenStage.getViewport().getWorldHeight();
        for (int i = 0; i < lineCount; i++) {
            float startX = (screenStage.getViewport().getWorldWidth() / (float) (lineCount + 1)) * (i + 1);

            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    dividerSolidWidth * 2, height,
                    dividerRightColor[i], dividerLeftColor[i], dividerLeftColor[i], dividerRightColor[i]);
            shapeDrawer.filledRectangle(startX + dividerSolidWidth, 0,
                    dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, dividerRightColor[i], dividerRightColor[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
            shapeDrawer.filledRectangle(startX - dividerSolidWidth, 0,
                    -dividerFadeWidth, height,
                    Constants.VIEWPORT_DIVIDER_FADE_COLOR, dividerLeftColor[i], dividerLeftColor[i], Constants.VIEWPORT_DIVIDER_FADE_COLOR);
        }
        batch.end();
    }

    private void drawDarkOverlay() {
        float height = screenStage.getViewport().getWorldHeight();
        float width = screenStage.getViewport().getWorldWidth();
        shapeDrawer.setColor(mapDarkOverlayColor);
        batch.begin();
        shapeDrawer.filledRectangle(0, 0, width, height);
        batch.end();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < mapViewports.length; i++) {
            if (playerIndex + i < players.length) {
                renderMap(playerIndex + i, delta);
            }
        }

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        if (splitScreenCount > 1 && mapViewports.length >= 2) {
            drawViewportDividers();
        }
        drawDarkOverlay();

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

        MapEntity map = mapManager.getPreparedMap();
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

