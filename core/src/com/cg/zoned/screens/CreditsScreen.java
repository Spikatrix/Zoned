package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;

public class CreditsScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ScreenViewport viewport;
    private FocusableStage stage;
    private AnimationManager animationManager;
    private BitmapFont font;

    private ParticleEffect clickEffect;

    private Color linkColor = new Color(.3f, .3f, 1f, 1f);

    public CreditsScreen(final Zoned game) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(game, this);
        this.font = game.skin.getFont(Constants.FONT_MANAGER.SMALL.getName());
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        animationManager.fadeInStage(stage);
    }

    private void setUpStage() {
        Table masterTable = new Table();
        masterTable.setFillParent(true);
        masterTable.center();

        Table table = new Table();
        //table.setDebug(true);
        table.center();
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);

        addCreditItem(table,
                Gdx.files.internal("icons/ic_zoned_desktop_icon.png"), "ZONED");

        addCreditItem(table,
                "Developer", "Spikatrix");

        addCreditItem(table,
                "Neon-UI Skin", "Raymond \"Raeleus\" Buckley");

        addCreditItem(table,
                "Inspired By",
                Gdx.files.internal("icons/codingame-logo.png"), "Back to the Code",
                "https://www.codingame.com/multiplayer/bot-programming/back-to-the-code");

        addCreditItem(table,
                "Contribute to the game",
                Gdx.files.internal("icons/github-logo.png"), "GitHub",
                "https://github.com/Spikatrix/Zoned");

        addCreditItem(table,
                "Feedback/Bug Reports",
                Gdx.files.internal("icons/gmail-logo.png"), "cg.devworks@gmail.com",
                "mailto:cg.devworks@gmail.com");

        addCreditItem(table,
                "Thank You", "for playing");

        masterTable.add(screenScrollPane).grow();

        stage.setScrollFocus(screenScrollPane);
        stage.addActor(masterTable);
    }

    private void addCreditItem(Table table, FileHandle imageLocation, String title) {
        final Table innerTable = new Table();
        Texture gameLogo = new Texture(imageLocation);
        usedTextures.add(gameLogo);

        final Image gameLogoImage = new Image(gameLogo);
        gameLogoImage.setScaling(Scaling.fit);
        gameLogoImage.getColor().a = .3f;

        final Label titleLabel = new Label(title, game.skin, Constants.FONT_MANAGER.LARGE.getName(), Color.GREEN);
        titleLabel.setAlignment(Align.center);

        Stack stack = new Stack();
        stack.add(gameLogoImage);
        stack.add(titleLabel);

        clickEffect = new ParticleEffect();
        clickEffect.load(Gdx.files.internal("particles/click_effect.p"), Gdx.files.internal("particles"));

        float height = titleLabel.getPrefHeight() * 3 / 2f;
        innerTable.add(stack).height(height);

        innerTable.setTouchable(Touchable.enabled);
        innerTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                /*clickEffect.setPosition(innerTable.getX(), 100);
                clickEffect.start();*/
            }
        });

        float spaceBottom = (stage.getHeight() / 4) - (height / 2);
        float padTop = (stage.getHeight() / 2) - (height / 2);

        table.add(innerTable).grow()
                .spaceBottom(spaceBottom)
                .padTop(padTop)
                .padLeft(10f)
                .padRight(10f);
        table.row();
    }

    private void addCreditItem(Table table, String title, FileHandle imageLocation, final String content, final String link) {
        Label titleLabel = new Label(title, game.skin, "themed");
        Label contentLabel = new Label(content, game.skin);

        titleLabel.setAlignment(Align.center);
        contentLabel.setAlignment(Align.center);

        Texture imageTexture = new Texture(imageLocation);
        usedTextures.add(imageTexture);
        Image image = new Image(imageTexture);
        image.setScaling(Scaling.fit);

        Table contentTable = new Table();
        contentTable.add(image).height(contentLabel.getPrefHeight()).width(contentLabel.getPrefHeight());
        contentTable.add(contentLabel).padLeft(20f);

        if (link != null) {
            contentLabel.setColor(linkColor);
            contentTable.setTouchable(Touchable.enabled);
            contentTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.net.openURI(link);
                }
            });
        }

        table.add(titleLabel).growX()
                .padTop(stage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
        table.add(contentTable).growX()
                .padBottom(stage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
    }

    private void addCreditItem(Table table, String title, final String content) {
        Label titleLabel = new Label(title, game.skin, "themed");
        Label contentLabel = new Label(content, game.skin);

        titleLabel.setAlignment(Align.center);
        contentLabel.setAlignment(Align.center);

        float lastItemExtraPaddingOffset =
                ((title.contains("Thank You")) ? (5 / 2f) : (1));

        table.add(titleLabel).growX()
                .padTop(stage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
        table.add(contentLabel).growX()
                .padBottom(lastItemExtraPaddingOffset * stage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage();
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.viewport.apply(true);

        stage.getBatch().begin();
        clickEffect.draw(stage.getBatch(), delta);
        stage.getBatch().end();

        UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        clickEffect.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private void onBackPressed() {
        animationManager.fadeOutStage(stage, this, new MainMenuScreen(game));
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
