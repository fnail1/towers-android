package ru.mail.my.towers.gis.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gis.IMapEngine;
import ru.mail.my.towers.gis.MapExtent;
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
        symbol.levelText = String.valueOf(network.level + 1);
        symbol.levelRect = new Rect();
        symbol.levelTextPaint = textPaint;
        textPaint.getTextBounds(symbol.levelText, 0, symbol.levelText.length(), symbol.levelRect);
        symbol.iconRect = new Rect();
        int sz = iconWidth / 2;
        symbol.iconRect.left = x - sz;
        symbol.iconRect.top = y - sz;
        symbol.iconRect.right = x + sz;
        symbol.iconRect.bottom = y + sz;
        symbol.hitArea = symbol.iconRect;
        symbol.levelLeft = symbol.iconRect.left + (sz - symbol.levelRect.width() / 2) - symbol.levelRect.left;
        symbol.levelBottom = symbol.iconRect.bottom - (sz - symbol.levelRect.height() / 2) + symbol.levelRect.bottom;
        return symbol;
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (NetworkPoint point : points) {
            canvas.drawRect(point.iconRect, point.paint);
            canvas.drawText(point.levelText, point.levelLeft, point.levelBottom, point.levelTextPaint);
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.GeoRequestResult out) {
        for (NetworkPoint point : points) {
            if (point.hitTest(x, y)) {
                int dx = point.hitArea.centerX() - x;
                int dy = point.hitArea.centerY() - y;
                out.networks.put(point.network._id, Math.sqrt(dx * dx + dy * dy));
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
