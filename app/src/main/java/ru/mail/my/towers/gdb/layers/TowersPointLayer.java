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
import ru.mail.my.towers.gdb.geometry.TowerPoint;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;

public class TowersPointLayer extends PointLayer {

    private TowerPoint[] points = {};

    private final int iconWidth;

    public TowersPointLayer(Context context) {
        iconWidth = context.getResources().getDimensionPixelOffset(R.dimen.tower_icon_size);
    }

    @Override
    public void buildScreenData(IMapEngine engine, ScreenDataObjects data, ScreenProjection projection, int generation) {
        if (data.towers == null)
            return;

        int idx = 0;
        TowerPoint[] points = new TowerPoint[data.towers.size()];
        LongSparseArray<Tower> selection = engine.getSelectedTowers();
        for (Tower tower : data.towers) {
            int x = projection.xi(tower.lat, tower.lng);
            int y = projection.yi(tower.lat, tower.lng);

            boolean selected = selection.get(tower._id) != null;

            int pointColor;
            TextPaint textPaint;
            if (selected) {
                pointColor = 0xFFFDFF00;
                textPaint = engine.getSelectionTextPaint();
            } else {
                pointColor = 0xFF000000 | tower.color;
                textPaint = engine.getPrimaryTextPaint();
            }


            TowerPoint tp = new TowerPoint();
            tp.tower = tower;
            tp.paint = engine.getPaint(pointColor);
            tp.levelText = String.valueOf(tower.level + 1);
            tp.levelRect = new Rect();
            tp.levelTextPaint = textPaint;
            textPaint.getTextBounds(tp.levelText, 0, tp.levelText.length(), tp.levelRect);
            tp.iconRect = new Rect();
            int sz = iconWidth / 2;
            tp.iconRect.left = x - sz;
            tp.iconRect.top = y - sz;
            tp.iconRect.right = x + sz;
            tp.iconRect.bottom = y + sz;
            tp.hitArea = tp.iconRect;
            tp.levelLeft = tp.iconRect.left + (sz - tp.levelRect.width() / 2) - tp.levelRect.left;
            tp.levelBottom = tp.iconRect.bottom - (sz - tp.levelRect.height() / 2) + tp.levelRect.bottom;

            points[idx++] = tp;
        }
        this.points = points;
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (TowerPoint point : points) {
            canvas.drawRect(point.iconRect, point.paint);
            canvas.drawText(point.levelText, point.levelLeft, point.levelBottom, point.levelTextPaint);
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.MapObjectsSet out) {
        for (TowerPoint point : points) {
            if (point.hitTest(x, y)) {
                out.towers.put(point.tower._id, point.tower);
                out.networks.put(point.tower.network, TowerNetwork.FAKE_INSTANCE);
            }
        }
    }

    @Override
    public void requestData(TowersMap towersMap, MapExtent mapExtent, ScreenDataObjects dataObjects) {
        if (dataObjects.networks == null) {
            dataObjects.networks = data().towers().selectNetworks(mapExtent);
        }

        if (dataObjects.towers == null) {
            dataObjects.towers = data().towers().select(dataObjects.networks);
        }
    }
}
