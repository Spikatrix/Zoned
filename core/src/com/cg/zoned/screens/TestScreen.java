package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.FPSDisplayer;
import com.cg.zoned.Zoned;
import com.payne.games.piemenu.PieMenu;

// Used for testing purposes, nvm about the crap in here
public class TestScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private ScreenViewport viewport;
    private Stage stage;
    private ShapeRenderer renderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private PieMenu menu;

    public TestScreen(final Zoned game) {
        this.game = game;

        this.renderer = new ShapeRenderer();
        this.renderer.setAutoShapeType(true);
        this.batch = new SpriteBatch();
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        Texture tmpTex = new Texture(pixmap);
        pixmap.dispose();
        TextureRegion whitePixel = new TextureRegion(tmpTex);

        PieMenu.PieMenuStyle style = new PieMenu.PieMenuStyle();
        style.separatorWidth = 1;
        style.backgroundColor = Color.BLACK;
        style.separatorColor = Color.BLACK;
        style.downColor = Color.WHITE;
        style.sliceColor = Color.RED;
        menu = new PieMenu(whitePixel, style, 92);

        menu.setInfiniteSelectionRange(true);
        menu.setSelectionButton(Input.Buttons.LEFT); // right-click for interactions with the widget

        /* Populating the widget. */
        final Drawable arrow = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("icons/ic_arrow.png"))));
        final Image img1 = new Image(arrow);
        img1.setOrigin(img1.getWidth() / 2, img1.getHeight() / 2);
        img1.setRotation(-45f);
        final Image img2 = new Image(arrow);
        img2.setOrigin(img1.getWidth() / 2, img1.getHeight() / 2);
        img2.setRotation(45f);
        final Image img3 = new Image(arrow);
        img3.setOrigin(img1.getWidth() / 2, img1.getHeight() / 2);
        img3.setRotation(135f);
        final Image img4 = new Image(arrow);
        img4.setOrigin(img1.getWidth() / 2, img1.getHeight() / 2);
        img4.setRotation(-135f);
        menu.addActor(img1);
        menu.addActor(img2);
        menu.addActor(img3);
        menu.addActor(img4);

        menu.setRotation(45f);

        menu.addListener(new PieMenu.PieMenuCallbacks() {
            @Override
            public void onHighlightChange(int highlightedIndex) {
                img1.setColor(Color.WHITE);
                img2.setColor(Color.WHITE);
                img3.setColor(Color.WHITE);
                img4.setColor(Color.WHITE);
                System.out.println("onHighlightChange - highlighted index: " + highlightedIndex);
                if (highlightedIndex == 0) img1.setColor(Color.BLACK);
                if (highlightedIndex == 1) img2.setColor(Color.BLACK);
                if (highlightedIndex == 2) img3.setColor(Color.BLACK);
                if (highlightedIndex == 3) img4.setColor(Color.BLACK);

            }
        });
        menu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                System.out.println("ChangeListener - selected index: " + menu.getSelectedIndex());
                menu.setVisible(false);
                menu.remove();
            }
        });

        InputMultiplexer inputMultiplexer = new InputMultiplexer(stage, this);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void resize(int width, int height) {
        this.viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.viewport.apply(true);

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isButtonJustPressed(menu.getSelectionButton())) {
            stage.addActor(menu);
            menu.centerOnMouse();
            menu.setVisible(true);
            transferInteraction(stage, menu);
        }

        renderer.setProjectionMatrix(this.viewport.getCamera().combined);
        batch.setProjectionMatrix(this.viewport.getCamera().combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.end();

        batch.begin();
        batch.end();

        FPSDisplayer.displayFPS(batch, font);
    }

    private void transferInteraction(Stage stage, PieMenu widget) {
        if (widget == null) throw new IllegalArgumentException("widget cannot be null.");
        if (widget.getPieMenuListener() == null)
            throw new IllegalArgumentException("inputListener cannot be null.");
        stage.addTouchFocus(widget.getPieMenuListener(), widget, widget, 0, widget.getSelectionButton());
    }

    @Override
    public void dispose() {
        stage.dispose();
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
        if (button == Input.Buttons.BACK) {
            game.setScreen(new MainMenuScreen(game));
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
