package com.cg.zoned.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import com.cg.zoned.Assets;
import com.cg.zoned.Preferences;
import com.cg.zoned.Zoned;
import com.cg.zoned.dataobjects.GameMode;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.AnimatedDrawable;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.PixmapFactory;

public class MainMenuScreen extends ScreenObject implements InputProcessor {
    private FocusableStage mainStage;
    private FocusableStage playModeStage;
    private Array<Actor> mainMenuUIButtons;
    private NinePatch roundedCornerNP;
    private Texture bgTexture;
    private float bgScrollAmount;
    private float bgAlpha;

    public MainMenuScreen(final Zoned game) {
        super(game);
        this.game.discordRPCManager.updateRPC("Main Menu");

        mainStage = screenStage;
        playModeStage = new FocusableStage(screenViewport, this.game.getScaleFactor(), this.game.skin);
        animationManager = new AnimationManager(this.game, this);
        uiButtonManager = new UIButtonManager(playModeStage, game.getScaleFactor(), usedTextures); // This is the one for playModeStage
        batch = new SpriteBatch();
        batch.setColor(1, 1, 1, bgAlpha);
    }

    @Override
    public void show() {
        setUpMainMenu();
        animationManager.startMainMenuAnimation(mainStage, mainMenuUIButtons);
        animationManager.setAnimationListener(stage -> {
            bgTexture = game.assets.getTexture(Assets.TextureObject.DIAMOND_TEXTURE);
            bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

            bgAlpha = 1;
        });

        new Thread(() -> {
            final int radius = 40;
            final Pixmap pixmap = PixmapFactory.getRoundedCornerPixmap(Color.GREEN, radius);
            Gdx.app.postRunnable(() -> {
                Texture roundedCornerBgColorTexture = new Texture(pixmap);
                usedTextures.add(roundedCornerBgColorTexture);
                roundedCornerNP = new NinePatch(roundedCornerBgColorTexture, radius, radius, radius, radius);
                pixmap.dispose();
            });
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
        HoverImageButton settingsButton = uiButtonManager.addSettingsButtonToStage(game.assets.getTexture(Assets.TextureObject.SETTINGS_TEXTURE));
        HoverImageButton creditsButton = uiButtonManager.addCreditsButtonToStage(game.assets.getTexture(Assets.TextureObject.CREDITS_TEXTURE));
        HoverImageButton devButton = null;
        if (game.preferences.getBoolean(Preferences.DEV_MODE_PREFERENCE, false)) {
            devButton = uiButtonManager.addDevButtonToStage(game.assets.getTexture(Assets.TextureObject.DEV_TEXTURE));
            devButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    bgAlpha = 0;
                    animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new DevScreen(game));
                }
            });
        }
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                bgAlpha = 0;
                animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new SettingsScreen(game));
            }
        });
        creditsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                bgAlpha = 0;
                animationManager.fadeOutStage(mainStage, MainMenuScreen.this, new CreditsScreen(game));
            }
        });

        mainStage.addFocusableActor(settingsButton);
        mainStage.addFocusableActor(creditsButton);
        mainStage.addFocusableActor(devButton);

        HoverImageButton exitButton = uiButtonManager.addExitButtonToStage(game.assets.getTexture(Assets.TextureObject.CROSS_TEXTURE));
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
        Texture playSheet = game.assets.getTexture(Assets.TextureObject.PLAY_SHEET_TEXTURE);
        int rowCount = 4, colCount = 4;

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

        Animation<TextureRegion> playButtonAnimation = new Animation<>(1 / 15f, playFrames);
        playButtonAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

        float buttonSize = 144f;

        final HoverImageButton playButton = new HoverImageButton(new AnimatedDrawable(playButtonAnimation, buttonSize, buttonSize, game.getScaleFactor()));
        playButton.setTransform(true);
        playButton.setHoverAlpha(.75f);
        playButton.setClickAlpha(.6f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (roundedCornerNP == null) {
                    // Should almost never happen cause processors are hella fast, even mobile ones
                    return;
                }

                if (!playModeStage.getRoot().hasChildren()) {
                    // Causes a bit of noticeable lag on my old mobile
                    setUpPlayMenu();
                }

                bgAlpha = .2f;

                animationManager.startPlayModeAnimation(mainStage, playModeStage, playButton);
            }
        });

        mainTable.add(playButton).pad(10f * game.getScaleFactor()).size(buttonSize * game.getScaleFactor());
        mainTable.row();

        return playButton;
    }

    private void setUpPlayMenu() {
        final Table playModeTable = new Table();
        playModeTable.setFillParent(true);
        playModeTable.center();

        final GameMode[] gameModes = new GameMode[]{
                new GameMode("Splitscreen\nMultiplayer", "images/multiplayer_icons/ic_splitscreen_multiplayer.png", PlayerSetUpScreen.class),
                new GameMode("Local\nNetwork\nMultiplayer", "images/multiplayer_icons/ic_local_multiplayer.png", HostJoinScreen.class),
        };

        Label chooseMode = new Label("Choose the game mode", game.skin, "themed-rounded-background");
        float headerPad = uiButtonManager.getHeaderPad(chooseMode.getPrefHeight());
        playModeTable.add(chooseMode).expandX().padTop(headerPad).colspan(gameModes.length);
        playModeTable.row();

        final float normalAlpha = .15f;
        final float hoverAlpha = .25f;
        final float clickAlpha = .5f;
        for (int i = 0; i < gameModes.length; i++) {
            Table table = new Table();
            table.center();

            final Image backgroundColorImage = new Image(roundedCornerNP);
            backgroundColorImage.getColor().a = normalAlpha;
            backgroundColorImage.setScaling(Scaling.stretch);

            Image backgroundImage = null;
            if (gameModes[i].getPreviewLocation() != null) {
                Texture backgroundImageTexture = new Texture(Gdx.files.internal(gameModes[i].getPreviewLocation()));
                usedTextures.add(backgroundImageTexture);
                backgroundImage = new Image(backgroundImageTexture);
                backgroundImage.setScaling(Scaling.fit);
                backgroundImage.getColor().a = .3f;
            }

            Label modeLabel = new Label(gameModes[i].getName(), game.skin);
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
                    try {
                        bgAlpha = 0;
                        animationManager.fadeOutStage(playModeStage, MainMenuScreen.this,
                                (Screen) ClassReflection.getConstructor(gameModes[finalI].getTargetClass(), Zoned.class).newInstance(game));
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

            float optionPadding = 50f, padLeft = optionPadding, padRight = optionPadding;
            if (i == 0) {
                if (gameModes.length != 1) {
                    padRight /= 2;
                }
            } else if (i == gameModes.length - 1) {
                padLeft /= 2;
            } else {
                padLeft /= 2;
                padRight /= 2;
            }
            table.add(stack).grow().padLeft(padLeft).padTop(optionPadding).padBottom(optionPadding).padRight(padRight);

            playModeTable.add(table).grow().uniform();
            playModeStage.addFocusableActor(table);
        }

        HoverImageButton hideButton = uiButtonManager.addHideButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
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
        super.render(delta);

        batch.begin();
        drawBG(batch, delta);
        batch.end();

        mainStage.act(delta);
        if (mainStage.getRoot().getColor().a > 0) {
            mainStage.draw();
        }

        playModeStage.act(delta);
        if (playModeStage.getRoot().getColor().a > 0) {
            playModeStage.draw();
        }

        displayFPS();
    }

    private void drawBG(Batch batch, float delta) {
        if (bgTexture != null) {
            int bgTextureW = bgTexture.getWidth();
            int bgTextureH = bgTexture.getHeight();
            float stageW = mainStage.getWidth();
            float stageH = mainStage.getHeight();

            Color color = batch.getColor();
            if (color.a != bgAlpha) {
                color.a += delta * 5f * (bgAlpha - color.a);
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
            bgScrollAmount += 20 * delta;
            if (bgScrollAmount > bgTextureH) {
                bgScrollAmount %= bgTextureH;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        playModeStage.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        // mainStage (screenStage) is disposed in super above
        playModeStage.dispose();
    }

    private void showExitDialog() {
        mainStage.showDialog("Are you sure that you want to exit?",
                new FocusableStage.DialogButton[]{ FocusableStage.DialogButton.Cancel, FocusableStage.DialogButton.Exit },
                false, button -> {
                    if (button == FocusableStage.DialogButton.Exit) {
                        Gdx.app.exit();
                    }
                });
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (mainStage.dialogIsActive()) {
            // Exit dialog is active
            // Return false as the dialog can take keyboard inputs
            return false;
        }

        if (mainStage.getRoot().getColor().a == 1f) {
            showExitDialog();
        } else {
            bgAlpha = 1f;
            animationManager.endPlayModeAnimation(mainStage, playModeStage);
        }

        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            return onBackPressed();
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
