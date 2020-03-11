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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Constants;
import com.cg.zoned.Zoned;

public class LoadingScreen extends ScreenAdapter {
    final Zoned game;

    private AssetManager assetManager;

    private Stage stage;
    private Skin progressBarSkin;
    private ProgressBar progressBar;
    private boolean finishedLoading;
    private boolean loadedFonts;

    private Pixmap loadingPixmap;
    private Texture loadingTexture;

    public LoadingScreen(final Zoned game) {
        this.game = game;
    }

    @Override
    public void show() {
        assetManager = new AssetManager();

        stage = new Stage(new ScreenViewport());
        progressBarSkin = createProgressBarSkin();

        setUpLoadingUI();
        finishedLoading = loadedFonts = false;

        FileHandleResolver resolver = new InternalFileHandleResolver();
        assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        assetManager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(resolver));

        generateCustomFont("fonts/austere.otf", Constants.FONT_SIZE_MANAGER.LARGE.getSize());
        generateCustomFont("fonts/glametrix.otf", Constants.FONT_SIZE_MANAGER.REGULAR.getSize());
    }

    private void setUpLoadingUI() {
        stage.clear();

        Table table = new Table();
        table.setFillParent(true);
        table.pad(100);

        progressBar = new ProgressBar(0, 1, .01f, false, progressBarSkin);
        progressBar.setAnimateDuration(.5f);

        table.add(progressBar).growX();

        stage.addActor(table);
    }

    private void generateCustomFont(String fontName, int fontSize) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        parameter.fontFileName = fontName;

        parameter.fontParameters.size = fontSize;
        //Gdx.app.log("font", "Screen density: " + Gdx.graphics.getDensity());
        // TODO: Figure out how to generate fonts based on the screen factors
        // And properly scale up/down so as to get a good UX on both Desktop and Mobile

        String fontId;
        if (fontSize == Constants.FONT_SIZE_MANAGER.LARGE.getSize()) {
            fontId = "large-font.otf";
        } else {
            fontId = "regular-font.otf";
        }

        assetManager.load(fontId, BitmapFont.class, parameter);
    }

    @Override
    public void render(float delta) {
        if (!finishedLoading && assetManager.update()) {
            finishedLoading = true;

            if (!loadedFonts) {
                ObjectMap<String, Object> fontMap = new ObjectMap<String, Object>();
                fontMap.put("large-font", assetManager.get("large-font.otf", BitmapFont.class));
                fontMap.put("regular-font", assetManager.get("regular-font.otf", BitmapFont.class));

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

                        game.setScreen(new MainMenuScreen(game));
                    }
                }));

                progressBar.addAction(sequenceAction);
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
        assetManager.dispose();
        loadingTexture.dispose();
        loadingPixmap.dispose();
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
        loadingPixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        loadingPixmap.setColor(color);
        loadingPixmap.fillRectangle(0, 0, width, height);

        loadingTexture = new Texture(loadingPixmap);
        TextureRegionDrawable textureRegionDrawable = new TextureRegionDrawable(new TextureRegion(loadingTexture));
        textureRegionDrawable.setMinWidth(0);
        return textureRegionDrawable;
    }
}
