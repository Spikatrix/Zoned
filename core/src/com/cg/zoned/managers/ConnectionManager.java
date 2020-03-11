package com.cg.zoned.managers;

import com.badlogic.gdx.Gdx;
import com.cg.zoned.Constants.Direction;
import com.cg.zoned.Player;
import com.cg.zoned.buffers.BufferDirections;
import com.cg.zoned.listeners.ClientGameListener;
import com.cg.zoned.listeners.ServerGameListener;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class ConnectionManager implements IConnectionHandlers {
    public boolean isActive;     // ConnectionManager will be inactive when playing in splitscreen mode

    private GameManager gameManager;

    private Server server;       // One of these two will be null (or both)
    private Client client;

    private Boolean sentResponse;

    public ConnectionManager(GameManager gameManager, Server server, Client client) {
        isActive = server != null || client != null;
        if (!isActive) {
            return;
        }

        this.gameManager = gameManager;

        this.server = server;
        this.client = client;
        this.sentResponse = false;

        if (server != null) {
            server.addListener(new ServerGameListener(this));
        } else { // else if Client is not null
            client.addListener(new ClientGameListener(this));
        }
    }

    public void updateServer(Server server) {
        this.server = server;
    } // TODO: Might come in handy in the future for reconnecting and shit idk

    public void updateClient(Client client) {
        this.client = client;
    }

    /**
     * Called when the server receives direction information from a client
     * The server then updates its buffer after finding the proper index of the buffer
     *
     * @param bd BufferDirection object containing client player's name and direction
     */
    @Override
    public void serverUpdateDirections(final BufferDirections bd) {
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
            }
        });
    }

    /**
     * Called when the client receives direction information from a server
     * The client then updates its buffer after finding the proper index of the buffer
     *
     * @param bd BufferDirection object containing each player's name and direction
     */
    @Override
    public void clientUpdateDirections(final BufferDirections bd) {
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
     *
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
     * Called when the server/client disconnects
     */
    public void disconnect(Connection connection) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (server != null && gameManager.connectionManager.isActive) {
                    gameManager.connectionManager.close();
                }
                gameManager.endGame();  // TODO: Handle disconnections in a better way
            }
        });
    }

    public void close() {
        if (server != null) {
            server.close();
            server = null;
        } else if (client != null) {
            client.close();
            client = null;
        }
    }
}
