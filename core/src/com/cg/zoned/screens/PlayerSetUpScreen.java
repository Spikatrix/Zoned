package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.MapSelector;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.PlayerSetUpParams;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.MapManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;
import com.cg.zoned.ui.ButtonGroup;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.Spinner;

public class PlayerSetUpScreen extends ScreenObject implements InputProcessor {
    private float bgAlpha = .25f;
    private float bgAnimSpeed = 1.8f;
    private Color[] currentBgColors;
    private Color[] targetBgColors;

    private PlayerSetUpParams playerSetUpParams;
    private int playerCount;
    private FocusableStage mapSelectorStage;
    private MapSelector mapSelector;
    private Spinner mapSpinner;
    private boolean extendedMapSelectorActive;

    public PlayerSetUpScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Setting up splitscreen multiplayer");

        this.animationManager = new AnimationManager(this.game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);

        this.batch = new SpriteBatch();
        this.shapeDrawer = new ShapeDrawer(batch, game.skin);

        this.playerCount = game.preferences.getInteger(Preferences.SPLITSCREEN_PLAYER_COUNT_PREFERENCE, 2);

        this.currentBgColors = new Color[this.playerCount];
        this.targetBgColors = new Color[this.playerCount];
        for (int i = 0; i < this.playerCount; i++) {
            this.currentBgColors[i] = new Color(0, 0, 0, bgAlpha);
            this.targetBgColors[i] = new Color(0, 0, 0, bgAlpha);
        }
    }

    public PlayerSetUpScreen(final Zoned game, PlayerSetUpParams playerSetUpParams) {
        this(game);
        this.playerSetUpParams = playerSetUpParams;
    }

    @Override
    public void show() {
        setUpMapSelectorStage();
        setUpStage();
        setUpUIButtons();

        animationManager.setAnimationListener(stage -> {
            boolean showTutorialDialogPrompt = game.preferences.getBoolean(Preferences.SHOW_TUTORIAL_PREFERENCE, true);
            if (showTutorialDialogPrompt) {
                showTutorialDialog();

                game.preferences.putBoolean(Preferences.SHOW_TUTORIAL_PREFERENCE, false);
                game.preferences.flush();
            }
        });
        screenStage.getRoot().getColor().a = 0;

        // The screen is faded in once the maps and the extended selector is loaded
        mapSelector.loadExternalMaps((mapList, externalMapStartIndex) -> {
            // Set up the extended map selector once external maps have been loaded
            mapSelector.setUpExtendedSelector(mapSelectorStage, new MapSelector.ExtendedMapSelectionListener() {
                @Override
                public void onExtendedMapSelectorOpened() {
                    openExtendedMapSelector();
                }

                @Override
                public void onMapSelect(int mapIndex) {
                    if (mapIndex != -1) {
                        mapSpinner.snapToStep(mapIndex - mapSpinner.getPositionIndex());
                    }
                    animationManager.endExtendedMapSelectorAnimation(screenStage, mapSelectorStage);
                    extendedMapSelectorActive = false;
                }
            });

            if (playerSetUpParams != null) {
                int snapCount = 0;
                for (MapEntity map : mapSelector.getMapManager().getMapList()) {
                    if (map.getName().equals(playerSetUpParams.mapName)) {
                        mapSpinner.snapToStep(snapCount);
                        break;
                    }
                    snapCount++;
                }

                if (playerSetUpParams.mapExtraParams != null) {
                    MapEntity map = mapSelector.getMapManager().getMap(playerSetUpParams.mapName);
                    MapExtraParams mapExtraParams = map.getExtraParams();
                    mapExtraParams.extraParams = playerSetUpParams.mapExtraParams.extraParams;
                    mapExtraParams.paramSelectTitle = playerSetUpParams.mapExtraParams.paramSelectTitle;
                    mapExtraParams.spinnerVars = playerSetUpParams.mapExtraParams.spinnerVars;
                    map.applyExtraParams();
                }
            }

            animationManager.fadeInStage(screenStage);
        });
    }

    private void openExtendedMapSelector() {
        extendedMapSelectorActive = true;
        animationManager.startExtendedMapSelectorAnimation(screenStage, mapSelectorStage, .15f);
        mapSpinner.snapToStep(0);
    }

    private void setUpMapSelectorStage() {
        mapSelectorStage = new FocusableStage(screenViewport);
        mapSelectorStage.getRoot().getColor().a = 0f;
    }

    private void setUpUIButtons() {
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
        HoverImageButton tutorialButton = uiButtonManager.addTutorialButtonToStage(game.assets.getTexture(Assets.TextureObject.TUTORIAL_TEXTURE));
        tutorialButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showTutorialDialog();
            }
        });
    }

    private void setUpStage() {
        final int NO_OF_COLORS = Constants.PLAYER_COLORS.size();

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
        Table playerList = new Table();
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

                playerItem.add(colorButtons[i][j]).size(promptLabels[i].getPrefHeight() * 1.75f);

                screenStage.addFocusableActor(colorButtons[i][j]);
            }

            final int finalI = i;
            colorButtonGroups[i].setOnCheckChangeListener(button -> {
                targetBgColors[finalI].set(button.getColor());
                targetBgColors[finalI].a = bgAlpha;
            });

            colorButtonGroups[i].uncheckAll();
            if (playerSetUpParams == null) {
                colorButtons[i][i % NO_OF_COLORS].setChecked(true);
            } else {
                colorButtons[i][PlayerColorHelper.getIndexFromColor(playerSetUpParams.playerColors[i])].setChecked(true);
            }
            screenStage.row();
            playerList.add(playerItem).right();
            playerList.row();
        }
        table.add(playerList).colspan(NO_OF_COLORS + 1).expandX();
        table.row();

        mapSelector = new MapSelector(screenStage, game.getScaleFactor(), game.assets, game.skin);
        mapSelector.setUsedTextureArray(usedTextures);
        mapSelector.getMapManager().enableExternalMapScanLogging(true);
        float spinnerHeight = game.skin.getFont(Assets.FontManager.REGULAR.getFontName()).getLineHeight() * 3;
        mapSpinner = mapSelector.loadMapSelectorSpinner(spinnerHeight * 1.5f, spinnerHeight);
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
            players[i].setControlIndex(i % Constants.PLAYER_CONTROLS.length);
        }

        int startPosSplitScreenCount = game.preferences.getInteger(Preferences.MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE, 2);
        animationManager.fadeOutStage(screenStage, this, new MapStartPosScreen(game, mapManager, players, startPosSplitScreenCount));
    }

    private void showTutorialDialog() {
        screenStage.showDialog("Start the tutorial?",
                new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.Yes },
                false, game.getScaleFactor(),
                button -> {
                    if (button == FocusableStage.DialogButton.Yes) {
                        animationManager.fadeOutStage(screenStage, PlayerSetUpScreen.this, new TutorialScreen(game));
                    }
                }, game.skin);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        for (int i = 0; i < currentBgColors.length; i++) {
            currentBgColors[i].lerp(targetBgColors[i], bgAnimSpeed * delta);
            currentBgColors[i].a = Math.min(targetBgColors[i].a, screenStage.getRoot().getColor().a / 2.5f);
        }

        batch.begin();
        for (int i = 0; i < currentBgColors.length; i++) {
            shapeDrawer.setColor(currentBgColors[i]);
            shapeDrawer.filledRectangle(i * screenStage.getWidth() / currentBgColors.length, 0,
                    screenStage.getWidth() / currentBgColors.length, screenStage.getHeight());
        }
        batch.end();

        screenStage.act(delta);
        screenStage.draw();

        mapSelectorStage.act(delta);
        if (mapSelectorStage.getRoot().getColor().a > 0) {
            mapSelectorStage.draw();
        }

        displayFPS();
    }

    @Override
    public void dispose() {
        super.dispose();
        mapSelectorStage.dispose();
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (extendedMapSelectorActive) {
            animationManager.endExtendedMapSelectorAnimation(screenStage, mapSelectorStage);
            extendedMapSelectorActive = false;
            return true;
        } else if (screenStage.dialogIsActive()) {
            return false;
        }

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
        } else if (!screenStage.dialogIsActive() && !extendedMapSelectorActive) {
            if (keycode == Input.Keys.E) {
                openExtendedMapSelector();
                return true;
            } else if (keycode == Input.Keys.S) {
                return mapSelector.extraParamShortcutPressed();
            } else if (keycode == Input.Keys.T) {
                showTutorialDialog();
                return true;
            }
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
