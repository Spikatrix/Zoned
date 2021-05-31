package com.cg.zoned.dataobjects;

import com.badlogic.gdx.graphics.Color;

public class PlayerColor {
    final String name;
    final Color color;
    
    public PlayerColor(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
