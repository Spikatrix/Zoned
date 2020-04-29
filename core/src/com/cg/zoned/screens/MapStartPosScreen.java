package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.GameTouchPoint;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.CustomButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

public class MapStartPosScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private Cell[][] mapGrid;
    private Array<GridPoint2> startPositions;
    private Array<String> startPosNames;

    private ScreenViewport viewport;
    private FocusableStage stage;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private boolean showFPSCounter;

    private Map map;
    private Vector2[] dragOffset;
    private Color mapDarkOverlayColor;
    private ExtendViewport[] mapViewports;
    private int splitScreenCount;
    private boolean firstPlayerOnly;
    private Player[] players;
    private Label[] playerLabels;
    private int playerIndex;

    public MapStartPosScreen(final Zoned game, Cell[][] mapGrid, Array<GridPoint2> startPositions, Array<String> startPosNames,
                             Player[] players, int splitScreenCount, boolean firstPlayerOnly) {
        this.game = game;

        this.mapGrid = mapGrid;
        this.startPositions = startPositions;
        this.startPosNames = startPosNames;
        this.players = players;
        this.splitScreenCount = splitScreenCount;
        this.firstPlayerOnly = firstPlayerOnly;

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpMap();
        setUpStage();
        setUpBackButton();

        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        InputMultiplexer inputMultiplexer = new InputMultiplexer(stage, this);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpMap() {
        map = new Map(mapGrid, startPositions, 0); // Wall count is unnecessary in this case so 0
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
    }

    private void setUpStage() {
        final Table masterTable = new Table();
        masterTable.center();
        masterTable.setFillParent(true);

        Label title = new Label("Choose start positions", game.skin, "themed");
        masterTable.add(title).colspan(splitScreenCount).expandX().pad(20f * game.getScaleFactor());
        masterTable.row();

        playerLabels = new Label[splitScreenCount];
        for (int i = 0; i < splitScreenCount; i++) {
            Table table = new Table();

            final boolean alignLeft = (i < (splitScreenCount / 2));
            if (i < players.length) {
                playerLabels[i] = new Label("Player " + (i + 1), game.skin, "custom-color");
                Color labelColor = new Color(players[i].color);
                labelColor.mul(10);
                playerLabels[i].setColor(labelColor);
                if (alignLeft) {
                    table.add(playerLabels[i]).padBottom(10f * game.getScaleFactor()).left().expandX();
                } else {
                    table.add(playerLabels[i]).padBottom(10f * game.getScaleFactor()).right().expandX();
                }
                table.row();

                Table scrollTable = new Table();
                ScrollPane startPosScrollPane = new ScrollPane(scrollTable);
                startPosScrollPane.setOverscroll(false, true);

                final CustomButtonGroup buttonGroup = new CustomButtonGroup();
                buttonGroup.setMinCheckCount(1);
                buttonGroup.setMaxCheckCount(1);
                for (int j = 0; j < startPositions.size; j++) {
                    String startPosName;
                    try {
                        startPosName = startPosNames.get(j);
                    } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                        startPosName = Character.toString((char) (j + MapManager.VALID_START_POSITIONS.charAt(0)));
                    }
                    startPosName += (" (" + (mapGrid.length - startPositions.get(j).y - 1) + ", " + (startPositions.get(j).x) + ")");

                    CheckBox startPosCheckBox = new CheckBox(startPosName, game.skin, "radio");
                    startPosCheckBox.getImageCell().width(startPosCheckBox.getLabel().getPrefHeight()).height(startPosCheckBox.getLabel().getPrefHeight());
                    startPosCheckBox.getImage().setScaling(Scaling.fill);
                    if (alignLeft) {
                        scrollTable.add(startPosCheckBox).left().expandX();
                    } else {
                        scrollTable.add(startPosCheckBox).right().expandX();
                    }
                    scrollTable.row();

                    buttonGroup.add(startPosCheckBox);

                    if (j == i % startPositions.size) {
                        startPosCheckBox.setChecked(true);
                    }
                }

                table.add(startPosScrollPane);

                final int finalI = i;
                buttonGroup.setOnCheckChangeListener(new CustomButtonGroup.OnCheckChangeListener() {
                    @Override
                    public void buttonPressed(Button button) {
                        int startPosIndex = buttonGroup.getCheckedIndex();

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

            if (alignLeft) {
                masterTable.add(table).expand().uniformX().left().padLeft(20f * game.getScaleFactor());
            } else {
                masterTable.add(table).expand().uniformX().right().padRight(20f * game.getScaleFactor());
            }
        }
        masterTable.row();

        final GameTouchPoint[] touchPoint = new GameTouchPoint[splitScreenCount];
        dragOffset = new Vector2[splitScreenCount];
        for (int i = 0; i < splitScreenCount; i++) {
            dragOffset[i] = new Vector2(0, 0);
            touchPoint[i] = new GameTouchPoint(0, 0, -1, null, -1);
        }
        stage.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int splitPaneIndex = 0;
                float width = stage.getViewport().getWorldWidth();
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
        TextButton doneButton = new TextButton("Done", game.skin);
        doneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Set player's start pos here

                if (playerIndex == players.length - splitScreenCount) {
                    onBackPressed(); // Done
                } /*else if (playerIndex + (2 * splitScreenCount) <= players.length) {
                    playerIndex += splitScreenCount;
                    for (int i = 0; i < splitScreenCount; i++) {
                        playerLabels[i].setText("Player " + (i + playerIndex + 1));
                        Color labelColor = new Color(players[i + playerIndex].color);
                        labelColor.mul(10);
                        playerLabels[i].setColor(labelColor);
                    }
                }*/ else {
                    // ok, I need some sleep. TODO: This. Tomorrow
                    int offset = Math.min(players.length - splitScreenCount, splitScreenCount);
                    playerIndex += offset;
                    for (int i = 0; i < splitScreenCount - offset; i++) {
                        playerLabels[i].setText("Player " + (i + playerIndex + 1));
                        Color labelColor = new Color(players[i + playerIndex].color);
                        labelColor.mul(10);
                        playerLabels[i].setColor(labelColor);
                    }
                    for (int i = 0; i < offset; i++) {
                        masterTable.removeActor(masterTable.getChild(masterTable.getChildren().size - 2));
                    }
                }
            }
        });
        masterTable.add(doneButton).expandX().colspan(splitScreenCount).width(200f * game.getScaleFactor()).pad(20f * game.getScaleFactor());

        stage.addActor(masterTable);
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

    private void focusAndRenderViewport(Viewport viewport, Player player, Vector2 vel, float delta) {
        focusCameraOnPlayer(viewport, player, vel, delta);
        viewport.apply();

        renderer.setProjectionMatrix(viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        map.render(players, renderer, (OrthographicCamera) viewport.getCamera(), delta);
        renderer.end();
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
        stage.resize(width, height);

        for (int i = 0; i < mapViewports.length; i++) {
            mapViewports[i].update(width / mapViewports.length, height);
            updateCamera(mapViewports[i].getCamera(), width / mapViewports.length, height);
            this.mapViewports[i].setScreenX(i * width / mapViewports.length);
        }
    }

    private void updateCamera(Camera camera, int width, int height) {
        camera.viewportHeight = Constants.WORLD_SIZE;
        camera.viewportWidth = Constants.WORLD_SIZE * height / width;
        camera.update();
    }

    private void drawViewportDividers() {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        int lineCount = splitScreenCount;

        float height = stage.getViewport().getWorldHeight();
        for (int i = 1; i < lineCount; i++) {
            float startX = (stage.getViewport().getWorldWidth() / (float) lineCount) * i;

            renderer.rect(startX - Constants.VIEWPORT_DIVIDER_SOLID_WIDTH, 0,
                    Constants.VIEWPORT_DIVIDER_SOLID_WIDTH * 2, height,
                    Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
            renderer.rect(startX + Constants.VIEWPORT_DIVIDER_SOLID_WIDTH, 0,
                    Constants.VIEWPORT_DIVIDER_FADE_WIDTH, height,
                    Color.BLACK, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK);
            renderer.rect(startX - Constants.VIEWPORT_DIVIDER_SOLID_WIDTH, 0,
                    -Constants.VIEWPORT_DIVIDER_FADE_WIDTH, height,
                    Color.BLACK, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.BLACK);
        }
        renderer.end();
    }

    private void drawDarkOverlay() {
        float height = stage.getViewport().getWorldHeight();
        float width = stage.getViewport().getWorldWidth();
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(mapDarkOverlayColor);
        renderer.rect(0, 0, width, height);
        renderer.end();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < mapViewports.length && i < players.length; i++) {
            focusAndRenderViewport(mapViewports[i], players[i], dragOffset[i], delta);
        }

        this.viewport.apply(true);
        renderer.setProjectionMatrix(this.viewport.getCamera().combined);

        if (splitScreenCount > 1) {
            drawViewportDividers();
        }
        drawDarkOverlay();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        stage.act(delta);
        stage.draw();

        renderer.setProjectionMatrix(this.viewport.getCamera().combined);
        batch.setProjectionMatrix(this.viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.end();

        batch.begin();
        batch.end();

        if (showFPSCounter) {
            FPSDisplayer.displayFPS(viewport, batch, font);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        renderer.dispose();
        batch.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        dispose();
        game.setScreen(new MainMenuScreen(game));
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
