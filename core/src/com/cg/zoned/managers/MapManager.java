package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.Cell;
import com.cg.zoned.Constants;
import com.cg.zoned.maps.ExternalMapReader;
import com.cg.zoned.maps.ExternalMapTemplate;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.maps.internalmaps.HoloMap;
import com.cg.zoned.maps.internalmaps.RectangleMap;
import com.cg.zoned.maps.internalmaps.XMap;

public class MapManager {
    public static final char EMPTY_CHAR = '.';
    public static final char WALL_CHAR = '#';
    public static final String VALID_START_POSITIONS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Array<MapEntity> mapList;

    private Cell[][] preparedMapGrid = null;
    private Array<GridPoint2> preparedStartPositions = null;
    private Array<String> preparedStartPosNames = null;
    private int wallCount = 0;

    private FileHandle externalMapDir = null;

    public MapManager() {
        this.mapList = new Array<>();

        loadDefaultMaps();
    }

    private void loadDefaultMaps() {
        mapList.add(new RectangleMap());
        mapList.add(new HoloMap());
        mapList.add(new XMap());
    }

    public void loadExternalMaps(final OnExternalMapLoadListener mapLoadListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ExternalMapReader externalMapReader = new ExternalMapReader();
                externalMapDir = externalMapReader.getExternalMapDir();
                externalMapReader.scanAndParseExternalMaps();
                Array<ExternalMapTemplate> externalLoadedMaps = externalMapReader.getLoadedMaps();

                int externalMapStartIndex = mapList.size;
                for (MapEntity externalMap : externalLoadedMaps) {
                    mapList.add(externalMap);
                }

                mapLoadListener.onExternalMapLoaded(getMapList(), externalMapStartIndex);
            }
        }).start();
    }

    public Texture getMapPreview(String mapName) {
        // Scan for the map preview in the internal directory
        try {
            return new Texture(Gdx.files.internal("icons/map_icons/" + mapName + ".png"));
        } catch (GdxRuntimeException ignored) {
        }

        // Scan for the map preview in the external directory
        try {
            return new Texture(Gdx.files.external(externalMapDir + "/" + mapName + ".png"));
        } catch (GdxRuntimeException | NullPointerException e) {
            Gdx.app.log(Constants.LOG_TAG, "Failed to load map preview image for '" + mapName + "' (" + e.getMessage() + ")");
            return null;
        }
    }

    public Array<MapEntity> getMapList() {
        return mapList;
    }

    public void prepareMap(int mapIndex) throws InvalidMapCharacter, NoStartPositionsFound, InvalidMapDimensions {
        MapEntity selectedMap = mapList.get(mapIndex);
        String mapData = selectedMap.getMapData();

        String[] mapRows = mapData.split("\n");
        if (mapRows.length != selectedMap.getRowCount()) {
            throw new InvalidMapDimensions("Row count does not match the map grid string");
        }

        for (String mapRow : mapRows) {
            if (mapRow.length() != selectedMap.getColCount()) {
                throw new InvalidMapDimensions("Col count does not match the map grid string");
            }
        }

        this.preparedStartPosNames = selectedMap.getStartPosNames();
        parseMapData(mapData);
    }

    private void parseMapData(String mapData) throws InvalidMapCharacter, NoStartPositionsFound {
        Array<GridPoint2> startPositions = new Array<>();
        String[] mapRows = mapData.split("\n");
        Cell[][] mapGrid = new Cell[mapRows.length][];
        int wallCount = 0;
        for (int i = mapRows.length - 1; i >= 0; i--) {
            int mirroredIndex = mapRows.length - i - 1;
            // Mirrored because reading starts from the top left but rendering starts from the bottom left
            mapGrid[mirroredIndex] = new Cell[mapRows[i].length()];
            for (int j = 0; j < mapRows[i].length(); j++) {
                char c = mapRows[i].charAt(j);
                mapGrid[mirroredIndex][j] = new Cell();
                if (c == EMPTY_CHAR) {
                    continue;
                } else if (c == WALL_CHAR) {
                    mapGrid[mirroredIndex][j].isMovable = false;
                    wallCount++;
                } else if (VALID_START_POSITIONS.indexOf(c) != -1) {
                    int index = c - VALID_START_POSITIONS.charAt(0);
                    for (int k = startPositions.size; k <= index; k++) {
                        startPositions.add(null);
                    }
                    startPositions.set(index, new GridPoint2(j, mirroredIndex));
                } else {
                    throw new InvalidMapCharacter("Unknown character '" + c + "' found when parsing the map");
                }
            }
        }

        if (startPositions.size == 0) {
            throw new NoStartPositionsFound("No start positions found in the map");
        }

        this.preparedMapGrid = mapGrid;
        this.preparedStartPositions = startPositions;
        this.wallCount = wallCount;
    }

    public Cell[][] getPreparedMapGrid() {
        return preparedMapGrid;
    }

    public Array<String> getPreparedStartPosNames() {
        return preparedStartPosNames;
    }

    public Array<GridPoint2> getPreparedStartPositions() {
        return preparedStartPositions;
    }

    public int getWallCount() {
        return wallCount;
    }

    public interface OnExternalMapLoadListener {
        void onExternalMapLoaded(Array<MapEntity> mapList, int externalMapStartIndex);
    }
}
