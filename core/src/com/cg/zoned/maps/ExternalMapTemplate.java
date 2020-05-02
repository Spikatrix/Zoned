package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;

public class ExternalMapTemplate implements MapEntity {
    private String mapName;
    private String mapGrid;

    private Array<String> startPosNames;

    private int rowCount;
    private int colCount;

    public ExternalMapTemplate(String mapName, String mapGrid, Array<String> startPosNames, int rowCount, int colCount) {
        this.mapName = mapName;
        this.mapGrid = mapGrid;
        this.rowCount = rowCount;
        this.colCount = colCount;

        this.startPosNames = startPosNames;
    }

    @Override
    public String getName() {
        return mapName;
    }

    @Override
    public MapExtraParams getExtraParams() {
        return null;
    }

    @Override
    public void applyExtraParams() {
    }

    @Override
    public Array<String> getStartPosNames() {
        return startPosNames;
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
