package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.Constants;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.StartPosition;
import com.cg.zoned.maps.ExternalMapScanner;
import com.cg.zoned.maps.ExternalMapTemplate;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapGridMissing;
import com.cg.zoned.maps.MapLoader;
import com.cg.zoned.maps.StartPositionsMissing;
import com.cg.zoned.maps.internalmaps.HoloMap;
import com.cg.zoned.maps.internalmaps.RectangleMap;
import com.cg.zoned.maps.internalmaps.XMap;

import java.io.FileNotFoundException;

public class MapManager {
    private final Array<MapEntity> mapList;
    private int internalMapCount = 0;

    private ExternalMapScanner externalMapScanner;
    private MapLoader mapLoader;

    private PreparedMapData preparedMapData;

    public MapManager() {
        mapList = new Array<>();
        preparedMapData = new PreparedMapData();

        externalMapScanner = new ExternalMapScanner();
        mapLoader = new MapLoader(getExternalMapDir());

        loadDefaultMaps();
    }

    private void loadDefaultMaps() {
        mapList.add(new RectangleMap());
        mapList.add(new HoloMap());
        mapList.add(new XMap());

        internalMapCount = mapList.size;
    }

    /**
     * Used to rescan external maps
     *
     * @param mapLoadListener Listener which fires when
     */
    public void loadExternalMaps(final ExternalMapScanListener mapLoadListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mapList.size > internalMapCount) {
                    // Remove all external maps
                    mapList.removeRange(internalMapCount, mapList.size - 1);
                }

                // Rescan for external map files
                Array<FileHandle> externalMapFileList = externalMapScanner.scanExternalMaps();

                // Create map templates with the scanned external map list and add them into the map list
                int externalMapStartIndex = mapList.size;
                for (FileHandle externalMapFile : externalMapFileList) {
                    addNewExternalMap(externalMapFile.nameWithoutExtension());
                }

                // Pass external map load complete information to listeners
                mapLoadListener.onExternalMapScanComplete(mapList, externalMapStartIndex);
            }
        }).start();
    }

    /**
     * Used to add external maps to {@link #mapList} manually
     *
     * @param mapName Name of the external map to add
     */
    public void addNewExternalMap(String mapName) {
        addNewExternalMap(new ExternalMapTemplate(mapName));
    }

    public void addNewExternalMap(MapEntity externalMap) {
        if (getMapIndex(externalMap) != -1) {
            // Map list already has a map with the same name
            Gdx.app.error(Constants.LOG_TAG, "Map with name '" + externalMap.getName() + "' already exists; skipping...");
            return;
        }

        mapList.add(externalMap);
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
            FileHandle fileHandle = Gdx.files.external(getExternalMapDir() + "/" + mapName + ".png");
            if (fileHandle.exists()) {
                return new Texture(fileHandle);
            }
        } catch (GdxRuntimeException | NullPointerException e) {
            Gdx.app.error(Constants.LOG_TAG, "Failed to load map preview image for '" + mapName + "' (" + e.getMessage() + ")");
        }

        return null;
    }

    public void loadMap(MapEntity map) throws InvalidMapCharacter, InvalidMapDimensions, StartPositionsMissing, MapGridMissing,
            FileNotFoundException, IndexOutOfBoundsException {
        loadMap(getMapIndex(map));
    }

    public void loadMap(int index) throws InvalidMapCharacter, InvalidMapDimensions, StartPositionsMissing, MapGridMissing,
            FileNotFoundException, IndexOutOfBoundsException {
        if (index >= mapList.size || index < 0) {
            throw new IndexOutOfBoundsException("Cannot load a non-existent map. Invalid map index");
        }

        mapLoader.loadMap(mapList.get(index));
        this.preparedMapData = mapLoader.getPreparedMapData();
    }

    public MapEntity getMap(String mapName) {
        for (MapEntity map : mapList) {
            if (map.getName().equals(mapName)) {
                return map;
            }
        }

        return null;
    }

    private int getMapIndex(MapEntity map) {
        for (int i = 0; i < mapList.size; i++) {
            if (mapList.get(i).getName().equals(map.getName())) {
                return i;
            }
        }

        return -1;
    }

    public void enableExternalMapScanLogging(boolean enableExternalMapScanLogging) {
        this.externalMapScanner.enableExternalMapScanLogging(enableExternalMapScanLogging);
    }

    public Array<MapEntity> getMapList() {
        return mapList;
    }

    public MapEntity getPreparedMap() {
        return this.preparedMapData.map;
    }

    public Cell[][] getPreparedMapGrid() {
        return this.preparedMapData.mapGrid;
    }

    public Array<StartPosition> getPreparedStartPositions() {
        return this.preparedMapData.startPositions;
    }

    public int getPreparedMapWallCount() {
        return this.preparedMapData.wallCount;
    }

    public FileHandle getExternalMapDir() {
        return this.externalMapScanner.getExternalMapDir();
    }

    public interface ExternalMapScanListener {
        void onExternalMapScanComplete(Array<MapEntity> mapList, int externalMapStartIndex);
    }
}
