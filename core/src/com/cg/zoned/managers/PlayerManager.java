package com.cg.zoned.managers;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Sort;
import com.cg.zoned.Constants.Direction;
import com.cg.zoned.Player;
import com.cg.zoned.TeamData;

import java.util.Comparator;

public class PlayerManager extends InputMultiplexer {
    private final GameManager gameManager;

    private ControlManager controlManager;

    private Player[] players;

    private Array<TeamData> teamData;

    public PlayerManager(GameManager gameManager, Player[] players, Stage stage, int controls) {
        this.gameManager = gameManager;

        this.players = players;

        this.teamData = new Array<TeamData>();
        initTeamColors();

        if (gameManager.connectionManager.isActive) { // Not split screen; only add first player's inputs
            this.addProcessor(players[0]);
        } else {
            for (Player player : players) {
                this.addProcessor(player);
            }
        }

        controlManager = new ControlManager(players, !gameManager.connectionManager.isActive, stage, controls);
        this.addProcessor(controlManager.getControls());
    }

    private void initTeamColors() {
        for (Player player : players) {
            boolean alreadyExists = false;
            for (TeamData teamData : this.teamData) {
                if (player.color.equals(teamData.color)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                teamData.add(new TeamData(player.color, player.score));
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

    public void incrementScore(Player player) {
        player.score++;
        for (TeamData teamData : this.teamData) {
            if (player.color.equals(teamData.color)) {
                teamData.score++;
                return;
            }
        }
    }

    public Player getPlayer(int index) {
        return players[index];
    }

    public Player[] getPlayers() {
        return players;
    }

    public Array<TeamData> getTeamData() {
        new Sort().sort(teamData, new TeamDataComparator());
        return teamData;
    }

    public void resize() {
        controlManager.resize();
    }

    public void renderPlayerControlPrompt(ShapeRenderer renderer, float delta) {
        controlManager.renderPlayerControlPrompt(renderer, delta);
    }

    private static class TeamDataComparator implements Comparator<TeamData> {
        @Override
        public int compare(TeamData t1, TeamData t2) {
            return t2.score - t1.score;
        }
    }
}
