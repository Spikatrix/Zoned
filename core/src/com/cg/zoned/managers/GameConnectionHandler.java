package com.cg.zoned.managers;

import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;

public interface GameConnectionHandler {
    void serverUpdateDirections(BufferDirections bd);

    void clientUpdateDirections(BufferDirections bd, int returnTripTime);

    void clientDisconnectedFromServer(Connection connection);

    void clientDisconnect(Connection connection);
}
