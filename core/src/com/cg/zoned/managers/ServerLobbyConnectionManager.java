package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferGameStart;
import com.cg.zoned.buffers.BufferKickClient;
import com.cg.zoned.buffers.BufferMapData;
import com.cg.zoned.buffers.BufferNewMap;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
import com.cg.zoned.dataobjects.PlayerItemAttributes;
import com.cg.zoned.dataobjects.PreparedMapData;
import com.cg.zoned.listeners.ServerLobbyScreenBridge;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.MapExtraParams;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class ServerLobbyConnectionManager extends Listener {
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

    /**
     * Bridges this manager with the client lobby screen
     */
    private ServerLobbyScreenBridge serverLobbyScreenBridge;

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public ServerLobbyConnectionManager(Server server, ServerLobbyScreenBridge serverLobbyScreenBridge) {
        playerConnections = new Array<>();
        playerNameResolved = new Array<>();

        this.serverLobbyScreenBridge = serverLobbyScreenBridge;
        this.server = server;
    }

    public void start() {
        playerConnections.add(null);
        playerNameResolved.add(true);

        server.addListener(this); // Kryonet packets will arrive directly in this class
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof BufferClientConnect) {
            BufferClientConnect bcc = (BufferClientConnect) object;
            this.receiveClientName(connection, bcc.playerName, bcc.version);
        } else if (object instanceof BufferPlayerData) {
            BufferPlayerData bpd = (BufferPlayerData) object;
            this.receiveClientData(connection, bpd.names[0], bpd.readyStatus[0], bpd.colorIndex[0], bpd.startPosIndex[0]);
        } else if (object instanceof BufferMapData) {
            BufferMapData bmd = (BufferMapData) object;
            this.serveMap(connection, bmd.mapName);
        }
    }

    /**
     * Called when the server detects a new client connection
     *
     * @param connection The newly connected client connection
     */
    @Override
    public void connected(Connection connection) {
        Gdx.app.postRunnable(() -> {
            final String clientIpAddress = connection.getRemoteAddressTCP().getAddress().getHostAddress();
            if (serverLobbyScreenBridge != null) {
                addNewPlayer(connection, clientIpAddress);
            } else {
                rejectConnection(connection, "No listener configured in the host (server) for incoming clients");
            }
        });
    }

    private void addNewPlayer(Connection connection, String clientIpAddress) {
        serverLobbyScreenBridge.playerConnected(clientIpAddress);
        playerNameResolved.add(false);
        playerConnections.add(connection);
    }

    /**
     * Called when the server receives client name information.
     * <p>
     * If the name is unique, the client connection is accepted and is displayed in the lobby
     * screen. The server then broadcasts information about all connected clients to all of them.
     *
     * @param connection        The client connection from where the packet was received
     * @param clientName        The name of the client player
     * @param clientGameVersion The game version of the client
     */
    public void receiveClientName(final Connection connection, final String clientName, final String clientGameVersion) {
        Gdx.app.postRunnable(() -> {
            if (!Constants.GAME_VERSION.equals(clientGameVersion)) {
                rejectConnection(connection, "Server client version mismatch!\n" +
                        "Server game version: " + Constants.GAME_VERSION + "\nYour game version: " + clientGameVersion);
                return;
            }

            int index = getConnectionIndex(connection);
            if (index == -1) { // Client joined way too fast
                addNewPlayer(connection, connection.getRemoteAddressTCP().getAddress().getHostAddress());
                index = getConnectionIndex(connection);
            }
            serverLobbyScreenBridge.updatePlayerDetails(index, clientName);
        });
    }

    /**
     * Kicks the client out of the lobby with the specified reason
     *
     * @param index  The index of the client to send a reject message
     * @param reason The reason message on why the client was kicked
     */
    public void kickPlayer(int index, String reason) {
        rejectConnection(playerConnections.get(index), reason);
    }

    /**
     * Rejects the client connection
     *
     * @param connection       The client connection to reject
     * @param rejectionMessage The rejection message on why the client was rejected
     */
    private void rejectConnection(Connection connection, String rejectionMessage) {
        BufferKickClient bkc = new BufferKickClient();
        bkc.kickReason = rejectionMessage;
        connection.sendTCP(bkc);
    }

    /**
     * Server uses this to broadcast player information to its connected clients
     *
     * @param index Index of the player to broadcast
     *              -1 to broadcast information about all clients
     */
    public void broadcastPlayerInfo(Array<PlayerItemAttributes> playerItemsAttributes, int index) {
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
        bpd.names = new String[size];
        bpd.readyStatus = new boolean[size];
        bpd.colorIndex = new int[size];
        bpd.startPosIndex = new int[size];

        for (int i = 0; i < size; i++) {
            if (!playerNameResolved.get(i)) {
                continue;
            }

            int attrIndex = index;
            if (attrIndex == -1) {
                attrIndex = i;
            }

            bpd.names[i] = playerItemsAttributes.get(attrIndex).getName();
            bpd.readyStatus[i] = playerItemsAttributes.get(attrIndex).isReady();
            bpd.colorIndex[i] = playerItemsAttributes.get(attrIndex).getColorIndex();
            bpd.startPosIndex[i] = playerItemsAttributes.get(attrIndex).getStartPosIndex();
        }

        sentToAcceptedClients(bpd);
    }

    /**
     * Called when the server receives a change in a client's data.
     * This change is broadcasted to all connected clients so that they can update their UI accordingly
     *
     * @param connection    Client's connection
     * @param name          Client's name
     * @param ready         Client's ready or not ready status
     * @param colorIndex    Client's color index
     * @param startPosIndex Client's start position index
     */
    public void receiveClientData(final Connection connection, String name, boolean ready, int colorIndex, int startPosIndex) {
        Gdx.app.postRunnable(() -> {
            int index = getConnectionIndex(connection);
            if (!playerNameResolved.get(index)) {
                // Happens in edge cases like when the map is changed right when a player joins
                return;
            }

            serverLobbyScreenBridge.updatePlayerDetails(index, name, ready, colorIndex, startPosIndex);
        });
    }

    /**
     * Called when a player is accepted by the server
     *
     * @param playerIndex The index of the player accepted
     */
    public void acceptPlayer(int playerIndex) {
        playerNameResolved.set(playerIndex, true);
    }

    /**
     * Broadcasts new map details to clients
     *
     * @param playerIndex Client index to broadcast new map info. -1 if broadcast to all clients
     * @param mapManager  MapManager object used to fetch map related info to send
     */
    public void sendMapDetails(int playerIndex, MapManager mapManager) {
        PreparedMapData preparedMapData = mapManager.getPreparedMapData();
        MapExtraParams extraParams = preparedMapData.map.getExtraParams();

        // Send map details to the client
        BufferNewMap bnm = new BufferNewMap();
        bnm.mapName = preparedMapData.map.getName();
        bnm.mapHash = preparedMapData.map.getMapData().hashCode();
        bnm.mapExtraParams = extraParams != null ? extraParams.extraParams : null;

        if (playerIndex > -1 && playerNameResolved.get(playerIndex)) {
            Connection connection = playerConnections.get(playerIndex);
            connection.sendTCP(bnm);
        } else {
            sentToAcceptedClients(bnm);
        }
    }

    /**
     * Called when a client requests external map data which it did not have
     * Server serves the map file data and its preview to the client
     *
     * @param connection The connection of the client that requested the map data
     * @param mapName    The name of the map requested by the client
     */
    public void serveMap(final Connection connection, final String mapName) {
        Gdx.app.postRunnable(() -> {
            FileHandle externalMapDir = serverLobbyScreenBridge.getExternalMapDir();
            FileHandle mapFile = Gdx.files.external(externalMapDir + "/" + mapName + ".map");
            FileHandle mapPreviewFile = Gdx.files.external(externalMapDir + "/" + mapName + ".png");

            MapEntity map = serverLobbyScreenBridge.fetchMap(mapName);

            BufferMapData bmd = new BufferMapData();
            bmd.mapName = mapName;
            bmd.mapData = mapFile.readString();
            bmd.mapHash = map.getMapData().hashCode();
            if (mapPreviewFile.exists()) {
                bmd.mapPreviewData = mapPreviewFile.readBytes();
            } else {
                bmd.mapPreviewData = null;
            }

            try {
                connection.sendTCP(bmd);
            } catch (KryoException e) {
                // Probably a buffer overflow due to the map preview image being too big
                Gdx.app.log(Constants.LOG_TAG, "Failed to serve map data (Is the map preview too large?): " + e.getMessage());
                Gdx.app.log(Constants.LOG_TAG, "Retrying to server without the map preview image...");

                // Resend map data without the preview
                bmd.mapPreviewData = null;
                connection.sendTCP(bmd);
            }
        });
    }

    /**
     * Sends the specified packets to only the accepted clients rather than all of them
     *
     * @param packet The object to send
     */
    private void sentToAcceptedClients(Object packet) {
        if (playerNameResolved.size == 0) {
            return;
        }

        Connection[] connections = server.getConnections();
        for (int i = 0; i < connections.length; i++) {
            if (playerNameResolved.get(i)) {
                connections[i].sendTCP(packet);
            }
        }
    }

    /**
     * Called to validate all data before starting the match
     *
     * @param playerAttributes Player attribute data array
     * @return null if no errors, a string showing the error otherwise
     */
    public String validateServerData(Array<PlayerItemAttributes> playerAttributes) {
        for (int i = 1; i < playerAttributes.size; i++) {
            PlayerItemAttributes playerAttribute = playerAttributes.get(i);
            if (!playerAttribute.isReady()) {
                return "All players are not ready";
            }
        }

        // No errors
        return null;
    }

    public void broadcastGameStart() {
        BufferGameStart bgs = new BufferGameStart();
        sentToAcceptedClients(bgs);

        emptyBuffers();
    }

    private int getConnectionIndex(Connection connection) {
        return playerConnections.indexOf(connection, true);
    }

    public void broadcastPlayerDisconnected(int playerIndex, String playerName) {
        // If not resolved, other clients don't have information about this client
        if (playerNameResolved.get(playerIndex)) {
            BufferPlayerDisconnected bpf = new BufferPlayerDisconnected();
            bpf.playerName = playerName;
            sentToAcceptedClients(bpf);
        }
    }

    /**
     * Called in the server when a client disconnects
     *
     * @param connection The connection of the client that disconnected
     */
    @Override
    public void disconnected(Connection connection) {
        Gdx.app.postRunnable(() -> {
            int index = getConnectionIndex(connection);

            if (index == -1) {
                return; // Some error occurred or server is shutting down
            }

            serverLobbyScreenBridge.playerDisconnected(index);

            playerNameResolved.removeIndex(index);
            playerConnections.removeIndex(index);

        });
    }

    private void emptyBuffers() {
        playerConnections.clear();
        serverLobbyScreenBridge = null;
        try {
            server.removeListener(this);
        } catch (IllegalArgumentException ignored) {
            // Probably clicked the back button more than once; ignore exception
        }
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

        void updatePlayerDetails(int index, String name, boolean ready, int colorIndex, int startPosIndex);

        void playerDisconnected(int itemIndex);

        FileHandle getExternalMapDir();

        MapEntity fetchMap(String mapName);
    }
}
