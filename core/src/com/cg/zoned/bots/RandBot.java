package com.cg.zoned.bots;

import com.badlogic.gdx.graphics.Color;
import com.cg.zoned.Player;
import com.cg.zoned.dataobjects.Cell;

import java.util.Random;

/**
 * A sample bot that returns a random direction
 */
public class RandBot extends BotEntity {
    private static final String name = "RandBot";

    private final Random random;

    public RandBot(Color color) {
        super(color, name);
        random = new Random();
    }

    @Override
    public Player.Direction processTurn(Cell[][] mapGrid, Player[] players) {
        return availableDirections[random.nextInt(availableDirections.length)];
    }

    @Override
    public String getName() {
        return name;
    }
}
