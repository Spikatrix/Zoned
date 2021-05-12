package com.cg.zoned.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.cg.zoned.Zoned;

public class DesktopLauncher {
    public static void main(String[] args) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Zoned";
        config.vSyncEnabled = true;
        config.samples = 4;

        // Sets 16:9 aspect ratio window size, undecorated if the debug param is passed
        setWindowAttr(config, args);

        // Uses the first icon which is supported
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_128x128.png", Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_64x64.png",   Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_32x32.png",   Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_16x16.png",   Files.FileType.Internal);

        new LwjglApplication(new Zoned(new DiscordRPCManager()), config);
    }

    private static void setWindowAttr(LwjglApplicationConfiguration config, String[] args) {
        if (args.length > 0 && args[0].toUpperCase().equals("DEBUG")) {
            // Launch in a smaller sized 16:9 aspect ratio undecorated window as they're easier to work with
            config.width = 640;
            config.height = 360;
            config.undecorated = true;
        } else {
            // Launch in a 16:9 aspect ratio window by default
            config.width = 960;
            config.height = 540;
        }
    }
}
