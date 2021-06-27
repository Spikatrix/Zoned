package com.cg.zoned.dataobjects;

import com.badlogic.gdx.math.GridPoint2;

public class GameTouchPoint {
    public GridPoint2 point;
    public int pointer;

    public GameTouchPoint() {
        this(-1, -1);
    }

    public GameTouchPoint(int x, int y) {
        this(x, y, -1);
    }

    public GameTouchPoint(int x, int y, int pointer) {
        this.point = new GridPoint2(x, y);
        this.pointer = pointer;
    }
}
