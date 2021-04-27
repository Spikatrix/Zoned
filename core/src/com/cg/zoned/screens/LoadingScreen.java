package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.cg.zoned.Assets;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.ControlManager;

public class LoadingScreen extends ScreenObject {
    private AssetManager assetManager;

    private Label loadingLabel;
    private boolean finishedLoading;
    private boolean loadedStageTwo;

    public LoadingScreen(final Zoned game) {
        super(game);

        game.discordRPCManager.updateRPC("Loading game");
        initSetup();
    }

    private void initSetup() {
        // Set up preferences
        game.preferences = Gdx.app.getPreferences(Preferences.ZONED_PREFERENCES);

        // Set up Discord RPC
        if (game.preferences.getBoolean(Preferences.DISCORD_RPC_PREFERENCE, true)) {
            game.discordRPCManager.initRPC();
        }

        // Reset player color alpha
        PlayerColorHelper.resetPlayerColorAlpha();

        // Validate touch controls
        if (game.preferences.getInteger(Preferences.CONTROL_PREFERENCE, 0) >= ControlManager.CONTROL_TYPES.length) {
            game.preferences.putInteger(Preferences.CONTROL_PREFERENCE, 0);
            game.preferences.flush();
        }
    }

    @Override
    public void show() {
        assetManager = new AssetManager();
        game.setAssetManager(assetManager); // For disposing

        setUpLoadingUI();
        finishedLoading = loadedStageTwo = false;

        FileHandleResolver resolver = new InternalFileHandleResolver();
        assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        assetManager.setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(resolver));

        for (Assets.FontManager font : Assets.FontManager.values()) {
            generateCustomFont("fonts/" + font.getFontFileName(), font.getFontName(), font.getFontSize());
        }
    }

    private void setUpLoadingUI() {
        screenStage.clear();

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Texture loadingImageTexture = new Texture(Gdx.files.internal("images/ic_loading.png"));
        usedTextures.add(loadingImageTexture);
        Image loading = new Image(loadingImageTexture);
        table.add(loading);
        table.row();

        Label.LabelStyle loadingLabelStyle = new Label.LabelStyle(
                new BitmapFont(Gdx.files.internal("fonts/loading/loading.fnt")), new Color(0, 0.8f, 0, 1f));
        loadingLabel = new Label("0%", loadingLabelStyle);
        loadingLabel.setAlignment(Align.center);
        table.add(loadingLabel).growX().padTop(24f * game.getScaleFactor());
        table.row();

        screenStage.addActor(table);
    }

    private void generateCustomFont(String fontFileName, String fontName, int fontSize) {
        FreetypeFontLoader.FreeTypeFontLoaderParameter parameter = new FreetypeFontLoader.FreeTypeFontLoaderParameter();

        parameter.fontFileName = fontFileName;
        if (fontName.endsWith("noscale")) {
            parameter.fontParameters.size = fontSize;
        } else {
            parameter.fontParameters.size = (int) (fontSize * game.getScaleFactor());
        }
        //Gdx.app.log(Constants.LOG_TAG, "Screen density: " + Gdx.graphics.getDensity());

        String fontId = fontName + ".otf";

        assetManager.load(fontId, BitmapFont.class, parameter);
    }

    @Override
    public void render(float delta) {
        if (!finishedLoading && assetManager.update()) {
            finishedLoading = true;

            if (!loadedStageTwo) {
                loadStageTwo();

                loadedStageTwo = true;
                finishedLoading = false;
            } else {
                endLoading();
            }
        }

        super.render(delta);

        float progress = assetManager.getProgress();
        progress = 0.5f * progress;
        if (loadedStageTwo) {
            loadingLabel.setText((int) ((0.5f + progress) * 100) + "%");
        } else {
            loadingLabel.setText((int) (        progress  * 100) + "%");
        }

        screenStage.act(delta);
        screenStage.draw();
    }

    private void loadStageTwo() {
        ObjectMap<String, Object> fontMap = new ObjectMap<>();
        for (Assets.FontManager font : Assets.FontManager.values()) {
            fontMap.put(font.getFontName(), assetManager.get(font.getFontName() + ".otf", BitmapFont.class));
        }

        SkinLoader.SkinParameter skinParameter = new SkinLoader.SkinParameter("neon-skin/neon-ui.atlas", fontMap);
        assetManager.load("neon-skin/neon-ui.json", Skin.class, skinParameter);

        TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
        textureParameter.minFilter = Texture.TextureFilter.Linear;
        textureParameter.magFilter = Texture.TextureFilter.Linear;

        Assets.TextureObject[] preloadTextures = Assets.TextureObject.values();
        for (Assets.TextureObject textureObject : preloadTextures) {
            assetManager.load(textureObject.getLocation(), Texture.class, textureParameter);
        }
    }

    private void endLoading() {
        SequenceAction sequenceAction = new SequenceAction();
        sequenceAction.addAction(Actions.fadeOut(1f));
        sequenceAction.addAction(Actions.run(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() { // Crashes on GWT without this
                    @Override
                    public void run() {
                        game.skin = assetManager.get("neon-skin/neon-ui.json", Skin.class);
                        game.initFPSUtils();
                        dispose();
                        game.setScreen(new MainMenuScreen(game));
                    }
                });
            }
        }));

        screenStage.addAction(sequenceAction);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
