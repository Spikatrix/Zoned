package com.cg.zoned.dataobjects;

import com.badlogic.gdx.graphics.Color;
import com.cg.zoned.maps.MapExtraParams;

public class PlayerSetUpParams {
    public String mapName;
    public MapExtraParams mapExtraParams;
    public Color[] playerColors;

    public PlayerSetUpParams(String mapName, MapExtraParams mapExtraParams, Color[] playerColors) {
        this.mapName = mapName;
        this.mapExtraParams = mapExtraParams;
        this.playerColors = playerColors;
    }
}
