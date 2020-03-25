package com.cg.zoned;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {
    public static final float WORLD_SIZE = 480.0f;

    public static final int SERVER_PORT = 22355;

    public static final float CELL_SIZE = 25.0f;
    public static final Color MAP_GRID_COLOR = Color.WHITE;
    public static final float MAP_GRID_LINE_WIDTH = 3f;

    /**
     * Width and color of the viewport divider between splitscreens
     */
    public static final float VIEWPORT_DIVIDER_TOTAL_WIDTH = 50f;
    public static final Color VIEWPORT_DIVIDER_FADE_COLOR = new Color(0f, 0f, 0f, 0f);

    /**
     * The number of players when playing on the same device
     * <p>
     * Beware: There may not be enough horizontal space especially on mobile devices to play comfortably
     */
    //public static final int NO_OF_PLAYERS = PLAYER_CONTROLS.length; // TODO: Max number of players

    public static final Color PLAYER_CIRCLE_COLOR = Color.WHITE;
    public static final float PLAYER_CIRCLE_WIDTH = 2.5f;

    /**
     * Values for smooth player movement among cells in the map
     */
    public static final float PLAYER_MOVEMENT_LERP_VALUE = .5f;  // Best value IMO: .5f
    public static final float PLAYER_MOVEMENT_MAX_TIME   = .25f; // Best value IMO: .25f

    public static final Map<String, Color> PLAYER_COLORS = new LinkedHashMap<String, Color>() {
        {
            put("GREEN",  new Color(0,    0.8f, 0,    1.0f));
            put("RED",    new Color(0.8f, 0,    0,    1.0f));
            put("BLUE",   new Color(0,    0,    0.8f, 1.0f));
            put("YELLOW", new Color(0.8f, 0.8f, 0,    1.0f));
            put("PURPLE", new Color(0.4f, 0,    0.8f, 1.0f));
        }
    };

    /**
     * Controls for players playing in splitscreen mode. Player one has the first control scheme, two has the second etc
     * In case of server-client multiplayer mode, the player will have the first control scheme.
     * <p>
     * Note that you may face issues when playing with the number of players unequal to the number of control schemes specified
     */
    public static final int[][] PLAYER_CONTROLS = new int[][]{
            {Input.Keys.W,  Input.Keys.D,     Input.Keys.S,    Input.Keys.A},
            {Input.Keys.UP, Input.Keys.RIGHT, Input.Keys.DOWN, Input.Keys.LEFT},
            {Input.Keys.I,  Input.Keys.L,     Input.Keys.K,    Input.Keys.J},
            {Input.Keys.T,  Input.Keys.H,     Input.Keys.G,    Input.Keys.F},
    };

    /**
     * Zoom minimum and maximum values
     * <p>
     * The camera can be zoomed in/out by the scroll wheel on Desktop and pinch/expand on Mobile
     */
    public static final float ZOOM_MIN_VALUE = 1f;
    public static final float ZOOM_MAX_VALUE = 1.6f;

    public static final float DESKTOP_FONT_SCALE_FACTOR = 1.0f;
    public static final float ANDROID_FONT_SCALE_FACTOR = 1.3f; // TODO: Optimize everything for tablets
    public static final float ANDROID_DIRECTION_ARROW_SCALE_FACTOR = 3f;

    public static final String LOG_TAG = "ZONED";
    public static final String ZONED_PREFERENCES = "Zoned_Preferences";
    public static final String FPS_PREFERENCE = "FPS_Preference";

    public enum Direction {UP, RIGHT, DOWN, LEFT}

    public enum FONT_MANAGER {
        LARGE("large-font", 80),
        REGULAR("regular-font", 36),
        SMALL("small-font", 24);

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