package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.maps.HoloMap;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.RectangleMap;
import com.cg.zoned.maps.XMap;

public class MapManager {
    private final char EMPTY_CHAR = '.';
    private final char WALL_CHAR = '#';
    private final String VALID_START_POSITIONS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Array<MapEntity> mapList;

    private Cell[][] preparedMapGrid = null;
    private Array<GridPoint2> preparedStartPositions = null;
    private int wallCount = 0;

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
        mapList.add(new RectangleMap());
        mapList.add(new HoloMap());
        mapList.add(new XMap());
    }

    public Texture getMapPreview(String mapName) {
        try {
            return new Texture(Gdx.files.internal("icons/map_icons/" + mapName + ".png"));
        } catch (GdxRuntimeException e) {
            Gdx.app.log(Constants.LOG_TAG, "Failed to load map preview image for '" + mapName + "'");
            return null;
        }
    }

    public Array<MapEntity> getMapList() {
        return mapList;
    }

    public void prepareMap(int mapIndex, Array<Integer> extraParams) throws InvalidMapCharacter {
        MapEntity selectedMap = mapList.get(mapIndex);
        // TODO: Apply extraParams here
        String mapData = selectedMap.getMapData();

        Array<GridPoint2> startPositions = new Array<GridPoint2>();
        String[] mapRows = mapData.split("\n");
        Cell[][] mapGrid = new Cell[mapRows.length][];
        int wallCount = 0;
        for (int i = 0; i < mapRows.length; i++) {
            mapGrid[i] = new Cell[mapRows[i].length()];
            for (int j = 0; j < mapRows[i].length(); j++) {
                char c = mapRows[i].charAt(j);
                mapGrid[i][j] = new Cell();
                if (c == EMPTY_CHAR) {
                    continue;
                } else if (c == WALL_CHAR) {
                    mapGrid[i][j].isMovable = false;
                    wallCount++;
                } else if (VALID_START_POSITIONS.indexOf(c) != -1) {
                    startPositions.add(new GridPoint2(i, j));
                } else {
                    throw new InvalidMapCharacter("Unknown character '" + c + "' found when parsing the map");
                }
            }
        }

        this.preparedMapGrid = mapGrid;
        this.preparedStartPositions = startPositions;
        this.wallCount = wallCount;
    }

    public Cell[][] getPreparedMapGrid() {
        return preparedMapGrid;
    }

    public Array<GridPoint2> getPreparedStartPositions() {
        return preparedStartPositions;
    }

    public int getWallCount() {
        return wallCount;
    }

    public void clearErrorMessage() {
        this.errorMessage = null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
