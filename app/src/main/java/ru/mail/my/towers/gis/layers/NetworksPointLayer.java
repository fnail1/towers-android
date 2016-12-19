package ru.mail.my.towers.gis.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;

import java.util.Locale;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gis.IMapEngine;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.gis.POI;
import ru.mail.my.towers.gis.ScreenDataObjects;
import ru.mail.my.towers.gis.ScreenProjection;
import ru.mail.my.towers.gis.TowersMap;
import ru.mail.my.towers.gis.geometry.NetworkPoint;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;

public class NetworksPointLayer extends PointLayer {
    private final int iconWidth;
    private NetworkPoint[] points = {};


    public NetworksPointLayer(Context context) {
        iconWidth = context.getResources().getDimensionPixelOffset(R.dimen.tower_icon_size);

    }

    @Override
    public void buildScreenData(IMapEngine engine, ScreenDataObjects dataObjects, ScreenProjection projection, int generation) {
        if (dataObjects.networks == null)
            return;

        int idx = 0;
        NetworkPoint[] points = new NetworkPoint[dataObjects.networks.size()];

        LongSparseArray<TowerNetwork> selection = engine.getSelectedNetworks();

        for (TowerNetwork network : dataObjects.networks) {
            if (selection.get(network._id) != null)
                continue;

            points[idx++] = buildNetworkPoint(engine, projection, network, 0xFF000000 | network.color, engine.getPrimaryTextPaint());
        }

        for (TowerNetwork network : dataObjects.networks) {
            if (selection.get(network._id) == null)
                continue;

            points[idx++] = buildNetworkPoint(engine, projection, network, 0xFFFDFF00, engine.getSelectionTextPaint());
        }
        this.points = points;
    }

    @NonNull
    public NetworkPoint buildNetworkPoint(IMapEngine engine, ScreenProjection projection, TowerNetwork network, int pointColor, TextPaint textPaint) {
        int x = projection.xi(network.lat, network.lng);
        int y = projection.yi(network.lat, network.lng);

        NetworkPoint symbol = new NetworkPoint();

        symbol.network = network;
        symbol.paint = engine.getPaint(pointColor);

        float level = Math.round((network.level + 1) * 100) / 100;
        symbol.text = String.format(Locale.getDefault(), "%d (%.2f)", network.count, level);
        symbol.textRect = new Rect();
        symbol.textPaint = textPaint;

        textPaint.getTextBounds(symbol.text, 0, symbol.text.length(), symbol.textRect);

        symbol.symbolRect = new Rect();
        int width = Math.max(symbol.textRect.width() + 2 * iconWidth / 3, iconWidth) / 2;
        int height = iconWidth / 2;
        symbol.symbolRect.left = x - width;
        symbol.symbolRect.top = y - height;
        symbol.symbolRect.right = x + width;
        symbol.symbolRect.bottom = y + height;
        symbol.hitArea = symbol.symbolRect;
        symbol.textLeft = symbol.symbolRect.left + (width - symbol.textRect.width() / 2) - symbol.textRect.left;
        symbol.textBottom = symbol.symbolRect.bottom - (height - symbol.textRect.height() / 2) - symbol.textRect.bottom;
        return symbol;
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (NetworkPoint point : points) {
            canvas.drawRect(point.symbolRect, point.paint);
            canvas.drawText(point.text, point.textLeft, point.textBottom, point.textPaint);
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.GeoRequestResult out) {
        for (NetworkPoint point : points) {
            if (point.hitTest(x, y)) {
                POI poi = new POI(point.hitArea.centerX(), point.hitArea.centerY(), point.hitArea.width() / 2);
                out.networks.put(point.network._id, poi);
            }
        }
    }

    @Override
    public void requestData(TowersMap towersMap, MapExtent mapExtent, ScreenDataObjects dataObjects) {
        if (dataObjects.networks == null) {
            dataObjects.networks = data().towers().selectNetworks(mapExtent);
        }
    }
}
