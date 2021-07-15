package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;

public interface ServerGameConnectionHandler {
    void serverUpdateDirections(BufferDirections bd);

    void serverClientDisconnected(Connection connection);

    void serverClientExited(Connection connection);
}
