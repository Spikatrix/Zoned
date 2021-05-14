package com.cg.zoned.managers;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.dataobjects.TeamData;

public class PlayerManager extends InputMultiplexer {
    private final GameManager gameManager;

    private ControlManager controlManager;

    private Player[] players;
    private Array<TeamData> teamData;

    public PlayerManager(GameManager gameManager, Player[] players, Stage stage, int controlIndex, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.gameManager = gameManager;

        this.players = players;
        initTeamData();

        if (gameManager.gameConnectionManager.isActive) {
            // Not in splitscreen mode; add only the first player's inputs
            this.addProcessor(players[0]);
        } else {
            for (Player player : players) {
                this.addProcessor(player);
            }
        }

        controlManager = new ControlManager(players, stage);
        controlManager.setUpOverlay(!gameManager.gameConnectionManager.isActive, controlIndex, skin, scaleFactor, usedTextures);
        this.addProcessor(controlManager.getControls());
    }

    private void initTeamData() {
        this.teamData = new Array<>();

        for (Player player : players) {
            boolean alreadyExists = false;
            for (TeamData teamData : this.teamData) {
                if (player.color.equals(teamData.getColor())) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                teamData.add(new TeamData(player.color));
            }
        }
    }

    public void setPlayerDirections(Player.Direction[] directions) {
        for (int i = 0; i < players.length; i++) {
            players[i].direction = directions[i];
        }
    }

    public void stopPlayers(boolean freeze) {
        for (Player player : players) {
            player.updatedDirection = null;

            if (freeze) { // Freezing a player might result in the player stopping in between cells
                player.direction = null;
            }
        }
    }

    public void updatePlayerDirections() {
        updatePlayerDirectionBuffer();

        // Apply directions only if all players have set a direction
        if (gameManager.directionBufferManager.getBufferUsedCount() == players.length) {
            for (Player player : players) {
                player.direction = player.updatedDirection;
            }
        }
    }

    public void updatePlayerDirectionBuffer() {
        for (int i = 0; i < players.length; i++) {
            gameManager.directionBufferManager.updateDirectionBuffer(players[i].updatedDirection, i);
        }
    }

    public void incrementScore(Player player) {
        for (TeamData teamData : this.teamData) {
            if (player.color.equals(teamData.getColor())) {
                teamData.incrementScore();
                return;
            }
        }
    }

    public void forceEndTurn() {
        for (Player player : players) {
            player.completeMovement();
        }
    }

    public void renderPlayerControlPrompt(ShapeDrawer shapeDrawer, float delta) {
        controlManager.renderPlayerControlPrompt(shapeDrawer, delta);
    }

    public int getPlayerIndex(String name) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].name.equals(name)) {
                return i;
            }
        }

        return -1;
    }

    public Player getPlayer(int index) {
        return players[index];
    }

    public Player[] getPlayers() {
        return players;
    }

    public Array<TeamData> getTeamData() {
        return teamData;
    }
}
