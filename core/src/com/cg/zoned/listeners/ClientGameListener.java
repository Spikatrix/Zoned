package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.managers.GameConnectionManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientGameListener extends Listener {
    private GameConnectionManager gameConnectionManager;

    public ClientGameListener(GameConnectionManager gameConnectionManager) {
        this.gameConnectionManager = gameConnectionManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connection.updateReturnTripTime();
            gameConnectionManager.clientUpdateDirections(bd, connection.getReturnTripTime());
        }

        super.received(connection, object);
    }

    @Override
    public void disconnected(Connection connection) {
        gameConnectionManager.disconnect(connection);
        super.disconnected(connection);
    }
}
