package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferGameEnd;
import com.cg.zoned.buffers.BufferPlayerLeft;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientGameListener extends Listener {
    private ClientGameConnectionHandler connectionHandler;

    private final int pingCountLimit = 8;
    private final int[] previousPings;
    private int pingListFillCount;
    private int pingListIndex;

    public ClientGameListener(ClientGameConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        this.previousPings = new int[pingCountLimit];
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferDirections) {
            BufferDirections bd = (BufferDirections) object;
            connectionHandler.clientUpdateDirections(bd, updatePing(connection));
        } else if (object instanceof BufferGameEnd) {
            BufferGameEnd bge = (BufferGameEnd) object;
            connectionHandler.clientGameEnd(bge.restartGame);
        } else if (object instanceof BufferPlayerLeft) {
            BufferPlayerLeft bpd = (BufferPlayerLeft) object;
            connectionHandler.clientPlayerDisconnected(bpd.playerName, bpd.disconnected);
        }
    }

    /**
     * Updates and records pings
     *
     * @param connection The connection with which pings are updated and recorded
     * @return Average of the last {@link #pingCountLimit} pings
     */
    private int updatePing(Connection connection) {
        connection.updateReturnTripTime();
        int ping = connection.getReturnTripTime();

        previousPings[pingListIndex] = ping;
        pingListIndex = (pingListIndex + 1) % pingCountLimit;
        if (pingListFillCount < pingCountLimit) {
            pingListFillCount++;
        }

        int pingSum = 0;
        for (int i = 0; i < pingListFillCount; i++) {
            pingSum += previousPings[i];
        }

        return pingSum / pingListFillCount;
    }

    @Override
    public void disconnected(Connection connection) {
        connectionHandler.clientDisconnect(connection);
    }
}
