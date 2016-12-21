package ru.mail.my.towers.gis.layers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gis.IMapEngine;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.gis.POI;
import ru.mail.my.towers.gis.ScreenDataObjects;
import ru.mail.my.towers.gis.ScreenProjection;
import ru.mail.my.towers.gis.TowersMap;
import ru.mail.my.towers.gis.geometry.TowerPoint;
import ru.mail.my.towers.model.Tower;

import static ru.mail.my.towers.TowersApp.data;

public class TowersPointLayer extends PointLayer {

    private TowerPoint[] points = {};

    private final int iconWidth;
    private final int hpOffset;
    private final int hpHalfWidth;
    private final int hpHeight;

    public TowersPointLayer(Context context) {
        Resources resources = context.getResources();
        iconWidth = resources.getDimensionPixelOffset(R.dimen.tower_icon_size);
        hpOffset = resources.getDimensionPixelOffset(R.dimen.tower_hp_offset);
        hpHalfWidth = resources.getDimensionPixelOffset(R.dimen.tower_hp_width) / 2;
        hpHeight = resources.getDimensionPixelOffset(R.dimen.tower_hp_height);
    }

    @Override
    public void buildScreenData(IMapEngine engine, ScreenDataObjects data, ScreenProjection projection, int generation) {
        if (data.towers == null)
            return;

        int idx = 0;
        TowerPoint[] points = new TowerPoint[data.towers.size()];
        LongSparseArray<Tower> selection = engine.getSelectedTowers();
        for (Tower tower : data.towers) {
            if (selection.get(tower._id) != null)
                continue;

            points[idx++] = buildTowerPoint(engine, tower, projection, 0xFF000000 | tower.color, engine.getPrimaryTextPaint());
        }

        for (Tower tower : data.towers) {
            if (selection.get(tower._id) == null)
                continue;

            points[idx++] = buildTowerPoint(engine, tower, projection, 0xFFFDFF00, engine.getSelectionTextPaint());
        }
        this.points = points;
    }

    @NonNull
    public TowerPoint buildTowerPoint(IMapEngine engine, Tower tower, ScreenProjection projection, int pointColor, TextPaint textPaint) {
        int x = projection.xi(tower.lat, tower.lng);
        int y = projection.yi(tower.lat, tower.lng);

        TowerPoint tp = new TowerPoint();
        tp.tower = tower;
        tp.paint = engine.getPaint(pointColor);
        tp.levelText = String.valueOf(tower.level + 1);
        tp.levelRect = new Rect();
        tp.levelTextPaint = textPaint;
        textPaint.getTextBounds(tp.levelText, 0, tp.levelText.length(), tp.levelRect);
        int sz = iconWidth / 2;
        tp.iconRect = new Rect(x - sz, y - sz, x + sz, y + sz);
        tp.hitArea = tp.iconRect;
        tp.levelLeft = tp.iconRect.left + (sz - tp.levelRect.width() / 2) - tp.levelRect.left;
        tp.levelBottom = tp.iconRect.bottom - (sz - tp.levelRect.height() / 2) + tp.levelRect.bottom;

        int hpTop = y + sz + hpOffset;
        int hpBottom = y + sz + hpOffset + hpHeight;
        tp.hpTotalRect = new Rect(x - hpHalfWidth, hpTop, x + hpHalfWidth, hpBottom);
        tp.hpTotalPaint = engine.getPaint(0xff000000);
        float health = (float) tower.health / tower.maxHealth;
        int cut = (int) (hpHalfWidth * 2 * (1 - health));
        tp.hpCurrentRect = new Rect(x - hpHalfWidth, hpTop, x + hpHalfWidth - cut, hpBottom);
        tp.hpCurrentPaint = engine.getPaint(calcHealthColor(health));
        return tp;
    }

    private int calcHealthColor(float health) {
        int red;
        int green;
        if (health > 0.5F) {
            float a = (health - .5F) * 2;
            red = (int) (0xff * (1 - a)) << 16;
            green = (int) (0xff * a) << 8;
        } else if (health > .25F) {
            float a = (health - .25F) * 4;
            red = 0xff << 16;
            green = (int) (0xff * a) << 8;
        } else {
            float a = health * 4;
            red = ((int) (0xff * a)) << 16;
            green = 0;
        }
        return 0xff000000 + red + green;
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (TowerPoint point : points) {
            canvas.drawRect(point.iconRect, point.paint);
            canvas.drawText(point.levelText, point.levelLeft, point.levelBottom, point.levelTextPaint);
            canvas.drawRect(point.hpTotalRect, point.hpTotalPaint);
            canvas.drawRect(point.hpCurrentRect, point.hpCurrentPaint);
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.GeoRequestResult out) {
        for (TowerPoint point : points) {
            if (point.hitTest(x, y)) {
                int dx = point.hitArea.centerX() - x;
                int dy = point.hitArea.centerY() - y;
                double d = Math.sqrt(dx * dx + dy * dy);
                POI poi = new POI(point.hitArea.centerX(), point.hitArea.centerY(), point.hitArea.width() / 2);
                out.towers.put(point.tower._id, poi);
                out.networks.put(point.tower.network, poi);
            }
        }
    }

    @Override
    public void requestData(TowersMap towersMap, MapExtent mapExtent, ScreenDataObjects dataObjects) {
        if (dataObjects.networks == null) {
            dataObjects.networks = data().networks().select(mapExtent);
        }

        if (dataObjects.towers == null) {
            dataObjects.towers = data().towers().select(dataObjects.networks);
        }
    }
}
