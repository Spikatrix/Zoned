package com.cg.zoned.managers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.cg.zoned.Constants;

import java.io.File;

public class BotController {
    // Will probably convert this into MapManager soon
    public BotController() {
        Gdx.app.log(Constants.LOG_TAG, "External file dir: " + Gdx.files.getExternalStoragePath());
        // On Android: /storage/emulated/0 (Need permission first)
        // On Desktop: /home/username/
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            File f = new File(Gdx.files.getExternalStoragePath());
            try {
                Gdx.app.log(Constants.LOG_TAG, "Starting file list: ");
                for (File file : f.listFiles()) {
                    if (!file.isDirectory())
                        Gdx.app.log(Constants.LOG_TAG, "Filename: " + file.getName());
                }
            } catch (NullPointerException e) {
                Gdx.app.log(Constants.LOG_TAG, "Npe " + e.getMessage());
            }
        }
    }
}
