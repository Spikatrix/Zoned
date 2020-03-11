package com.cg.zoned.listeners;

import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.buffers.BufferPlayerData;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import com.cg.zoned.screens.ClientLobbyScreen;

public class ClientLobbyListener extends Listener {

    private ClientLobbyScreen clientLobby;

    public ClientLobbyListener(ClientLobbyScreen cl) {
        this.clientLobby = cl;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferPlayerData) {
            BufferPlayerData bpd = (BufferPlayerData) object;
            clientLobby.receivePlayerData(bpd.nameStrings, bpd.whoStrings, bpd.readyStrings, bpd.colorStrings);
        } else if (object instanceof BufferServerRejectedConnection) {
            BufferServerRejectedConnection bsrc = (BufferServerRejectedConnection) object;
            clientLobby.displayError(bsrc.errorMsg);
        } else if (object instanceof BufferGameStart) {
            BufferGameStart bgs = (BufferGameStart) object;
            clientLobby.startGame(bgs.playerNames, bgs.startIndices, bgs.rows, bgs.cols);
        }

        super.received(connection, object);
    }

    @Override
    public void connected(Connection connection) {
        super.connected(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        clientLobby.disconnect();
        super.disconnected(connection);
    }


}