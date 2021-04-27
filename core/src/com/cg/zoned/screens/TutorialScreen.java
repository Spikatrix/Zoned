package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.TutorialItem;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ControlManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.HoverImageButton;

import java.util.Random;

public class TutorialScreen extends ScreenObject implements InputProcessor {
    private Map map;
    private Cell[][] mapGrid;
    private ExtendViewport mapViewport;
    private Color mapOverlayColor;
    private Color mapDarkOverlayColor;
    private Color mapNoOverlayColor;
    private boolean drawOverlay;
    private Player[] players;
    private ControlManager controlManager;

    private Table tutorialTable;
    private ScrollPane tutorialScrollPane;
    private Label mainLabel;
    private Label subLabel;
    private float textboxHeight;
    private Array<TutorialItem> tutorialPrompts;
    private int tutorialPromptIndex = 0;

    public TutorialScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Playing the Tutorial");

        this.animationManager = new AnimationManager(game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);

        initMap();
    }

    private void initMap() {
        populateMapGrid();
        this.map = new Map(mapGrid, 0, shapeDrawer);
        this.map.initFloodFillVars();
        this.mapViewport = new ExtendViewport(Constants.WORLD_SIZE, Constants.WORLD_SIZE);
        this.mapOverlayColor = new Color(0, 0, 0, .8f);
        this.mapDarkOverlayColor = new Color(0, 0, 0, .8f);
        this.mapNoOverlayColor = new Color(0, 0, 0, 0f);
        this.drawOverlay = true;
        this.players = new Player[1];
        this.players[0] = new Player(PlayerColorHelper.getColorFromString("GREEN"), "Player");
        this.players[0].position = new Vector2(Math.round(this.mapGrid.length / 2f), Math.round(this.mapGrid[0].length / 2f));
        this.players[0].setRoundedPosition();
        this.players[0].setControlIndex(0);
        BitmapFont playerLabelFont = game.skin.getFont(Assets.FontManager.PLAYER_LABEL_NOSCALE.getFontName());
        this.map.createPlayerLabelTextures(this.players, shapeDrawer, playerLabelFont);
        this.controlManager = new ControlManager(players, screenStage);
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

        animationManager.fadeInStage(screenStage);
    }

    private void setUpStage() {
        mainLabel = new Label("Welcome to the tutorial!", game.skin, "themed");
        subLabel = new Label("Tap here to continue >", game.skin);
        textboxHeight = (mainLabel.getPrefHeight() * 2) + 20f;

        tutorialTable = new Table();
        tutorialTable.setSize(screenStage.getWidth(), textboxHeight);
        tutorialTable.setPosition(0, 0);
        tutorialTable.left().bottom().pad(10f);

        Table innerTable = new Table();
        tutorialScrollPane = new ScrollPane(innerTable);
        tutorialScrollPane.setOverscroll(false, false);

        innerTable.add(mainLabel).grow();
        innerTable.row();
        innerTable.add(subLabel).grow();
        innerTable.row();

        tutorialPromptIndex = 0;
        tutorialPrompts = new Array<>();
        tutorialPrompts.addAll(
                new TutorialItem(
                        "Every game is played in a grid based map",
                        "Each player is represented by a circle",
                        false),

                new TutorialItem(
                        "Try moving around the grid (Tap here when done)",
                        ((Gdx.app.getType() == Application.ApplicationType.Android) ?
                                ("You can move by using the touchscreen") :
                                ("You can move by using the mouse or the keyboard (WASD)")),
                        true),

                new TutorialItem(
                        "The main objective is to capture as many cells as you can",
                        "You can capture cells by moving onto black (uncaptured) cells",
                        false),

                new TutorialItem(
                        "You can surround a group of cells to capture them all at once",
                        "Try it out (Tap here when done)",
                        true),

                new TutorialItem(
                        "Surrounded cells are captured based on certain rules",
                        "1. Every color in the border of the surrounded region is the player's color",
                        false),

                new TutorialItem(
                        "Surrounded cells are captured based on certain rules",
                        "2. Every cell interior to the region is either empty (black) or the player's color",
                        false),

                new TutorialItem(
                        "Walls may be present on certain maps and they block players",
                        "Cells in the map with a wall are white in color",
                        false),

                new TutorialItem(
                        "That's basically it!",
                        "Now go have fun competing with friends and assert your dominance!",
                        true),

                new TutorialItem(
                        "Thank you for playing the tutorial!",
                        "Tap here to finish",
                        true)
        );

        tutorialTable.setTouchable(Touchable.enabled);
        tutorialTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showNextTutorialItem();
            }
        });

        tutorialTable.add(tutorialScrollPane);
        screenStage.addActor(tutorialTable);
        screenStage.setScrollFocus(tutorialScrollPane);
    }

    private void showNextTutorialItem() {
        tutorialTable.clearActions();

        players[0].updatedDirection = players[0].direction = null;
        players[0].setRoundedPosition();
        players[0].position.set(players[0].roundedPosition.x, players[0].roundedPosition.y);

        if (mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor == null) {
            mapGrid[(int) players[0].position.y][(int) players[0].position.x].cellColor =
                    new Color(players[0].color.r, players[0].color.g, players[0].color.b, 0.1f);
        }

        if (tutorialPromptIndex == tutorialPrompts.size) {
            togglePlayerInteractable(false);
            animationManager.fadeOutStage(screenStage, TutorialScreen.this, new MainMenuScreen(game));
            return;
        }

        if (!tutorialScrollPane.isRightEdge()) {
            tutorialScrollPane.scrollTo(tutorialScrollPane.getScrollX() + tutorialScrollPane.getWidth(), 0,
                    tutorialScrollPane.getWidth(), tutorialScrollPane.getHeight());
            return;
        } else {
            tutorialScrollPane.scrollTo(0, 0, tutorialScrollPane.getWidth(), tutorialScrollPane.getHeight());
        }

        tutorialTable.addAction(Actions.sequence(
                Actions.fadeOut(.2f, Interpolation.fastSlow),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        String mainText = tutorialPrompts.get(tutorialPromptIndex).mainItem;
                        if (mainText.contains("Walls")) {
                            generateRandomWalls();
                            map.createMapTexture(shapeDrawer);
                        }

                        displayNextTutorialText(mainLabel, subLabel, mainText, tutorialPrompts.get(tutorialPromptIndex).subItem);
                        togglePlayerInteractable(tutorialPrompts.get(tutorialPromptIndex).enablePlayerInteraction);

                        tutorialPromptIndex++;
                    }
                }),
                Actions.fadeIn(.2f, Interpolation.fastSlow)
        ));
    }

    private void generateRandomWalls() {
        int rowCount = mapGrid.length;
        int colCount = mapGrid[0].length;

        players[0].setRoundedPosition();
        int playerPosX = players[0].roundedPosition.x;
        int playerPosY = players[0].roundedPosition.y;

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            int randomY = rand.nextInt(rowCount);
            int randomX = rand.nextInt(colCount);

            if (!(randomX == playerPosX && randomY == playerPosY)) {
                mapGrid[randomY][randomX].cellColor = null;
                mapGrid[randomY][randomX].isMovable = false;
            }
        }
    }

    private void displayNextTutorialText(Label mainLabel, Label subLabel, String mainText, String subText) {
        mainLabel.setText(mainText);
        subLabel.setText(subText);
    }

    private void togglePlayerInteractable(boolean playerInteractable) {
        this.drawOverlay = !playerInteractable;

        InputMultiplexer inputMultiplexer = new InputMultiplexer(this, screenStage);
        if (playerInteractable) {
            inputMultiplexer.addProcessor(players[0]);
            inputMultiplexer.addProcessor(controlManager.getControls());
        }

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void setUpBackButton() {
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        tutorialTable.setSize(width, textboxHeight);
        mapViewport.update(width, (int) Math.max(0, height - textboxHeight));
        mapViewport.setScreenPosition(0, (int) textboxHeight);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        players[0].direction = players[0].updatedDirection;
        map.update(null, players, delta);

        renderMap(delta);

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        drawDarkOverlay(delta);

        batch.begin();
        shapeDrawer.filledRectangle(0, textboxHeight, screenStage.getWidth(), 2f, Color.WHITE);
        batch.end();

        screenStage.act(delta);
        screenStage.draw();

        displayFPS();
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

        float height = screenStage.getViewport().getWorldHeight();
        float width = screenStage.getViewport().getWorldWidth();
        batch.begin();
        shapeDrawer.setColor(mapOverlayColor);
        shapeDrawer.filledRectangle(0, 0, width, height);
        batch.end();
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
     * false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (screenStage.dialogIsActive()) {
            return false;
        }

        togglePlayerInteractable(false);
        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
        } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            showNextTutorialItem();
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
            return onBackPressed();
        } else if (button == Input.Buttons.FORWARD) {
            showNextTutorialItem();
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
