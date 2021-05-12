package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.managers.GameConnectionManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ServerGameListener extends Listener {
    private GameConnectionManager gameConnectionManager;

    public ServerGameListener(GameConnectionManager gameConnectionManager) {
        this.gameConnectionManager = gameConnectionManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connection.updateReturnTripTime();
            gameConnectionManager.serverUpdateDirections(bd);
        } else if (object instanceof BufferClientConnect) {
            gameConnectionManager.rejectNewConnection(connection);
        }

        super.received(connection, object);
    }

    @Override
    public void disconnected(Connection connection) {
        gameConnectionManager.serverDisconnect(connection);
        super.disconnected(connection);
    }
}
