package com.cg.zoned.managers;

public class DiscordRPCManager implements DiscordRPCBridge {
    private DiscordRPCBridge discordRPCBridge;

    public DiscordRPCManager(DiscordRPCBridge discordRPCBridge) {
        this.discordRPCBridge = discordRPCBridge;
    }

    @Override
    public void initRPC() {
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
