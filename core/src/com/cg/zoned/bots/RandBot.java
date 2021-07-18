package com.cg.zoned.bots;

import com.cg.zoned.Player;
import com.cg.zoned.dataobjects.Cell;

import java.util.Random;

/**
 * A sample bot that returns a random direction
 */
public class RandBot extends BotEntity {
    private final Random random;

    public RandBot() {
        random = new Random();
    }

    @Override
    public Player.Direction processTurn(Cell[][] mapGrid, Player[] players) {
        return availableDirections[random.nextInt(availableDirections.length)];
    }

    @Override
    public String getName() {
        return "RandBot";
    }
}
