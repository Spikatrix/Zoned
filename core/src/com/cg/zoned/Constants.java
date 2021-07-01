package com.cg.zoned;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.cg.zoned.dataobjects.PlayerColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String GAME_VERSION = "0.0.6-dev";
    // Remember to update the versionName in Android's build.gradle
    // And project.setVersion in Desktop's build.gradle
    // And the version in the badge in the README file
    // Or for Linux users, use the versionUpdate.sh script

    /**
     * Enables Discord Rich Presence on Windows and Linux
     * Overrides in-game RPC setting
     */
    public static final boolean ENABLE_DISCORD_RPC = true;

    /**
     * Displays extended GL stats in GameScreen including Draw calls, GL calls etc
     */
    public static final boolean DISPLAY_EXTENDED_GL_STATS = false;

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

    public static final Color PLAYER_CIRCLE_COLOR = Color.WHITE;
    public static final float PLAYER_CIRCLE_WIDTH = 2.5f;

    /**
     * Values for smooth player movement among cells in the map
     * Also, this animation speed is what determines each turn time
     */
    // Best values (0.5f, 0.25f)
    // Slow values (1.0f, 0.50f)
    public static final float PLAYER_MOVEMENT_LERP_VALUE = 0.50f;
    public static final float PLAYER_MOVEMENT_MAX_TIME   = 0.25f;

    /**
     * Available player colors
     */
    public static final List<PlayerColor> PLAYER_COLORS = Collections.unmodifiableList(Arrays.asList(
            new PlayerColor("GREEN",  new Color(0,     0.8f,  0,    1.0f)), // Alpha won't matter
            new PlayerColor("RED",    new Color(0.8f,  0f,    0,    1.0f)),
            new PlayerColor("BLUE",   new Color(0.16f, 0.16f, 0.8f, 1.0f)), // A bit of r & g as deep blue is hard to read on a black bg
            new PlayerColor("YELLOW", new Color(0.8f,  0.8f,  0,    1.0f)),
            new PlayerColor("PINK",   new Color(0.8f,  0,     0.8f, 1.0f))
    ));

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

    public static final float DESKTOP_UI_SCALE_FACTOR = 0.9f;
    // Values from https://developer.android.com/training/multiscreen/screendensities minus 0.3f
    public static final float ANDROID_LDPI_UI_SCALE_FACTOR    = 0.55f;
    public static final float ANDROID_MDPI_UI_SCALE_FACTOR    = 0.7f;
    public static final float ANDROID_HDPI_UI_SCALE_FACTOR    = 1.2f;
    public static final float ANDROID_XHDPI_UI_SCALE_FACTOR   = 1.7f;
    public static final float ANDROID_XXHDPI_UI_SCALE_FACTOR  = 2.7f;
    public static final float ANDROID_XXXHDPI_UI_SCALE_FACTOR = 3.7f;

    public static final String LOG_TAG = "ZONED";

    // Preferences moved to Preferences.java
    // Directions moved to Player.java
    // Controls moved to managers/ControlManager.java
    // Fonts moved to Assets.java
}
