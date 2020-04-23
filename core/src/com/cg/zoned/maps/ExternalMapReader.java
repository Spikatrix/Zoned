package com.cg.zoned.maps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;

public class ExternalMapReader {
    public static final String mapDirName = "ZonedExternalMaps";

    private Array<ExternalMapTemplate> loadedMaps;

    public ExternalMapReader() {
        this.loadedMaps = new Array<ExternalMapTemplate>();
    }

    public void scanAndParseExternalMaps() {
        Array<FileHandle> mapFiles = scanExternalMaps();
        parseScannedMaps(mapFiles);
    }

    private Array<FileHandle> scanExternalMaps() {
        // Gdx.files.getExternalStoragePath() (And Gdx.files.external("") relative to)
        //  - On Android: /storage/emulated/0/
        //  - On Desktop (Linux): /home/<username>/
        //  - On Desktop (Windows): C:\Users\<username>\

        Array<FileHandle> mapFiles = new Array<FileHandle>();

        FileHandle mapDir = null;
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            mapDir = Gdx.files.external("Android/data/com.cg.zoned/files/" + mapDirName);
        } else if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            if (Gdx.files.getExternalStoragePath().startsWith("/home")) {
                // Linux
                mapDir = Gdx.files.external("Zoned/" + mapDirName);
            } else {
                // Windows
                mapDir = Gdx.files.external("Documents/Zoned/" + mapDirName);
            }
        }

        try {
            mapDir.mkdirs(); // Create them folders if they don't exist

            Gdx.app.log(Constants.LOG_TAG, "Scanning for external maps on "
                    + Gdx.files.getExternalStoragePath() + mapDir.path());

            for (FileHandle mapFile : mapDir.list(".map")) {
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

            String mapGrid = null, mapName = null;
            int rowCount = 0, colCount = 0;

            StringBuilder mapGridBuilder = new StringBuilder();

            String mapNamePrompt = "Map name:";
            String rowCountPrompt = "Row count:";
            String colCountPrompt = "Col count:";

            String[] fileLines = fileContents.split("\n");
            for (int i = 0; i < fileLines.length; i++) {
                if (i == 0 && fileLines[i].startsWith(mapNamePrompt)) {
                    mapName = fileLines[i].substring(mapNamePrompt.length()).trim();
                } else if (i == 1 && fileLines[i].startsWith(rowCountPrompt)) {
                    rowCount = Integer.parseInt(fileLines[i].substring(rowCountPrompt.length()).trim());
                } else if (i == 2 && fileLines[i].startsWith(colCountPrompt)) {
                    colCount = Integer.parseInt(fileLines[i].substring(colCountPrompt.length()).trim());
                } else {
                    mapGridBuilder.append(fileLines[i]);
                }
            }

            mapGrid = mapGridBuilder.toString();

            if (!mapGrid.isEmpty() && mapName != null && rowCount > 0 && colCount > 0) {
                loadedMaps.add(new ExternalMapTemplate(mapName, mapGrid, rowCount, colCount));
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
}
