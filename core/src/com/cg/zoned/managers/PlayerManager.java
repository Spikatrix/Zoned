package com.cg.zoned.managers;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerEntity;
import com.cg.zoned.ShapeDrawer;
import com.cg.zoned.dataobjects.TeamData;

public class PlayerManager extends InputMultiplexer {
    private final GameManager gameManager;

    private ControlManager controlManager;

    private PlayerEntity[] players;
    private Array<TeamData> teamData;

    public PlayerManager(GameManager gameManager, PlayerEntity[] players, Stage stage, int controlIndex, Skin skin, float scaleFactor, Array<Texture> usedTextures) {
        this.gameManager = gameManager;

        this.players = players;
        initTeamData();

        if (gameManager.gameConnectionManager.isActive) {
            // Not in splitscreen mode; add only the first player's inputs
            this.addProcessor((Player) players[0]);
        } else {
            for (PlayerEntity player : players) {
                if (player instanceof Player) {
                    this.addProcessor((Player) player); // Enables keyboard controls for the player
                } else {
                    // player is a bot
                }
            }
        }

        controlManager = new ControlManager(players, stage);
        controlManager.setUpOverlay(!gameManager.gameConnectionManager.isActive, controlIndex, skin, scaleFactor, usedTextures);
        this.addProcessor(controlManager.getControls()); // Enables on-screen touch controls for players
    }

    private void initTeamData() {
        this.teamData = new Array<>();

        for (PlayerEntity player : players) {
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
        for (PlayerEntity player : players) {
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
            for (PlayerEntity player : players) {
                player.direction = player.updatedDirection;
            }
        }
    }

    public void updatePlayerDirectionBuffer() {
        for (int i = 0; i < players.length; i++) {
            gameManager.directionBufferManager.updateDirectionBuffer(players[i].updatedDirection, i);
        }
    }

    /**
     * Increments the score of the team the player belongs to
     *
     * @param player The player whose team's score is to be incremented
     */
    public void incrementScore(PlayerEntity player) {
        for (TeamData teamData : this.teamData) {
            if (player.color.equals(teamData.getColor())) {
                teamData.incrementScore();
                return;
            }
        }
    }

    /**
     * Resets the score of every team
     */
    public void resetScores() {
        for (TeamData teamData : this.teamData) {
            teamData.resetScore();
        }
    }

    /**
     * Gets the color of the team with the highest score
     *
     * @return The color of the leading team
     *         Returns null if two or more teams are leading with the same score
     */
    public Color getLeadingTeamColor() {
        TeamData teamData = getLeadingTeam();
        if (teamData != null) {
            return teamData.getColor();
        }
        return null;
    }

    /**
     * Computes and returns the team with the highest score
     *
     * @return The leading team object
     *         Returns null if two or more teams are leading with the same score
     */
    private TeamData getLeadingTeam() {
        int highscore = 0;
        TeamData leadingTeam = null;

        for (TeamData teamData : gameManager.playerManager.getTeamData()) {
            if (teamData.getScore() > highscore) {
                highscore = teamData.getScore();
                leadingTeam = teamData;
            } else if (teamData.getScore() == highscore) {
                leadingTeam = null;
            }
        }

        return leadingTeam;
    }

    /**
     * Used to check if at least one player is/was moving.
     * Sets all players to their target position if completeMovement is set.
     *
     * @param completeMovement Used to complete all player's movements
     * @return true if a player is/was moving, false if all players were done moving already
     */
    public boolean movementInProgress(boolean completeMovement) {
        boolean movementStatus = false;
        for (PlayerEntity player : players) {
            if (player.isMoving()) {
                movementStatus = true;
                if (completeMovement) {
                    player.completeMovement();
                }
            }
        }

        return movementStatus;
    }

    public void renderPlayerControlPrompt(ShapeDrawer shapeDrawer, float delta) {
        controlManager.renderPlayerControlPrompt(shapeDrawer, delta);
    }

    public static int getPlayerIndex(PlayerEntity[] players, String name) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].name.equals(name)) {
                return i;
            }
        }

        return -1;
    }

    public PlayerEntity getPlayer(int index) {
        return players[index];
    }

    public PlayerEntity[] getPlayers() {
        return players;
    }

    public Array<TeamData> getTeamData() {
        return teamData;
    }
}
