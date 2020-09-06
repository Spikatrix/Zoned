package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.Player.Direction;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
import com.cg.zoned.buffers.BufferServerRejectedConnection;
import com.cg.zoned.listeners.ClientGameListener;
import com.cg.zoned.listeners.ServerGameListener;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GameConnectionManager implements IConnectionHandlers {
    public boolean isActive;     // GameConnectionManager will be inactive when playing in splitscreen mode

    private GameManager gameManager;

    private Server server;       // One of these two will be null (or both)
    private Client client;

    private Listener connListener; // Kryonet to this manager

    private Array<Connection> discardConnections; // Used to store client connections that came in when in-game

    private int ping;
    private boolean sentResponse;

    // I've put a bunch of Gdx.app.postRunnables in order to properly sync multiple requests

    public GameConnectionManager(GameManager gameManager, Server server, Client client) {
        isActive = server != null || client != null;
        if (!isActive) {
            return;
        }

        this.gameManager = gameManager;

        this.server = server;
        this.client = client;
        this.sentResponse = false;

        this.discardConnections = new Array<>();

        if (server != null) {
            connListener = new ServerGameListener(this);
            server.addListener(connListener);
        } else if (client != null) {
            connListener = new ClientGameListener(this);
            client.addListener(connListener);
        }
    }

    /**
     * Called when the server receives direction information from a client
     * The server then updates its buffer after finding the proper index of the buffer
     *
     * @param bd             BufferDirection object containing client player's name and direction
     * @param returnTripTime The return trip time a.k.a ping of the packet received
     */
    @Override
    public void serverUpdateDirections(final BufferDirections bd, final int returnTripTime) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Player[] players = gameManager.playerManager.getPlayers();
                for (int i = 0; i < players.length; i++) {
                    if (players[i].name.equals(bd.playerNames[0])) {
                        players[i].updatedDirection = bd.directions[0];
                        gameManager.directionBufferManager.updateDirection(bd.directions[0], i);
                        break;
                    }
                }

                /* Check if the server got all directions for the next turn.
                   If so, set the player directions and broadcast information to all clients */
                if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
                    gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());
                    broadcastDirections();
                    gameManager.directionBufferManager.clearBuffer();
                    sentResponse = false;
                }

                ping = returnTripTime;
            }
        });
    }

    /**
     * Called when the client receives direction information from a server
     * The client then updates its buffer after finding the proper index of the buffer
     *
     * @param bd             BufferDirection object containing each player's name and direction
     * @param returnTripTime The return trip time a.k.a ping of the packet received
     */
    @Override
    public void clientUpdateDirections(final BufferDirections bd, final int returnTripTime) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Player[] players = gameManager.playerManager.getPlayers();
                for (int i = 0; i < bd.playerNames.length; i++) {
                    for (int j = 0; j < players.length; j++) {
                        if (players[j].name.equals(bd.playerNames[i])) {
                            if (gameManager.directionBufferManager.getDirection(j) == null && bd.directions[i] != null) {
                                gameManager.directionBufferManager.updateDirection(bd.directions[i], j);
                                players[j].updatedDirection = bd.directions[i];
                            }
                            break;
                        }
                    }
                }

                if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
                    gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());
                    gameManager.directionBufferManager.clearBuffer();
                    sentResponse = false;
                }

                ping = returnTripTime;
            }
        });
    }

    /**
     * Called from the game screen's render loop. Sets and broadcasts the direction of the server/client
     */
    public void serverClientCommunicate() {
        Player[] players = gameManager.playerManager.getPlayers();

        if (!sentResponse && players[0].direction == null && players[0].updatedDirection != null) {
            gameManager.directionBufferManager.updateDirection(players[0].updatedDirection, 0);
            if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
                gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());
            }

            broadcastDirections();

            if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
                gameManager.directionBufferManager.clearBuffer();
                sentResponse = false;
            } else {
                sentResponse = true;
            }
        }
    }

    /**
     * Broadcasts directions to other devices
     * <p>
     * In case of the server, it will send information about all players to all clients
     * In cast of a   client, it will send information about itself to the server
     */
    private void broadcastDirections() {
        Player[] players = gameManager.playerManager.getPlayers();
        Direction[] directions = gameManager.directionBufferManager.getDirectionBuffer();
        int size = server != null ? players.length : 1;

        BufferDirections bd = new BufferDirections();
        bd.playerNames = new String[size];
        bd.directions = new Direction[size];
        for (int i = 0; i < size; i++) {
            bd.playerNames[i] = players[i].name;
            bd.directions[i] = directions[i];
        }

        if (server != null) {
            server.sendToAllTCP(bd);
        } else {
            client.sendTCP(bd);
        }
    }

    /**
     * Called in the server any of its clients disconnects
     */
    @Override
    public void serverDisconnect(final Connection connection) {
        if (discardConnections.indexOf(connection, true) != -1) {
            discardConnections.removeValue(connection, true);
            return;
        }

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (server != null) {
                    gameManager.serverPlayerDisconnected(connection);
                    sentResponse = false;
                    Player player1 = gameManager.playerManager.getPlayers()[0];
                    player1.updatedDirection = player1.direction = null;
                } else {
                    endGame();
                }
            }
        });
    }

    /**
     * Called in the client when the client disconnects
     */
    @Override
    public void clientDisconnect(final Connection connection) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                endGame();
            }
        });
    }

    private void endGame() {
        gameManager.gameConnectionManager.close();
        gameManager.endGame();
    }

    /**
     * Called in the server when a new client connects when in the GameScreen
     * Server rejects incoming connections when a match is already underway
     *
     * @param connection The client's connection
     */
    public void rejectNewConnection(Connection connection) {
        discardConnections.add(connection);

        BufferServerRejectedConnection bsrc = new BufferServerRejectedConnection();
        bsrc.errorMsg = "Server is busy playing a match.\nPlease try again later";
        connection.sendTCP(bsrc);
    }

    public void sendPlayerDisconnectedBroadcast(String playerName) {
        BufferPlayerDisconnected bpd = new BufferPlayerDisconnected();
        bpd.playerName = playerName;
        server.sendToAllTCP(bpd);
    }

    public void clientPlayerDisconnected(String playerName) {
        gameManager.clientPlayerDisconnected(playerName);
        sentResponse = false;
        Player player1 = gameManager.playerManager.getPlayers()[0];
        player1.updatedDirection = player1.direction = null;
    }

    public int getPing() {
        return ping;
    }

    public void close() {
        if (server != null) {
            server.removeListener(connListener);
            server.close();
            connListener = null;
            server = null;

            endGame();
        } else if (client != null) {
            client.removeListener(connListener);
            client.close();
            connListener = null;
            client = null;

            endGame();
        }
    }
}
