package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.ui.FocusableStage;

public class VictoryScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private FocusableStage stage;
    private Viewport viewport;
    private AnimationManager animationManager;

    private SpriteBatch batch;
    private BitmapFont font;
    private ParticleEffect emitter;

    public VictoryScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(this.game, this);

        this.batch = new SpriteBatch();
        this.font = game.skin.getFont("regular-font");
    }

    @Override
    public void show() {
        setUpStage();
        animationManager.fadeInStage(stage);

        emitter = new ParticleEffect();
        emitter.load(Gdx.files.internal("particles/radial_particle_emitter.p"), Gdx.files.internal("particles"));
        emitter.start();
    }

    private void setUpStage() {
        Table table = new Table();
        //table.setDebug(true);
        table.setFillParent(true);
        table.center();

        // TODO: Add UI components

        stage.addActor(table);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        emitter.setPosition(width / 2f, height / 2f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(true);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        emitter.draw(batch, delta);
        batch.end();

        stage.draw();
        stage.act(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
        emitter.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            animationManager.fadeOutStage(stage, new MainMenuScreen(game));
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
