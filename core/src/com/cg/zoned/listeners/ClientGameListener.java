package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferGameEnd;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientGameListener extends Listener {
    private ClientGameConnectionHandler connectionHandler;

    public ClientGameListener(ClientGameConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connection.updateReturnTripTime(); // Updates the ping
            connectionHandler.clientUpdateDirections(bd, connection.getReturnTripTime());
        } else if (object instanceof BufferGameEnd) {
            BufferGameEnd bge = (BufferGameEnd) object;
            connectionHandler.clientGameEnd(bge.restartGame);
        } else if (object instanceof BufferPlayerDisconnected) {
            BufferPlayerDisconnected bpd = (BufferPlayerDisconnected) object;
            connectionHandler.clientPlayerDisconnected(bpd.playerName, bpd.disconnected);
        }
    }

    @Override
    public void disconnected(Connection connection) {
        connectionHandler.clientDisconnect(connection);
    }
}
