package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cg.zoned.Constants;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferMapData;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.dataobjects.PlayerItemAttributes;
import com.cg.zoned.listeners.ClientLobbyListener;
import com.esotericsoftware.kryonet.Client;

public class ClientLobbyConnectionManager {
    private Client client;

    /**
     * Clients use this to store and track names of all players in the lobby
     */
    private Array<String> playerNames;

    private ClientPlayerListener clientPlayerListener; // This manager to screen
    private ClientLobbyListener clientLobbyListener; // Kryonet to this manager

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public ClientLobbyConnectionManager(Client client, ClientPlayerListener clientPlayerListener) {
        playerNames = new Array<>();

        this.clientPlayerListener = clientPlayerListener;
        this.client = client;
    }

    public void start(String clientName) {
        playerNames.add(clientName);

        clientLobbyListener = new ClientLobbyListener(this);
        client.addListener(clientLobbyListener);
    }

    /**
     * Called when the client receives broadcast information from the server about players
     *
     * @param nameStrings     Names of players
     * @param ready           Ready or not status of players
     * @param colorIndices    Player color indices
     * @param startPosIndices Start positions indices of all players
     */
    public void receiveServerPlayerData(String[] nameStrings, final boolean[] ready, int[] colorIndices, int[] startPosIndices) {
        Gdx.app.postRunnable(() -> clientPlayerListener.updatePlayers(playerNames, nameStrings, ready, colorIndices, startPosIndices));

    }

    /**
     * Called when the server rejects the client's connection (Client was kicked)
     *
     * @param errorMsg A String holding the reason why the server rejected the client's connection
     */
    public void connectionRejected(final String errorMsg) {
        Gdx.app.postRunnable(() -> clientPlayerListener.disconnectWithMessage(errorMsg));
    }

    /**
     * Called when the client receives information from the server that the map has changed
     *
     * @param mapName        The name of the new map
     * @param mapExtraParams The extra params of the new map, if any
     * @param mapHash        The hash of the contents of the map in the server
     */
    public void newMapSet(final String mapName, final int[] mapExtraParams, final int mapHash) {
        Gdx.app.postRunnable(() -> clientPlayerListener.mapChanged(mapName, mapExtraParams, mapHash, false));
    }

    /**
     * Called by the client to request new external map data as it doesn't exist in the client
     *
     * @param mapName The name of the map to request from the server
     */
    public void requestMap(String mapName) {
        BufferMapData bmd = new BufferMapData();
        bmd.mapName = mapName;
        bmd.mapData = null;
        bmd.mapPreviewData = null;

        client.sendTCP(bmd);
    }

    public void downloadMap(final String mapName, final String mapData, final int mapHash, final byte[] mapPreviewData) {
        Gdx.app.postRunnable(() -> {
            FileHandle externalMapDir = clientPlayerListener.getExternalMapDir();

            try {
                FileHandle mapFile = externalMapDir.child(mapName + ".map");
                mapFile.writeString(mapData, false);
            } catch (GdxRuntimeException e) {
                clientPlayerListener.disconnectWithMessage("Failed to download the map '" + "' (" + e.getMessage() + ")");
                return;
            }

            String mapDownloadLogMsg = "Downloaded map '" + mapName + "'";
            if (mapPreviewData != null) {
                try {
                    FileHandle mapPreviewDataFile = externalMapDir.child(mapName + ".png");
                    mapPreviewDataFile.writeBytes(mapPreviewData, false);
                } catch (GdxRuntimeException e) {
                    mapDownloadLogMsg += ". Failed to download preview";
                }
            } else {
                mapDownloadLogMsg += ". Map preview unavailable";
            }
            Gdx.app.log(Constants.LOG_TAG, mapDownloadLogMsg);

            clientPlayerListener.mapChanged(mapName, null, mapHash, true);
        });
    }

    public void broadcastClientInfo(PlayerItemAttributes playerAttribute) {
        BufferPlayerData bpd = new BufferPlayerData();
        bpd.names = new String[]{ playerAttribute.getName() };
        bpd.readyStatus = new boolean[]{ playerAttribute.isReady() };
        bpd.colorIndex = new int[]{ playerAttribute.getColorIndex() };
        bpd.startPosIndex = new int[]{ playerAttribute.getStartPosIndex() };

        client.sendTCP(bpd);
    }

    public void startGame() {
        clientPlayerListener.startGame();

        emptyBuffers();
    }

    private void emptyBuffers() {
        playerNames.clear();

        try {
            client.removeListener(clientLobbyListener);
        } catch (IllegalArgumentException ignored) {
            // Probably clicked the back button more than once; ignore exception
        }
        clientLobbyListener = null;
        clientPlayerListener = null;
    }

    public void closeConnection() {
        emptyBuffers();

        if (client.isConnected()) {
            client.close();
        }
    }

    public void clientDisconnected() {
        clientPlayerListener.disconnectWithMessage("Lost connection to the server");
    }

    public void sendClientNameToServer(String clientName) {
        BufferClientConnect bcc = new BufferClientConnect();
        bcc.playerName = clientName;
        bcc.version = Constants.GAME_VERSION;
        client.sendTCP(bcc);
    }

    /**
     * Called when the client receives information from the server that some
     * other client got disconnected
     *
     * @param playerName The name of the client that got disconnected
     */
    public void playerDisconnected(String playerName) {
        // playerIndex should never be -1
        int playerIndex = playerNames.indexOf(playerName, false);

        playerNames.removeIndex(playerIndex);
        clientPlayerListener.playerDisconnected(playerIndex);
    }

    public Client getClient() {
        return client;
    }

    public interface ClientPlayerListener {
        void disconnectWithMessage(String errorMsg);

        void startGame();

        void disconnectClient();

        void mapChanged(String mapName, int[] extraParams, int mapHash, boolean reloadExternalMaps);

        void updatePlayers(Array<String> playerNames, String[] nameStrings, boolean[] readyStrings, int[] colorStrings, int[] startPosStrings);

        void playerDisconnected(int playerIndex);

        FileHandle getExternalMapDir();
    }
}
