package com.cg.zoned.maps.internalmaps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;

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

    public HoloMap() {
    }

    @Override
    public MapExtraParams getExtraParamPrompts() {
        return null;
    }

    @Override
    public void applyExtraParams() {
    }

    @Override
    public Array<String> getStartPosNames() {
        return null;
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
