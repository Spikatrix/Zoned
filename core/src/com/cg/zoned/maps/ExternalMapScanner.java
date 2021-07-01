package com.cg.zoned.maps;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;

/**
 * ExternalMapScanner scans external maps for the MapManager to handle
 * <p>
 * Save map files in the "ZonedExternalMaps" directory along optionally with a .png preview
 * and the game should automatically pick it up and you can play on that map
 * <p>
 * Directory to save the .map and the .png preview files
 * - On Android: /storage/emulated/0/Android/data/com.cg.zoned/files/ZonedExternalMaps/
 * - On Linux: /home/username/.zoned/ZonedExternalMaps/
 * - On Windows: C:\\Users\\username\\Documents\\Zoned\\ZonedExternalMaps\\
 */
public class ExternalMapScanner {
    public static final String mapDirName = "ZonedExternalMaps";
    private FileHandle externalMapDir;
    private boolean enableExternalMapLogging = false;

    public ExternalMapScanner() {
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
                externalMapDir = Gdx.files.external("Library/Application Support/Zoned/" + mapDirName);
            }
        }

        try {
            externalMapDir.mkdirs(); // Create them folders if they don't exist
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("Failed to create external map directory", true);
        }
    }

    public Array<FileHandle> scanExternalMaps() {
        Array<FileHandle> scannedMaps = new Array<>();

        try {
            log("Scanning for external maps on " + Gdx.files.getExternalStoragePath() + externalMapDir.path());

            for (FileHandle mapFile : externalMapDir.list(".map")) {
                if (!mapFile.isDirectory()) {
                    log("Map found: " + mapFile.name());
                    scannedMaps.add(mapFile);
                }
            }
            log("External map scan complete (" + scannedMaps.size + " maps found)");
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("NPE during external map scan: " + e.getMessage(), true);
        }

        return scannedMaps;
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

    public void enableExternalMapScanLogging(boolean enableExternalMapScanLogging) {
        this.enableExternalMapLogging = enableExternalMapScanLogging;
    }

    public FileHandle getExternalMapDir() {
        return externalMapDir;
    }
}
