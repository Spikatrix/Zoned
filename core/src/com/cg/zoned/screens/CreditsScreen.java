package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
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
        table.setDebug(true);
        table.center();
        ScrollPane screenScrollPane = new ScrollPane(table);
        screenScrollPane.setOverscroll(false, true);

        Table firstInnerTable = new Table();
        Texture gameLogo = new Texture(Gdx.files.internal("icons/ic_zoned_desktop_icon.png"));
        usedTextures.add(gameLogo);
        Image gameLogoImage = new Image(gameLogo);
        Label gameTitle = new Label("ZONED", game.skin, Constants.FONT_MANAGER.LARGE.getName(), Color.GREEN);
        gameTitle.setAlignment(Align.center);
        firstInnerTable.add(gameLogoImage);
        firstInnerTable.row();
        firstInnerTable.add(gameTitle);
        addCreditItemToTable(table, firstInnerTable);

        // TODO: Work on this screen

        Table secondInnerTable = getInnerTable("Programmer", "Spikatrix");
        addCreditItemToTable(table, secondInnerTable);

        Table thirdInnerTable = getInnerTable("Neon-UI Skin", "Raymond \"Raeleus\" Buckley");
        addCreditItemToTable(table, thirdInnerTable);

        masterTable.add(screenScrollPane).grow();

        stage.setScrollFocus(screenScrollPane);
        stage.addActor(masterTable);
    }

    private Table getInnerTable(String title, String name) {
        Table innerTable = new Table();

        Label titleLabel = new Label(title, game.skin, "themed");
        Label nameLabel = new Label(name, game.skin);

        titleLabel.setAlignment(Align.center);
        nameLabel.setAlignment(Align.center);

        innerTable.add(titleLabel);
        innerTable.row();
        innerTable.add(nameLabel);

        return innerTable;
    }

    private void addCreditItemToTable(Table table, Actor actor) {
        float actorHeight = actor.getHeight();
        if (actor instanceof Table) {
            for (Actor innerActor : ((Table) actor).getChildren()) {
                actorHeight += innerActor.getHeight();
            }
        }

        Gdx.app.log(Constants.LOG_TAG, "Actor height: " + actorHeight);

        table.add(actor).grow()
                .padBottom((stage.getHeight() / 2) - (actorHeight / 2))
                .padTop((stage.getHeight() / 2) - (actorHeight / 2))
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

        UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
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
