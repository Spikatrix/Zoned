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
        config.addIcon("images/ic_zoned_desktop_icon.png", Files.FileType.Internal);
        new LwjglApplication(new Zoned(new DiscordRPCManager()), config);
    }
}
