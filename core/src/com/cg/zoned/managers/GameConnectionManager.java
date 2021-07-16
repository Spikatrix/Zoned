package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.Player.Direction;
import com.cg.zoned.Zoned;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.buffers.BufferGameEnd;
import com.cg.zoned.buffers.BufferPlayerLeft;
import com.cg.zoned.listeners.ClientGameConnectionHandler;
import com.cg.zoned.listeners.ClientGameListener;
import com.cg.zoned.listeners.ServerGameConnectionHandler;
import com.cg.zoned.listeners.ServerGameListener;
import com.cg.zoned.screens.ClientLobbyScreen;
import com.cg.zoned.screens.HostJoinScreen;
import com.cg.zoned.screens.MainMenuScreen;
import com.cg.zoned.screens.ScreenObject;
import com.cg.zoned.screens.ServerLobbyScreen;
import com.cg.zoned.ui.FocusableStage;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GameConnectionManager implements ServerGameConnectionHandler, ClientGameConnectionHandler {
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

    // The server lobby reference is kept alive to switch to after the match
    private ServerLobbyScreen serverLobby;

    // Used by clients to store directions received from the server for processing later
    private Array<BufferDirections> clientDirectionBacklog;

    // Used by clients to display their ping to the server. On the server, this is always zero
    private int ping;

    // Gdx.app.postRunnables are required in several methods below that run on the kryonet thread
    // in order to properly sync multiple requests with the main GDX thread

    public GameConnectionManager(GameManager gameManager, ServerLobbyScreen serverLobby, Client client) {
        isActive = serverLobby != null || client != null;
        if (!isActive) {
            return;
        }

        this.gameManager = gameManager;
        this.serverLobby = serverLobby;
        this.client = client;

        if (serverLobby != null) {
            // Server user
            this.server = serverLobby.getServer();
            connListener = new ServerGameListener(this);
            server.addListener(connListener);
        } else {
            // Client user
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
            clientDirectionBacklog.add(bd);
            ping = returnTripTime;
        });
    }

    /**
     * Called in every frame of the client for processing the direction backlog for applying turns.
     * The backlog is filled from {@link #clientUpdateDirections(BufferDirections, int)} when receiving
     * direction information from the server
     *
     * @param map The map object used to fast forward turns in case multiple packets are available for processing
     */
    private void processClientBacklog(Map map) {
        if (clientDirectionBacklog.isEmpty()) {
            return;
        }

        Player[] players = gameManager.playerManager.getPlayers();

        if (gameManager.playerManager.movementInProgress(true)) {
            // Update the map data for the previous turn
            map.updateMap(gameManager.playerManager);
        }

        for (int i = 0; i < clientDirectionBacklog.size; i++) {
            BufferDirections bd = clientDirectionBacklog.get(i);

            for (int j = 0; j < bd.playerNames.length; j++) {
                int playerIndex = PlayerManager.getPlayerIndex(players, bd.playerNames[j]);
                gameManager.directionBufferManager.updateDirectionBuffer(bd.directions[j], playerIndex);
            }

            gameManager.playerManager.setPlayerDirections(gameManager.directionBufferManager.getDirectionBuffer());

            if (i != clientDirectionBacklog.size - 1) {
                // Fast forward player movement and update map data for the
                // current turn as there are more turns left to process
                map.update(gameManager.playerManager, Constants.PLAYER_MOVEMENT_MAX_TIME);
            }
        }

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
        gameManager.playerManager.updatePlayerDirectionBuffer();

        if (readyForNextTurn()) {
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

        // Sends the direction only when it changes instead of wasting bandwidth sending the same direction
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

    private boolean readyForNextTurn() {
        Player[] players = gameManager.playerManager.getPlayers();
        return (!gameManager.playerManager.movementInProgress(false) &&
                gameManager.directionBufferManager.getBufferUsedCount() == players.length);
    }

    public boolean broadcastGameEnd(boolean restart, FocusableStage screenStage) {
        BufferGameEnd bge = new BufferGameEnd();
        bge.restartGame = restart;

        if (client != null) {
            if (restart) { // The client can't restart matches
                screenStage.showOKDialog("Only the host can restart the match", false, null);
                return false;
            }
            // Client exited the current match
            client.sendTCP(bge);
        } else if (server != null) {
            // Server ended/restarted the current match
            server.sendToAllTCP(bge);
        } else {
            // Shouldn't really happen
            screenStage.showOKDialog("Connection lost", false, null);
            return false;
        }

        return true;
    }

    /**
     * Called in the client when it receives word that the server restarted/ended the current match
     *
     * @param restartGame Indicates whether the server restarted/ended the current match
     */
    @Override
    public void clientGameEnd(boolean restartGame) {
        gameManager.clientGameEnd(restartGame);
    }

    public String getClientPlayerName(Connection connection) {
        int connIndex = serverLobby.getConnectionIndex(connection);
        if (connIndex != -1) {
            return gameManager.playerManager.getPlayer(connIndex).name;
        }

        return null;
    }

    /**
     * Called in the server when any of its clients exits the match
     */
    @Override
    public void serverClientExited(Connection connection) {
        handleClientLeave(connection, false);
    }

    /**
     * Called in the server when any of its clients disconnects
     */
    @Override
    public void serverClientDisconnected(final Connection connection) {
        handleClientLeave(connection, true);
    }

    private void handleClientLeave(final Connection connection, boolean disconnected) {
        Gdx.app.postRunnable(() -> {
            String playerName = getClientPlayerName(connection);
            if (!disconnected) {
                broadcastClientExit(playerName);
            }

            gameManager.serverClientLeft(playerName, disconnected);
            gameManager.playerManager.getPlayers()[0].direction = null;
        });
    }

    private void broadcastClientExit(String playerName) {
        BufferPlayerLeft bpd = new BufferPlayerLeft();
        bpd.playerName = playerName;
        bpd.disconnected = false;

        server.sendToAllTCP(bpd);
    }

    public Object getServerClientObject() {
        if (serverLobby != null) {
            return serverLobby;
        } else if (client != null) {
            return client;
        } else {
            return null;
        }
    }

    /**
     * Called in the client when it disconnects
     */
    @Override
    public void clientDisconnect(final Connection connection) {
        Gdx.app.postRunnable(gameManager::clientDisconnected);
    }

    /**
     * Called in the client when it receives the information that
     * a player has been disconnected from the server
     *
     * @param playerName The name of the player that got disconnected or left
     * @param disconnected Indicates whether the player got disconnected or left
     */
    public void clientPlayerDisconnected(String playerName, boolean disconnected) {
        Gdx.app.postRunnable(() -> {
            Player[] players = gameManager.playerManager.getPlayers();
             if (PlayerManager.getPlayerIndex(players, playerName) == -1) {
                return;
            }

            gameManager.clientPlayerDisconnected(playerName, disconnected);
            Player player = gameManager.playerManager.getPlayers()[0];
            previousDirection = player.updatedDirection = player.direction = null;
        });
    }

    public int getPing() {
        return ping;
    }

    public ScreenObject getLobbyScreen(Zoned game) {
        if (serverLobby != null) {
            return serverLobby;
        } else if (client != null) {
            if (client.isConnected()) {
                return new ClientLobbyScreen(game, client);
            } else {
                return new HostJoinScreen(game);
            }
        } else {
            return new MainMenuScreen(game);
        }
    }

    public void close() {
        removeGameListener();
        if (serverLobby != null) {
            serverLobby.clearAndCloseLobby();
            serverLobby.dispose();
            serverLobby = null;
        } else if (client != null) {
            client.close();
            client = null;
        }
    }

    public void removeGameListener() {
        if (connListener != null) {
            if (server != null) {
                server.removeListener(connListener);
                connListener = null;
            } else if (client != null) {
                client.removeListener(connListener);
                connListener = null;
            }
        }
    }
}
