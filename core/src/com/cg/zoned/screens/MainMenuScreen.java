package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.AnimatedDrawable;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

public class MainMenuScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private FocusableStage mainStage;
    private FocusableStage playModeStage;
    private Viewport viewport;
    private boolean exitDialogIsActive = false;
    private Array<Actor> mainMenuUIButtons;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;
    private Texture roundedCornerBgColorTexture;

    private ParticleEffect emitterLeft, emitterRight;

    public MainMenuScreen(final Zoned game) {
        this.game = game;
        this.game.discordRPCManager.updateRPC("Main Menu");

        viewport = new ScreenViewport();
        mainStage = new FocusableStage(viewport);
        playModeStage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);

        emitterLeft = new ParticleEffect();
        emitterRight = new ParticleEffect();
        font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpMainMenu();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        animationManager.startMainMenuAnimation(mainStage, mainMenuUIButtons);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                emitterLeft.load(Gdx.files.internal("particles/left_emitter.p"), Gdx.files.internal("particles"));
                emitterRight.load(Gdx.files.internal("particles/right_emitter.p"), Gdx.files.internal("particles"));
                emitterRight.setPosition(viewport.getWorldWidth(), 0);
                emitterLeft.start();
                emitterRight.start();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Pixmap pixmap = getRoundedCornerPixmap(Color.GREEN, 480, 640, 50);
                // I suspect pixmap generation caused a noticeable lag so run in a new thread
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        roundedCornerBgColorTexture = new Texture(pixmap);
                        usedTextures.add(roundedCornerBgColorTexture);
                        pixmap.dispose();
                    }
                });
            }
        }).start();
    }

    private void setUpMainMenu() {
        final Table mainTable = new Table();
        mainTable.setFillParent(true);
        //mainTable.setDebug(true);
        mainTable.center();

        Label gameTitle = new Label("ZONED", game.skin, Constants.FONT_MANAGER.LARGE.getName(), Color.GREEN);
        mainTable.add(gameTitle).pad(10f * game.getScaleFactor());
        mainTable.row();

        HoverImageButton playButton = setUpAnimatedPlayButton(mainTable);
        mainStage.addFocusableActor(playButton);
        mainStage.row();

        UIButtonManager uiButtonManager = new UIButtonManager(mainStage, game.getScaleFactor(), usedTextures);
        HoverImageButton settingsButton = uiButtonManager.addSettingsButtonToStage(game.assets.getSettingsButtonTexture());
        HoverImageButton creditsButton = uiButtonManager.addCreditsButtonToStage(game.assets.getCreditsButtonTexture());
        HoverImageButton devButton = null;
        if (game.preferences.getBoolean(Constants.DEV_MODE_PREFERENCE, false)) {
            devButton = uiButtonManager.addDevButtonToStage(game.assets.getDevButtonTexture());
            devButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    emitterLeft.allowCompletion();
                    emitterRight.allowCompletion();
                    animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new DevScreen(game));
                }
            });
        }
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                emitterLeft.allowCompletion();
                emitterRight.allowCompletion();
                animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new SettingsScreen(game));
            }
        });
        creditsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                emitterLeft.allowCompletion();
                emitterRight.allowCompletion();
                animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new CreditsScreen(game));
            }
        });

        mainStage.addFocusableActor(settingsButton);
        mainStage.addFocusableActor(creditsButton);
        mainStage.addFocusableActor(devButton);

        HoverImageButton exitButton = uiButtonManager.addExitButtonToStage(game.assets.getCrossButtonTexture());
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });

        mainMenuUIButtons = new Array<>();
        mainMenuUIButtons.add(playButton);
        mainMenuUIButtons.add(settingsButton);
        mainMenuUIButtons.add(creditsButton);
        if (devButton != null) {
            mainMenuUIButtons.add(devButton);
        }
        mainMenuUIButtons.add(exitButton);

        mainStage.setFocusedActor(playButton);
        mainStage.addActor(mainTable);
    }

    private HoverImageButton setUpAnimatedPlayButton(Table mainTable) {
        Texture playSheet = game.assets.getPlayButtonTexture(); // Loaded via the assetManager since it causes a noticeable lag on my phone otherwise
        //usedTextures.add(playSheet); Texture is loaded via the assetManager so that will take care of its disposal
        int rowCount = 6, colCount = 5;

        TextureRegion[][] tmp = TextureRegion.split(playSheet,
                playSheet.getWidth() / colCount,
                playSheet.getHeight() / rowCount);

        TextureRegion[] playFrames = new TextureRegion[rowCount * colCount];
        int index = 0;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                playFrames[index++] = tmp[i][j];
            }
        }

        Animation playButtonAnimation = new Animation<>(1 / 15f, playFrames);

        float buttonWidth = 144f;
        float buttonHeight = 144f;

        final HoverImageButton playButton = new HoverImageButton(new AnimatedDrawable(playButtonAnimation, buttonWidth, buttonHeight, game.getScaleFactor()));
        playButton.setTransform(true);
        playButton.setHoverAlpha(.75f);
        playButton.setClickAlpha(.6f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (roundedCornerBgColorTexture == null) {
                    // Thread didn't finish loading the pixmap
                    // Should almost never happen cause processors are hella fast, even mobile ones
                    return;
                }

                if (!playModeStage.getRoot().hasChildren()) {
                    setUpPlayMenu();
                }
                animationManager.startPlayModeAnimation(mainStage, playModeStage, playButton);
            }
        });
        mainTable.add(playButton).pad(10f * game.getScaleFactor())
                .width(buttonWidth * game.getScaleFactor())
                .height(buttonHeight * game.getScaleFactor());
        mainTable.row();

        return playButton;
    }

    private void setUpPlayMenu() {
        final Table playModeTable = new Table();
        playModeTable.setFillParent(true);
        playModeTable.center();

        Label chooseMode = new Label("Choose the game mode", game.skin, "themed");
        playModeTable.add(chooseMode).expandX().pad(20f).colspan(2);
        playModeTable.row();

        final int gameModeCount = 2;

        TextureRegionDrawable whiteBG = new TextureRegionDrawable(roundedCornerBgColorTexture);

        String[] backgroundImageLocations = new String[]{
                "icons/multiplayer_icons/ic_splitscreen_multiplayer.png",
                "icons/multiplayer_icons/ic_local_multiplayer.png",
        };
        String[] modeLabelStrings = new String[]{
                "Splitscreen\nMultiplayer",
                "Local\nMultiplayer\n(WiFi)",
        };
        final Class[] screenClasses = new Class[]{
                PlayerSetUpScreen.class,
                HostJoinScreen.class,
        };

        if (screenClasses.length != gameModeCount ||
                modeLabelStrings.length != gameModeCount ||
                backgroundImageLocations.length != gameModeCount) {
            throw new IndexOutOfBoundsException("Game mode count does not match asset the count");
        }

        final float normalAlpha = .15f;
        final float hoverAlpha = .3f;
        final float clickAlpha = .7f;
        for (int i = 0; i < gameModeCount; i++) {
            Table table = new Table();
            table.center();

            final Image backgroundColorImage = new Image(whiteBG);
            backgroundColorImage.getColor().a = normalAlpha;
            backgroundColorImage.setScaling(Scaling.stretch);

            Texture backgroundImageTexture = new Texture(Gdx.files.internal(backgroundImageLocations[i]));
            usedTextures.add(backgroundImageTexture);
            Image backgroundImage = new Image(backgroundImageTexture);
            backgroundImage.setScaling(Scaling.fit);
            backgroundImage.getColor().a = .3f;

            Label modeLabel = new Label(modeLabelStrings[i], game.skin);
            modeLabel.setAlignment(Align.center);

            final int finalI = i;
            table.addListener(new ClickListener() {
                boolean hasTouched = false;

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (!hasTouched) {
                        backgroundColorImage.addAction(Actions.alpha(hoverAlpha, .15f));
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (!hasTouched) {
                        backgroundColorImage.addAction(Actions.alpha(normalAlpha, .15f));
                    }
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    hasTouched = true;
                    backgroundColorImage.addAction(Actions.alpha(clickAlpha, .15f));
                    return super.touchDown(event, x, y, pointer, button);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    hasTouched = false;
                    backgroundColorImage.addAction(Actions.alpha(normalAlpha, .15f));
                    super.touchUp(event, x, y, pointer, button);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    emitterLeft.allowCompletion();
                    emitterRight.allowCompletion();
                    try {
                        animationManager.fadeOutStage(playModeStage, MainMenuScreen.this, (Screen) screenClasses[finalI].getConstructors()[0].newInstance(game));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Stack stack = new Stack(backgroundColorImage, backgroundImage, modeLabel);

            table.add(stack).expand().pad(40f);

            playModeTable.add(table).expand().uniform();
            playModeStage.addFocusableActor(table);
        }

        UIButtonManager uiButtonManager = new UIButtonManager(playModeStage, game.getScaleFactor(), usedTextures);
        HoverImageButton exitButton = uiButtonManager.addHideButtonToStage(game.assets.getBackButtonTexture());
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.endPlayModeAnimation(mainStage, playModeStage);
            }
        });

        playModeStage.getRoot().getColor().a = 0;

        playModeStage.addActor(playModeTable);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        mainStage.getBatch().begin();
        emitterLeft.draw(mainStage.getBatch(), delta);
        emitterRight.draw(mainStage.getBatch(), delta);
        mainStage.getBatch().end();

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, mainStage.getBatch(), font);
        }

        mainStage.draw();
        mainStage.act(delta);

        playModeStage.draw();
        playModeStage.act(delta);
    }

    @Override
    public void resize(int width, int height) {
        mainStage.resize(width, height);
        playModeStage.resize(width, height);
        emitterRight.setPosition(viewport.getWorldWidth(), 0);
    }

    private Pixmap getRoundedCornerPixmap(Color color, int width, int height, int radius) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius, radius, radius);
        pixmap.fillCircle(width - radius, height - radius, radius);
        pixmap.fillCircle(radius, height - radius, radius);
        pixmap.fillRectangle(0, radius, width, height - (radius * 2));
        pixmap.fillRectangle(radius, 0, width - (radius * 2), height);

        return pixmap;
    }

    @Override
    public void dispose() {
        mainStage.dispose();
        playModeStage.dispose();
        emitterLeft.dispose();
        emitterRight.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void showExitDialog() {
        if (exitDialogIsActive) return;

        final Array<String> dialogButtonTexts = new Array<>();
        dialogButtonTexts.add("Cancel");
        dialogButtonTexts.add("Exit");
        exitDialogIsActive = true;
        mainStage.showDialog("Are you sure that you want to exit?", dialogButtonTexts,
                false,
                game.getScaleFactor(), new FocusableStage.DialogResultListener() {
                    @Override
                    public void dialogResult(String buttonText) {
                        exitDialogIsActive = false;
                        if (buttonText.equals(dialogButtonTexts.get(1))) {
                            Gdx.app.exit();
                        }
                    }
                }, game.skin);
    }

    private void onBackPressed() {
        if (mainStage.getRoot().getColor().a == 1f) {
            showExitDialog();
        } else {
            animationManager.endPlayModeAnimation(mainStage, playModeStage);
        }
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
