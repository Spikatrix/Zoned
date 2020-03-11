package com.cg.zoned.listeners;

import com.cg.zoned.screens.ServerLobbyScreen;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferPlayerData;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ServerLobbyListener extends Listener {
    private ServerLobbyScreen serverLobby;

    public ServerLobbyListener(ServerLobbyScreen serverLobby) {
        this.serverLobby = serverLobby;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferClientConnect) {
            BufferClientConnect bcc = (BufferClientConnect) object;
            serverLobby.receiveClientName(connection, bcc.playerName);
        } else if (object instanceof BufferPlayerData) {
            BufferPlayerData bpd = (BufferPlayerData) object;
            serverLobby.receiveClientData(connection, bpd.nameStrings[0], bpd.whoStrings[0], bpd.readyStrings[0], bpd.colorStrings[0]);
        }

        super.received(connection, object);
    }

    @Override
    public void connected(Connection connection) {
        serverLobby.connect(connection);
        super.connected(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        serverLobby.disconnect(connection);
        super.disconnected(connection);
    }
}
