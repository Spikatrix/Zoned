package com.cg.zoned.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cg.zoned.Assets;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.Preferences;
import com.cg.zoned.Zoned;
import com.cg.zoned.managers.ControlManager;
import com.cg.zoned.ui.FocusableStage;
import com.cg.zoned.ui.PixmapFactory;

public class LoadingScreen extends ScreenObject {
    private AssetManager assetManager;

    private Skin tempSkin;
    private Label loadingLabel;
    private ProgressBar progressBar;
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

        screenViewport = new ScreenViewport();
        screenStage = new FocusableStage(screenViewport);
        tempSkin = createTempSkin();

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

        progressBar = new ProgressBar(0, 1, .01f, false, tempSkin);
        progressBar.setAnimateDuration(.68f);
        progressBar.setAnimateInterpolation(Interpolation.fastSlow);

        table.add(progressBar).growX()
                .padLeft(100f * game.getScaleFactor()).padRight(100f * game.getScaleFactor())
                .padTop(16f * game.getScaleFactor());

        Table loadingContainer = new Table();
        loadingContainer.setFillParent(true);
        loadingContainer.bottom().right();
        loadingLabel = new Label("0", tempSkin, "loading-font", Color.WHITE);
        loadingContainer.add(loadingLabel);
        loadingContainer.add(new Label("%", tempSkin, "loading-font", Color.WHITE));
        loadingContainer.pad(16f * game.getScaleFactor());

        screenStage.addActor(loadingContainer);
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
            progressBar.setValue(0.5f + progress);
        } else {
            progressBar.setValue(progress);
        }

        loadingLabel.setText((int) (progressBar.getValue() * 100));

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
        tempSkin.dispose();
    }

    /*
     * Creates a temporary skin with a horizontal progress bar and a limited loading font
     */
    private Skin createTempSkin() {
        Skin tempSkin = new Skin();
        tempSkin.add("progress-bar", createNinePatchDrawable(14, 14, 6, Color.GREEN), Drawable.class);
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        progressBarStyle.knobBefore = tempSkin.getDrawable("progress-bar");
        tempSkin.add("default-horizontal", progressBarStyle);

        tempSkin.add("loading-font", new BitmapFont(Gdx.files.internal("fonts/loading/loading.fnt")), BitmapFont.class);

        return tempSkin;
    }

    private NinePatchDrawable createNinePatchDrawable(int width, int height, int radius, Color color) {
        Pixmap loadingPixmap = PixmapFactory.getRoundedCornerPixmap(color, width, height, radius);

        Texture loadingTexture = new Texture(loadingPixmap);
        usedTextures.add(loadingTexture);
        loadingPixmap.dispose();

        NinePatch ninePatch = new NinePatch(loadingTexture, radius, radius, radius, radius);
        NinePatchDrawable ninePatchDrawable = new NinePatchDrawable(ninePatch);

        return ninePatchDrawable;
    }
}
