package com.cg.zoned.managers;

import com.cg.zoned.Player.Direction;

import java.util.Arrays;

public class DirectionBufferManager {
    private Direction[] directionBuffer; // Used to store the directions of all players. Only when it is full will the game start
    private int directionBufferUsed;
    private int ignoredPlayerCount;

    public DirectionBufferManager(int playerCount) {
        directionBuffer = new Direction[playerCount];
        directionBufferUsed = 0;
        ignoredPlayerCount = 0;
    }

    public void updateDirectionBuffer(Direction direction, int index) {
        if (directionBuffer[index] == null && direction != null) {
            directionBufferUsed++;
        } else if (directionBuffer[index] != null && direction == null) {
            directionBufferUsed--;
        }
        directionBuffer[index] = direction;
    }

    public void updateDirections(Direction[] directions) {
        clearBuffer();
        for (int i = 0; i < directions.length; i++) {
            updateDirectionBuffer(directions[i], i);
        }
    }

    public Direction getDirection(int index) {
        return directionBuffer[index];
    }

    public Direction[] getDirectionBuffer() {
        return directionBuffer;
    }

    public int getBufferUsedCount() {
        return directionBufferUsed + ignoredPlayerCount;
    }

    public void ignorePlayer() {
        clearBuffer();
        ignoredPlayerCount++;
    }

    public void clearBuffer() {
        Arrays.fill(directionBuffer, null);
        directionBufferUsed = 0;
    }
}
