package com.cg.zoned.maps;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.SpinnerVars;

public class RectangleMap implements MapEntity {
    private String mapGridString = "" + // Stupid auto-code formatter messes up the arrangement, so added this line
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

    private int rowCount = 10;
    private int colCount = 10;

    private MapExtraParams mapExtraParams = null;

    public RectangleMap() throws InvalidMapDimensions {
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
    public MapExtraParams getExtraParamPrompts() {
        if (mapExtraParams != null) {
            return mapExtraParams;
        }

        String promptTitle = "Grid size";
        Array<SpinnerVars> spinnerVars = new Array<SpinnerVars>();
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
        char startPos = 'A';
        for (int i = 0; i < this.rowCount; i++) {
            for (int j = 0; j < this.colCount; j++) {
                if ((i == 0 || i == this.rowCount - 1) && (j == 0 || j == this.colCount - 1)) {
                    mapGridBuilder.append(startPos);
                    startPos++;
                } else {
                    mapGridBuilder.append('.');
                }
            }

            mapGridBuilder.append('\n');
        }

        this.mapGridString = mapGridBuilder.toString();
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
