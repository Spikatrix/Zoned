package com.cg.zoned.maps.internalmaps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;

public class HoloMap implements MapEntity {
    private String mapGridString = "" + // Added this line as the auto-code formatter messes up the arrangement
            "........A##C........\n" +
            "....#..........#....\n" +
            "....#..........#....\n" +
            ".........##.........\n" +
            "#..................#\n" +
            "#..................#\n" +
            ".........##.........\n" +
            "....#..........#....\n" +
            "....#..........#....\n" +
            "........D##B........\n";

    private Array<StartPosition> startPositions = new Array<>();

    private int rowCount = 10;
    private int colCount = 20;

    public HoloMap() {
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
