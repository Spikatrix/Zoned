package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferMapData;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.managers.ServerLobbyConnectionManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ServerLobbyListener extends Listener {
    private ServerLobbyConnectionManager serverLobbyConnectionManager;

    public ServerLobbyListener(ServerLobbyConnectionManager serverLobbyConnectionManager) {
        this.serverLobbyConnectionManager = serverLobbyConnectionManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferClientConnect) {
            BufferClientConnect bcc = (BufferClientConnect) object;
            serverLobbyConnectionManager.receiveClientName(connection, bcc.playerName, bcc.version);
        } else if (object instanceof BufferPlayerData) {
            BufferPlayerData bpd = (BufferPlayerData) object;
            serverLobbyConnectionManager.receiveClientData(connection, bpd.nameStrings[0], bpd.whoStrings[0], bpd.readyStrings[0], bpd.colorStrings[0], bpd.startPosStrings[0]);
        } else if (object instanceof BufferMapData) {
            BufferMapData bmd = (BufferMapData) object;
            serverLobbyConnectionManager.serveMap(connection, bmd.mapName);
        }

        super.received(connection, object);
    }

    @Override
    public void connected(Connection connection) {
        serverLobbyConnectionManager.clientConnected(connection);
        super.connected(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        serverLobbyConnectionManager.clientDisconnected(connection);
        super.disconnected(connection);
    }
}
