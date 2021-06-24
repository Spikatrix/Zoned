package com.cg.zoned.managers;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.Map;
import com.cg.zoned.Player;
import com.cg.zoned.buffers.BufferDirections;

public class ClientPredictionHandler {
    // The maximum number of predictions to be performed
    private final int MAX_PREDICTIONS = 5;

    // These fields are required for processing predictions
    private final Player[] players;
    private final DirectionBufferManager directionBufferManager;
    private final PlayerManager playerManager;

    // Used by clients to store predicted directions
    private Array<Player.Direction[]> clientPredictedLog;

    // Used by clients to store the last verified player positions
    private GridPoint2[] verifiedPlayerPos;

    // Used by clients to store the last verified previous player positions
    private GridPoint2[] verifiedPrevPlayerPos;

    public ClientPredictionHandler(Player[] players,
                                   DirectionBufferManager directionBufferManager, PlayerManager playerManager) {
        this.players = players;
        this.directionBufferManager = directionBufferManager;
        this.playerManager = playerManager;

        clientPredictedLog = new Array<>();
        initClientPredictionArray();
    }

    private void initClientPredictionArray() {
        verifiedPlayerPos = new GridPoint2[players.length];
        verifiedPrevPlayerPos = new GridPoint2[players.length];
        for (int i = 0; i < players.length; i++) {
            verifiedPlayerPos[i] = players[i].getRoundedPosition();
            verifiedPrevPlayerPos[i] = players[i].getPreviousPosition();
        }
    }

    public void verifyClientPredictions(Array<BufferDirections> clientDirectionBacklog, Map map) {
        for (int i = 0; i < clientPredictedLog.size; i++) {
            if (i >= clientDirectionBacklog.size) {
                // We're running on predictions, the server hasn't sent the actual directions yet
                break;
            }

            Player.Direction[] predictedDirections = clientPredictedLog.get(i);

            // Check if the predicted direction is correct or not
            if (predictedDirectionIsCorrect(predictedDirections, clientDirectionBacklog.get(i))) {
                // Prediction was right, hooray!
                updateMapColors(map, predictedDirections);
                clientPredictedLog.removeIndex(i);
                clientDirectionBacklog.removeIndex(i--);
            } else {
                // Uh oh, we got an incorrect prediction
                clientPredictedLog.clear();
                correctClient();
                break;
            }
        }

    }

    private boolean predictedDirectionIsCorrect(Player.Direction[] predictedDirections, BufferDirections clientDirectionBacklog) {
        int size = predictedDirections.length;
        for (int i = 0; i < size; i++) {
            int playerIndex = PlayerManager.getPlayerIndex(players, clientDirectionBacklog.playerNames[i]);
            if (predictedDirections[playerIndex] != clientDirectionBacklog.directions[i]) {
                return false;
            }
        }

        return true;
    }

    public void updateMapColors(Map map, Player.Direction[] directions) {
        for (int playerIndex = 0; playerIndex < players.length; playerIndex++) {
            updateVerifiedPlayerPosition(playerIndex, directions[playerIndex], map);
        }
        updateMap(map);
    }

    public void applyPrediction() {
        Player.Direction[] directions = directionBufferManager.getDirectionBuffer().clone();

        clientPredictedLog.add(directions);
        playerManager.setPlayerDirections(directions);
    }

    private void correctClient() {
        for (int i = 0; i < players.length; i++) {
            players[i].completeMovement();

            // Done this way so that the player's previous position is preserved
            players[i].setPosition(verifiedPrevPlayerPos[i]);
            players[i].setPosition(verifiedPlayerPos[i]);
        }
    }

    public void updateVerifiedPlayerPosition(int playerIndex, Player.Direction direction, Map map) {
        verifiedPrevPlayerPos[playerIndex].set(verifiedPlayerPos[playerIndex]);
        if (map.isValidMovement(verifiedPlayerPos[playerIndex], direction)) {
            Player.updateTargetPosition(verifiedPlayerPos[playerIndex], direction);
        }
    }

    public void updateMap(Map map) {
        map.updateMap(players, playerManager, verifiedPrevPlayerPos, verifiedPlayerPos);
    }

    public boolean reachedPredictionLimit() {
        return clientPredictedLog.size == MAX_PREDICTIONS;
    }

    public int getSize() {
        return clientPredictedLog.size;
    }
}
