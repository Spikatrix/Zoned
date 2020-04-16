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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

public class MainMenuScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage mainStage;
    private FocusableStage playModeStage;
    private Viewport viewport;
    private TextButton[] mainMenuButtons;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private ParticleEffect emitterLeft, emitterRight;

    public MainMenuScreen(final Zoned game) {
        this.game = game;

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
        setUpPlayMenu();
        showFPSCounter = game.preferences.getBoolean(Constants.FPS_PREFERENCE, false);
        animationManager.startMainMenuAnimation(mainStage, mainMenuButtons);
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
    }

    private void setUpMainMenu() {
        final Table mainTable = new Table();
        mainTable.setFillParent(true);
        //mainTable.setDebug(true);
        mainTable.center();

        Label gameTitle = new Label("ZONED", game.skin, Constants.FONT_MANAGER.LARGE.getName(), Color.GREEN);
        mainTable.add(gameTitle).pad(5f * game.getScaleFactor());
        mainTable.row();

        HoverImageButton playButton = new HoverImageButton(new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/ui_icons/ic_play.png"))));
        playButton.setHoverAlpha(.85f);
        playButton.setClickAlpha(.75f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.startPlayModeAnimation(mainStage, playModeStage);
            }
        });
        mainTable.add(playButton).pad(10f);
        mainTable.row();

        mainStage.addFocusableActor(playButton);
        mainStage.row();

        mainMenuButtons = new TextButton[]{
                /*new TextButton("Play Splitscreen Multiplayer", game.skin),
                new TextButton("Play Local Multiplayer", game.skin),*/
                new TextButton("Settings", game.skin),
                new TextButton("Testing room", game.skin),
                new TextButton("Exit", game.skin)
        };

        final Screen[] screens = new Screen[]{
                /*new PlayerSetUpScreen(game),
                new HostJoinScreen(game),*/
                new SettingsScreen(game),
                new TestScreen(game),
        };

        for (int i = 0; i < mainMenuButtons.length; i++) {
            if (i == mainMenuButtons.length - 1) {
                mainMenuButtons[i].addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Gdx.app.exit();
                    }
                });
                continue;
            }

            final int screenIndex = i;
            mainMenuButtons[i].addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    emitterLeft.allowCompletion();
                    emitterRight.allowCompletion();
                    animationManager.fadeOutStage(mainStage, screens[screenIndex]);
                }
            });
        }

        for (TextButton button : mainMenuButtons) {
            mainTable.add(button).padRight(10 * game.getScaleFactor()).padLeft(10 * game.getScaleFactor()).width(350 * game.getScaleFactor()).expandX();
            mainTable.row();

            mainStage.addFocusableActor(button);
            mainStage.row();
        }

        mainStage.setFocusedActor(mainMenuButtons[0]);
        mainStage.addActor(mainTable);
    }

    private void setUpPlayMenu() {
        final Table playModeTable = new Table();
        playModeTable.setFillParent(true);
        playModeTable.center();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        TextureRegionDrawable whiteBG = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();

        Table leftTable = new Table();
        leftTable.center();

        Table rightTable = new Table();
        rightTable.center();

        final Image leftBackgroundColor = new Image(whiteBG);
        final Image rightBackgroundColor = new Image(whiteBG);

        leftBackgroundColor.getColor().a = 0;
        rightBackgroundColor.getColor().a = 0;

        leftBackgroundColor.setScaling(Scaling.stretch);
        rightBackgroundColor.setScaling(Scaling.stretch);

        Image splitscreenMultiplayerImage = new Image(new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/multiplayer_icons/ic_splitscreen_multiplayer.png"))));
        Image localMultiplayerImage = new Image(new TextureRegionDrawable(new Texture(Gdx.files.internal("icons/multiplayer_icons/ic_local_multiplayer.png"))));

        splitscreenMultiplayerImage.setScaling(Scaling.fit);
        localMultiplayerImage.setScaling(Scaling.fit);

        splitscreenMultiplayerImage.getColor().a = .3f;
        localMultiplayerImage.getColor().a = .3f;

        Label splitscreenMultiplayerLabel = new Label("Splitscreen\nMultiplayer", game.skin);
        Label localMultiplayerLabel = new Label("Local\nMultiplayer\n(WiFi)", game.skin);

        leftTable.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                leftBackgroundColor.addAction(Actions.alpha(.3f, .15f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                leftBackgroundColor.addAction(Actions.alpha(0, .15f));
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(playModeStage, new PlayerSetUpScreen(game));
            }
        });

        rightTable.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                rightBackgroundColor.addAction(Actions.alpha(.3f, .15f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                rightBackgroundColor.addAction(Actions.alpha(0, .15f));
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.fadeOutStage(playModeStage, new HostJoinScreen(game));
            }
        });

        splitscreenMultiplayerLabel.setAlignment(Align.center);
        localMultiplayerLabel.setAlignment(Align.center);

        Stack leftStack = new Stack(leftBackgroundColor, splitscreenMultiplayerImage, splitscreenMultiplayerLabel);
        Stack rightStack = new Stack(rightBackgroundColor, localMultiplayerImage, localMultiplayerLabel);

        leftTable.add(leftStack).expand().pad(40f);
        rightTable.add(rightStack).expand().pad(40f);

        Label chooseMode = new Label("Choose a game mode", game.skin, "themed");
        playModeTable.add(chooseMode).expandX().pad(10f).colspan(2);
        playModeTable.row();

        playModeTable.add(leftTable).expand().uniform();
        playModeTable.add(rightTable).expand().uniform();

        HoverImageButton exitButton = UIButtonManager.addBackButtonToStage(playModeStage, game.getScaleFactor());
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                animationManager.endPlayModeAnimation(mainStage, playModeStage);
            }
        });

        playModeStage.getRoot().getColor().a = 0;

        playModeStage.addFocusableActor(leftTable);
        playModeStage.addFocusableActor(rightTable);
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
            FPSDisplayer.displayFPS(viewport, mainStage.getBatch(), font);
        }

        mainStage.draw();
        mainStage.act(delta);

        playModeStage.draw();
        playModeStage.act(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        emitterRight.setPosition(viewport.getWorldWidth(), 0);
    }

    @Override
    public void dispose() {
        mainStage.dispose();
        playModeStage.dispose();
        emitterLeft.dispose();
        emitterRight.dispose();
    }

    private void onBackPressed() {
        if (mainStage.getRoot().getColor().a == 1f) {
            Gdx.app.exit();
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
