package com.cg.zoned;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public class Assets {
    private AssetManager assetManager;

    public Texture getPlayButtonTexture() {
        return assetManager.get("icons/ui_icons/ic_play_sheet.png", Texture.class);
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void dispose() {
        assetManager.dispose();
    }
}
