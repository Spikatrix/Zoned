package com.cg.zoned.maps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.managers.MapManager;

/**
 * ExternalMapReader scans and parses external maps for the MapManager to handle
 * <p>
 * Save map files in the "ZonedExternalMaps" directory along optionally with a .png preview
 * and the game should automatically pick it up and you can play on that map
 * <p>
 * Directory to save the .map and the .png preview files
 * - On Android: /storage/emulated/0/Android/data/com.cg.zoned/files/ZonedExternalMaps/
 * - On Linux: /home/username/.zoned/ZonedExternalMaps/
 * - On Windows: C:\\Users\\username\\Documents\\Zoned\\ZonedExternalMaps\\
 */
public class ExternalMapReader {
    public static final String mapDirName = "ZonedExternalMaps";
    private FileHandle externalMapDir;

    private Array<ExternalMapTemplate> loadedMaps;
    private boolean enableExternalMapLogging = false;

    public ExternalMapReader() {
        this.loadedMaps = new Array<>();

        setExternalMapDir();
    }

    private void setExternalMapDir() {
        // Gdx.files.getExternalStoragePath() (And Gdx.files.external("") is relative to)
        //  - On Android: /storage/emulated/0/
        //  - On Desktop (Linux): /home/<username>/
        //  - On Desktop (Windows): C:\Users\<username>\

        externalMapDir = null;
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // Android
            externalMapDir = Gdx.files.external(mapDirName);
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("linux")) {
                // Linux
                externalMapDir = Gdx.files.external(".zoned/" + mapDirName);
            } else if (os.startsWith("win")) {
                // Windows
                externalMapDir = Gdx.files.external("Documents/Zoned/" + mapDirName);
            } else {
                // Mac (I don't own a mac, so I can't test this)
                externalMapDir = Gdx.files.external("Library/Application Support/Zoned/");
            }
        }

        try {
            externalMapDir.mkdirs(); // Create them folders if they don't exist
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("Failed to create external map directory", true);
        }
    }

    public void scanAndParseExternalMaps() {
        Array<FileHandle> mapFiles = scanExternalMaps();
        if (mapFiles.notEmpty()) {
            parseScannedMaps(mapFiles);
        }
    }

    public void parseExternalMap(String mapName) {
        FileHandle mapFile = externalMapDir.child(mapName + ".map");
        if (!mapFile.exists() || mapFile.isDirectory()) {
            return;
        }

        parseMap(mapFile);
    }

    private Array<FileHandle> scanExternalMaps() {
        Array<FileHandle> mapFiles = new Array<>();

        try {
            log("Scanning for external maps on " + Gdx.files.getExternalStoragePath() + externalMapDir.path());

            for (FileHandle mapFile : externalMapDir.list(".map")) {
                if (!mapFile.isDirectory()) {
                    log("Map found: " + mapFile.name());
                    mapFiles.add(mapFile);
                }
            }
            log("External map scan complete (" + mapFiles.size + " maps found)");
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("NPE during external map scan: " + e.getMessage(), true);
        }

        return mapFiles;
    }

    private void parseScannedMaps(Array<FileHandle> mapFiles) {
        log("Preparing to parse the scanned maps");
        for (FileHandle mapFile : mapFiles) {
            parseMap(mapFile);
        }
        log("Map parsing completed");
    }

    private void parseMap(FileHandle mapFile) {
        String fileContents = mapFile.readString();

        String mapGrid = null, mapName = mapFile.nameWithoutExtension();
        Array<String> startPosNames = new Array<>();
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
                        MapManager.VALID_START_POSITIONS.indexOf(fileLine.charAt(0)) != -1 &&
                        fileLine.charAt(1) == ':') {
                    char startPosChar = fileLine.charAt(0);
                    String startPosName = fileLine.substring(2).trim();

                    int index = startPosChar - MapManager.VALID_START_POSITIONS.charAt(0);
                    for (int j = startPosNames.size; j <= index; j++) {
                        startPosNames.add(null);
                    }

                    startPosNames.set(index, startPosName);
                } else {
                    mapGridBuilder.append(fileLine).append('\n');
                }
            }
        }

        mapGrid = mapGridBuilder.toString();

        if (!mapGrid.isEmpty() && mapName != null && rowCount > 0 && colCount > 0) {
            loadedMaps.add(new ExternalMapTemplate(mapName, mapGrid, startPosNames, rowCount, colCount));
            log("Successfully parsed " + mapFile.name());
        } else {
            log("Failed to parse " + mapFile.name(), true);
        }
    }

    private void log(String message, boolean isError) {
        if (enableExternalMapLogging) {
            if (isError) {
                Gdx.app.error(Constants.LOG_TAG, message);
            } else {
                Gdx.app.log(Constants.LOG_TAG, message);
            }
        }
    }

    private void log(String message) {
        this.log(message, false);
    }

    public void enableExternalMapLogging(boolean enableExternalMapLogging) {
        this.enableExternalMapLogging = enableExternalMapLogging;
    }

    public Array<ExternalMapTemplate> getLoadedMaps() {
        return loadedMaps;
    }

    public FileHandle getExternalMapDir() {
        return externalMapDir;
    }
}
