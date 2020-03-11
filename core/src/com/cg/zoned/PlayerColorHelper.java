package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.cg.zoned.Constants;

import java.util.Map;

public final class PlayerColorHelper {
    public static Color getColorFromString(String color) {
        return Constants.PLAYER_COLORS.get(color.toUpperCase());
    }

    public static String getStringFromColor(Color color) {
        for (Map.Entry<String, Color> entry : Constants.PLAYER_COLORS.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static Color getConstantColor(Color color) {
        for (String key : Constants.PLAYER_COLORS.keySet()) {
            if (Constants.PLAYER_COLORS.get(key).equals(color)) {
                return Constants.PLAYER_COLORS.get(key);
            }
        }

        return null;
    }

    public static Color getColorFromIndex(int index) {
        int currentPos = 0;
        for (String key : Constants.PLAYER_COLORS.keySet()) {
            if (currentPos == index) {
                return Constants.PLAYER_COLORS.get(key);
            }
            currentPos++;
        }

        return null;
    }
}
