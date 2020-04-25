package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
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
        setUpStage();
        setUpBackButton();

        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);

        InputMultiplexer inputMultiplexer = new InputMultiplexer(stage, this);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpStage() {
        Table masterTable = new Table();
        masterTable.center();
        masterTable.setFillParent(true);

        Label title = new Label("Choose start positions", game.skin, "themed");
        masterTable.add(title).colspan(2).expandX();
        masterTable.row();

        int tableCount = 2;
        for (int i = 0; i < tableCount; i++) {
            Table table = new Table();
            if (i % 2 == 0) {
                table.left();
            } else {
                table.right();
            }

            Label playerLabel = new Label("Player " + (i + 1), game.skin, "themed");
            table.add(playerLabel).padBottom(10f * game.getScaleFactor());
            table.row();

            for (int j = 0; j < startPositions.size; j++) {
                String startPosName;
                try {
                    startPosName = startPosNames.get(j);
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    startPosName = Character.toString((char) (j + MapManager.VALID_START_POSITIONS.charAt(0)));
                }
                startPosName += (" (" + (mapGrid.length - startPositions.get(j).y - 1) + ", " + (startPositions.get(j).x) + ")");

                Label startPosNameLabel = new Label(startPosName, game.skin);
                table.add(startPosNameLabel);
                table.row();
            }

            masterTable.add(table).expand().uniformX().pad(10f * game.getScaleFactor());
        }

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

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
