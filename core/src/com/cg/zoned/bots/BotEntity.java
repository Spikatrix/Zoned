package com.cg.zoned.bots;

import com.badlogic.gdx.graphics.Color;
import com.cg.zoned.Player;
import com.cg.zoned.PlayerEntity;
import com.cg.zoned.dataobjects.Cell;

/**
 * All bots are required to extend this class
 */
public abstract class BotEntity extends PlayerEntity {

    public BotEntity(Color color, String name) {
        super(color, name);
    }

    /**
     * The available directions to move to. Bots can use this if needed
     */
    Player.Direction[] availableDirections = Player.Direction.values();

    /**
     * Called in every turn to receive the bot's direction for that turn
     * Note that there is a time limit within which the bot must return the turn.
     * The previous direction will be used if the bot goes over the time limit,
     *
     * @param mapGrid The 2D array of map cells which the bot can use to analyze the map
     * @param players The list of players which the bot can use to where they are on the map etc
     * @return The direction that the bot decides to move to
     *         The return value must not be null
     */
    abstract public Player.Direction processTurn(Cell[][] mapGrid, Player[] players);

    /**
     * Used to display the bots' name.
     *
     * @return The name of the bot
     */
    abstract public String getName();
}
