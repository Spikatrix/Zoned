package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;

import java.util.Locale;
import java.util.Map;

public final class PlayerColorHelper {
    public static void resetPlayerColorAlpha() {
        for (Map.Entry<String, Color> entry : Constants.PLAYER_COLORS.entrySet()) {
            entry.getValue().a = 1f;
        }
    }

    public static Color getColorFromString(String color) {
        return Constants.PLAYER_COLORS.get(color.toUpperCase(Locale.ENGLISH));
    }

    public static String getStringFromColor(Color color) {
        for (Map.Entry<String, Color> entry : Constants.PLAYER_COLORS.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
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
