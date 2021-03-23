package com.cg.zoned.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class PixmapFactory {
    public static Pixmap getRoundedCornerPixmap(Color color, int radius) {
        final int width = 10 + (radius * 2);
        final int height = 10 + (radius * 2);

        return getRoundedCornerPixmap(color, width, height, radius);
    }

    public static Pixmap getRoundedCornerPixmap(Color color, int width, int height, int radius) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius, radius, radius);
        pixmap.fillCircle(width - radius, height - radius, radius);
        pixmap.fillCircle(radius, height - radius, radius);
        pixmap.fillRectangle(0, radius, width, height - (radius * 2));
        pixmap.fillRectangle(radius, 0, width - (radius * 2), height);

        return pixmap;
    }
}
