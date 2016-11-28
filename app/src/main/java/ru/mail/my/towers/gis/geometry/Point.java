package ru.mail.my.towers.gis.geometry;

import android.graphics.Rect;

public class Point extends Geometry {
    public Rect hitArea;
    @Override
    public boolean hitTest(int x, int y) {
        return hitArea.contains(x, y);
    }
}
