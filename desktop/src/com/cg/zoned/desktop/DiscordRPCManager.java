package com.cg.zoned.desktop;

import com.badlogic.gdx.Gdx;
import com.cg.zoned.Constants;
import com.cg.zoned.managers.DiscordRPCBridge;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

// Desktop only (Windows and Linux)
public class DiscordRPCManager implements DiscordRPCBridge {
    private final String applicationID = "726680223499812954";

    private DiscordRichPresence richPresence = null;

    public DiscordRPCManager() {
    }

    public void initRPC() {
        if (Constants.ENABLE_DISCORD_RPC) {
            if (richPresence == null) {
                DiscordRPC.discordInitialize(applicationID, null, false);
                richPresence = new DiscordRichPresence.Builder("")
                        .setBigImage("zoned_logo", "v" + Constants.GAME_VERSION)
                        .setStartTimestamps(System.currentTimeMillis())
                        .build();
            } else {
                Gdx.app.error(Constants.LOG_TAG, "Ignoring RPC init request as the previous RPC hasn't been shutdown");
            }
        }
    }

    public void updateRPC(String state) {
        if (richPresence != null) {
            richPresence.details = null;
            richPresence.smallImageKey = richPresence.smallImageText = null;
            richPresence.state = state;
            DiscordRPC.discordUpdatePresence(richPresence);
        }
    }

    public void updateRPC(String state, String mapName, int playerCount) {
        if (richPresence != null) {
            richPresence.details = state;
            richPresence.state = "with " + playerCount + " other players";
            richPresence.smallImageText = mapName;
            richPresence.smallImageKey = mapName.toLowerCase().replace(' ', '_') + "_map";
            if (richPresence.smallImageText.length() == 1) { // Weird bug with 1 letter text not triggering RPC update
                richPresence.smallImageText = " " + richPresence.smallImageText + " ";
            }
            DiscordRPC.discordUpdatePresence(richPresence);
        }
    }

    public void shutdownRPC() {
        if (richPresence != null) {
            DiscordRPC.discordShutdown();
            richPresence = null;
        }
    }
}
