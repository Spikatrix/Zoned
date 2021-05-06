package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.Constants;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.ExternalMapReader;
import com.cg.zoned.maps.ExternalMapTemplate;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.maps.internalmaps.HoloMap;
import com.cg.zoned.maps.internalmaps.RectangleMap;
import com.cg.zoned.maps.internalmaps.XMap;

import java.util.Comparator;

public class MapManager {
    public static final char EMPTY_CHAR = '.';
    public static final char WALL_CHAR = '#';

    // Each character in this string is treated as a valid start position indicator in maps
    public static final String VALID_START_POSITIONS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Array<MapEntity> mapList;

    private MapEntity preparedMap = null;
    private Cell[][] preparedMapGrid = null;
    private Array<StartPosition> preparedStartPositions = null;
    private int wallCount = 0;
    private int internalMapCount = 0;

    private ExternalMapReader externalMapReader;
    private FileHandle externalMapDir;

    public MapManager() {
        this.mapList = new Array<>();

        loadDefaultMaps();

        this.externalMapReader = new ExternalMapReader();
        externalMapDir = externalMapReader.getExternalMapDir();
    }

    private void loadDefaultMaps() {
        mapList.add(new RectangleMap());
        mapList.add(new HoloMap());
        mapList.add(new XMap());

        internalMapCount = mapList.size;
    }

    public void loadExternalMaps(final OnExternalMapLoadListener mapLoadListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mapList.size != internalMapCount) {
                    mapList.removeRange(internalMapCount, mapList.size - 1);
                }

                externalMapReader.scanAndParseExternalMaps();
                Array<ExternalMapTemplate> externalLoadedMaps = externalMapReader.getLoadedMaps();

                int externalMapStartIndex = mapList.size;
                for (MapEntity externalMap : externalLoadedMaps) {
                    mapList.add(externalMap);
                }

                mapLoadListener.onExternalMapsLoaded(getMapList(), externalMapStartIndex);
            }
        }).start();
    }

    public void loadExternalMap(String mapName) {
        // Loading on the main thread itself because it's just one map
        // (And because new threads messes up stuff in client lobby where this is called xD)

        for (MapEntity map : mapList) {
            if (map.getName().equals(mapName)) { // Map already loaded
                return;
            }
        }

        int sizeBefore = externalMapReader.getLoadedMaps().size;
        externalMapReader.parseExternalMap(mapName);
        Array<ExternalMapTemplate> externalMapList = externalMapReader.getLoadedMaps();

        if (externalMapList.size > sizeBefore) {
            mapList.add(externalMapList.get(sizeBefore));
        }
    }

    public Texture getMapPreview(String mapName) {
        // Scan for the map preview in the internal directory
        try {
            FileHandle fileHandle = Gdx.files.internal("images/map_icons/" + mapName + ".png");
            if (fileHandle.exists()) {
                return new Texture(fileHandle);
            }
        } catch (GdxRuntimeException ignored) {
        }

        // Scan for the map preview in the external directory
        try {
            FileHandle fileHandle = Gdx.files.external(externalMapDir + "/" + mapName + ".png");
            if (fileHandle.exists()) {
                return new Texture(fileHandle);
            }
        } catch (GdxRuntimeException | NullPointerException e) {
            Gdx.app.log(Constants.LOG_TAG, "Failed to load map preview image for '" + mapName + "' (" + e.getMessage() + ")");
        }

        return null;
    }

    public Array<MapEntity> getMapList() {
        return mapList;
    }

    public MapEntity getMap(String mapName) {
        for (MapEntity map : mapList) {
            if (map.getName().equals(mapName)) {
                return map;
            }
        }

        return null;
    }

    public void prepareMap(MapEntity map) throws NoStartPositionsFound, InvalidMapDimensions, InvalidMapCharacter {
        prepareMap(mapList.indexOf(map, false));
    }

    public void prepareMap(int mapIndex) throws InvalidMapCharacter, NoStartPositionsFound, InvalidMapDimensions {
        MapEntity selectedMap = mapList.get(mapIndex);
        String mapData = selectedMap.getMapData();
        Array<StartPosition> startPositions = selectedMap.getStartPositions();

        String[] mapRows = mapData.split("\n");
        if (mapRows.length != selectedMap.getRowCount()) {
            throw new InvalidMapDimensions("Row count does not match the map grid string");
        }

        for (String mapRow : mapRows) {
            if (mapRow.length() != selectedMap.getColCount()) {
                throw new InvalidMapDimensions("Col count does not match the map grid string");
            }
        }

        this.preparedMap = selectedMap;
        parseMapData(mapData, startPositions);
    }

    private void parseMapData(String mapData, Array<StartPosition> startPositions) throws InvalidMapCharacter, NoStartPositionsFound {
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
                    boolean foundStartPosName = false;
                    GridPoint2 startPosLocation = new GridPoint2(j, mirroredIndex);
                    for (int k = 0; k < startPositions.size; k++) {
                        if (startPositions.get(k).getAltName() == c) {
                            startPositions.get(k).setLocation(startPosLocation);
                            foundStartPosName = true;
                            break;
                        }
                    }
                    if (!foundStartPosName) {
                        startPositions.add(new StartPosition(null, c, startPosLocation));
                    }
                } else {
                    resetMapParseParams();
                    throw new InvalidMapCharacter("Unknown character '" + c + "' found when parsing the map");
                }
            }
        }

        // Remove start positions with an unknown location
        for (int i = 0; i < startPositions.size; i++) {
            if (startPositions.get(i).getLocation() == null) {
                startPositions.removeIndex(i--);
            }
        }

        if (startPositions.size == 0) {
            resetMapParseParams();
            throw new NoStartPositionsFound("No start positions found in the map");
        }

        // Sort all start positions based on its alt name
        startPositions.sort(new Comparator<StartPosition>() {
            @Override
            public int compare(StartPosition sp1, StartPosition sp2) {
                return sp1.getAltName() - sp2.getAltName();
            }
        });

        this.preparedMapGrid = mapGrid;
        this.preparedStartPositions = startPositions;
        this.wallCount = wallCount;
    }

    private void resetMapParseParams() {
        this.preparedMap = null;
        this.preparedMapGrid = null;
        this.preparedStartPositions = null;
    }

    public void enableExternalMapLogging(boolean enableExternalMapLogging) {
        this.externalMapReader.enableExternalMapLogging(enableExternalMapLogging);
    }

    public MapEntity getPreparedMap() {
        return preparedMap;
    }

    public Cell[][] getPreparedMapGrid() {
        return preparedMapGrid;
    }

    public Array<StartPosition> getPreparedStartPositions() {
        return preparedStartPositions;
    }

    public int getWallCount() {
        return wallCount;
    }

    public FileHandle getExternalMapDir() {
        return externalMapDir;
    }

    public interface OnExternalMapLoadListener {
        void onExternalMapsLoaded(Array<MapEntity> mapList, int externalMapStartIndex);
    }
}
