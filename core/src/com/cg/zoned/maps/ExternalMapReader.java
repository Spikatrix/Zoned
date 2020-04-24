package com.cg.zoned.maps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;

public class ExternalMapReader {
    public static final String mapDirName = "ZonedExternalMaps";
    private FileHandle externalMapDir;

    private Array<ExternalMapTemplate> loadedMaps;

    public ExternalMapReader() {
        this.loadedMaps = new Array<>();

        setExternalMapDir();
    }

    private void setExternalMapDir() {
        // Gdx.files.getExternalStoragePath() (And Gdx.files.external("") relative to)
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
            int rowCount = 0, colCount = 0;

            StringBuilder mapGridBuilder = new StringBuilder();

            String rowCountPrompt = "Row count:";
            String colCountPrompt = "Col count:";

            String[] fileLines = fileContents.split("\r?\n");
            for (int i = 0; i < fileLines.length; i++) {
                if (i == 0 && fileLines[i].startsWith(rowCountPrompt)) {
                    rowCount = Integer.parseInt(fileLines[i].substring(rowCountPrompt.length()).trim());
                } else if (i == 1 && fileLines[i].startsWith(colCountPrompt)) {
                    colCount = Integer.parseInt(fileLines[i].substring(colCountPrompt.length()).trim());
                } else {
                    mapGridBuilder.append(fileLines[i]).append('\n');
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

    public FileHandle getExternalMapDir() {
        return externalMapDir;
    }
}
