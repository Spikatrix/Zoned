package com.cg.zoned.maps.internalmaps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;

public class XMap implements MapEntity {
    private String mapGridString = "" + // Stupid auto-code formatter messes up the arrangement, so added this line
            "A.............C\n" +
            ".#...........#.\n" +
            "..#.........#..\n" +
            "...............\n" +
            "....#.....#....\n" +
            ".....#...#.....\n" +
            "...............\n" +
            ".......#.......\n" +
            "...............\n" +
            ".....#...#.....\n" +
            "....#.....#....\n" +
            "...............\n" +
            "..#.........#..\n" +
            ".#...........#.\n" +
            "D.............B\n";

    private int rowCount = 15;
    private int colCount = 15;

    public XMap() {
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
        return "X";
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
