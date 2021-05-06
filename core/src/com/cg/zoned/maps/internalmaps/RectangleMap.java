package com.cg.zoned.maps.internalmaps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.SpinnerVars;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;

public class RectangleMap implements MapEntity {
    private String mapGridString = "" + // Added this line as the auto-code formatter messes up the arrangement
            "A........C\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "..........\n" +
            "D........B\n";

    private Array<StartPosition> startPositions = new Array<>();

    private int rowCount = 10;
    private int colCount = 10;

    private MapExtraParams mapExtraParams = null;

    public RectangleMap() {
        startPositions.addAll(
                new StartPosition("Top Left",     'A'),
                new StartPosition("Bottom Right", 'B'),
                new StartPosition("Top Right",    'C'),
                new StartPosition("Bottom Left",  'D')
        );
    }

    @Override
    public MapExtraParams getExtraParams() {
        if (mapExtraParams != null) {
            return mapExtraParams;
        }

        String promptTitle = "Grid size";
        Array<SpinnerVars> spinnerVars = new Array<>();
        spinnerVars.add(new SpinnerVars("Row count: ", 3, 100, this.rowCount));
        spinnerVars.add(new SpinnerVars("Col count: ", 3, 100, this.colCount));

        mapExtraParams = new MapExtraParams(promptTitle, spinnerVars);
        return mapExtraParams;
    }

    @Override
    public void applyExtraParams() {
        this.rowCount = mapExtraParams.extraParams[0];
        this.colCount = mapExtraParams.extraParams[1];

        mapExtraParams.spinnerVars.get(0).snapValue = this.rowCount;
        mapExtraParams.spinnerVars.get(1).snapValue = this.colCount;

        StringBuilder mapGridBuilder = new StringBuilder();
        String startPositions = "ACDB";
        int startPosCharIndex = 0;
        for (int i = 0; i < this.rowCount; i++) {
            for (int j = 0; j < this.colCount; j++) {
                if ((i == 0 || i == this.rowCount - 1) && (j == 0 || j == this.colCount - 1)) {
                    mapGridBuilder.append(startPositions.charAt(startPosCharIndex));
                    startPosCharIndex++;
                } else {
                    mapGridBuilder.append('.');
                }
            }

            mapGridBuilder.append('\n');
        }

        this.mapGridString = mapGridBuilder.toString();
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
        return "Rectangle";
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
