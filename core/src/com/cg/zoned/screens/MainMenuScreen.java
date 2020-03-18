package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;

public class MainMenuScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage stage;
    private Viewport viewport;
    private TextButton[] mainMenuButtons;
    private AnimationManager animationManager;

    private ParticleEffect emitterLeft, emitterRight;

    public MainMenuScreen(final Zoned game) {
        this.game = game;

        viewport = new ScreenViewport();
        stage = new FocusableStage(viewport);
        animationManager = new AnimationManager(this.game, this);

        emitterLeft = new ParticleEffect();
        emitterRight = new ParticleEffect();
    }

    @Override
    public void show() {
        setUpMainMenu();
        animationManager.startMainMenuAnimation(stage, mainMenuButtons);
        animationManager.setAnimationListener(new AnimationManager.AnimationListener() {
            @Override
            public void animationEnd(Stage stage) {
                emitterLeft.load(Gdx.files.internal("particles/right_emitter.p"), Gdx.files.internal("particles"));
                emitterLeft.start();
                emitterRight.load(Gdx.files.internal("particles/left_emitter.p"), Gdx.files.internal("particles"));
                emitterRight.setPosition(viewport.getScreenWidth(), 0);
                emitterRight.start();
            }
        });
    }

    private void setUpMainMenu() {
        Table table = new Table();
        table.setFillParent(true);
        //table.setDebug(true);
        table.center();

        Label gameTitle = new Label("ZONED", game.skin, "large-font", Color.GREEN);
        table.add(gameTitle).pad(5f * game.getScaleFactor());
        table.row();

        mainMenuButtons = new TextButton[]{
                new TextButton("Play on this device", game.skin),
                new TextButton("Play on multiple devices", game.skin),
                new TextButton("Settings", game.skin),
                new TextButton("Testing room", game.skin),
                new TextButton("Exit", game.skin)
        };

        final Screen[] screens = new Screen[]{
                new PlayerSetUpScreen(game),
                new HostJoinScreen(game),
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
                    animationManager.fadeOutStage(stage, screens[screenIndex]);
                }
            });
        }

        for (TextButton button : mainMenuButtons) {
            table.add(button).padRight(10 * game.getScaleFactor()).padLeft(10 * game.getScaleFactor()).width(350 * game.getScaleFactor()).expandX();
            table.row();

            stage.addFocusableActor(button);
            stage.row();
        }

        stage.setFocusedActor(mainMenuButtons[0]);
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);

        stage.getBatch().begin();
        emitterLeft.draw(stage.getBatch(), delta);
        emitterRight.draw(stage.getBatch(), delta);
        stage.getBatch().end();

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        emitterRight.setPosition(width, 0);
    }

    @Override
    public void dispose() {
        stage.dispose();
        emitterLeft.dispose();
        emitterRight.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            Gdx.app.exit();
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
