package com.cg.zoned.maps;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.Cell;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.dataobjects.StartPosition;

import java.io.FileNotFoundException;
import java.util.Comparator;

/**
 * MapLoader helps parse and prepare maps for the MapManager to handle.
 * Throws corresponding exceptions in case of issues during map loading.
 */
public class MapLoader {
    public static final char EMPTY_CHAR = '.';
    public static final char WALL_CHAR = '#';

    // Each character in this string is treated as a valid start position indicator in maps
    public static final String VALID_START_POSITIONS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private FileHandle externalMapDir;

    private PreparedMapData preparedMapData;

    public MapLoader(FileHandle externalMapDir) {
        this.externalMapDir = externalMapDir;
        this.preparedMapData = new PreparedMapData();
    }

    /**
     * Loads the specified map
     * .
     * External maps are both parsed and prepared while internal maps are only prepared as
     * they're already stored pre-parsed. See {@link #parseMap(ExternalMapTemplate)} and {@link #prepareMap(MapEntity)}
     *
     * @param map The map to be loaded
     * @throws InvalidMapDimensions Thrown when the map has invalid row/col dimensions than the specified grid
     * @throws MapGridMissing Thrown when the map grid is missing in the map
     * @throws StartPositionsMissing Thrown when the map grid has no start positions specified
     * @throws InvalidMapCharacter Thrown when the map grid has an unknown invalid character in it
     * @throws FileNotFoundException Thrown when the map file is not found
     */
    public void loadMap(MapEntity map) throws InvalidMapDimensions, MapGridMissing, StartPositionsMissing, InvalidMapCharacter, FileNotFoundException {
        if (map instanceof ExternalMapTemplate) {
            // The supplied map is an external map; parse it first before preparing
            parseMap((ExternalMapTemplate) map);
        }
        prepareMap(map);
    }

    /**
     * Parses the specified map. It goes through the map file contents and creates the external map format object.
     * This is to be done only for external maps. Internal maps are already stored pre-parsed.
     *
     * @param map The map to be parsed
     * @throws FileNotFoundException Thrown when the external map file is not found
     */
    private void parseMap(ExternalMapTemplate map) throws FileNotFoundException {
        FileHandle mapFile = externalMapDir.child(map.getName() + ".map");
        if (!mapFile.exists()) {
            map.updateMapData(null, null, -1, -1);
            throw new FileNotFoundException("Map file '" + mapFile.name() + "' not found");
        }

        String fileContents = mapFile.readString();

        String mapGrid = null, mapName = mapFile.nameWithoutExtension();
        Array<StartPosition> startPositions = new Array<>();
        int rowCount = 0, colCount = 0;

        StringBuilder mapGridBuilder = new StringBuilder();

        String rowCountPrompt = "Row count:";
        String colCountPrompt = "Col count:";

        String[] fileLines = fileContents.split("\r?\n");
        for (String fileLine : fileLines) {
            if (fileLine.startsWith(rowCountPrompt)) {
                rowCount = Integer.parseInt(fileLine.substring(rowCountPrompt.length()).trim());
            } else if (fileLine.startsWith(colCountPrompt)) {
                colCount = Integer.parseInt(fileLine.substring(colCountPrompt.length()).trim());
            } else {
                if (fileLine.trim().length() >= 3 &&
                        VALID_START_POSITIONS.indexOf(fileLine.charAt(0)) != -1 &&
                        fileLine.charAt(1) == ':') {
                    char startPosChar = fileLine.charAt(0);
                    String startPosName = fileLine.substring(2).trim();

                    startPositions.add(new StartPosition(startPosName, startPosChar));
                } else {
                    mapGridBuilder.append(fileLine).append('\n');
                }
            }
        }

        mapGrid = mapGridBuilder.toString();

        if (!mapGrid.isEmpty() && mapName != null && rowCount > 0 && colCount > 0) {
            map.updateMapData(mapGrid, startPositions, rowCount, colCount);
        }
    }

    /**
     * Prepares the specified map. It goes through the parsed map data and prepares them into a playable format,
     * while checking for irregularities in the map data. The prepared map data can be accessed using {@link #getPreparedMapData()}
     *
     * @param map The map file to be prepared
     * @throws InvalidMapDimensions Thrown when the map has invalid row/col dimensions than the specified grid
     * @throws MapGridMissing Thrown when the map grid is missing in the map
     * @throws StartPositionsMissing Thrown when the map grid has no start positions specified
     * @throws InvalidMapCharacter Thrown when the map grid has an unknown invalid character in it
     */
    private void prepareMap(MapEntity map) throws InvalidMapCharacter, StartPositionsMissing, InvalidMapDimensions, MapGridMissing {
        String mapData = map.getMapData();
        Array<StartPosition> startPositions = map.getStartPositions();

        if (mapData == null) {
            throw new MapGridMissing("Map grid must be specified in the map");
        }

        String[] mapRows = mapData.split("\n");
        if (mapRows.length != map.getRowCount()) {
            throw new InvalidMapDimensions("Row count does not match the map grid string");
        }

        for (String mapRow : mapRows) {
            if (mapRow.length() != map.getColCount()) {
                throw new InvalidMapDimensions("Col count does not match the map grid string");
            }
        }

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
            throw new StartPositionsMissing("No start positions found in the map");
        }

        // Sort all start positions based on its alt name
        startPositions.sort(new Comparator<StartPosition>() {
            @Override
            public int compare(StartPosition sp1, StartPosition sp2) {
                return sp1.getAltName() - sp2.getAltName();
            }
        });

        preparedMapData.map = map;
        preparedMapData.mapGrid = mapGrid;
        preparedMapData.startPositions = startPositions;
        preparedMapData.wallCount = wallCount;
    }

    public PreparedMapData getPreparedMapData() {
        return preparedMapData;
    }
}
