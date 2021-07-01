package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferKickClient;
import com.cg.zoned.buffers.BufferMapData;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
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
            clientLobbyConnectionManager.receiveServerPlayerData(bpd.names, bpd.readyStatus, bpd.colorIndex, bpd.startPosIndex);
        } else if (object instanceof BufferKickClient) {
            BufferKickClient bkc = (BufferKickClient) object;
            clientLobbyConnectionManager.connectionRejected(bkc.kickReason);
        } else if (object instanceof BufferPlayerDisconnected) {
            BufferPlayerDisconnected bpd = (BufferPlayerDisconnected) object;
            clientLobbyConnectionManager.playerDisconnected(bpd.playerName);
        } else if (object instanceof BufferNewMap) {
            BufferNewMap bnm = (BufferNewMap) object;
            clientLobbyConnectionManager.newMapSet(bnm.mapName, bnm.mapExtraParams, bnm.mapHash);
        } else if (object instanceof BufferMapData) {
            BufferMapData bmd = (BufferMapData) object;
            clientLobbyConnectionManager.downloadMap(bmd.mapName, bmd.mapData, bmd.mapHash, bmd.mapPreviewData);
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