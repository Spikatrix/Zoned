package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.internalmaps.RectangleMap;

/**
 * All maps are required to extend this class
 */
public abstract class MapEntity {

    /**
     * Used to display the Map's name.
     * Make sure that it does not conflict with other map names as that could create issues.
     * Also, save the map's preview with the <MapName>.png in images/map_icons for internal maps
     * or the custom external map directory if you want to show a preview of it in the map
     * selector spinner
     *
     * @return The map's name
     */
    abstract public String getName();

    /**
     * Used to get extra param prompts and values if the map supports them
     * (See {@link RectangleMap}.java for an example)
     * Custom external maps do not support extra params
     *
     * @return The {@link MapExtraParams} object containing the title and prompt strings
     * null if the map does not support extra params
     */
    public MapExtraParams getExtraParams() {
        return null;
    }

    /**
     * Used to apply the user selected params if the map supports extra params. The param values
     * set by the user is accessible in the {@link MapExtraParams} object
     * returned by {@link #getExtraParams()}
     */
    public void applyExtraParams() {
    }

    /**
     * Returns the names of each start position in the map. This is optional but recommended
     *
     * @return An Array of Strings containing the names of each start position in order
     * null if you don't want the map start position names to be specified (It'll display A, B, C, etc instead)
     */
    abstract public Array<StartPosition> getStartPositions();

    /**
     * Gets the row count of the map
     *
     * @return The row count of the map
     */
    abstract public int getRowCount();

    /**
     * Gets the col count of the map
     *
     * @return The col count of the map
     */
    abstract public int getColCount();

    /**
     * Gets the map grid data
     *
     * @return The map grid data as a string
     */
    abstract public String getMapData();
}
