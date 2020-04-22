package com.cg.zoned.maps;

public class HoloMap implements MapEntity {
    private String mapGridString = "" + // Stupid auto-code formatter messes up the arrangement, so added this line
            "A...##...C\n" +
            "..........\n" +
            "..........\n" +
            "....##....\n" +
            "#........#\n" +
            "#........#\n" +
            "....##....\n" +
            "..........\n" +
            "..........\n" +
            "D...##...B\n";

    private int rowCount = 10;
    private int colCount = 10;

    public HoloMap() throws InvalidMapDimensions {
        String[] mapRows = mapGridString.split("\n");
        if (mapRows.length != rowCount) {
            throw new InvalidMapDimensions("Row count does not match the provided string");
        }

        for (String mapRow : mapRows) {
            if (colCount != mapRow.length()) {
                throw new InvalidMapDimensions("Col count does not match the provided string");
            }
        }
    }

    @Override
    public MapExtraParams getExtraParamPrompts() {
        return null;
    }

    @Override
    public void applyExtraParams() {

    }

    @Override
    public String getMapData() {
        return mapGridString;
    }

    @Override
    public String getName() {
        return "Holo";
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColCount() {
        return colCount;
    }
}
