package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.Player.Direction;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferKickClient;
import com.cg.zoned.buffers.BufferPlayerDisconnected;
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

    // Used by clients to handle player position predictions
    private ClientPredictionHandler clientPredictionHandler;

    // Used by clients to display their ping to the server. On the server, this is always zero
    private int ping;

    // Gdx.app.postRunnables are required in several methods below that run on the kryonet thread
    // in order to properly sync multiple requests with the main GDX thread

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
     * Used to enable client side predictions, called from the client
     */
    public void initClientPrediction() {
        Player[] players = gameManager.playerManager.getPlayers();
        clientPredictionHandler = new ClientPredictionHandler(players,
                gameManager.directionBufferManager, gameManager.playerManager);
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

                int playerIndex = PlayerManager.getPlayerIndex(players, bd.playerNames[0]);
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
            Gdx.app.log(Constants.LOG_TAG, "Incoming package");
            clientDirectionBacklog.add(bd);
            ping = returnTripTime;
        });
    }

    /**
     * Called in every frame of the client for processing the direction backlog for applying turns.
     * The backlog is filled from {@link #clientUpdateDirections(BufferDirections, int)} when receiving
     * direction information from the server
     *
     * The client predicted positions are also checked and applied accordingly from here if it was initialized from
     * {@link #initClientPrediction()}. The predicted positions are filled from {@link #clientCommunicate()}
     *
     * @param map The map object used to fast forward turns in case multiple packets are available for processing
     */
    private void processClientLogs(Map map) {
        if (clientPredictionHandler != null) {
            clientPredictionHandler.verifyClientPredictions(clientDirectionBacklog, map);
        }

        if (clientDirectionBacklog.notEmpty()) {
            processClientBacklog(map);
        }
    }

    private void processClientBacklog(Map map) {
        Player[] players = gameManager.playerManager.getPlayers();

        if (clientPredictionHandler == null && gameManager.playerManager.movementInProgress(true)) {
            // Update the map data for the previous turn
            Gdx.app.log(Constants.LOG_TAG, "Calling updateMap from backlog");
            map.updateMap(gameManager.playerManager);
        }

        Gdx.app.log(Constants.LOG_TAG, "Processing " + clientDirectionBacklog.size + " turns...");
        for (int i = 0; i < clientDirectionBacklog.size; i++) {
            BufferDirections bd = clientDirectionBacklog.get(i);
            Gdx.app.log(Constants.LOG_TAG, "Handling backlog [" + bd.directions[0] + " " + bd.directions[1] + "] (server, client)");

            for (int j = 0; j < bd.playerNames.length; j++) {
                int playerIndex = PlayerManager.getPlayerIndex(players, bd.playerNames[j]);
                gameManager.directionBufferManager.updateDirectionBuffer(bd.directions[j], playerIndex);
                /*if (clientPredictionHandler != null) {
                    clientPredictionHandler.updateVerifiedPlayerPosition(playerIndex, bd.directions[j], map);
                }*/
            }

            Gdx.app.log(Constants.LOG_TAG, "Setting dirs [" + gameManager.directionBufferManager.getDirection(0) + ", " + gameManager.directionBufferManager.getDirection(1) + "]");
            gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());

            if (clientPredictionHandler != null) {
                clientPredictionHandler.updateMapColors(map, gameManager.directionBufferManager.getDirectionBuffer());
            }

            if (i != clientDirectionBacklog.size - 1) {
                // Fast forward player movement and update map data for the
                // current turn as there are more turns left to process
                Gdx.app.log(Constants.LOG_TAG, "Fast forwarding turn");
                if (clientPredictionHandler == null) {
                    map.update(gameManager.playerManager, Constants.PLAYER_MOVEMENT_MAX_TIME);
                } else {
                    map.updatePlayerPositions(players, Constants.PLAYER_MOVEMENT_MAX_TIME);
                }
            }
        }

        clientDirectionBacklog.clear();
    }

    /**
     * Called from the game screen's render loop.
     * Sets and broadcasts the direction of the player in the server/client
     *
     * @param map Used by the client to fast forward turns in case it missed a couple
     * @param delta
     */
    public void serverClientCommunicate(Map map, float delta) {
        if (server != null) {
            serverCommunicate();
        } else if (client != null) {
            processClientLogs(map);

            clientCommunicate();
            performClientPredictions();
        }

        if (server != null || (client != null && clientPredictionHandler == null)) {
            // Update the map for the server and the client if prediction is not enabled
            map.update(gameManager.playerManager, delta);
        } else if (client != null) {
            // Move players in the client without updating the map as prediction is enabled
            map.updatePlayerPositions(gameManager.playerManager.getPlayers(), delta);
        }
    }

    /**
     * Called in the server every render frame to process turns in the server
     */
    private void serverCommunicate() {
        gameManager.playerManager.updatePlayerDirectionBuffer();

        if (readyForNextTurn()) {
            serverProcessTurn();
        }
    }

    private void serverProcessTurn() {
        gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());
        Gdx.app.log(Constants.LOG_TAG, "Server broadcastin' [" + gameManager.directionBufferManager.getDirection(0) + " " + gameManager.directionBufferManager.getDirection(1) + "]");
        broadcastDirections();
        gameManager.directionBufferManager.clearBuffer();
    }

    /**
     * Called in the client every render frame to send its direction information
     */
    private void clientCommunicate() {
        Player player = gameManager.playerManager.getPlayer(0);

        // Sends the direction only when it changes instead of wasting bandwidth sending the same direction
        if (previousDirection != player.updatedDirection) {
            previousDirection = player.updatedDirection;
            gameManager.directionBufferManager.updateDirectionBuffer(player.updatedDirection, 0);
            broadcastDirections();
        }
    }

    private void performClientPredictions() {
        if (clientPredictionHandler != null && readyForNextTurn() && !clientPredictionHandler.reachedPredictionLimit()) {
            // It's time for the next turn, but the client hasn't received the next turn info from the server
            // So the client predicts and interpolates player position in the mean time instead of waiting
            Direction[] directions = gameManager.directionBufferManager.getDirectionBuffer();
            Gdx.app.log(Constants.LOG_TAG, "Performing prediction with [" + directions[0] + ", " + directions[1] + "]");
            clientPredictionHandler.applyPrediction();
            Gdx.app.log(Constants.LOG_TAG, "Prediction size: " + clientPredictionHandler.getSize() + " Backlog size: " + clientDirectionBacklog.size);
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

    private boolean readyForNextTurn() {
        Player[] players = gameManager.playerManager.getPlayers();
        return (!gameManager.playerManager.movementInProgress(false) &&
                gameManager.directionBufferManager.getBufferUsedCount() == players.length);
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
        this.close();
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

        BufferKickClient bkc = new BufferKickClient();
        bkc.kickReason = "Server is busy playing a match.\nPlease try again later";
        connection.sendTCP(bkc);
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
