package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.MapSelector;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.ButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class PlayerSetUpScreen extends ScreenObject implements InputProcessor {
    private float bgAlpha = .25f;
    private float bgAnimSpeed = 1.8f;
    private Color[] currentBgColors;
    private Color[] targetBgColors;

    private int playerCount;
    private Table playerList;
    private MapSelector mapSelector;

    public PlayerSetUpScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Setting up splitscreen multiplayer");

        this.screenViewport = new ScreenViewport();
        this.screenStage = new FocusableStage(this.screenViewport);
        this.animationManager = new AnimationManager(this.game, this);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);

        this.playerCount = game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);
        this.playerList = new Table();

        this.currentBgColors = new Color[this.playerCount];
        this.targetBgColors = new Color[this.playerCount];
        for (int i = 0; i < this.playerCount; i++) {
            this.currentBgColors[i] = new Color(0, 0, 0, bgAlpha);
            this.targetBgColors[i] = new Color(0, 0, 0, bgAlpha);
        }
    }

    @Override
    public void show() {
        setUpStage();
        setUpUIButtons();

        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                boolean showTutorialDialogPrompt = game.preferences.getBoolean(Preferences.SHOW_TUTORIAL_PREFERENCE, true);
                if (showTutorialDialogPrompt) {
                    showTutorialDialog();

                    game.preferences.putBoolean(Preferences.SHOW_TUTORIAL_PREFERENCE, false);
                    game.preferences.flush();
                }
            }
        });
        animationManager.fadeInStage(screenStage);
    }

    private void setUpUIButtons() {
        UIButtonManager uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getBackButtonTexture());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
        HoverImageButton tutorialButton = uiButtonManager.addTutorialButtonToStage(game.assets.getTutorialButtonTexture());
        tutorialButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showTutorialDialog();
            }
        });
    }

    private void setUpStage() {
        final int NO_OF_COLORS = Constants.PLAYER_COLORS.size();
        final float COLOR_BUTTON_DIMENSIONS = 60f * game.getScaleFactor();

        Table masterTable = new Table();
        masterTable.setFillParent(true);
        // masterTable.setDebug(true);
        masterTable.center();

        Table table = new Table();
        table.center();
        table.pad(20f);
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);
        // Have to set overscrollX to false since on Android, it seems to overscroll even when there is space
        // But on Desktop it works perfectly well.

        Label[] promptLabels = new Label[playerCount];
        Button[][] colorButtons = new Button[playerCount][];
        final ButtonGroup[] colorButtonGroups = new ButtonGroup[playerCount];
        for (int i = 0; i < playerCount; i++) {
            Table playerItem = new Table();
            playerItem.center();
            promptLabels[i] = new Label("Player " + (i + 1) + " color: ", game.skin, "themed");
            playerItem.add(promptLabels[i]);

            colorButtons[i] = new Button[NO_OF_COLORS];
            colorButtonGroups[i] = new ButtonGroup();
            colorButtonGroups[i].setMinCheckCount(1);
            colorButtonGroups[i].setMaxCheckCount(1);

            for (int j = 0; j < NO_OF_COLORS; j++) {
                colorButtons[i][j] = new Button(game.skin, "color-button");
                colorButtons[i][j].setColor(PlayerColorHelper.getColorFromIndex(j));
                colorButtonGroups[i].add(colorButtons[i][j]);

                playerItem.add(colorButtons[i][j]).width(COLOR_BUTTON_DIMENSIONS).height(COLOR_BUTTON_DIMENSIONS);

                screenStage.addFocusableActor(colorButtons[i][j]);
            }

            final int finalI = i;
            colorButtonGroups[i].setOnCheckChangeListener(new ButtonGroup.OnCheckChangeListener() {
                @Override
                public void buttonPressed(Button button) {
                    targetBgColors[finalI].set(button.getColor());
                    targetBgColors[finalI].a = bgAlpha;
                }
            });

            colorButtonGroups[i].uncheckAll();
            colorButtons[i][i % NO_OF_COLORS].setChecked(true);
            screenStage.row();
            playerList.add(playerItem).right();
            playerList.row();
        }
        table.add(playerList).colspan(NO_OF_COLORS + 1).expandX();
        table.row();

        mapSelector = new MapSelector(screenStage, game.getScaleFactor(), game.assets, game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        mapSelector.getMapManager().enableExternalMapLogging(true);
        Spinner mapSpinner = mapSelector.loadMapSelectorSpinner(150 * game.getScaleFactor(),
                game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight() * 3);
        mapSelector.loadExternalMaps();
        table.add(mapSpinner).colspan(NO_OF_COLORS + 1).pad(20 * game.getScaleFactor()).expandX();
        table.row();

        screenStage.addFocusableActor(mapSelector.getLeftButton(), 1);
        screenStage.addFocusableActor(mapSelector.getRightButton(), NO_OF_COLORS - 1);
        screenStage.row();

        if (playerCount == 2) {
            Table infoTable = new Table();
            infoTable.center();
            Texture infoIconTexture = new Texture(Gdx.files.internal("images/ui_icons/ic_info.png"));
            usedTextures.add(infoIconTexture);
            Image infoImage = new Image(infoIconTexture);
            Label infoLabel = new Label("First to capture more than 50% of the grid wins", game.skin);
            infoTable.add(infoImage).height(infoLabel.getPrefHeight()).width(infoLabel.getPrefHeight()).padRight(20f);
            infoTable.add(infoLabel);
            table.add(infoTable).colspan(NO_OF_COLORS + 1).padBottom(20f * game.getScaleFactor()).expandX();
            table.row();
        }

        TextButton startButton = new TextButton("Next", game.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Array<Color> playerColors = new Array<>();
                for (ButtonGroup buttonGroup : colorButtonGroups) {
                    playerColors.add(buttonGroup.getChecked().getColor());
                }

                if (mapSelector.loadSelectedMap()) {
                    startGame(playerColors, mapSelector.getMapManager());
                }
            }

        });
        table.add(startButton).width(200 * game.getScaleFactor()).colspan(NO_OF_COLORS + 1).expandX();
        screenStage.addFocusableActor(startButton, NO_OF_COLORS);
        masterTable.add(screenScrollPane).grow();
        screenStage.setScrollFocus(screenScrollPane);
        screenStage.addActor(masterTable);
    }

    private void startGame(Array<Color> playerColors, MapManager mapManager) {
        final Player[] players = new Player[playerColors.size];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(playerColors.get(i), PlayerColorHelper.getStringFromColor(playerColors.get(i)));
        }

        for (int i = 0; i < players.length; i++) {
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
        }

        int startPosSplitScreenCount = game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);
        animationManager.fadeOutStage(screenStage, this, new MapStartPosScreen(game, mapManager, players, startPosSplitScreenCount, false));
    }

    private void showTutorialDialog() {
        screenStage.showDialog("Start the tutorial?",
                new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.Yes },
                false, game.getScaleFactor(),
                new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(FocusableStage.DialogButton button) {
                        if (button == FocusableStage.DialogButton.Yes) {
                            animationManager.fadeOutStage(screenStage, PlayerSetUpScreen.this, new TutorialScreen(game));
                        }
                    }
                }, game.skin);
    }

    @Override
    public void resize(int width, int height) {
        screenStage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (int i = 0; i < currentBgColors.length; i++) {
            currentBgColors[i].lerp(targetBgColors[i], bgAnimSpeed * delta);
            currentBgColors[i].a = Math.min(targetBgColors[i].a, screenStage.getRoot().getColor().a / 2.5f);
        }

        screenViewport.apply(true);

        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.begin();
        for (int i = 0; i < currentBgColors.length; i++) {
            shapeDrawer.setColor(currentBgColors[i]);
            shapeDrawer.filledRectangle(i * screenStage.getWidth() / currentBgColors.length, 0,
                    screenStage.getWidth() / currentBgColors.length, screenStage.getHeight());
        }
        batch.end();

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(screenViewport, screenStage.getBatch(), smallFont);
        }

        screenStage.draw();
        screenStage.act(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
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

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
        } else if (keycode == Input.Keys.S && !screenStage.dialogIsActive()) {
            // S as the 'Settings' button is used for the extra params option
            return mapSelector.extraParamShortcutPressed();
        } else if (keycode == Input.Keys.T && !screenStage.dialogIsActive()) {
            showTutorialDialog();
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
