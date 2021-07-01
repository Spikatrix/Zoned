package com.cg.zoned.dataobjects;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.maps.MapEntity;

public class PreparedMapData {
    public MapEntity map;
    public Cell[][] mapGrid;
    public Array<StartPosition> startPositions;
    public int wallCount;

    public PreparedMapData() {
        this.map = null;
        this.mapGrid = null;
        this.startPositions = null;
        this.wallCount = -1;
    }
}
