package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.CustomButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

public class MapStartPosScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private int splitParts = 2;

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
    private ExtendViewport[] mapViewports;
    private Player[] players;

    public MapStartPosScreen(final Zoned game, Cell[][] mapGrid, Array<GridPoint2> startPositions, Array<String> startPosNames) {
        this.game = game;

        this.mapGrid = mapGrid;
        this.startPositions = startPositions;
        this.startPosNames = startPosNames;

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
        mapViewports = new ExtendViewport[splitParts];
        players = new Player[splitParts];
        for (int i = 0; i < splitParts; i++) {
            mapViewports[i] = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
            players[i] = new Player(PlayerColorHelper.getColorFromIndex(i), "name");
            players[i].setStartPos(startPositions.get(i % startPositions.size));
            mapGrid[(int) players[i].position.y][(int) players[i].position.x].cellColor = players[i].color;
        }
    }

    private void setUpStage() {
        Table masterTable = new Table();
        masterTable.center();
        masterTable.setFillParent(true);

        Label title = new Label("Choose start positions", game.skin, "themed");
        masterTable.add(title).colspan(2).expandX().pad(20f * game.getScaleFactor());
        masterTable.row();

        for (int i = 0; i < splitParts; i++) {
            Table table = new Table();

            boolean alignLeft = (i % 2 == 0);

            Label playerLabel = new Label("Player " + (i + 1), game.skin, "themed");
            if (alignLeft) {
                table.add(playerLabel).padBottom(10f * game.getScaleFactor()).left().expandX().colspan(2);
            } else {
                table.add(playerLabel).padBottom(10f * game.getScaleFactor()).right().expandX().colspan(2);
            }
            table.row();

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
                    table.add(startPosCheckBox).left().expandX();
                } else {
                    table.add(startPosCheckBox).right().expandX();
                }
                table.row();

                buttonGroup.add(startPosCheckBox);

                if (j == i % startPositions.size) {
                    startPosCheckBox.setChecked(true);
                }
            }

            final int finalI = i;
            buttonGroup.setOnCheckChangeListener(new CustomButtonGroup.OnCheckChangeListener() {
                @Override
                public void buttonPressed(Button button) {
                    int startPosIndex = buttonGroup.getCheckedIndex();

                    int oldPosX = (int) players[finalI].position.x;
                    int oldPosY = (int) players[finalI].position.y;

                    players[finalI].position.y = startPositions.get(startPosIndex).y;
                    players[finalI].position.x = startPositions.get(startPosIndex).x;

                    mapGrid[oldPosY][oldPosX].cellColor = null;
                    for (Player player : players) {
                        if (player.position.x == oldPosX && player.position.y == oldPosY) {
                            mapGrid[oldPosY][oldPosX].cellColor = player.color;
                            break;
                        }
                    }

                    mapGrid[(int) players[finalI].position.y][(int) players[finalI].position.x].cellColor = players[finalI].color;
                }
            });

            if (alignLeft) {
                masterTable.add(table).expand().uniformX().left().padLeft(20f * game.getScaleFactor());
            } else {
                masterTable.add(table).expand().uniformX().right().padRight(20f * game.getScaleFactor());
            }
        }
        masterTable.row();

        TextButton doneButton = new TextButton("Done", game.skin);
        masterTable.add(doneButton).expandX().colspan(2).width(200f * game.getScaleFactor()).pad(20f * game.getScaleFactor());

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

    private void focusAndRenderViewport(Viewport viewport, Player player, float delta) {
        focusCameraOnPlayer(viewport, player, delta);
        viewport.apply();

        renderer.setProjectionMatrix(viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        map.render(players, renderer, (OrthographicCamera) viewport.getCamera(), delta);
        renderer.end();
    }

    private void focusCameraOnPlayer(Viewport viewport, Player player, float delta) {
        OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();

        float lerp = 2.5f;
        Vector3 position = camera.position;

        float posX = (player.position.x * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;
        float posY = (player.position.y * Constants.CELL_SIZE) + Constants.CELL_SIZE / 2.0f;

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

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < mapViewports.length; i++) {
            focusAndRenderViewport(mapViewports[i], players[i], delta);
        }

        this.viewport.apply(true);

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
