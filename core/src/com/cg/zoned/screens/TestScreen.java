package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.Zoned;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.crashinvaders.vfx.effects.MotionBlurEffect;
import com.crashinvaders.vfx.filters.MotionBlurFilter;

// Used for testing purposes, nvm about the crap in here
public class TestScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private ScreenViewport viewport;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private VfxManager vfxManager;
    private BloomEffect bloomEffect;
    private MotionBlurEffect motionBlurEffect;

    private Rectangle[] rects;
    private Color[] colors;
    private int index = 0;

    private float x;
    private float y;
    private float width;
    private float height;
    private Color color;

    public TestScreen(final Zoned game) {
        this.game = game;

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.font = game.skin.getFont(Constants.FONT_SIZE_MANAGER.REGULAR.getName());

        this.rects = new Rectangle[]{
                new Rectangle(40, 40, 40, 40),
                new Rectangle(40, 200, 100, 150),
                new Rectangle(400, 200, 200, 20),
                new Rectangle(400, 40, 40, 320),
        };
        this.colors = new Color[]{
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.YELLOW,
        };

        x = this.rects[0].x;
        y = this.rects[0].y;
        width = this.rects[0].width;
        height = this.rects[0].height;
        color = new Color(this.colors[0]);
    }

    @Override
    public void show() {
        this.vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        this.bloomEffect = new BloomEffect(Pixmap.Format.RGBA8888);
        this.motionBlurEffect = new MotionBlurEffect(Pixmap.Format.RGBA8888, MotionBlurFilter.BlurFunction.MIX, .5f);

        this.vfxManager.addEffect(this.bloomEffect);
        this.vfxManager.addEffect(this.motionBlurEffect);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void resize(int width, int height) {
        this.viewport.update(width, height, true);
        this.vfxManager.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.viewport.apply(true);
        this.vfxManager.cleanUpBuffers();

        renderer.setProjectionMatrix(this.viewport.getCamera().combined);
        batch.setProjectionMatrix(this.viewport.getCamera().combined);

        lerpRect(delta);

        vfxManager.beginCapture();
        vfxManager.setBlendingEnabled(true);
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        renderer.setColor(color);
        renderer.rect(x, y, width, height);

        renderer.end();
        vfxManager.endCapture();

        vfxManager.applyEffects();
        vfxManager.renderToScreen();

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 5, Gdx.graphics.getHeight() - 15);
        font.draw(batch, "Bloom + MotionBlur", 5, Gdx.graphics.getHeight() - 45); // ~10 FPS on my Mobile although I get 60 FPS on my laptop :(
        batch.end();
    }

    private void lerpRect(float delta) {
        float lerp = 1.8f;
        float threshold = 3f;

        x += (rects[index].x - x) * lerp * delta;
        y += (rects[index].y - y) * lerp * delta;
        width += (rects[index].width - width) * lerp * delta;
        height += (rects[index].height - height) * lerp * delta;

        color.lerp(colors[index], lerp * delta);

        if (Math.abs(x - rects[index].x) <= threshold &&
                Math.abs(y - rects[index].y) <= threshold &&
                Math.abs(width - rects[index].width) <= threshold &&
                Math.abs(height - rects[index].height) <= threshold) {
            index = (index + 1) % rects.length;
        }
    }

    @Override
    public void dispose() {
        vfxManager.dispose();
        bloomEffect.dispose();
        motionBlurEffect.dispose();
        renderer.dispose();
        batch.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            game.setScreen(new MainMenuScreen(game));
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
