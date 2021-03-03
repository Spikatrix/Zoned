package com.cg.zoned.dataobjects;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class GameTouchPoint {
    public GridPoint2 point;
    public int pointer;
    public Image clickImage;
    public int playerIndex;

    public GameTouchPoint(int x, int y, int pointer, Image clickImage, int playerIndex) {
        this.point = new GridPoint2(x, y);
        this.pointer = pointer;
        this.clickImage = clickImage;
        this.playerIndex = playerIndex;
    }
}
