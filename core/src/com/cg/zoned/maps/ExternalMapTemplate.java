package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.StartPosition;

public class ExternalMapTemplate extends MapEntity {
    private String mapName;
    private String mapGrid;

    private Array<StartPosition> startPositions;

    private int rowCount;
    private int colCount;

    public ExternalMapTemplate(String mapName) {
        this(mapName, null, null, -1, -1);
    }

    public ExternalMapTemplate(String mapName, String mapGrid, Array<StartPosition> startPositions, int rowCount, int colCount) {
        this.mapName = mapName;
        updateMapData(mapGrid, startPositions, rowCount, colCount);
    }

    public void updateMapData(String mapGrid, Array<StartPosition> startPositions, int rowCount, int colCount) {
        this.mapGrid = mapGrid;
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.startPositions = startPositions;
    }

    @Override
    public String getName() {
        return mapName;
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
