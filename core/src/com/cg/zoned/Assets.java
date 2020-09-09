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

    // TODO: The default font size is way too big (really noticeable on mobile screens)
    public enum FontManager {
        STYLED_LARGE        ("recharge.otf",  65),
        STYLED_SMALL        ("recharge.otf",  24),
        REGULAR             ("glametrix.otf", 32), // Default font, required to be named as 'REGULAR'
        SMALL               ("bebasneue.otf", 18),
        PLAYER_LABEL_NOSCALE("bebasneue.otf",
                (int) (Map.playerLabelRegionScale * (Constants.CELL_SIZE - (2 * Constants.MAP_GRID_LINE_WIDTH))));
        // Player label font height is based on cell size minus a bit of line width as calculated above

        private String fontFileName;
        private int fontSize;

        FontManager(String fontFileName, int fontSize) {
            this.fontFileName = fontFileName;
            this.fontSize = fontSize;
        }

        public int getFontSize() {
            return fontSize;
        }

        public String getFontName() {
            return this.toString().toLowerCase();
        }

        public String getFontFileName() {
            return fontFileName;
        }
    }
}
