package com.cg.zoned.maps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.managers.MapManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExternalMapReader scans and parses external maps for the MapManager to handle
 * <p>
 * Save map files in the "ZonedExternalMaps" directory along optionally with a .png preview
 * and the game should automatically pick it up and you can play on that map
 * <p>
 * Directory to save the .map and the .png preview files
 * - On Android: /storage/emulated/0/Android/data/com.cg.zoned/files/ZonedExternalMaps/
 * - On Linux: /home/username/Zoned/ZonedExternalMaps/
 * - On Windows: C:\\Users\\username\\Documents\\Zoned\\ZonedExternalMaps\\
 */
public class ExternalMapReader {
    public static final String mapDirName = "ZonedExternalMaps";
    private FileHandle externalMapDir;

    private Array<ExternalMapTemplate> loadedMaps;

    public ExternalMapReader() {
        this.loadedMaps = new Array<>();

        setExternalMapDir();
    }

    private void setExternalMapDir() {
        // Gdx.files.getExternalStoragePath() (And Gdx.files.external("") is relative to)
        //  - On Android: /storage/emulated/0/
        //  - On Desktop (Linux): /home/<username>/
        //  - On Desktop (Windows): C:\Users\<username>\

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            externalMapDir = Gdx.files.external("Android/data/com.cg.zoned/files/" + mapDirName);
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            if (Gdx.files.getExternalStoragePath().startsWith("/home")) {
                // Linux
                externalMapDir = Gdx.files.external("Zoned/" + mapDirName);
            } else {
                // Windows
                externalMapDir = Gdx.files.external("Documents/Zoned/" + mapDirName);
            }
        }

        try {
            externalMapDir.mkdirs(); // Create them folders if they don't exist
        } catch (NullPointerException e) {
            e.printStackTrace();
            Gdx.app.log(Constants.LOG_TAG, "Failed to create external map directory");
        }
    }

    public void scanAndParseExternalMaps() {
        Array<FileHandle> mapFiles = scanExternalMaps();
        if (mapFiles.notEmpty()) {
            parseScannedMaps(mapFiles);
        }
    }

    private Array<FileHandle> scanExternalMaps() {
        Array<FileHandle> mapFiles = new Array<>();

        try {
            Gdx.app.log(Constants.LOG_TAG, "Scanning for external maps on "
                    + Gdx.files.getExternalStoragePath() + externalMapDir.path());

            for (FileHandle mapFile : externalMapDir.list(".map")) {
                if (!mapFile.isDirectory()) {
                    Gdx.app.log(Constants.LOG_TAG, "Map found: " + mapFile.name());
                    mapFiles.add(mapFile);
                }
            }

            Gdx.app.log(Constants.LOG_TAG, "External map scan complete (" + mapFiles.size + " maps found)");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Gdx.app.log(Constants.LOG_TAG, "NPE during external map scan: " + e.getMessage());
        }

        return mapFiles;
    }

    private void parseScannedMaps(Array<FileHandle> mapFiles) {
        Gdx.app.log(Constants.LOG_TAG, "Preparing to parse the scanned maps");
        for (FileHandle mapFile : mapFiles) {
            String fileContents = mapFile.readString();

            String mapGrid = null, mapName = mapFile.nameWithoutExtension();
            Array<String> startPosNames = new Array<>();
            int rowCount = 0, colCount = 0;

            StringBuilder mapGridBuilder = new StringBuilder();

            String rowCountPrompt = "Row count:";
            String colCountPrompt = "Col count:";

            Pattern startPosPattern = Pattern.compile(
                    "(^[" + MapManager.VALID_START_POSITIONS.charAt(0) + "-" + MapManager.VALID_START_POSITIONS.charAt(MapManager.VALID_START_POSITIONS.length() - 1) + "])" +
                            ":(.*)",
                    Pattern.MULTILINE);

            String[] fileLines = fileContents.split("\r?\n");
            for (String fileLine : fileLines) {
                if (fileLine.startsWith(rowCountPrompt)) {
                    rowCount = Integer.parseInt(fileLine.substring(rowCountPrompt.length()).trim());
                } else if (fileLine.startsWith(colCountPrompt)) {
                    colCount = Integer.parseInt(fileLine.substring(colCountPrompt.length()).trim());
                } else {
                    Matcher matcher = startPosPattern.matcher(fileLine);
                    if (matcher.matches()) {
                        char startPosChar = matcher.group(1).trim().charAt(0);
                        String startPosName = matcher.group(2).trim();

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
                Gdx.app.log(Constants.LOG_TAG, "Successfully parsed " + mapFile.name());
            } else {
                Gdx.app.log(Constants.LOG_TAG, "Failed to parse " + mapFile.name());
            }
        }
        Gdx.app.log(Constants.LOG_TAG, "Map parsing completed");
    }

    public Array<ExternalMapTemplate> getLoadedMaps() {
        return loadedMaps;
    }

    public FileHandle getExternalMapDir() {
        return externalMapDir;
    }
}
