package ru.mail.my.towers.gis.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.util.LongSparseArray;
import android.util.SparseArray;

import java.util.ArrayList;

import ru.mail.my.towers.gis.IMapEngine;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.gis.POI;
import ru.mail.my.towers.gis.ScreenDataObjects;
import ru.mail.my.towers.gis.ScreenProjection;
import ru.mail.my.towers.gis.TowersMap;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;

public class TowersPolygonLayer extends PolygonLayer {
    private SparseArray<TowerCircle> circles = new SparseArray<>();
    private ScreenProjection projection;

    public TowersPolygonLayer(Context context) {

    }

    @Override
    public void buildScreenData(IMapEngine engine, ScreenDataObjects dataObjects, ScreenProjection projection, int generation) {
        if (dataObjects.towers == null)
            return;

        this.projection = projection;
//        LongSparseArray<TowerNetwork> selectedNetworks = engine.getSelectedNetworks();
        SparseArray<TowerCircle> circles = new SparseArray<>();


        for (Tower tower : dataObjects.towers) {
            int x = projection.xi(tower.lat, tower.lng);
            int y = projection.yi(tower.lat, tower.lng);
            float radius = (float) (tower.radius * projection.scale);
//            boolean selectedNetwork = selectedNetworks.get(tower.network) != null;
            int circleColor = 0x66000000 + (tower.color & 0x00ffffff);

            TowerCircle circle = circles.get(circleColor);
            if (circle == null) {
                circle = new TowerCircle(engine.getPaint(circleColor));
                circles.put(circleColor, circle);
            }
            circle.clipPath.addCircle((float) x, (float) y, radius, Path.Direction.CCW);
            circle.meta.add(new CircleDescriptor(x, y, radius, tower));
        }

        synchronized (this) {
            this.circles = circles;
        }
    }

    @Override
    public void draw(IMapEngine engine, Canvas canvas) {
        for (int i = 0; i < circles.size(); i++) {
            TowerCircle circle = circles.valueAt(i);
            canvas.save();
            canvas.clipPath(circle.clipPath);
//            canvas.drawRect(0, 0, (float) projection.screenWidth, (float) projection.screenHeight, circle.paint);
            canvas.drawColor(circle.paint.getColor());
            canvas.restore();
        }
    }

    @Override
    public void requestObjectsAt(IMapEngine engine, int x, int y, TowersMap.GeoRequestResult out) {
        synchronized (this) {
            for (int i = 0; i < circles.size(); i++) {
                TowerCircle circle = circles.valueAt(i);
                for (CircleDescriptor c : circle.meta) {
                    int dx = c.x - x;
                    int dy = c.y - y;
                    int d2 = dx * dx + dy * dy;
                    if (d2 < c.r * c.r) {
                        POI poi = new POI(c.x, c.y, c.r);
                        out.towers.put(c.tower._id, poi);
                        out.networks.put(c.tower.network, poi);
                    }
                }
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

    public static class TowerCircle {
        public final Path clipPath;
        public final Paint paint;
        public final ArrayList<CircleDescriptor> meta = new ArrayList<>();

        public TowerCircle(Paint paint) {
            clipPath = new Path();
            this.paint = paint;
        }
    }

    public static class CircleDescriptor {
        public final int x, y;
        public final float r;
        public final Tower tower;

        public CircleDescriptor(int x, int y, float r, Tower tower) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.tower = tower;
        }
    }
}
