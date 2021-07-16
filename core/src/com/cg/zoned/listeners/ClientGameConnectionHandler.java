package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;

public interface ClientGameConnectionHandler {
    void clientUpdateDirections(BufferDirections bd, int returnTripTime);

    void clientDisconnect(Connection connection);

    void clientPlayerDisconnected(String playerName, boolean disconnected);

    void clientGameEnd(boolean restartGame);
}
