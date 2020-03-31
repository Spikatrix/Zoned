package com.cg.zoned.managers;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.cg.zoned.Player;
import com.cg.zoned.screens.GameScreen;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

public class GameManager {
    private final GameScreen gameScreen;

    public ConnectionManager connectionManager;
    public DirectionBufferManager directionBufferManager;
    public PlayerManager playerManager;

    public GameManager(final GameScreen gameScreen, Server server, Client client, Player[] players, Stage stage, int controls) {
        this.gameScreen = gameScreen;

        this.connectionManager = new ConnectionManager(this, server, client);
        this.directionBufferManager = new DirectionBufferManager(players.length);
        this.playerManager = new PlayerManager(this, players, stage, controls);
    }

    public void endGame() {
        gameScreen.disconnected();
    }
}
