package com.cg.zoned;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.cg.zoned.dataobjects.PlayerColor;

import static com.cg.zoned.Constants.PLAYER_COLORS;

public final class PlayerColorHelper {
    public static void resetPlayerColorAlpha() {
        for (PlayerColor playerColor : PLAYER_COLORS) {
            playerColor.getColor().a = 1f;
        }
    }

    public static String getStringFromColor(Color color) {
        for (PlayerColor playerColor : PLAYER_COLORS) {
            if (playerColor.getColor().equals(color)) {
                return playerColor.getName();
            }
        }

        return null;
    }

    public static Color getColorFromIndex(int index) {
        return PLAYER_COLORS.get(index).getColor();
    }

    public static int getIndexFromColor(Color color) {
        for (int i = 0; i < PLAYER_COLORS.size(); i++) {
            if (PLAYER_COLORS.get(i).getColor().equals(color)) {
                return i;
            }
        }

        return -1;
    }

    public static String getStringFromIndex(int index) {
        return PLAYER_COLORS.get(index).getName();
    }

    public static Array<String> getNameList() {
        Array<String> nameList = new Array<>();
        for (PlayerColor playerColor : PLAYER_COLORS) {
            nameList.add(playerColor.getName());
        }

        return nameList;
    }
}
