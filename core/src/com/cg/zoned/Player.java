package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

public class Player extends PlayerEntity {

    public Player(Color color, String name) {
        super(color, name);
    }

    public Player(Player player) {
        super(player);
    }
}
