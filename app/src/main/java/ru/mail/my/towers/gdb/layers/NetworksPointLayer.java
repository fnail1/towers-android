package ru.mail.my.towers.gdb.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gdb.IMapEngine;
import ru.mail.my.towers.gdb.MapExtent;
import ru.mail.my.towers.gdb.ScreenDataObjects;
import ru.mail.my.towers.gdb.ScreenProjection;
import ru.mail.my.towers.gdb.TowersMap;
import ru.mail.my.towers.gdb.geometry.NetworkPoint;
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
            int x = projection.xi(network.lat, network.lng);
            int y = projection.yi(network.lat, network.lng);

            boolean selected = selection.get(network._id) != null;
            int pointColor;
            TextPaint textPaint;
            if (selected) {
                pointColor = 0xFFFDFF00;
                textPaint = engine.getSelectionTextPaint();
            } else {
                pointColor = 0xFF000000 | network.color;
                textPaint = engine.getPrimaryTextPaint();
            }


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

            points[idx++] = symbol;
        }
        this.points = points;
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (NetworkPoint point : points) {
            canvas.drawRect(point.iconRect, point.paint);
            canvas.drawText(point.levelText, point.levelLeft, point.levelBottom, point.levelTextPaint);
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.MapObjectsSet out) {
        for (NetworkPoint point : points) {
            if (point.hitTest(x, y)) {
                out.networks.put(point.network._id, point.network);
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
