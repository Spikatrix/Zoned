package com.cg.zoned.dataobjects;

import com.badlogic.gdx.math.GridPoint2;
import com.cg.zoned.managers.MapManager;

/**
 * Used to handle map start position name and location
 */
public class StartPosition {
    private String name;
    private Character altName;
    private GridPoint2 location;

    public StartPosition(String name, Character altName) {
        this(name, altName, null);
    }

    public StartPosition(String name, Character altName, GridPoint2 location) {
        this.name = name;
        this.altName = altName;
        this.location = location;

        if (this.altName == null) {
            throw new NullPointerException("Start position alt name must not be null");
        } else if (MapManager.VALID_START_POSITIONS.indexOf(this.altName) == -1) {
            throw new IllegalArgumentException("Map alt name must be a valid start position character");
        }
    }

    public void setLocation(GridPoint2 location) {
        this.location = location;
    }

    public String getName() {
        if (name != null) {
            return name;
        } else {
            return Character.toString(getAltName());
        }
    }

    public char getAltName() {
        return altName;
    }

    public GridPoint2 getLocation() {
        return location;
    }
}
