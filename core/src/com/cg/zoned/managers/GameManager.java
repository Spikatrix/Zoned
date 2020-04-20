package com.cg.zoned.managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.screens.GameScreen;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

public class GameManager {
    private final GameScreen gameScreen;

    public ConnectionManager connectionManager;
    public DirectionBufferManager directionBufferManager;
    public PlayerManager playerManager;

    public GameManager(final GameScreen gameScreen, Server server, Client client, Player[] players, Stage stage, int controls, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.gameScreen = gameScreen;

        this.connectionManager = new ConnectionManager(this, server, client);
        this.directionBufferManager = new DirectionBufferManager(players.length);
        this.playerManager = new PlayerManager(this, players, stage, controls, skin, scaleFactor, usedTextures);
    }

    public void endGame() {
        gameScreen.disconnected();
    }
}
