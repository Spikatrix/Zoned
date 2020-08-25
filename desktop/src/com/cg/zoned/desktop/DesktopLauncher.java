package com.cg.zoned.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.cg.zoned.Constants;
import com.cg.zoned.Zoned;

public class DesktopLauncher {
    public static void main(String[] args) {
        if (args.length > 0 && !args[0].equals(Constants.GAME_VERSION)) {
            // Used as a precaution
            // TODO: Too annoying. Remove?
            System.err.println("GAME VERSION MISMATCH (" + args[0] + " and " + Constants.GAME_VERSION + ")");
            return;
        }

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Zoned";
        config.vSyncEnabled = true;
        config.addIcon("icons/ic_zoned_desktop_icon.png", Files.FileType.Internal);
        new LwjglApplication(new Zoned(), config);
    }
}
