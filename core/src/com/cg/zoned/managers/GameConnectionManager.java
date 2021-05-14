package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
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

public class GameConnectionManager implements GameConnectionHandler {
    // GameConnectionManager will be inactive when playing in splitscreen mode
    public boolean isActive;

    private GameManager gameManager;

    // At least one of these two will be null
    private Server server;
    private Client client;

    // Bridges the Kryonet thread to this manager methods
    private Listener connListener;

    // Used by the client to store the previously sent direction
    private Direction previousDirection;

    // Used by the server to store client connections that came in when a match is already underway
    private Array<Connection> discardConnections;

    // Used by clients to store directions received from the server for processing later
    private Array<BufferDirections> clientDirectionBacklog;

    // Used by clients to display their ping to the server. On the server, this is always zero
    private int ping;

    // Gdx.app.postRunnables are required in several methods below running on the kryonet thread
    // in order to properly sync multiple requests

    public GameConnectionManager(GameManager gameManager, Server server, Client client) {
        isActive = server != null || client != null;
        if (!isActive) {
            return;
        }

        this.gameManager = gameManager;

        this.server = server;
        this.client = client;

        if (server != null) {
            connListener = new ServerGameListener(this);
            discardConnections = new Array<>();
            server.addListener(connListener);
        } else if (client != null) {
            connListener = new ClientGameListener(this);
            clientDirectionBacklog = new Array<>();
            client.addListener(connListener);
        }
    }

    /**
     * Called when the server receives direction information from a client
     * The server then updates the client's `updatedDirection` which is used
     * in {@link PlayerManager#updatePlayerDirectionBuffer()} called from {@link #serverCommunicate()}
     *
     * @param bd BufferDirection object containing client player's name and direction
     */
    @Override
    public void serverUpdateDirections(final BufferDirections bd) {
        Gdx.app.postRunnable(() -> {
                Player[] players = gameManager.playerManager.getPlayers();

                int playerIndex = gameManager.playerManager.getPlayerIndex(bd.playerNames[0]);
                if (playerIndex != -1) {
                    players[playerIndex].updatedDirection = bd.directions[0];
                }
        });
    }

    /**
     * Called when the client receives direction information from a server.
     * The client then adds the received packet to the client backlog for processing.
     *
     * @param bd             BufferDirection object containing each player's name and direction
     * @param returnTripTime The return trip time a.k.a ping of the packet received
     */
    @Override
    public void clientUpdateDirections(final BufferDirections bd, final int returnTripTime) {
        Gdx.app.postRunnable(() -> {
            clientDirectionBacklog.add(bd);
            ping = returnTripTime;
        });
    }

    /**
     * Called in  every frame of the client for processing the direction backlog for applying turns.
     * The backlog is filled from {@link #clientUpdateDirections(BufferDirections, int)} when receiving
     * direction information from the server
     *
     * @param map The map object used to fast forward turns in case multiple packets are available for processing
     */
    private void processClientBacklog(Map map) {
        if (client == null || clientDirectionBacklog.size == 0) {
            return;
        }

        // Force end the current turn, sets all players to their target position
        gameManager.playerManager.forceEndTurn();

        Player[] players = gameManager.playerManager.getPlayers();
        boolean fastForwardTurns = clientDirectionBacklog.size > 1;

        if (fastForwardTurns) {
            // Update the map data for the previous turn
            map.updateMap(players, gameManager.playerManager);
        }

        for (int i = 0; i < clientDirectionBacklog.size; i++) {
            BufferDirections bd = clientDirectionBacklog.get(i);

            for (int j = 0; j < bd.playerNames.length; j++) {
                int playerIndex = gameManager.playerManager.getPlayerIndex(bd.playerNames[j]);
                gameManager.directionBufferManager.updateDirectionBuffer(bd.directions[j], playerIndex);
            }

            gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());

            if (fastForwardTurns) {
                // Fast forward player movement and update map data
                map.update(gameManager.playerManager, Constants.PLAYER_MOVEMENT_MAX_TIME);
            }
        }

        gameManager.directionBufferManager.clearBuffer(); // Not really required to clear
        clientDirectionBacklog.clear();
    }

    /**
     * Called from the game screen's render loop.
     * Sets and broadcasts the direction of the player in the server/client
     *
     * @param map Used by the client to fast forward turns in case it missed a couple
     */
    public void serverClientCommunicate(Map map) {
        if (server != null) {
            serverCommunicate();
        } else if (client != null) {
            processClientBacklog(map);
            clientCommunicate();
        }
    }

    /**
     * Called in the server every render frame to process turns in the server
     */
    private void serverCommunicate() {
        Player[] players = gameManager.playerManager.getPlayers();
        gameManager.playerManager.updatePlayerDirectionBuffer();

        if (players[0].direction == null && gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
            serverProcessTurn();
        }
    }

    private void serverProcessTurn() {
        gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());
        broadcastDirections();
        gameManager.directionBufferManager.clearBuffer();
    }

    /**
     * Called in the client every render frame to send its direction information
     */
    private void clientCommunicate() {
        Player player = gameManager.playerManager.getPlayer(0);

        // No need to waste bandwidth sending the same direction over and over again, send only when it changes
        if (previousDirection != player.updatedDirection) {
            previousDirection = player.updatedDirection;
            gameManager.directionBufferManager.updateDirectionBuffer(player.updatedDirection, 0);
            broadcastDirections();
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
        } else if (client != null) {
            client.sendTCP(bd);
        }
    }

    /**
     * Called in the server when any of its clients disconnects
     */
    @Override
    public void serverDisconnect(final Connection connection) {
        if (discardConnections.indexOf(connection, true) != -1) {
            discardConnections.removeValue(connection, true);
            return;
        }

        Gdx.app.postRunnable(() -> {
            if (server != null) {
                gameManager.serverPlayerDisconnected(connection);
                Player player = gameManager.playerManager.getPlayers()[0];
                player.updatedDirection = player.direction = null;
            } else {
                endGame();
            }
        });
    }

    /**
     * Called in the client when it disconnects
     */
    @Override
    public void clientDisconnect(final Connection connection) {
        Gdx.app.postRunnable(this::endGame);
    }

    private void endGame() {
        gameManager.gameConnectionManager.close();
        gameManager.endGame();
    }

    /**
     * Called in the server when a new client connects when the server already started a match.
     * Server rejects incoming connections when a match is underway
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

    /**
     * Called in the client when it receives the information that
     * a player has been disconnected from the server
     *
     * @param playerName The name of the player that got disconnected
     */
    public void clientPlayerDisconnected(String playerName) {
        gameManager.clientPlayerDisconnected(playerName);
        previousDirection = null;
        Player player = gameManager.playerManager.getPlayers()[0];
        player.updatedDirection = player.direction = null;
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
            isActive = false;

            endGame();
        } else if (client != null) {
            client.removeListener(connListener);
            client.close();
            connListener = null;
            client = null;
            isActive = false;

            endGame();
        }
    }
}
