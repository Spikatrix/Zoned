package com.cg.zoned.managers;

import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;

public interface IConnectionHandlers {
    void serverUpdateDirections (BufferDirections bd);
    void clientUpdateDirections (BufferDirections bd);

    void disconnect (Connection connection);
}
