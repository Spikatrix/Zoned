package com.cg.zoned.listeners;

import com.badlogic.gdx.files.FileHandle;
import com.cg.zoned.maps.MapEntity;

/**
 * Bridges the server lobby connection manager and the server lobby screen
 */
public interface ServerLobbyScreenBridge {
    void playerConnected(String ipAddress);

    void updatePlayerDetails(int index, String clientName, boolean nameAlreadyValidated);

    void updatePlayerDetails(int index, String name, boolean ready, boolean inGame, int colorIndex, int startPosIndex);

    void playerDisconnected(int itemIndex);

    FileHandle getExternalMapDir();

    MapEntity fetchMap(String mapName);
}
