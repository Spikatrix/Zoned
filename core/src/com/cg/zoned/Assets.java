package com.cg.zoned;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public class Assets {
    private AssetManager assetManager;

    public Texture getPlayButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_play_sheet.png", Texture.class);
    }

    public Texture getBackButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_back.png", Texture.class);
    }

    public Texture getCreditsButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_credits.png", Texture.class);
    }

    public Texture getCrossButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_cross.png", Texture.class);
    }

    public Texture getDevButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_dev.png", Texture.class);
    }

    public Texture getSettingsButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_settings.png", Texture.class);
    }

    public Texture getTutorialButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_tutorial.png", Texture.class);
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void dispose() {
        assetManager.dispose();
    }
}
