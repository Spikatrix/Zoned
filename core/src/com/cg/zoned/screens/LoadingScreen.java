package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.Zoned;

public class LoadingScreen extends ScreenAdapter {
    final Zoned game;

    private Array<Texture> usedTextures = new Array<>();

    private AssetManager assetManager;

    private Stage stage;
    private Skin progressBarSkin;
    private ProgressBar progressBar;
    private boolean finishedLoading;
    private boolean loadedFonts;

    public LoadingScreen(final Zoned game) {
        this.game = game;
    }

    @Override
    public void show() {
        assetManager = new AssetManager();
        game.setAssetManager(assetManager); // For disposing

        stage = new Stage(new ScreenViewport());
        progressBarSkin = createProgressBarSkin();

        setUpLoadingUI();
        finishedLoading = loadedFonts = false;

        FileHandleResolver resolver = new InternalFileHandleResolver();
        assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        assetManager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(resolver));

        generateCustomFont("fonts/austere.otf", Constants.FONT_MANAGER.LARGE);
        generateCustomFont("fonts/glametrix.otf", Constants.FONT_MANAGER.REGULAR);
        generateCustomFont("fonts/glametrix.otf", Constants.FONT_MANAGER.SMALL);
        generateCustomFont("fonts/glametrix.otf", Constants.FONT_MANAGER.PLAYER_LABEL);

        game.preferences = Gdx.app.getPreferences(Constants.ZONED_PREFERENCES);
    }

    private void setUpLoadingUI() {
        stage.clear();

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Texture loadingImageTexture = new Texture(Gdx.files.internal("icons/ic_loading.png"));
        usedTextures.add(loadingImageTexture);
        Image loading = new Image(loadingImageTexture);
        table.add(loading);
        table.row();

        progressBar = new ProgressBar(0, 1, .01f, false, progressBarSkin);
        progressBar.setAnimateDuration(.5f);

        table.add(progressBar).growX()
                .padLeft(100f * game.getScaleFactor()).padRight(100f * game.getScaleFactor())
                .padTop(32f * game.getScaleFactor());

        stage.addActor(table);
    }

    private void generateCustomFont(String fontName, Constants.FONT_MANAGER fontManager) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();

        parameter.fontFileName = fontName;
        if (fontManager.getName().equals("player-label-font")) {
            parameter.fontParameters.size = fontManager.getSize();
        } else {
            parameter.fontParameters.size = (int) (fontManager.getSize() * game.getScaleFactor());
        }
        //Gdx.app.log(Constants.LOG_TAG, "Screen density: " + Gdx.graphics.getDensity());

        String fontId = fontManager.getName() + ".otf";

        assetManager.load(fontId, BitmapFont.class, parameter);
    }

    @Override
    public void render(float delta) {
        if (!finishedLoading && assetManager.update()) {
            finishedLoading = true;

            if (!loadedFonts) {
                ObjectMap<String, Object> fontMap = new ObjectMap<String, Object>();
                for (Constants.FONT_MANAGER font : Constants.FONT_MANAGER.values()) {
                    fontMap.put(font.getName(), assetManager.get(font.getName() + ".otf", BitmapFont.class));
                }

                SkinLoader.SkinParameter parameter = new SkinLoader.SkinParameter("neon-skin/neon-ui.atlas", fontMap);
                assetManager.load("neon-skin/neon-ui.json", Skin.class, parameter);

                loadedFonts = true;
                finishedLoading = false;
            } else {
                SequenceAction sequenceAction = new SequenceAction();
                sequenceAction.addAction(Actions.fadeOut(1f));
                sequenceAction.addAction(Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        game.skin = assetManager.get("neon-skin/neon-ui.json", Skin.class);
                        dispose();
                        game.setScreen(new MainMenuScreen(game));
                    }
                }));

                stage.addAction(sequenceAction);
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = assetManager.getProgress();
        progress = 0.5f * progress;
        if (loadedFonts) {
            progressBar.setValue(0.5f + progress);
        } else {
            progressBar.setValue(progress);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        progressBarSkin.dispose();
        for (Texture texture : usedTextures) {
            texture.dispose();
        }
    }

    private Skin createProgressBarSkin() {
        Skin tempSkin = new Skin();
        tempSkin.add("progress-bar", createDrawable(1, 20, Color.GREEN), Drawable.class);
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        progressBarStyle.knobBefore = tempSkin.getDrawable("progress-bar");
        tempSkin.add("default-horizontal", progressBarStyle);

        return tempSkin;
    }

    private Drawable createDrawable(int width, int height, Color color) {
        Pixmap loadingPixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        loadingPixmap.setColor(color);
        loadingPixmap.fillRectangle(0, 0, width, height);

        Texture loadingTexture = new Texture(loadingPixmap);
        usedTextures.add(loadingTexture);
        loadingPixmap.dispose();
        TextureRegionDrawable textureRegionDrawable = new TextureRegionDrawable(loadingTexture);
        textureRegionDrawable.setMinWidth(0);
        return textureRegionDrawable;
    }
}
