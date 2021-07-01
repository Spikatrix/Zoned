package com.cg.zoned.dataobjects;

import com.badlogic.gdx.math.GridPoint2;
import com.cg.zoned.maps.MapLoader;

/**
 * Used to handle map start position name and location
 */
public class StartPosition {
    private String name;
    private Character altName;
    private String viewName;

    private GridPoint2 location;

    public StartPosition(Character altName) {
        this(null, altName);
    }

    public StartPosition(String name, Character altName) {
        this(name, altName, null);
    }

    public StartPosition(String name, Character altName, GridPoint2 location) {
        this.name = name;
        this.altName = altName;
        setLocation(location);

        if (this.altName == null) {
            throw new NullPointerException("Start position alt name must not be null");
        } else if (MapLoader.VALID_START_POSITIONS.indexOf(this.altName) == -1) {
            throw new IllegalArgumentException("Map alt name must be a valid start position character");
        }
    }

    public void setLocation(GridPoint2 location) {
        this.location = location;

        if (location == null) {
            this.viewName = null;
        } else {
            this.viewName = getName() + " (" + location.x + ", " + location.y + ")";
        }
    }

    /**
     * Fetches the name of the start position, falling back to {@link #getAltName()} if null
     *
     * @return The name of the start position
     */
    public String getName() {
        if (name != null) {
            return name;
        } else {
            return Character.toString(getAltName());
        }
    }

    /**
     * Fetches the single character alternate start position name. Will always be non-null
     *
     * @return The alternate name of the start position
     */
    public char getAltName() {
        return altName;
    }

    /**
     * Fetches the start position name with coordinate information in it
     *
     * @return The name with coordinates in parenthesis
     */
    public String getViewName() {
        return viewName;
    }

    public GridPoint2 getLocation() {
        return location;
    }
}
