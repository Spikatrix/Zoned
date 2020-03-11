package com.cg.zoned.listeners;

import com.cg.zoned.managers.ConnectionManager;
import com.cg.zoned.buffers.BufferDirections;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ServerGameListener extends Listener {
    private ConnectionManager connectionManager;

    public ServerGameListener(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connectionManager.serverUpdateDirections(bd);
        }

        super.received(connection, object);
    }

    @Override
    public void disconnected(Connection connection) {
        connectionManager.disconnect(connection);
        super.disconnected(connection);
    }
}
