package ru.mail.my.towers.gis.layers;

import android.graphics.Canvas;

import ru.mail.my.towers.gis.IMapEngine;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.gis.ScreenDataObjects;
import ru.mail.my.towers.gis.ScreenProjection;
import ru.mail.my.towers.gis.TowersMap;

public abstract class Layer {
    public double minVisibleScale;
    public double maxVisibleScale;

    public boolean isVisible(double scale) {
        return minVisibleScale <= scale && scale < maxVisibleScale;
    }

    public abstract void buildScreenData(IMapEngine engine, ScreenDataObjects dataObjects, ScreenProjection projection, int generation);

    public abstract void draw(IMapEngine engine, Canvas canvas);

    public abstract void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.GeoRequestResult out);

    public abstract void requestData(TowersMap towersMap, MapExtent mapExtent, ScreenDataObjects screenDataObjects);
}
