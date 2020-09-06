package com.cg.zoned;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {
    public static final String GAME_VERSION = "0.0.3-dev";
    // Remember to update the versionName in Android's build.gradle
    // And project.setVersion in Desktop's build.gradle
    // And the version in the badge in the README file
    // Or for Linux users, use the versionUpdate.sh script

    /**
     * Enables Discord Rich Presence functionality on Windows and Linux
     * <p>
     * Note: When this is enabled, running multiple instances of the game will crash with a SIGSEGV
     */
    public static final boolean ENABLE_DISCORD_RPC = true;

    public static final float WORLD_SIZE = 480.0f;

    public static final int SERVER_PORT = 22355; // Random port I chose ¯\_(ツ)_/¯

    public static final float CELL_SIZE = 25.0f;
    public static final Color MAP_GRID_COLOR = Color.WHITE;
    public static final float MAP_GRID_LINE_WIDTH = 3f;

    /**
     * Width and color of the viewport divider between splitscreens
     */
    public static final float VIEWPORT_DIVIDER_SOLID_WIDTH = 3f;
    public static final float VIEWPORT_DIVIDER_FADE_WIDTH = 25f;
    public static final Color VIEWPORT_DIVIDER_FADE_COLOR = new Color(0f, 0f, 0f, 0f);

    /**
     * The number of players when playing on the same device
     * <p>
     * Beware: There may not be enough horizontal space especially on mobile devices to play comfortably
     */
    //public static final int NO_OF_PLAYERS = 2; Moved to dev mode settings

    public static final Color PLAYER_CIRCLE_COLOR = Color.WHITE;
    public static final float PLAYER_CIRCLE_WIDTH = 2.5f;

    /**
     * Values for smooth player movement among cells in the map
     * Also, this animation speed is what determines each turn time
     */
    public static final float PLAYER_MOVEMENT_LERP_VALUE = .5f;  // Best value IMO: .5f
    public static final float PLAYER_MOVEMENT_MAX_TIME   = .25f; // Best value IMO: .25f

    /**
     * Available player colors
     */
    public static final Map<String, Color> PLAYER_COLORS = new LinkedHashMap<String, Color>() {
        {
            put("GREEN",  new Color(0,    0.8f, 0,    1.0f)); // Alpha won't matter
            put("RED",    new Color(0.8f, 0,    0,    1.0f));
            put("BLUE",   new Color(0,    0,    0.8f, 1.0f));
            put("YELLOW", new Color(0.8f, 0.8f, 0,    1.0f));
            put("PINK",   new Color(0.8f, 0,    0.8f, 1.0f));
        }
    };

    /**
     * Controls for players playing in splitscreen mode. Player one has the first control scheme, two has the second etc
     * In case of server-client multiplayer mode, the player will have the first control scheme.
     */
    public static final int[][] PLAYER_CONTROLS = new int[][]{
            {Input.Keys.W,  Input.Keys.A,    Input.Keys.S,    Input.Keys.D},
            {Input.Keys.UP, Input.Keys.LEFT, Input.Keys.DOWN, Input.Keys.RIGHT},
            {Input.Keys.I,  Input.Keys.J,    Input.Keys.K,    Input.Keys.L},
            {Input.Keys.T,  Input.Keys.F,    Input.Keys.G,    Input.Keys.H},
    };

    /**
     * Zoom minimum and maximum values
     * <p>
     * The camera can be zoomed in/out by using the button on the top-center in the GameScreen
     */
    public static final float ZOOM_MIN_VALUE = 1f;
    public static final float ZOOM_MAX_VALUE = 1.6f;

    public static final float DESKTOP_FONT_SCALE_FACTOR = 1.0f;
    // Values from https://developer.android.com/training/multiscreen/screendensities
    public static final float ANDROID_LDPI_FONT_SCALE_FACTOR    = 0.75f;
    public static final float ANDROID_MDPI_FONT_SCALE_FACTOR    = 1.0f;
    public static final float ANDROID_HDPI_FONT_SCALE_FACTOR    = 1.5f;
    public static final float ANDROID_XHDPI_FONT_SCALE_FACTOR   = 2.0f;
    public static final float ANDROID_XXHDPI_FONT_SCALE_FACTOR  = 3.0f;
    public static final float ANDROID_XXXHDPI_FONT_SCALE_FACTOR = 4.0f;

    public static final String LOG_TAG = "ZONED";

    // Preferences file name
    public static final String ZONED_PREFERENCES = "Zoned_Preferences";
    // Show FPS or not
    public static final String FPS_PREFERENCE = "FPS_Preference";
    // 0 = Piemenu control, 1 = Fling control
    public static final String CONTROL_PREFERENCE = "Control_Preference";
    // The last used name to be autofilled in the Player name field in the Host join screen
    public static final String NAME_PREFERENCE = "Name_Preference";
    // Shows the tutorial prompt the first time the user visits the splitscreen multiplayer screen
    public static final String SHOW_TUTORIAL_PREFERENCE = "Show_Tutorial_Preference";
    // Player count in splitscreen multiplayer mode (Developer options)
    public static final String SPLITSCREEN_PLAYER_COUNT_PREFERENCE = "Splitscreen_Player_Count_Preference";
    // Splitscreen count in the Map start pos screen (Developer option)
    public static final String MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE = "Map_Start_Pos_Splitscreen_Count_Preference";
    // Developer mode unlocked or not
    public static final String DEV_MODE_PREFERENCE = "Dev_Mode_Preference";
    // Enable Discord RPC or not
    public static final String DISCORD_RPC_PREFERENCE = "Discord_RPC_Preference";

    public static final int PIE_MENU_CONTROL = 0;
    public static final int FLING_CONTROL = 1;

    public enum Direction {UP, LEFT, DOWN, RIGHT}

    public enum FONT_MANAGER {
        LARGE       ("large-font",                "recharge.otf",  70),
        REGULAR     ("regular-font",              "glametrix.otf", 32),
        SMALL       ("small-font",                "bebasneue.otf", 18),
        PLAYER_LABEL("player-label-font-noscale", "bebasneue.otf",
                (int) (com.cg.zoned.Map.playerLabelRegionScale * (Constants.CELL_SIZE - (2 * Constants.MAP_GRID_LINE_WIDTH))));
        // Player label font height is based on cell size minus a bit of line width as calculated above

        private String fontName;
        private String fontFileName;
        private int fontSize;

        FONT_MANAGER(String fontName, String fontFileName, int fontSize) {
            this.fontName = fontName;
            this.fontFileName = fontFileName;
            this.fontSize = fontSize;
        }

        public int getFontSize() {
            return fontSize;
        }

        public String getFontName() {
            return fontName;
        }

        public String getFontFileName() {
            return fontFileName;
        }
    }
}