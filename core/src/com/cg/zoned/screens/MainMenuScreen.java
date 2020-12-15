package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
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
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Assets;
import com.cg.zoned.GameMode;
import com.cg.zoned.Preferences;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.AnimatedDrawable;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.PixmapFactory;

public class MainMenuScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private FocusableStage mainStage;
    private FocusableStage playModeStage;
    private Viewport viewport;
    private SpriteBatch batch;
    private boolean exitDialogIsActive = false;
    private Array<Actor> mainMenuUIButtons;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;
    private NinePatch roundedCornerNP;
    private Texture bgTexture;
    private float bgScrollAmount;
    private float bgAlpha;

    private ParticleEffect emitterLeft, emitterRight;

    public MainMenuScreen(final Zoned game) {
        this.game = game;
        this.game.discordRPCManager.updateRPC("Main Menu");

        viewport = new ScreenViewport();
        mainStage = new FocusableStage(viewport);
        playModeStage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);
        batch = new SpriteBatch();
        batch.setColor(1, 1, 1, bgAlpha);

        emitterLeft = new ParticleEffect();
        emitterRight = new ParticleEffect();
        font = game.skin.getFont(Assets.FontManager.SMALL.getFontName());
    }

    @Override
    public void show() {
        setUpMainMenu();
        showFPSCounter = game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false);
        animationManager.startMainMenuAnimation(mainStage, mainMenuUIButtons);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                emitterLeft.load(Gdx.files.internal("particles/left_emitter.p"), Gdx.files.internal("particles"));
                emitterRight.load(Gdx.files.internal("particles/right_emitter.p"), Gdx.files.internal("particles"));
                emitterRight.setPosition(viewport.getWorldWidth(), 0);
                emitterLeft.start();
                emitterRight.start();

                bgTexture = game.assets.getGameBgTexture();
                bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

                bgAlpha = 1;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int radius = 40;
                final Pixmap pixmap = PixmapFactory.getRoundedCornerPixmap(Color.GREEN, radius);
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        Texture roundedCornerBgColorTexture = new Texture(pixmap);
                        usedTextures.add(roundedCornerBgColorTexture);
                        roundedCornerNP = new NinePatch(roundedCornerBgColorTexture, radius, radius, radius, radius);
                        pixmap.dispose();

                        setUpPlayMenu();
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

        Label gameTitle = new Label("ZONED", game.skin, Assets.FontManager.STYLED_LARGE.getFontName(), Color.GREEN);
        mainTable.add(gameTitle).pad(10f * game.getScaleFactor());
        mainTable.row();

        HoverImageButton playButton = setUpAnimatedPlayButton(mainTable);
        mainStage.addFocusableActor(playButton);
        mainStage.row();

        UIButtonManager uiButtonManager = new UIButtonManager(mainStage, game.getScaleFactor(), usedTextures);
        HoverImageButton settingsButton = uiButtonManager.addSettingsButtonToStage(game.assets.getSettingsButtonTexture());
        HoverImageButton creditsButton = uiButtonManager.addCreditsButtonToStage(game.assets.getCreditsButtonTexture());
        HoverImageButton devButton = null;
        if (game.preferences.getBoolean(Preferences.DEV_MODE_PREFERENCE, false)) {
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

        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            mainStage.setFocusedActor(playButton);
        }
        mainStage.addActor(mainTable);
    }

    private HoverImageButton setUpAnimatedPlayButton(Table mainTable) {
        Texture playSheet = game.assets.getPlayButtonTexture();
        int rowCount = 5, colCount = 6;

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
                if (roundedCornerNP == null || !playModeStage.getRoot().hasChildren()) {
                    // Should almost never happen cause processors are hella fast, even mobile ones
                    return;
                }

                bgAlpha = 0f;

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

        Label chooseMode = new Label("Choose the game mode", game.skin, "themed-rounded-background");
        playModeTable.add(chooseMode).expandX().pad(20f).colspan(2);
        playModeTable.row();

        final GameMode[] gameModes = new GameMode[]{
                new GameMode("Splitscreen\nMultiplayer", "images/multiplayer_icons/ic_splitscreen_multiplayer.png", PlayerSetUpScreen.class),
                new GameMode("Local\nNetwork\nMultiplayer", "images/multiplayer_icons/ic_local_multiplayer.png", HostJoinScreen.class),
        };

        final float normalAlpha = .15f;
        final float hoverAlpha = .3f;
        final float clickAlpha = .7f;
        for (int i = 0; i < gameModes.length; i++) {
            Table table = new Table();
            table.center();

            final Image backgroundColorImage = new Image(roundedCornerNP);
            backgroundColorImage.getColor().a = normalAlpha;
            backgroundColorImage.setScaling(Scaling.stretch);

            Image backgroundImage = null;
            if (gameModes[i].previewLocation != null) {
                Texture backgroundImageTexture = new Texture(Gdx.files.internal(gameModes[i].previewLocation));
                usedTextures.add(backgroundImageTexture);
                backgroundImage = new Image(backgroundImageTexture);
                backgroundImage.setScaling(Scaling.fit);
                backgroundImage.getColor().a = .3f;
            }

            Label modeLabel = new Label(gameModes[i].name, game.skin);
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
                        animationManager.fadeOutStage(playModeStage, MainMenuScreen.this, (Screen) ClassReflection.getConstructors(gameModes[finalI].targetClass)[0].newInstance(game));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Stack stack;
            if (backgroundImage != null) {
                stack = new Stack(backgroundColorImage, backgroundImage, modeLabel);
            } else {
                stack = new Stack(backgroundColorImage, modeLabel);
            }

            float optionPadding = 50f;
            if (i == 0) {
                if (gameModes.length == 1) {
                    table.add(stack).grow().padLeft(optionPadding).padTop(optionPadding).padBottom(optionPadding).padRight(optionPadding);
                } else {
                    table.add(stack).grow().padLeft(optionPadding).padTop(optionPadding).padBottom(optionPadding).padRight(optionPadding / 2);
                }
            } else if (i == gameModes.length - 1) {
                table.add(stack).grow().padLeft(optionPadding / 2).padTop(optionPadding).padBottom(optionPadding).padRight(optionPadding);
            } else {
                table.add(stack).grow().padLeft(optionPadding / 2).padTop(optionPadding).padBottom(optionPadding).padRight(optionPadding / 2);
            }

            playModeTable.add(table).grow().uniform();
            playModeStage.addFocusableActor(table);
        }

        UIButtonManager uiButtonManager = new UIButtonManager(playModeStage, game.getScaleFactor(), usedTextures);
        HoverImageButton hideButton = uiButtonManager.addHideButtonToStage(game.assets.getBackButtonTexture());
        hideButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                bgAlpha = 1f;
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

        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();
        emitterLeft.draw(batch, delta);
        emitterRight.draw(batch, delta);
        drawBG(batch, delta);
        batch.end();

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, batch, font);
        }

        mainStage.draw();
        mainStage.act(delta);

        playModeStage.draw();
        playModeStage.act(delta);
    }

    private void drawBG(Batch batch, float delta) {
        if (bgTexture != null) {
            int bgTextureW = bgTexture.getWidth();
            int bgTextureH = bgTexture.getHeight();
            float stageW = mainStage.getWidth();
            float stageH = mainStage.getHeight();

            Color color = batch.getColor();
            if (color.a != bgAlpha) {
                color.a += delta * 6f * (bgAlpha - color.a);
                if (color.a < 0) {
                    color.a = 0;
                }
                else if (color.a > 1) {
                    color.a = 1;
                }

                batch.setColor(color);
            }

            batch.draw(bgTexture, 0, 0,
                    (int) (bgTextureW - ((stageW % bgTextureW)) / 2),
                    (int) (bgTextureH - bgScrollAmount - ((stageH % bgTextureH)) / 2),
                    (int) stageW, (int) stageH);
            bgScrollAmount += 20 * delta * game.getScaleFactor();
            if (bgScrollAmount > bgTextureH) {
                bgScrollAmount %= bgTextureH;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        mainStage.resize(width, height);
        playModeStage.resize(width, height);
        emitterRight.setPosition(viewport.getWorldWidth(), 0);
    }

    @Override
    public void dispose() {
        mainStage.dispose();
        playModeStage.dispose();
        emitterLeft.dispose();
        emitterRight.dispose();
        batch.dispose();
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
            bgAlpha = 1f;
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
