package com.cg.zoned.maps;

public class ExternalMapTemplate implements MapEntity {
    private String mapName;
    private String mapGrid;

    private int rowCount;
    private int colCount;

    public ExternalMapTemplate(String mapName, String mapGrid, int rowCount, int colCount) {
        this.mapName = mapName;
        this.mapGrid = mapGrid;
        this.rowCount = rowCount;
        this.colCount = colCount;
    }

    @Override
    public String getName() {
        return mapName;
    }

    @Override
    public MapExtraParams getExtraParamPrompts() {
        return null;
    }

    @Override
    public void applyExtraParams() {
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
