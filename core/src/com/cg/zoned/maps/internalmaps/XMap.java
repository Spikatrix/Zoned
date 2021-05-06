package com.cg.zoned.maps.internalmaps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;

public class XMap implements MapEntity {
    private String mapGridString = "" + // Added this line as the auto-code formatter messes up the arrangement
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

    private Array<StartPosition> startPositions = new Array<>();

    private int rowCount = 15;
    private int colCount = 15;

    public XMap() {
        startPositions.addAll(
                new StartPosition("Top Left",     'A'),
                new StartPosition("Bottom Right", 'B'),
                new StartPosition("Top Right",    'C'),
                new StartPosition("Bottom Left",  'D')
        );
    }

    @Override
    public MapExtraParams getExtraParams() {
        return null;
    }

    @Override
    public void applyExtraParams() {
    }

    @Override
    public Array<StartPosition> getStartPositions() {
        return startPositions;
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
