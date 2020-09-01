package com.cg.zoned.managers;

public interface DiscordRPCBridge {
    void initRPC();

    void updateRPC(String state);

    void updateRPC(String state, String mapName, int playerCount);

    void shutdownRPC();
}
