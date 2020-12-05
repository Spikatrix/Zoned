package com.cg.zoned.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cg.zoned.Zoned;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Zoned");
        config.useVsync(true);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        config.setWindowIcon(Files.FileType.Internal, "icons/ic_zoned_desktop_icon.png");
        new Lwjgl3Application(new Zoned(new DiscordRPCManager()), config);
    }
}
