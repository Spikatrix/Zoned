package com.cg.zoned.managers;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.maps.DefaultRectangleMap;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;

public class MapManager {
    private Array<MapEntity> mapList;

    private String errorMessage = null;

    public MapManager() {
        this.mapList = new Array<MapEntity>();

        try {
            loadDefaultMaps();
        } catch (InvalidMapDimensions e) {
            errorMessage = e.getMessage();
        }
    }

    private void loadDefaultMaps() throws InvalidMapDimensions {
        mapList.add(new DefaultRectangleMap());
    }

    public void clearErrorMessage() {
        this.errorMessage = null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
