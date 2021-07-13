package com.cg.zoned.listeners;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Bridges the client lobby connection manager and the client lobby screen
 */
public interface ClientLobbyScreenBridge {
    void disconnectWithMessage(String errorMsg);

    void startGame();

    void disconnectClient();

    void mapChanged(String mapName, int[] extraParams, int mapHash, boolean reloadExternalMaps);

    void updatePlayers(Array<String> playerNames, String[] nameStrings, boolean[] ready, boolean[] inGame, int[] colorIndices, int[] startPosIndices);

    void playerDisconnected(int playerIndex);

    FileHandle getExternalMapDir();
}
