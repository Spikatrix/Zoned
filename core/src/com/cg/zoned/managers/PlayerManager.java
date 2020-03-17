package com.cg.zoned.managers;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Constants.Direction;
import com.cg.zoned.Player;

public class PlayerManager extends InputMultiplexer {
    private final GameManager gameManager;

    private Player[] players;
    private int[] playerScores;

    private Array<Color> teamColors;

    public PlayerManager(GameManager gameManager, Player[] players) {
        this.gameManager = gameManager;

        this.players = players;
        this.playerScores = new int[players.length];

        this.teamColors = new Array<Color>();
        initTeamColors();

        if (gameManager.connectionManager.isActive) { // Not split screen; only add first player's inputs
            this.addProcessor(players[0]);
        } else {
            for (Player player : players) {
                this.addProcessor(player);
            }
        }
        this.addProcessor(new PlayerTouchManager(players, !gameManager.connectionManager.isActive));
    }

    private void initTeamColors() {
        for (Player player : players) {
            if (!teamColors.contains(player.color, false)) {
                teamColors.add(player.color);
            }
        }
    }

    public void setPlayerDirections(Direction[] directions) {
        for (int i = 0; i < players.length; i++) {
            players[i].direction = directions[i];
        }
    }

    public void stopPlayers() {
        for (Player player : players) {
            player.direction = player.updatedDirection = null;
        }
    }

    public void updatePlayerDirections() {
        if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
            for (Player player : players) {
                player.direction = player.updatedDirection;
            }
            return;
        }

        for (int i = 0; i < players.length; i++) {
            gameManager.directionBufferManager.updateDirection(players[i].updatedDirection, i);
        }
    }

    public Player getPlayer(int index) {
        return players[index];
    }

    public Player[] getPlayers() {
        return players;
    }

    public int getPlayerScore(int index) {
        return playerScores[index];
    }

    public int[] getPlayerScores() {
        return playerScores;
    }

    public int getTeamCount() {
        return teamColors.size;
    }
}
