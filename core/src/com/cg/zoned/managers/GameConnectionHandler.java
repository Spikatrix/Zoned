package com.cg.zoned.managers;

import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;

public interface GameConnectionHandler {
    void serverUpdateDirections(BufferDirections bd, int returnTripTime);

    void clientUpdateDirections(BufferDirections bd, int returnTripTime);

    void serverDisconnect(Connection connection);

    void clientDisconnect(Connection connection);
}
