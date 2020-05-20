package com.cg.zoned;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {
    public static final String GAME_VERSION = "0.0.1-beta";

    public static final float WORLD_SIZE = 480.0f;

    public static final int SERVER_PORT = 22355; // Random port I chose *shrug*

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
            put("GREEN",  new Color(0,    0.8f, 0,    1.0f));
            put("RED",    new Color(0.8f, 0,    0,    1.0f));
            put("BLUE",   new Color(0,    0,    0.8f, 1.0f));
            put("YELLOW", new Color(0.8f, 0.8f, 0,    1.0f));
            put("PINK",   new Color(0.8f, 0,    0.8f, 1.0f));
        }
    };

    /**
     * Controls for players playing in splitscreen mode. Player one has the first control scheme, two has the second etc
     * In case of server-client multiplayer mode, the player will have the first control scheme.
     * <p>
     * Note that you may face issues when playing with the number of players greater than the number of control schemes specified
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
    public static final float ANDROID_LDPI_FONT_SCALE_FACTOR = 0.75f;
    public static final float ANDROID_MDPI_FONT_SCALE_FACTOR = 1.0f;
    public static final float ANDROID_HDPI_FONT_SCALE_FACTOR = 1.5f;
    public static final float ANDROID_XHDPI_FONT_SCALE_FACTOR = 2.0f;
    public static final float ANDROID_XXHDPI_FONT_SCALE_FACTOR = 3.0f;
    public static final float ANDROID_XXXHDPI_FONT_SCALE_FACTOR = 4.0f;

    public static final String LOG_TAG = "ZONED";
    public static final String ZONED_PREFERENCES = "Zoned_Preferences";
    public static final String FPS_PREFERENCE = "FPS_Preference";
    public static final String CONTROL_PREFERENCE = "Control_Preference";
    public static final String NAME_PREFERENCE = "Name_Preference"; // The last used name
    public static final String SPLITSCREEN_PLAYER_COUNT_PREFERENCE = "Splitscreen_Player_Count_Preference";
    public static final String MAP_START_POS_SPLITSCREEN_COUNT_PREFERENCE = "Map_Start_Pos_Splitscreen_Count_Preference";
    public static final String DEV_MODE_PREFERENCE = "Dev_Mode_Preference";

    public static final int PIE_MENU_CONTROL = 0;
    public static final int FLING_CONTROL = 1;


    public enum Direction {UP, LEFT, DOWN, RIGHT}
    public enum FONT_MANAGER {
        LARGE("large-font", 80),
        REGULAR("regular-font", 36),
        SMALL("small-font", 18),
        PLAYER_LABEL("player-label-font", 20);


        private String name;
        private int size;

        FONT_MANAGER(String name, int size) {
            this.name = name;
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        public String getName() {
            return name;
        }
    }
}