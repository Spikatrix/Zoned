package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.managers.ClientLobbyConnectionManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientLobbyListener extends Listener {
    private ClientLobbyConnectionManager clientLobbyConnectionManager;

    public ClientLobbyListener(ClientLobbyConnectionManager clientLobbyConnectionManager) {
        this.clientLobbyConnectionManager = clientLobbyConnectionManager;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferPlayerData) {
            BufferPlayerData bpd = (BufferPlayerData) object;
            clientLobbyConnectionManager.receiveServerPlayerData(bpd.nameStrings, bpd.whoStrings, bpd.readyStrings, bpd.colorStrings, bpd.startPosStrings);
        } else if (object instanceof BufferServerRejectedConnection) {
            BufferServerRejectedConnection bsrc = (BufferServerRejectedConnection) object;
            clientLobbyConnectionManager.connectionRejected(bsrc.errorMsg);
        } else if (object instanceof BufferNewMap) {
            BufferNewMap bnm = (BufferNewMap) object;
            clientLobbyConnectionManager.newMapSet(bnm.mapName, bnm.mapExtraParams);
        } else if (object instanceof BufferGameStart) {
            clientLobbyConnectionManager.startGame();
        }

        super.received(connection, object);
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        clientLobbyConnectionManager.clientDisconnected();
        super.disconnected(connection);
    }


}