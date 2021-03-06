package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Overlay;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.TutorialItem;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.ControlManager;
import com.cg.zoned.managers.SplitViewportManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.HoverImageButton;

import java.util.Random;

public class TutorialScreen extends ScreenObject implements InputProcessor {
    private Map map;
    private Cell[][] mapGrid;
    private SplitViewportManager splitViewportManager;
    private Overlay mapOverlay;
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
        this.map = new Map(mapGrid, shapeDrawer);
        this.map.initFloodFillVars();
        this.mapOverlay = new Overlay(new Color(0, 0, 0, 0.7f), 2.5f);

        this.players = new Player[1];
        this.players[0] = new Player(PlayerColorHelper.getColorFromIndex(0), "Player");
        this.players[0].setPosition(new GridPoint2(Math.round(this.mapGrid.length / 2f), Math.round(this.mapGrid[0].length / 2f)));

        this.splitViewportManager = new SplitViewportManager(1, Constants.WORLD_SIZE, this.players[0].getPosition());
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
        float tablePad = 16f * game.getScaleFactor();
        textboxHeight = (mainLabel.getPrefHeight() * 2) + (tablePad * 2);

        tutorialTable = new Table();
        tutorialTable.setSize(screenStage.getWidth(), textboxHeight);
        tutorialTable.setPosition(0, 0);
        tutorialTable.left().bottom().pad(tablePad);

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

        Player player = players[0];
        player.completeMovement();
        player.updatedDirection = player.direction = null;

        Cell playerCell = mapGrid[player.getRoundedPositionY()][player.getRoundedPositionX()];
        if (playerCell.cellColor == null) {
            playerCell.cellColor = new Color(player.color.r, player.color.g, player.color.b, 0.1f);
            map.updateMap(players, null);
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
                Actions.run(() -> {
                    String mainText = tutorialPrompts.get(tutorialPromptIndex).mainItem;
                    if (mainText.contains("Walls")) {
                        generateRandomWalls();
                        map.createMapTexture(shapeDrawer);
                    }

                    displayNextTutorialText(mainLabel, subLabel, mainText, tutorialPrompts.get(tutorialPromptIndex).subItem);
                    togglePlayerInteractable(tutorialPrompts.get(tutorialPromptIndex).enablePlayerInteraction);

                    tutorialPromptIndex++;
                }),
                Actions.fadeIn(.2f, Interpolation.fastSlow)
        ));
    }

    private void generateRandomWalls() {
        int rowCount = mapGrid.length;
        int colCount = mapGrid[0].length;

        Player player = players[0];
        int playerPosX = player.getRoundedPositionX();
        int playerPosY = player.getRoundedPositionY();

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
        this.mapOverlay.drawOverlay(!playerInteractable);

        InputMultiplexer inputMultiplexer = new InputMultiplexer(this, screenStage);
        if (playerInteractable) {
            inputMultiplexer.addProcessor(players[0]);                   // Enables Keyboard controls for the player
            inputMultiplexer.addProcessor(controlManager.getControls()); // Enables on-screen touch controls for players
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
        splitViewportManager.resize(width, (int) Math.max(0, height - textboxHeight));
        splitViewportManager.setScreenPosition(0, (int) textboxHeight);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        players[0].direction = players[0].updatedDirection;
        map.update(null, players, delta);

        splitViewportManager.render(shapeDrawer, batch, map, players, delta);

        this.screenViewport.apply(true);
        batch.setProjectionMatrix(this.screenViewport.getCamera().combined);

        batch.begin();
        mapOverlay.render(shapeDrawer, screenStage, delta);
        shapeDrawer.filledRectangle(0, textboxHeight, screenStage.getWidth(), 2f, Color.WHITE);
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
