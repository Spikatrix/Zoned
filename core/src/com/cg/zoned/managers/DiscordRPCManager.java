package com.cg.zoned.managers;

public class DiscordRPCManager {
    private DiscordRPCBridge discordRPCBridge;

    public DiscordRPCManager(DiscordRPCBridge discordRPCBridge) {
        this.discordRPCBridge = discordRPCBridge;

        if (this.discordRPCBridge != null) {
            this.discordRPCBridge.initRPC();
        }
    }

    public void updateRPC(String state) {
        if (discordRPCBridge != null) {
            discordRPCBridge.updateRPC(state);
        }
    }

    public void updateRPC(String state, String mapName, int playerCount) {
        if (discordRPCBridge != null) {
            discordRPCBridge.updateRPC(state, mapName, playerCount);
        }
    }

    public void shutdownRPC() {
        if (discordRPCBridge != null) {
            discordRPCBridge.shutdownRPC();
        }
    }
}
