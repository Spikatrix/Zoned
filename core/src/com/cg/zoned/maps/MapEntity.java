package com.cg.zoned.maps;

/**
 * All maps are required to implement this interface
 */
public interface MapEntity {

    /**
     * Used to display the Map's name.
     * Make sure that it does not conflict with other map names as that could create issues.
     * Also, save the map's preview with the <MapName>.png in icons/map_icons if you want to show
     * a preview of it in the map selector spinner
     *
     * @return The map's name
     */
    String getName();

    /**
     * Used to get extra param prompts and values if the map supports them
     * (See {@link RectangleMap}.java for an example)
     *
     * @return The {@link MapExtraParams} object containing the title and prompt strings
     * null if the map does not support extra params
     */
    MapExtraParams getExtraParamPrompts();

    /**
     * Used to apply the user selected params if the map supports extra params. The param values
     * set by the user is accessible in the {@link MapExtraParams} object
     * returned by {@link #getExtraParamPrompts()}
     */
    void applyExtraParams();

    /**
     * Gets the row count of the map
     *
     * @return The row count of the map
     */
    int getRowCount();

    /**
     * Gets the col count of the map
     *
     * @return The col count of the map
     */
    int getColCount();

    /**
     * Gets the map grid data
     *
     * @return The map grid data as a string
     */
    String getMapData();
}
