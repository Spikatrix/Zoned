package com.cg.zoned;

import com.badlogic.gdx.math.GridPoint2;

public class ClickPoint {
    public GridPoint2 point;
    public int pointer;

    public ClickPoint(int x, int y, int pointer) {
        this.point = new GridPoint2(x, y);
        this.pointer = pointer;
    }
}
