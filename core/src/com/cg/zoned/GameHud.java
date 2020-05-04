package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

// TODO: Do something with this poor lil unused and forgotten class lol
public class GameHud {
    public static Color getGoodTextColor(Color bgColor) {
        int o = MathUtils.round(((bgColor.r * 299) +
                (bgColor.g * 587) +
                (bgColor.b * 114)) / 1000);

        if (o > 125) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }
}
