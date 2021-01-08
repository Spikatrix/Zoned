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
        config.samples = 4; // See issue #4

        // Uses the first icon which is supported in that implementation
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_128x128.png", Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_64x64.png",   Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_32x32.png",   Files.FileType.Internal);
        config.addIcon("images/desktop_icons/ic_zoned_desktop_icon_16x16.png",   Files.FileType.Internal);

        new LwjglApplication(new Zoned(new DiscordRPCManager()), config);
    }
}
