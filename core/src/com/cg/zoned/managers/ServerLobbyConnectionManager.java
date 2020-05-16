package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.listeners.ServerLobbyListener;
import com.cg.zoned.maps.MapExtraParams;
import com.cg.zoned.ui.DropDownMenu;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class ServerLobbyConnectionManager {
    private Server server;

    /**
     * The server will store connections and keep track to all clients in here
     */
    private Array<Connection> playerConnections;

    /**
     * If true, that client player's name is already resolved; safe to send that player's info to clients
     */
    private Array<Boolean> playerNameResolved;

    // Why not use playerNames for servers as well since clients already use them?
    // Cause connections and disconnections can't be handled using names as we get only the Connection object

    private ServerPlayerListener serverPlayerListener; // This manager to screen
    private ServerLobbyListener serverLobbyListener; // Kryonet to this manager

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public ServerLobbyConnectionManager(Server server, ServerPlayerListener serverPlayerListener) {
        playerConnections = new Array<>();
        playerNameResolved = new Array<>();

        this.serverPlayerListener = serverPlayerListener;
        this.server = server;
    }

    public void start() {
        playerConnections.add(null);
        playerNameResolved.add(true);

        serverLobbyListener = new ServerLobbyListener(this);
        server.addListener(serverLobbyListener);
    }

    /**
     * Called when the server detects a new client connection
     *
     * @param connection The newly connected client connection
     */
    public void clientConnected(final Connection connection) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                final String clientIpAddress = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                if (serverPlayerListener != null) {
                    addNewPlayer(connection, clientIpAddress);
                } else {
                    rejectConnection(connection, "No listener configured in the host (server) for incoming clients");
                }
            }
        });
    }

    private void addNewPlayer(Connection connection, String clientIpAddress) {
        serverPlayerListener.playerConnected(clientIpAddress);
        playerNameResolved.add(false);
        playerConnections.add(connection);
    }

    /**
     * Called when the server receives client name information.
     * <p>
     * If the name is unique, the client connection is accepted and is displayed in the lobby
     * screen. The server then broadcasts information about all connected clients to all of them.
     *
     * @param connection The client connection from where the packet was received
     * @param clientName The name of the client player
     */
    public void receiveClientName(final Connection connection, final String clientName) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                int index = getConnectionIndex(connection);
                if (index == -1) { // Client joined way too fast
                    addNewPlayer(connection, connection.getRemoteAddressTCP().getAddress().getHostAddress());
                    index = getConnectionIndex(connection);
                }
                serverPlayerListener.updatePlayerDetails(index, clientName);
            }
        });
    }

    /**
     * Rejects the index'th client connection
     *
     * @param index            The index of the client to send a reject message
     * @param rejectionMessage The rejection message on why the client was rejected
     */
    public void rejectConnection(int index, String rejectionMessage) {
        rejectConnection(playerConnections.get(index), rejectionMessage);
    }

    /**
     * Rejects the client connection
     *
     * @param connection       The client connection to reject
     * @param rejectionMessage The rejection message on why the client was rejected
     */
    private void rejectConnection(Connection connection, String rejectionMessage) {
        BufferServerRejectedConnection bsrc = new BufferServerRejectedConnection();
        bsrc.errorMsg = rejectionMessage;
        connection.sendTCP(bsrc);
    }

    /**
     * Server uses this to broadcast player information to its connected clients
     *
     * @param index Index of the player to broadcast
     *              -1 to broadcast information about all clients
     */
    public void broadcastPlayerInfo(SnapshotArray<Actor> playerItems, int index) {
        int size;
        if (index == -1) { // Broadcast all info
            int resolvedPlayerNameCount = 0;
            for (boolean playerNameValid : playerNameResolved) {
                if (playerNameValid) {
                    resolvedPlayerNameCount++;
                }
            }
            size = resolvedPlayerNameCount;
        } else {
            size = 1;
        }

        BufferPlayerData bpd = new BufferPlayerData();
        bpd.nameStrings = new String[size];
        bpd.whoStrings = new String[size];
        bpd.readyStrings = new String[size];
        bpd.colorStrings = new String[size];
        bpd.startPosStrings = new String[size];

        for (int i = 0; i < size; i++) {
            if (!playerNameResolved.get(i)) {
                continue;
            }

            Table playerItem;
            if (index == -1) {
                playerItem = (Table) playerItems.get(i);
            } else {
                playerItem = (Table) playerItems.get(index);
            }

            bpd.nameStrings[i] = ((Label) playerItem.findActor("name-label")).getText().toString();
            bpd.whoStrings[i] = ((Label) playerItem.findActor("who-label")).getText().toString();
            bpd.readyStrings[i] = ((Label) playerItem.findActor("ready-label")).getText().toString();

            if (index == 0 || (i == 0 && index == -1)) {
                bpd.colorStrings[i] = ((DropDownMenu) playerItem.findActor("color-selector")).getSelected();
                bpd.startPosStrings[i] = ((DropDownMenu) playerItem.findActor("startPos-selector")).getSelected();
            } else {
                bpd.colorStrings[i] = ((Label) playerItem.findActor("color-label")).getText().toString();
                bpd.startPosStrings[i] = ((Label) playerItem.findActor("startPos-label")).getText().toString();
            }
        }

        Connection[] connections = server.getConnections();
        for (int i = 0; i < connections.length; i++) {
            if (playerNameResolved.get(i)) {
                connections[i].sendTCP(bpd);
            }
        }
    }

    /**
     * Called when the server receives a change in a client's data.
     * This change is broadcasted to all connected clients so that they can update their UI accordingly
     *
     * @param connection Client's connection
     * @param name       Client's name
     * @param who        Client's who string. Not really used here tho so nvm.
     * @param ready      Client's ready or not ready status
     * @param color      Client's current color
     * @param startPos   Client's start position
     */
    public void receiveClientData(final Connection connection, final String name, final String who, final String ready, final String color, final String startPos) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                int index = getConnectionIndex(connection);
                serverPlayerListener.updatePlayerDetails(index, name, who, ready, color, startPos);
            }
        });
    }

    /**
     * Called in the server when a client disconnects
     *
     * @param connection The connection of the client that disconnected
     */
    public void clientDisconnected(final Connection connection) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                int index = getConnectionIndex(connection);

                if (index == -1) {
                    return; // Some error occurred or server is shutting down
                }

                serverPlayerListener.playerDisconnected(index);

                playerNameResolved.removeIndex(index);
                playerConnections.removeIndex(index);

            }
        });
    }

    public void acceptPlayer(int playerIndex, MapManager mapManager) {
        playerNameResolved.set(playerIndex, true);
        sendMapDetails(playerIndex, mapManager);
    }

    /**
     * Broadcasts new map details to clients
     *
     * @param playerIndex Client index to broadcast new map info. -1 if broadcast to all clients
     * @param mapManager  MapManager object used to fetch map related info to send
     */
    public void sendMapDetails(int playerIndex, MapManager mapManager) {
        MapExtraParams extraParams = mapManager.getPreparedMap().getExtraParams();

        // Send map details to the client
        BufferNewMap bnm = new BufferNewMap();
        bnm.mapName = mapManager.getPreparedMap().getName();
        bnm.mapExtraParams = extraParams != null ? extraParams.extraParams : null;

        if (playerIndex > -1) {
            Connection connection = playerConnections.get(playerIndex);
            connection.sendTCP(bnm);
        } else {
            server.sendToAllTCP(bnm);
        }
    }

    public String validateServerData(SnapshotArray<Actor> playerItems) {
        if (server.getConnections().length < 1) {
            return "Insufficient players to start the match";
        }

        for (int i = 1; i < playerItems.size; i++) {
            Table playerItem = (Table) playerItems.get(i);

            String ready = ((Label) playerItem.findActor("ready-label")).getText().toString();
            if (!ready.equals("Ready")) {
                return "All players are not ready";
            }
        }

        // No errors
        return null;
    }

    public void broadcastGameStart() {
        BufferGameStart bgs = new BufferGameStart();
        server.sendToAllTCP(bgs);

        emptyBuffers();
    }

    private int getConnectionIndex(Connection connection) {
        return playerConnections.indexOf(connection, true);
    }

    private void emptyBuffers() {
        playerConnections.clear();

        server.removeListener(serverLobbyListener);
        serverLobbyListener = null;
        serverPlayerListener = null;
    }

    public void closeConnection() {
        emptyBuffers();

        server.close();
    }

    public Server getServer() {
        return server;
    }

    public interface ServerPlayerListener {
        void playerConnected(String ipAddress);

        void updatePlayerDetails(int index, String clientName);

        void updatePlayerDetails(int index, String name, String who, String ready, String color, String startPos);

        void playerDisconnected(int itemIndex);
    }
}
