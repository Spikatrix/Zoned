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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Preferences;
import com.cg.zoned.UITextDisplayer;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.ParticleEffectActor;

public class CreditsScreen extends ScreenAdapter implements InputProcessor {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private ScreenViewport viewport;
    private FocusableStage stage;
    private AnimationManager animationManager;
    private boolean showFPSCounter;
    private BitmapFont font;

    private Color linkColor = new Color(.4f, .4f, 1f, 1f);
    private ParticleEffect clickParticleEffect = null;

    public CreditsScreen(final Zoned game) {
        this.game = game;
        game.discordRPCManager.updateRPC("Viewing credits");

        this.viewport = new ScreenViewport();
        this.stage = new FocusableStage(this.viewport);
        this.animationManager = new AnimationManager(game, this);
        this.font = game.skin.getFont(Assets.FontManager.SMALL.getFontName());
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        showFPSCounter = game.preferences.getBoolean(Preferences.FPS_PREFERENCE, false);

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
                Gdx.files.internal("images/ic_zoned_desktop_icon.png"),
                "ZONED",
                "Game version: " + Constants.GAME_VERSION);

        addCreditItem(table,
                "Developer", "Spikatrix");

        addCreditItem(table,
                "Original Neon-UI Skin", "Raymond \"Raeleus\" Buckley");

        addCreditItem(table,
                "Powered By",
                Gdx.files.internal("images/credits_icons/ic_libgdx.png"), null,
                "https://libgdx.com");

        addCreditItem(table,
                "Inspired By",
                Gdx.files.internal("images/credits_icons/ic_codingame.png"), "Back to the Code",
                "https://www.codingame.com/multiplayer/bot-programming/back-to-the-code");

        addCreditItem(table,
                "Contribute to the game",
                Gdx.files.internal("images/credits_icons/ic_github.png"), "GitHub",
                "https://github.com/Spikatrix/Zoned");

        addCreditItem(table,
                "Feedback",
                Gdx.files.internal("images/credits_icons/ic_gmail.png"), "cg.devworks@gmail.com",
                "mailto:cg.devworks@gmail.com?subject=Zoned+Feedback");

        addCreditItem(table,
                "Thank You", "for playing");

        masterTable.add(screenScrollPane).grow();

        stage.setScrollFocus(screenScrollPane);
        stage.addActor(masterTable);
    }

    private void addCreditItem(Table table, FileHandle imageLocation, String title, String version) {
        final Table innerTable = new Table();
        Texture gameLogo = new Texture(imageLocation);
        usedTextures.add(gameLogo);

        clickParticleEffect = new ParticleEffect();
        clickParticleEffect.load(Gdx.files.internal("particles/radial_particle_emitter.p"), Gdx.files.internal("particles"));
        final ParticleEffectActor particleEffectActor = new ParticleEffectActor(clickParticleEffect);

        final Image gameLogoImage = new Image(gameLogo);
        gameLogoImage.setScaling(Scaling.fit);
        gameLogoImage.getColor().a = .3f;

        final Label titleLabel = new Label(title, game.skin, Assets.FontManager.STYLED_LARGE.getFontName(), Color.GREEN);
        titleLabel.setAlignment(Align.center);

        Stack stack = new Stack();
        stack.add(particleEffectActor);
        stack.add(gameLogoImage);
        stack.add(titleLabel);

        float height = titleLabel.getPrefHeight() * 3 / 2f;
        innerTable.add(stack).height(height);

        if (title.equals("ZONED")) {
            final int[] clickCount = {0}; // Have to use an array here because Java

            innerTable.setTouchable(Touchable.enabled);
            innerTable.setTransform(true);
            innerTable.setOrigin(innerTable.getPrefWidth() / 2, height / 2);
            innerTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    innerTable.clearActions();
                    innerTable.setScale(1.2f);
                    innerTable.addAction(Actions.scaleTo(1.0f, 1.0f, .3f, Interpolation.smooth));

                    particleEffectActor.start();

                    clickCount[0]++;
                    if (clickCount[0] >= 5) {
                        toggleDevMode();
                        clickCount[0] = 0;
                    }
                }
            });
        }

        Label versionLabel = new Label(version, game.skin);
        versionLabel.setAlignment(Align.center);

        float spaceBottom = (stage.getHeight() / 4) - (height / 2) - versionLabel.getPrefHeight();
        float padTop = (stage.getHeight() / 2) - (height / 2);

        spaceBottom = Math.max(spaceBottom, 10f);

        table.add(innerTable).expandX()
                .padTop(padTop)
                .padLeft(10f)
                .padRight(10f);
        table.row();

        table.add(versionLabel).growX()
                .spaceBottom(spaceBottom)
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
        if (content != null) {
            contentTable.add(image).height(contentLabel.getPrefHeight()).width(contentLabel.getPrefHeight());
            contentTable.add(contentLabel).padLeft(20f);
        } else {
            contentTable.add(image).height(contentLabel.getPrefHeight() * 4 / 3).width(3 * stage.getWidth() / 4);
        }

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

        if (title.contains("Thank You")) { // Last item in the credits screen
            table.add(titleLabel).growX()
                    .padTop(stage.getHeight() / 2)
                    .padLeft(10f)
                    .padRight(10f);
            table.row();
            table.add(contentLabel).growX()
                    .padBottom((stage.getHeight() / 2) - titleLabel.getHeight())
                    .padLeft(10f)
                    .padRight(10f);
            // Hacky line above, but this whole thing is kinda hacky and doesn't work on resize anyway
        } else {
            table.add(titleLabel).growX()
                    .padTop(stage.getHeight() / 5)
                    .padLeft(10f)
                    .padRight(10f);
            table.row();
            table.add(contentLabel).growX()
                    .padBottom(stage.getHeight() / 5)
                    .padLeft(10f)
                    .padRight(10f);
        }
        table.row();
    }

    private void toggleDevMode() {
        // owo what's this

        boolean devModeUnlocked = game.preferences.getBoolean(Preferences.DEV_MODE_PREFERENCE, false);
        devModeUnlocked = !devModeUnlocked;
        game.preferences.putBoolean(Preferences.DEV_MODE_PREFERENCE, devModeUnlocked);
        game.preferences.flush();

        stage.showOKDialog("Developer mode " + ((devModeUnlocked) ? ("un") : ("re")) + "locked!",
                false, game.getScaleFactor(),
                null, game.skin);
    }

    private void setUpBackButton() {
        UIButtonManager uiButtonManager = new UIButtonManager(stage, game.getScaleFactor(), usedTextures);
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getBackButtonTexture());
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

        if (showFPSCounter) {
            UITextDisplayer.displayFPS(viewport, stage.getBatch(), font);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
        if (clickParticleEffect != null) {
            clickParticleEffect.dispose();
            clickParticleEffect = null;
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
