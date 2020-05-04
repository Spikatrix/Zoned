package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.listeners.ServerLobbyListener;
import com.cg.zoned.maps.MapEntity;
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
     * If true, that player's name is already resolved; safe to send that player's info to clients
     */
    private Array<Boolean> playerNameResolved;

    // Why not use playerNames for servers as well since clients already use them?
    // Cause connections and  disconnections can't be handled using names as we get only the Connection object

    /**
     * Each row of the playerList which contains information about each player
     */
    private Array<HorizontalGroup> playerItems;

    private ServerPlayerListener serverPlayerListener; // This manager to screen
    private ServerLobbyListener serverLobbyListener; // Kryonet to this manager

    private String currentUserName;

    public ServerLobbyConnectionManager(Server server, ServerPlayerListener serverPlayerListener, String currentUserName) {
        playerConnections = new Array<>();
        playerNameResolved = new Array<>();
        playerItems = new Array<>();

        this.currentUserName = currentUserName;
        this.serverPlayerListener = serverPlayerListener;

        this.server = server;
    }

    public void start() {
        playerItems.add(serverPlayerListener.playerConnected(null));
        playerConnections.add(null);
        playerNameResolved.add(true);

        serverLobbyListener = new ServerLobbyListener(this);
        server.addListener(serverLobbyListener);
    }


    /**
     * Called when the server receives client name information.
     * <p>
     * If the name is unique, it is added into {@link ServerLobbyConnectionManager#playerItems} and
     * is displayed in the lobby screen. The server then broadcasts information about all connected
     * clients to all of them.
     *
     * @param connection The client connection from where the packet was received
     * @param playerName The name of the client player
     */
    public void receiveClientName(Connection connection, String playerName) {
        for (HorizontalGroup hg : playerItems) {
            String n = ((Label) hg.findActor("name-label")).getText().toString();

            if (n.equals(playerName)) {
                rejectConnection(connection, "Another player is using the same name\nPlease use a different name");
                return;
            }
        }

        int index = playerConnections.indexOf(connection, true);
        HorizontalGroup hg = playerItems.get(index);
        Label nameLabel = hg.findActor("name-label");
        nameLabel.setText(playerName);
        playerNameResolved.set(index, true);

        broadcastPlayerInfo(-1);
    }

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
    public void broadcastPlayerInfo(int index) {
        int size;
        if (index == -1) { // Broadcast all info
            int resolvedPlayerNameCount = 0;
            for (boolean playerNameValid : playerNameResolved) { // Will this non-reentrant iterator be unsafe?
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

        for (int i = 0; i < size; i++) {
            if (!playerNameResolved.get(i)) {
                continue;
            }

            if (index == -1) {
                bpd.nameStrings[i] = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
                bpd.whoStrings[i] = ((Label) this.playerItems.get(i).findActor("who-label")).getText().toString();
                bpd.readyStrings[i] = ((Label) this.playerItems.get(i).findActor("ready-label")).getText().toString();

                if (i != 0) {
                    bpd.colorStrings[i] = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
                } else {
                    bpd.colorStrings[i] = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
                }
            } else {
                bpd.nameStrings[0] = ((Label) this.playerItems.get(index).findActor("name-label")).getText().toString();
                bpd.whoStrings[0] = ((Label) this.playerItems.get(index).findActor("who-label")).getText().toString();
                bpd.readyStrings[0] = ((Label) this.playerItems.get(index).findActor("ready-label")).getText().toString();

                if (index != 0) {
                    bpd.colorStrings[0] = ((Label) this.playerItems.get(index).findActor("color-label")).getText().toString();
                } else {
                    bpd.colorStrings[0] = ((DropDownMenu) this.playerItems.get(index).findActor("color-selector")).getSelected();
                }
            }
        }

        server.sendToAllTCP(bpd);
    }

    /**
     * Called when the server detects a new client connection
     *
     * @param connection The newly connected client connection
     */
    public void clientConnected(final Connection connection) {
        final String clientIpAddress = connection.getRemoteAddressTCP().getAddress().getHostAddress();
        if (serverPlayerListener != null) {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    playerItems.add(serverPlayerListener.playerConnected(clientIpAddress));
                    playerNameResolved.add(false);
                    playerConnections.add(connection);
                }
            });
        } else {
            rejectConnection(connection, "No listener configured in the host (server) for incoming clients");
        }
    }

    /**
     * Called when the server receives a change in a client's data.
     * This change is broadcasted to all connected clients so that they can update their UI accordingly
     *
     * @param connection The client connection
     * @param name       The name of the client
     * @param who        TODO: Forgot this lol. "(Host)" or "(You)" or "(DEL)" string I think
     * @param ready      Client's ready or not ready status
     * @param color      Client's current color
     */
    public void receiveClientData(Connection connection, String name, String who, String ready, String color) {
        int index = playerConnections.indexOf(connection, true);
        HorizontalGroup playerItem = playerItems.get(index);

        // Only needs to set the ready and color labels as others will not be changed
        Label readyLabel = playerItem.findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) playerItem.findActor("color-label")).setText(color);

        broadcastPlayerInfo(index);
    }

    /**
     * Called in the server when a client disconnects
     *
     * @param connection The connection of the client that disconnected
     */
    public void clientDisconnected(Connection connection) {
        int index = playerConnections.indexOf(connection, true);

        ((Label) playerItems.get(index).findActor("who-label")).setText("(DEL)"); // Notify all clients to delete this player
        broadcastPlayerInfo(index);

        HorizontalGroup playerItem = playerItems.get(index);

        playerItems.removeIndex(index);
        playerNameResolved.removeIndex(index);
        playerConnections.removeIndex(index);

        serverPlayerListener.playerDisconnected(playerItem);
    }

    public String validateServerData() {
        if (server.getConnections().length < 1) {
            return "Insufficient players to start the match";
        }

        for (int i = 1; i < playerItems.size; i++) {
            String ready = ((Label) playerItems.get(i).findActor("ready-label")).getText().toString();
            if (!ready.equals("Ready")) {
                return "All players are not ready";
            }
        }

        // No errors
        return null;
    }

    public void broadcastGameStart(MapManager mapManager) {
        MapEntity preparedMap = mapManager.getPreparedMap();

        BufferGameStart bgs = new BufferGameStart();
        bgs.mapName = preparedMap.getName();
        if (preparedMap.getExtraParams() != null) {
            bgs.mapExtraParams = preparedMap.getExtraParams().extraParams;
        } else {
            bgs.mapExtraParams = null;
        }

        server.sendToAllTCP(bgs);
    }

    public Player[] inflatePlayerList() {
        int size = this.playerItems.size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            String name = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
            String color;
            if (i == 0) {
                color = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
            } else {
                color = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
            }
            players[i] = new Player(PlayerColorHelper.getColorFromString(color), name);
        }

        return players;
    }

    public void closeConnection() {
        playerItems.clear();
        playerConnections.clear();

        server.close();
        server.removeListener(serverLobbyListener);
        serverLobbyListener = null;
    }

    public void setServerPlayerListener(ServerPlayerListener serverPlayerListener) {
        // Should not be null. Will cause NPE if null.
        this.serverPlayerListener = serverPlayerListener;
    }

    public Array<HorizontalGroup> getPlayerItems() { // This required? hmm
        return playerItems;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public Server getServer() {
        return server;
    }

    public interface ServerPlayerListener {
        HorizontalGroup playerConnected(String ipAddress);

        void playerDisconnected(HorizontalGroup playerItem);
    }
}
