package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;

public interface MapEntity {
    String getName();

    void applyExtraParams(Array<Object> extraParams);

    int getRowCount();

    int getColCount();

    String getMapData();
}
