package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.utils.Scaling;
import com.cg.zoned.Assets;
import com.cg.zoned.Constants;
import com.cg.zoned.Preferences;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.AnimationManager;
import com.cg.zoned.managers.UIButtonManager;
import com.cg.zoned.ui.HoverImageButton;
import com.cg.zoned.ui.ParticleEffectActor;

public class CreditsScreen extends ScreenObject implements InputProcessor {
    private final Color linkColor = new Color(.4f, .4f, 1f, 1f);
    private ParticleEffect clickParticleEffect = null;

    public CreditsScreen(final Zoned game) {
        super(game);
        game.discordRPCManager.updateRPC("Viewing credits");

        this.animationManager = new AnimationManager(game, this);
        this.uiButtonManager = new UIButtonManager(screenStage, game.getScaleFactor(), usedTextures);
    }

    @Override
    public void show() {
        setUpStage();
        setUpBackButton();

        animationManager.fadeInStage(screenStage);
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
                Gdx.files.internal("images/desktop_icons/ic_zoned_desktop_icon_128x128.png"),
                "ZONED",
                "Game version: " + Constants.GAME_VERSION);

        addCreditItem(table,
                "Developer", "Spikatrix");

        addCreditItem(table,
                "Original Neon-UI Skin", "Raymond \"Raeleus\" Buckley");

        addCreditItem(table,
                "Powered by",
                Gdx.files.internal("images/credits_icons/ic_libgdx.png"), null,
                "https://libgdx.com");

        addCreditItem(table,
                "Inspired by",
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

        screenStage.setScrollFocus(screenScrollPane);
        screenStage.addActor(masterTable);
    }

    private void addCreditItem(Table table, FileHandle imageLocation, String title, String version) {
        final Table innerTable = new Table();
        Texture gameLogo = new Texture(imageLocation);
        usedTextures.add(gameLogo);

        clickParticleEffect = new ParticleEffect();
        clickParticleEffect.load(Gdx.files.internal("particles/radial_particle_emitter.p"), Gdx.files.internal("particles"));
        clickParticleEffect.scaleEffect(game.getScaleFactor());
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

        screenStage.addFocusableActor(innerTable);
        screenStage.setFocusedActor(innerTable);
        screenStage.row();

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

        float spaceBottom = (screenStage.getHeight() / 4) - (height / 2) - versionLabel.getPrefHeight();
        float padTop = (screenStage.getHeight() / 2) - (height / 2);

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
            contentTable.add(contentLabel).padLeft(12f * game.getScaleFactor());
        } else {
            contentTable.add(image).height(contentLabel.getPrefHeight() * 4 / 3).width(3 * screenStage.getWidth() / 4);
        }

        screenStage.addFocusableActor(contentTable);
        screenStage.row();

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
                .padTop(screenStage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
        table.add(contentTable).growX()
                .padBottom(screenStage.getHeight() / 5)
                .padLeft(10f)
                .padRight(10f);
        table.row();
    }

    private void addCreditItem(Table table, String title, final String content) {
        Label titleLabel = new Label(title, game.skin, "themed");
        Label contentLabel = new Label(content, game.skin);

        titleLabel.setAlignment(Align.center);
        contentLabel.setAlignment(Align.center);

        screenStage.addFocusableActor(titleLabel);
        screenStage.row();

        float padTop = screenStage.getHeight() / 5;
        float padBottom = screenStage.getHeight() / 5;

        if (title.contains("Thank You")) { // Last item in the credits screen
            padTop = screenStage.getHeight() / 2;
            padBottom = (screenStage.getHeight() / 2) - titleLabel.getHeight();
        }

        table.add(titleLabel).growX()
                .padTop(padTop)
                .padLeft(10f)
                .padRight(10f);
        table.row();
        table.add(contentLabel).growX()
                .padBottom(padBottom)
                .padLeft(10f)
                .padRight(10f);
        table.row();
    }

    private void toggleDevMode() {
        // owo what's this

        boolean devModeUnlocked = game.preferences.getBoolean(Preferences.DEV_MODE_PREFERENCE, false);
        devModeUnlocked = !devModeUnlocked;
        game.preferences.putBoolean(Preferences.DEV_MODE_PREFERENCE, devModeUnlocked);
        game.preferences.flush();

        screenStage.showOKDialog("Developer mode " + ((devModeUnlocked) ? ("un") : ("re")) + "locked!", null);
    }

    private void setUpBackButton() {
        HoverImageButton backButton = uiButtonManager.addBackButtonToStage(game.assets.getTexture(Assets.TextureObject.BACK_TEXTURE));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackPressed();
            }
        });
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        screenStage.act(delta);
        screenStage.draw();

        displayFPS();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (clickParticleEffect != null) {
            clickParticleEffect.dispose();
            clickParticleEffect = null;
        }
    }

    /**
     * Actions to do when the back/escape button is pressed
     *
     * @return true if the action has been handled from this screen
     *         false if the action needs to be sent down the inputmultiplexer chain
     */
    private boolean onBackPressed() {
        if (screenStage.dialogIsActive()) {
            return false;
        }

        animationManager.fadeOutStage(screenStage, this, new MainMenuScreen(game));
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
