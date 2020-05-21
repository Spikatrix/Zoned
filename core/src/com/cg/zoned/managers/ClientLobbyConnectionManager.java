package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.listeners.ClientLobbyListener;
import com.cg.zoned.ui.DropDownMenu;
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
     * @param whoStrings      Who strings of players
     * @param readyStrings    Ready or Not Ready strings of players
     * @param colorStrings    Colors of players
     * @param startPosStrings Start positions of all players
     */
    public void receiveServerPlayerData(final String[] nameStrings, final String[] whoStrings, final String[] readyStrings, final String[] colorStrings, final String[] startPosStrings) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                clientPlayerListener.updatePlayers(playerNames, nameStrings, whoStrings, readyStrings, colorStrings, startPosStrings);
            }
        });

    }

    /**
     * Called when the server rejects the client's connection
     *
     * @param errorMsg A String holding the reason why the server rejected the client's connection
     */
    public void connectionRejected(final String errorMsg) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                clientPlayerListener.displayServerError(errorMsg);
            }
        });
    }

    /**
     * Called when the client receives information from the server that the map has changed
     *
     * @param mapName        The name of the new map
     * @param mapExtraParams The extra params of the new map, if any
     */
    public void newMapSet(final String mapName, final int[] mapExtraParams) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                clientPlayerListener.mapChanged(mapName, mapExtraParams);
            }
        });
    }

    public void broadcastClientInfo(Table playerItem) {
        BufferPlayerData bpd = new BufferPlayerData();
        bpd.nameStrings = new String[]{
                ((Label) playerItem.findActor("name-label")).getText().toString()
        };
        bpd.whoStrings = new String[]{
                ((Label) playerItem.findActor("who-label")).getText().toString()
        };
        bpd.readyStrings = new String[]{
                ((Label) playerItem.findActor("ready-label")).getText().toString()
        };
        bpd.colorStrings = new String[]{
                ((DropDownMenu) playerItem.findActor("color-selector")).getSelected()
        };
        bpd.startPosStrings = new String[]{
                ((DropDownMenu) playerItem.findActor("startPos-selector")).getSelected()
        };

        client.sendTCP(bpd);
    }

    public void startGame() {
        clientPlayerListener.startGame();

        emptyBuffers();
    }

    private void emptyBuffers() {
        playerNames.clear();

        client.removeListener(clientLobbyListener);
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
        clientPlayerListener.disconnected();
    }

    public void sendClientNameToServer(String clientName) {
        BufferClientConnect bcc = new BufferClientConnect();
        bcc.playerName = clientName;
        bcc.version = Constants.GAME_VERSION;
        client.sendTCP(bcc);
    }

    public Client getClient() {
        return client;
    }

    public interface ClientPlayerListener {
        void displayServerError(String errorMsg);

        void startGame();

        void disconnected();

        void mapChanged(String mapName, int[] extraParams);

        void updatePlayers(Array<String> playerNames, String[] nameStrings, String[] whoStrings, String[] readyStrings, String[] colorStrings, String[] startPosStrings);
    }
}
