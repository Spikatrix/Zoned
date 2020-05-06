package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerColorHelper;
import com.cg.zoned.buffers.BufferClientConnect;
import com.cg.zoned.buffers.BufferPlayerData;
import com.cg.zoned.listeners.ClientLobbyListener;
import com.cg.zoned.maps.InvalidMapCharacter;
import com.cg.zoned.maps.InvalidMapDimensions;
import com.cg.zoned.maps.MapEntity;
import com.cg.zoned.maps.NoStartPositionsFound;
import com.cg.zoned.ui.DropDownMenu;
import com.esotericsoftware.kryonet.Client;

public class ClientLobbyConnectionManager {
    private Client client;

    /**
     * Clients use this to store and track names of all players in the lobby
     */
    private Array<String> playerNames;

    /**
     * Each row of the playerList which contains information about each player
     */
    private Array<Table> playerItems;

    private ClientPlayerListener clientPlayerListener; // This manager to screen
    private ClientLobbyListener clientLobbyListener; // Kryonet to this manager

    private String currentUserName;

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public ClientLobbyConnectionManager(Client client, ClientPlayerListener clientPlayerListener, String currentUserName) {
        playerNames = new Array<>();
        playerItems = new Array<>();

        this.currentUserName = currentUserName;
        this.clientPlayerListener = clientPlayerListener;

        this.client = client;
    }

    public void start() {
        playerItems.add(clientPlayerListener.addPlayer(null, null, null, null, null));
        playerNames.add(currentUserName);

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
                for (int i = 0; i < nameStrings.length; i++) {
                    if (nameStrings[i].equals(currentUserName)) { // No need to update information for this client itself
                        continue;
                    }

                    if (whoStrings[i].equals("(DEL)")) {
                        int index = playerNames.indexOf(nameStrings[i], false);
                        if (index != -1) { // Can be -1 too
                            playerNames.removeIndex(index);
                            clientPlayerListener.removePlayer(playerItems.get(index));
                            playerItems.removeIndex(index);
                        }

                        continue;
                    }

                    int index = playerNames.indexOf(nameStrings[i], false);
                    if (index != -1) {
                        updatePlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i], startPosStrings[i], index);
                    } else {
                        playerItems.add(clientPlayerListener.addPlayer(nameStrings[i], whoStrings[i], readyStrings[i], colorStrings[i], startPosStrings[i]));
                        playerNames.add(nameStrings[i]);
                    }
                }
            }
        });

    }

    private void updatePlayer(String name, String who, String ready, String color, String startPos, int index) {
        ((Label) this.playerItems.get(index).findActor("name-label")).setText(name);

        if (who.endsWith("(Host, You)")) {
            who = who.substring(0, who.lastIndexOf(',')) + ')'; // Replace with "(Host)"
        }
        ((Label) this.playerItems.get(index).findActor("who-label")).setText(who);

        Label readyLabel = this.playerItems.get(index).findActor("ready-label");
        if (ready.equals("Ready")) {
            readyLabel.setColor(Color.GREEN);
        } else {
            readyLabel.setColor(Color.RED);
        }
        readyLabel.setText(ready);

        ((Label) this.playerItems.get(index).findActor("color-label")).setText(color);
        ((Label) this.playerItems.get(index).findActor("startPos-label")).setText(startPos);
    }

    public void displayError(final String errorMsg) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                clientPlayerListener.displayError(errorMsg);
            }
        });
    }

    public void newMapSet(final boolean gameStart, final String mapName, final int[] mapExtraParams) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                final MapManager mapManager = new MapManager();
                MapEntity map = mapManager.getMap(mapName);
                if (map == null) {
                    // Should never happen cause server loads a valid internal map before sending it to all the clients
                    clientPlayerListener.displayError("Unknown map received: '" + mapName + "'");
                    return;
                }

                if (mapExtraParams != null) {
                    map.getExtraParams().extraParams = mapExtraParams;
                    map.applyExtraParams();
                }

                try {
                    mapManager.prepareMap(map);
                } catch (InvalidMapCharacter | NoStartPositionsFound | InvalidMapDimensions e) {
                    // Should never happen cause the server does this check before sending to all the clients
                    e.printStackTrace();
                    clientPlayerListener.displayError("Error: " + e.getMessage());
                    return;
                }

                if (gameStart) {
                    clientPlayerListener.startGame(mapManager);
            /*client.removeListener(clientLobbyListener); TODO: THIS ?
            clientPlayerListener = null;*/
                } else {
                    clientPlayerListener.mapChanged(mapManager);
                }
            }
        });
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

    public void broadcastClientInfo() {
        BufferPlayerData bpd = new BufferPlayerData();
        bpd.nameStrings = new String[]{
                ((Label) playerItems.get(0).findActor("name-label")).getText().toString()
        };
        bpd.whoStrings = new String[]{
                ((Label) playerItems.get(0).findActor("who-label")).getText().toString()
        };
        bpd.readyStrings = new String[]{
                ((Label) playerItems.get(0).findActor("ready-label")).getText().toString()
        };
        bpd.colorStrings = new String[]{
                ((DropDownMenu) playerItems.get(0).findActor("color-selector")).getSelected()
        };
        bpd.startPosStrings = new String[]{
                ((DropDownMenu) playerItems.get(0).findActor("startPos-selector")).getSelected()
        };

        client.sendTCP(bpd);
    }

    public void closeConnection() {
        playerNames.clear();
        playerItems.clear();

        if (client.isConnected()) {
            client.close();
        }
        client.removeListener(clientLobbyListener);
        clientPlayerListener = null;
    }

    public void clientDisconnected() {
        clientPlayerListener.disconnected();
    }

    public void mapChanged(Array<String> startPosLocations) {
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
    }

    public void toggleReadyState(TextButton readyButton) {
        Label readyLabel = playerItems.get(0).findActor("ready-label");

        if (readyLabel.getText().toString().equals("Not ready")) {
            readyLabel.setColor(Color.GREEN);
            readyLabel.setText("Ready");
            readyButton.setText("Unready");
        } else {
            readyLabel.setColor(Color.RED);
            readyLabel.setText("Not ready");
            readyButton.setText("Ready up");
        }

        broadcastClientInfo();
    }

    public void sendClientNameToServer() {
        BufferClientConnect bcc = new BufferClientConnect();
        bcc.playerName = this.currentUserName;
        client.sendTCP(bcc);
    }

    public void setClientPlayerListener(ClientPlayerListener clientPlayerListener) {
        // Should not be null. Will cause NPE if null.
        this.clientPlayerListener = clientPlayerListener;
    }

    public Array<Table> getPlayerItems() { // This required? hmm
        return playerItems;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public Client getClient() {
        return client;
    }

    public interface ClientPlayerListener {
        Table addPlayer(String name, String who, String ready, String color, String startPos);

        void displayError(String errorMsg);

        void startGame(MapManager mapManager);

        void removePlayer(Table playerItem);

        void disconnected();

        void mapChanged(MapManager mapManager);
    }
}
