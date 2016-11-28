package ru.mail.my.towers.gdb.layers;

import android.graphics.Canvas;
import android.util.LongSparseArray;

import java.util.ArrayList;

import ru.mail.my.towers.gdb.IMapEngine;
import ru.mail.my.towers.gdb.MapExtent;
import ru.mail.my.towers.gdb.ScreenDataObjects;
import ru.mail.my.towers.gdb.ScreenProjection;
import ru.mail.my.towers.gdb.TowersMap;

public abstract class Layer {
    public double minVisibleScale;
    public double maxVisibleScale;

    public boolean isVisible(double scale) {
        return minVisibleScale <= scale && scale < maxVisibleScale;
    }

    public abstract void buildScreenData(IMapEngine engine, ScreenDataObjects dataObjects, ScreenProjection projection, int generation);

    public abstract void draw(IMapEngine engine, Canvas canvas);

    public abstract void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.MapObjectsSet out);

    public abstract void requestData(TowersMap towersMap, MapExtent mapExtent, ScreenDataObjects screenDataObjects);
}
