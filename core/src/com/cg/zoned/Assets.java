package com.cg.zoned;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public class Assets {
    private AssetManager assetManager;

    public static final String TEXTURE_DIAMOND_LOCATION    = "images/ic_diamond.png";
    public static final String TEXTURE_PLAY_SHEET_LOCATION = "images/ui_icons/ic_play_sheet.png";
    public static final String TEXTURE_BACK_LOCATION       = "images/ui_icons/ic_back.png";
    public static final String TEXTURE_CREDITS_LOCATION    = "images/ui_icons/ic_credits.png";
    public static final String TEXTURE_CROSS_LOCATION      = "images/ui_icons/ic_cross.png";
    public static final String TEXTURE_DEV_LOCATION        = "images/ui_icons/ic_dev.png";
    public static final String TEXTURE_SETTINGS_LOCATION   = "images/ui_icons/ic_settings.png";
    public static final String TEXTURE_TUTORIAL_LOCATION   = "images/ui_icons/ic_tutorial.png";

    public Texture getGameBgTexture() {
        return assetManager.get(TEXTURE_DIAMOND_LOCATION, Texture.class);
    }

    public Texture getPlayButtonTexture() {
        return assetManager.get(TEXTURE_PLAY_SHEET_LOCATION, Texture.class);
    }

    public Texture getBackButtonTexture() {
        return assetManager.get(TEXTURE_BACK_LOCATION, Texture.class);
    }

    public Texture getCreditsButtonTexture() {
        return assetManager.get(TEXTURE_CREDITS_LOCATION, Texture.class);
    }

    public Texture getCrossButtonTexture() {
        return assetManager.get(TEXTURE_CROSS_LOCATION, Texture.class);
    }

    public Texture getDevButtonTexture() {
        return assetManager.get(TEXTURE_DEV_LOCATION, Texture.class);
    }

    public Texture getSettingsButtonTexture() {
        return assetManager.get(TEXTURE_SETTINGS_LOCATION, Texture.class);
    }

    public Texture getTutorialButtonTexture() {
        return assetManager.get(TEXTURE_TUTORIAL_LOCATION, Texture.class);
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
