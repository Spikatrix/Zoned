package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.StartPosition;

public class ExternalMapTemplate implements MapEntity {
    private String mapName;
    private String mapGrid;

    private Array<StartPosition> startPositions;

    private int rowCount;
    private int colCount;

    public ExternalMapTemplate(String mapName, String mapGrid, Array<StartPosition> startPositions, int rowCount, int colCount) {
        this.mapName = mapName;
        this.mapGrid = mapGrid;
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.startPositions = startPositions;
    }

    @Override
    public String getName() {
        return mapName;
    }

    // External maps do not support extra params
    @Override
    public MapExtraParams getExtraParams() {
        return null;
    }

    @Override
    public void applyExtraParams() {
    }

    @Override
    public Array<StartPosition> getStartPositions() {
        return startPositions;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColCount() {
        return colCount;
    }

    @Override
    public String getMapData() {
        return mapGrid;
    }
}
