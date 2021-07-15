package com.cg.zoned.managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.screens.GameScreen;
import com.cg.zoned.screens.ServerLobbyScreen;
import com.esotericsoftware.kryonet.Client;

public class GameManager {
    private final GameScreen gameScreen;

    public boolean gameOver;

    public GameConnectionManager gameConnectionManager;
    public DirectionBufferManager directionBufferManager;
    public PlayerManager playerManager;

    public GameManager(final GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.gameOver = false;
    }

    public void setUpConnectionManager(ServerLobbyScreen serverLobby, Client client) {
        this.gameConnectionManager = new GameConnectionManager(this, serverLobby, client);
    }

    public void setUpDirectionAndPlayerBuffer(Player[] players, Stage stage, int controlIndex, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.directionBufferManager = new DirectionBufferManager(players.length);
        this.playerManager = new PlayerManager(this, players, stage, controlIndex, skin, scaleFactor, usedTextures);
    }

    public void serverClientLeft(String playerName, boolean disconnected) {
        gameScreen.serverClientLeft(playerName, disconnected);
    }

    public void clientPlayerDisconnected(String playerName, boolean disconnected) {
        gameScreen.clientPlayerDisconnected(playerName, disconnected);
    }

    public void clientGameEnd(boolean restartGame) {
        gameScreen.clientGameEnd(restartGame);
    }

    public void clientDisconnected() {
        gameScreen.clientDisconnected();
    }
}
