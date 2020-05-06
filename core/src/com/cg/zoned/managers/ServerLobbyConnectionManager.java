package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.buffers.BufferNewMap;
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
    private Array<Table> playerItems;

    private ServerPlayerListener serverPlayerListener; // This manager to screen
    private ServerLobbyListener serverLobbyListener; // Kryonet to this manager

    private String currentUserName;
    private String currentMapName;

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public ServerLobbyConnectionManager(Server server, ServerPlayerListener serverPlayerListener, String currentUserName) {
        playerConnections = new Array<>();
        playerNameResolved = new Array<>();
        playerItems = new Array<>();

        this.currentUserName = currentUserName;
        this.serverPlayerListener = serverPlayerListener;

        this.server = server;
    }

    public void start(String mapName) {
        this.currentMapName = mapName;

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
    public void receiveClientName(final Connection connection, final String playerName) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (Table playerItem : playerItems) {
                    String n = ((Label) playerItem.findActor("name-label")).getText().toString();

                    if (n.equals(playerName)) {
                        rejectConnection(connection, "Another player is using the same name\nPlease use a different name");
                        return;
                    }
                }

                int index = playerConnections.indexOf(connection, true);
                Table playerItem = playerItems.get(index);
                Label nameLabel = playerItem.findActor("name-label");
                nameLabel.setText(playerName);
                playerNameResolved.set(index, true);

                BufferNewMap bnm = new BufferNewMap(); // Send map details
                bnm.mapName = currentMapName;
                bnm.mapExtraParams = null;
                bnm.gameStart = false;
                connection.sendTCP(bnm);

                broadcastPlayerInfo(-1);
            }
        });
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
        bpd.startPosStrings = new String[size];

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
                    bpd.startPosStrings[i] = ((Label) this.playerItems.get(i).findActor("startPos-label")).getText().toString();
                } else {
                    bpd.colorStrings[i] = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
                    bpd.startPosStrings[i] = ((DropDownMenu) this.playerItems.get(i).findActor("startPos-selector")).getSelected();
                }
            } else {
                bpd.nameStrings[0] = ((Label) this.playerItems.get(index).findActor("name-label")).getText().toString();
                bpd.whoStrings[0] = ((Label) this.playerItems.get(index).findActor("who-label")).getText().toString();
                bpd.readyStrings[0] = ((Label) this.playerItems.get(index).findActor("ready-label")).getText().toString();

                if (index != 0) {
                    bpd.colorStrings[0] = ((Label) this.playerItems.get(index).findActor("color-label")).getText().toString();
                    bpd.startPosStrings[0] = ((Label) this.playerItems.get(index).findActor("startPos-label")).getText().toString();
                } else {
                    bpd.colorStrings[0] = ((DropDownMenu) this.playerItems.get(index).findActor("color-selector")).getSelected();
                    bpd.startPosStrings[0] = ((DropDownMenu) this.playerItems.get(index).findActor("startPos-selector")).getSelected();
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
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                final String clientIpAddress = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                if (serverPlayerListener != null) {
                    playerItems.add(serverPlayerListener.playerConnected(clientIpAddress));
                    playerNameResolved.add(false);
                    playerConnections.add(connection);

                } else {
                    rejectConnection(connection, "No listener configured in the host (server) for incoming clients");
                }
            }
        });
    }

    /**
     * Called when the server receives a change in a client's data.
     * This change is broadcasted to all connected clients so that they can update their UI accordingly
     *
     * @param connection Client's connection
     * @param name       Client's name
     * @param who        TODO: Forgot this lol. "(Host)" or "(You)" or "(DEL)" string I think
     * @param ready      Client's ready or not ready status
     * @param color      Client's current color
     * @param startPos   Client's start position
     */
    public void receiveClientData(final Connection connection, String name, String who, final String ready, final String color, final String startPos) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                int index = getConnectionIndex(connection);
                Table playerItem = playerItems.get(index);

                // Only needs to set the ready and color labels as others will not be changed
                Label readyLabel = playerItem.findActor("ready-label");
                if (ready.equals("Ready")) {
                    readyLabel.setColor(Color.GREEN);
                } else {
                    readyLabel.setColor(Color.RED);
                }
                readyLabel.setText(ready);

                ((Label) playerItem.findActor("color-label")).setText(color);
                ((Label) playerItem.findActor("startPos-label")).setText(startPos);

                serverPlayerListener.updatePlayerDetails(index, color, startPos);

                broadcastPlayerInfo(index);
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

                ((Label) playerItems.get(index).findActor("who-label")).setText("(DEL)"); // Notify all clients to delete this player
                broadcastPlayerInfo(index);

                Table playerItem = playerItems.get(index);

                playerItems.removeIndex(index);
                playerNameResolved.removeIndex(index);
                playerConnections.removeIndex(index);

                serverPlayerListener.playerDisconnected(playerItem, index);
            }
        });
    }

    public void mapChanged(String mapName, Array<String> startPosLocations) {
        this.currentMapName = mapName;

        for (Table playerItem : playerItems) {
            Label startPosLabel = playerItem.findActor("startPos-label");
            if (startPosLabel != null) {
                startPosLabel.setText(startPosLocations.first());
            } else {
                DropDownMenu startPosSelector = playerItem.findActor("startPos-selector");
                startPosSelector.setItems(startPosLocations);
                startPosSelector.setSelectedIndex(0);
            }
        }

        BufferNewMap bnm = new BufferNewMap();
        bnm.mapName = currentMapName;
        bnm.mapExtraParams = null;
        bnm.gameStart = false;
        server.sendToAllTCP(bnm);
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

        BufferNewMap bnm = new BufferNewMap();
        bnm.gameStart = true;
        bnm.mapName = preparedMap.getName();
        if (preparedMap.getExtraParams() != null) {
            bnm.mapExtraParams = preparedMap.getExtraParams().extraParams;
        } else {
            bnm.mapExtraParams = null;
        }

        server.sendToAllTCP(bnm);

        /*server.removeListener(serverLobbyListener); TODO: THIS ?
        serverPlayerListener = null;*/
    }

    public Player[] inflatePlayerList(MapManager mapManager) {
        int size = this.playerItems.size;

        final Player[] players = new Player[size];
        for (int i = 0; i < size; i++) {
            String name = ((Label) this.playerItems.get(i).findActor("name-label")).getText().toString();
            String position;
            String color;
            if (i == 0) {
                color = ((DropDownMenu) this.playerItems.get(i).findActor("color-selector")).getSelected();
                position = ((DropDownMenu) this.playerItems.get(i).findActor("startPos-selector")).getSelected();
            } else {
                color = ((Label) this.playerItems.get(i).findActor("color-label")).getText().toString();
                position = ((Label) this.playerItems.get(i).findActor("startPos-label")).getText().toString();
            }
            players[i] = new Player(PlayerColorHelper.getColorFromString(color), name);

            position = position.substring(0, position.lastIndexOf('(')).trim();
            int startPosIndex = mapManager.getPreparedStartPosNames().indexOf(position, false);
            players[i].setStartPos(mapManager.getPreparedStartPositions().get(startPosIndex));
        }

        return players;
    }

    public int getConnectionIndex(Connection connection) {
        return playerConnections.indexOf(connection, true);
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

    public Array<Table> getPlayerItems() { // This required? hmm
        return playerItems;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public Server getServer() {
        return server;
    }

    public interface ServerPlayerListener {
        Table playerConnected(String ipAddress);

        void playerDisconnected(Table playerItem, int itemIndex);

        void updatePlayerDetails(int index, String color, String startPos);
    }
}
