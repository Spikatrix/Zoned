package com.cg.zoned.maps;

public class DefaultRectangleMap implements MapEntity {
    private String mapGridString = "0........2\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "3........1\n";

    private int rowCount = 10;
    private int colCount = 10;

    public DefaultRectangleMap() throws InvalidMapDimensions {
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
    public String getName() {
        return "Default Rectangle";
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
