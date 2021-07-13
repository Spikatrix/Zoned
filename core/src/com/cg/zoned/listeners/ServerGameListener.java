package com.cg.zoned.listeners;

import com.badlogic.gdx.utils.Array;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.managers.GameConnectionManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ServerGameListener extends Listener {
    private GameConnectionManager gameConnectionManager;

    // Used by the server to store client connections that came in when a match is already underway
    private Array<Connection> discardConnections;

    public ServerGameListener(GameConnectionManager gameConnectionManager) {
        this.gameConnectionManager = gameConnectionManager;
        discardConnections = new Array<>();
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connection.updateReturnTripTime();
            gameConnectionManager.serverUpdateDirections(bd);
        }
    }

    @Override
    public void connected(Connection connection) {
        // Ignore new connections when a match is already underway
        discardConnections.add(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        if (!discardConnections.removeValue(connection, true)) {
            gameConnectionManager.clientDisconnectedFromServer(connection);
        }
    }
}
