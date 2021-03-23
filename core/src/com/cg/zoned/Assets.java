package com.cg.zoned;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

import java.util.Locale;

public class Assets {
    private AssetManager assetManager;

    public Texture getTexture(TextureObject textureObject) {
        return assetManager.get(textureObject.location, Texture.class);
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void dispose() {
        assetManager.dispose();
    }

    public enum TextureObject {
        DIAMOND_TEXTURE    ("images/ic_diamond.png"),
        PLAY_SHEET_TEXTURE ("images/ui_icons/ic_play_sheet.png"),
        BACK_TEXTURE       ("images/ui_icons/ic_back.png"),
        CREDITS_TEXTURE    ("images/ui_icons/ic_credits.png"),
        CROSS_TEXTURE      ("images/ui_icons/ic_cross.png"),
        DEV_TEXTURE        ("images/ui_icons/ic_dev.png"),
        SETTINGS_TEXTURE   ("images/ui_icons/ic_settings.png"),
        TUTORIAL_TEXTURE   ("images/ui_icons/ic_tutorial.png");

        private final String location;

        TextureObject(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }
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
            return this.toString().toLowerCase(Locale.ENGLISH);
        }

        public String getFontFileName() {
            return fontFileName;
        }
    }
}
