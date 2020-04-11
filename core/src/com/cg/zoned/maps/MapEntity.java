package com.cg.zoned.maps;

public interface MapEntity {
    char EMPTY_CHAR = '.';
    char WALL_CHAR = '#';
    String VALID_START_POSITIONS = "0123456789"; // These shouldn't be here

    String getName();

    int getRowCount();

    int getColCount();

    String getMapData();
}
