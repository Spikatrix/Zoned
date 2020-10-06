package com.cg.zoned.managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.screens.GameScreen;

public class GameManager {
    private final GameScreen gameScreen;

    public boolean gameOver;

    public DirectionBufferManager directionBufferManager;
    public PlayerManager playerManager;

    public GameManager(final GameScreen gameScreen) {
        this.gameScreen = gameScreen;

        this.gameOver = false;
    }

    public void setUpDirectionAndPlayerBuffer(Player[] players, Stage stage, int controlIndex, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.directionBufferManager = new DirectionBufferManager(players.length);
        this.playerManager = new PlayerManager(this, players, stage, controlIndex, skin, scaleFactor, usedTextures);

    }
}
