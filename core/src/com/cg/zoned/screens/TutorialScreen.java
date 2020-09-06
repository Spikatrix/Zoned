package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.TutorialItem;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ControlManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

import java.util.Random;

public class TutorialScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ScreenViewport viewport;
    private FocusableStage stage;
    private Table tutorialTable;
    private AnimationManager animationManager;
    private BitmapFont font;
    private boolean showFPSCounter;
    private Label dummyLabel;

    private ShapeDrawer shapeDrawer;
    private SpriteBatch batch;
    private Map map;
    private Cell[][] mapGrid;
    private ExtendViewport mapViewport;
    private Color mapOverlayColor;
    private Color mapDarkOverlayColor;
    private Color mapNoOverlayColor;
    private boolean drawOverlay;
    private Player[] players;
    private BitmapFont playerLabelFont;
    private ControlManager controlManager;

    public TutorialScreen(final Zoned game) {
        this.game = game;
        game.discordRPCManager.updateRPC("Playing the Tutorial");

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(game, this);
        this.font = game.skin.getFont(Assets.FontManager.SMALL.getFontName());

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, usedTextures);

        this.dummyLabel = new Label("DUMMY", game.skin); // Used to set the height of the tutorial table

        initMap();
    }

    private void initMap() {
        populateMapGrid();
        this.map = new Map(mapGrid, 0, shapeDrawer);
        this.mapViewport = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        this.mapOverlayColor = new Color(0, 0, 0, .8f);
        this.mapDarkOverlayColor = new Color(0, 0, 0, .8f);
        this.mapNoOverlayColor = new Color(0, 0, 0, 0f);
        this.drawOverlay = true;
        this.players = new Player[1];
        this.players[0] = new Player(PlayerColorHelper.getColorFromString("GREEN"), "Player");
        this.players[0].position = new Vector2(Math.round(this.mapGrid.length / 2f), Math.round(this.mapGrid[0].length / 2f));
        this.players[0].setControlIndex(0);
        this.playerLabelFont = game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName());
        this.map.createPlayerLabelTextures(this.players, shapeDrawer, playerLabelFont);
        this.controlManager = new ControlManager(players, stage);
        this.controlManager.setUpControls(game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0),
                false, game.getScaleFactor(), usedTextures);
    }

    private void populateMapGrid() {
        int rowCount = 20;
        int colCount = 20;

        mapGrid = new Cell[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                mapGrid[i][j] = new Cell();
            }
        }
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        showFPSCounter = game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false);

        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        tutorialTable = new Table();
        tutorialTable.setSize(stage.getWidth(), (dummyLabel.getPrefHeight() * 2) + 20f);
        tutorialTable.setPosition(0, 0);
        tutorialTable.left().bottom().pad(10f);

        Table innerTable = new Table();
        final ScrollPane scrollPane = new ScrollPane(innerTable);
        scrollPane.setOverscroll(false, false);

        final Label mainLabel = new Label("Welcome to the tutorial!", game.skin, "themed");
        innerTable.add(mainLabel).grow();
        innerTable.row();

        final Label subLabel = new Label("Tap here to continue >", game.skin);
        innerTable.add(subLabel).grow();
        innerTable.row();

        final int[] tutorialPromptIndex = {0};
        final Array<TutorialItem> tutorialPrompts = new Array<>();
        tutorialPrompts.add(new TutorialItem(
                "Every game is played in a grid based map",
                "Each player is represented by a circle",
                false));

        tutorialPrompts.add(new TutorialItem(
                "Try moving around the grid (Tap here when done)",
                ((Gdx.app.getType() == Application.ApplicationType.Android) ?
                        ("You can move by using the touchscreen") :
                        ("You can move by using the mouse or the keyboard (WASD)")),
                true));

        tutorialPrompts.add(new TutorialItem(
                "The main objective is to capture as many cells as you can",
                "You can capture cells by moving onto black (uncaptured) cells",
                false));

        tutorialPrompts.add(new TutorialItem(
                "Try capturing a group of cells at once (Tap here when done)",
                "You can do that by surrounding a group of cells",
                true));

        tutorialPrompts.add(new TutorialItem(
                "Surrounded cells are captured based on certain rules",
                "1. Every color in the border of the surrounded region is the player's color",
                false));

        tutorialPrompts.add(new TutorialItem(
                "Surrounded cells are captured based on certain rules",
                "2. Every cell interior to the region is either empty (black) or the player's color",
                false));

        tutorialPrompts.add(new TutorialItem(
                "Walls may be present on certain maps and they block players",
                "Walls are white in color",
                false));

        tutorialPrompts.add(new TutorialItem(
                "That's basically it!",
                "Now go have fun competing with friends and assert your dominance!",
                true));

        tutorialPrompts.add(new TutorialItem(
                "Thank you for playing the tutorial!",
                "Tap here to finish",
                true));

        tutorialTable.setTouchable(Touchable.enabled);
        tutorialTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tutorialTable.clearActions();

                players[0].updatedDirection = players[0].direction = null;
                players[0].position.x = Math.round(players[0].position.x);
                players[0].position.y = Math.round(players[0].position.y);

                if (mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor == null) {
                    mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor =
                            new Color(players[0].color.r, players[0].color.g, players[0].color.b, 0.1f);
                }

                if (tutorialPromptIndex[0] == tutorialPrompts.size) {
                    togglePlayerInterable(false);
                    animationManager.fadeOutStage(stage, TutorialScreen.this, new MainMenuScreen(game));
                    return;
                }

                if (!scrollPane.isRightEdge()) {
                    scrollPane.scrollTo(scrollPane.getScrollX() + scrollPane.getWidth(), 0,
                            scrollPane.getWidth(), scrollPane.getHeight());
                    return;
                } else {
                    scrollPane.scrollTo(0, 0, scrollPane.getWidth(), scrollPane.getHeight());
                }

                tutorialTable.addAction(Actions.sequence(
                        Actions.fadeOut(.2f, Interpolation.fastSlow),
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                String mainText = tutorialPrompts.get(tutorialPromptIndex[0]).mainItem;
                                if (mainText.contains("Walls")) {
                                    generateRandomWalls();
                                    map.createMapTexture(shapeDrawer);
                                }

                                displayNextTutorialText(mainLabel, subLabel, mainText, tutorialPrompts.get(tutorialPromptIndex[0]).subItem);
                                togglePlayerInterable(tutorialPrompts.get(tutorialPromptIndex[0]).enablePlayerInteraction);

                                tutorialPromptIndex[0]++;
                            }
                        }),
                        Actions.fadeIn(.2f, Interpolation.fastSlow)
                ));
            }
        });

        tutorialTable.add(scrollPane);
        stage.addActor(tutorialTable);
        stage.setScrollFocus(scrollPane);
    }

    private void generateRandomWalls() {
        int rowCount = mapGrid.length;
        int colCount = mapGrid[0].length;

        int playerPosX = Math.round(players[0].position.x);
        int playerPosY = Math.round(players[0].position.y);

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            int randomY = rand.nextInt(rowCount);
            int randomX = rand.nextInt(colCount);

            if (randomX != playerPosX && randomY != playerPosY) {
                mapGrid[randomY][randomX].cellColor = null;
                mapGrid[randomY][randomX].isMovable = false;
            }
        }
    }

    private void displayNextTutorialText(Label mainLabel, Label subLabel, String mainText, String subText) {
        mainLabel.setText(mainText);
        subLabel.setText(subText);
    }

    private void togglePlayerInterable(boolean playerInteractable) {
        this.drawOverlay = !playerInteractable;

        InputMultiplexer inputMultiplexer = new InputMultiplexer(this, stage);
        if (playerInteractable) {
            inputMultiplexer.addProcessor(players[0]);
            inputMultiplexer.addProcessor(controlManager.getControls());
        }

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getBackButtonTexture());
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

        tutorialTable.setSize(width, (dummyLabel.getPrefHeight() * 2) + 20f);
        mapViewport.update(width, (int) Math.max(0, height - ((dummyLabel.getPrefHeight() * 2) + 20f)));
        updateCamera(mapViewport.getCamera(), width, (int) Math.max(0, height - ((dummyLabel.getPrefHeight() * 2) + 20f)));
        mapViewport.setScreenPosition(0, (int) ((dummyLabel.getPrefHeight() * 2) + 20f));
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

        players[0].direction = players[0].updatedDirection;
        map.update(null, players, delta);

        renderMap(delta);

        this.viewport.apply(true);
        batch.setProjectionMatrix(this.viewport.getCamera().combined);

        drawDarkOverlay(delta);

        batch.begin();
        shapeDrawer.filledRectangle(0, (dummyLabel.getPrefHeight() * 2) + 20f, stage.getWidth(), 8f,
                Constants.VIEWPORT_DIVIDER_FADE_COLOR, Constants.VIEWPORT_DIVIDER_FADE_COLOR, Color.WHITE, Color.WHITE);
        batch.end();

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.act(delta);
        stage.draw();
    }

    private void renderMap(float delta) {
        Viewport viewport = mapViewport;
        Player player = players[0];

        focusCameraOnPlayer(viewport, player, delta);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        map.render(players, shapeDrawer, (OrthographicCamera) viewport.getCamera(), delta);
        batch.end();
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

    private void drawDarkOverlay(float delta) {
        if (drawOverlay) {
            mapOverlayColor.lerp(mapDarkOverlayColor, 1.8f * delta);
        } else {
            mapOverlayColor.lerp(mapNoOverlayColor, 1.8f * delta);
        }

        float height = stage.getViewport().getWorldHeight();
        float width = stage.getViewport().getWorldWidth();
        batch.begin();
        shapeDrawer.setColor(mapOverlayColor);
        shapeDrawer.filledRectangle(0, 0, width, height);
        batch.end();
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
        map.dispose();
    }

    private void onBackPressed() {
        togglePlayerInterable(false);
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
