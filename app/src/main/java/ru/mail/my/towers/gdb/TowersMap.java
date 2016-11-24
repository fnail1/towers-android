package ru.mail.my.towers.gdb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gdb.layers.TowerNetworksLayer;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;

public class TowersMap {
    private static final double SCALE_DETAILED = 5.0;
    private static final double SCALE_MIDDLE = 2.0;

    private final Point screenPointBuffer = new Point();
    private final LatLng[] mapPointsBuffer = new LatLng[4];
    private final TowerNetworksLayer towerNetworksLayer = new TowerNetworksLayer();
    private final Location locationBuffer1 = new Location("");
    private final Location locationBuffer2 = new Location("");
    private final SparseArray<Paint> paints = new SparseArray<>();
    private final int iconWidth;

    private int screenWidth;
    private int screenHeight;
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;
    private double scale;
    private TowerPoint[] points = {};
    private SparseArray<TowerCircle> circles = new SparseArray<>();

    public TowersMap(Context context) {
        iconWidth = context.getResources().getDimensionPixelOffset(R.dimen.tower_icon_size);
    }


    public void onCameraMove(GoogleMap map) {
        Projection projection = map.getProjection();
        screenPointBuffer.set(0, 0);
        mapPointsBuffer[0] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(screenWidth, 0);
        mapPointsBuffer[1] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(0, screenHeight);
        mapPointsBuffer[2] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(screenWidth, screenHeight);
        mapPointsBuffer[3] = projection.fromScreenLocation(screenPointBuffer);

        LatLng latLng = mapPointsBuffer[0];
        double minLat = latLng.latitude;
        double maxLat = latLng.latitude;
        double minLng = latLng.longitude;
        double maxLng = latLng.longitude;
        for (int i = 1; i < mapPointsBuffer.length; i++) {
            latLng = mapPointsBuffer[i];
            if (minLat > latLng.latitude)
                minLat = latLng.latitude;
            else if (maxLat < latLng.latitude)
                maxLat = latLng.latitude;

            if (minLng > latLng.longitude)
                minLng = latLng.longitude;
            else if (maxLng < latLng.longitude)
                maxLng = latLng.longitude;
        }
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLng = minLng;
        this.maxLng = maxLng;

        locationBuffer1.setLatitude(minLat);
        locationBuffer1.setLongitude(minLng);

        locationBuffer2.setLatitude(maxLat);
        locationBuffer2.setLongitude(maxLng);


        scale = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) / locationBuffer1.distanceTo(locationBuffer2);

        prepareData(projection);
    }

    public void onResize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void onDraw(Canvas canvas) {
        for (int i = 0; i < circles.size(); i++) {
            TowersMap.TowerCircle circle = circles.valueAt(i);
            canvas.save();
            canvas.clipPath(circle.clipPath);
            canvas.drawRect(0, 0, screenWidth, screenHeight, circle.paint);
            canvas.restore();
        }


        for (int i = 0; i < points.length; i++) {
            TowerPoint tp = points[i];
            int x = tp.rect.centerX();
            int y = tp.rect.centerY();
            int sz = (int) ((iconWidth / 2) * (1 + (float) (tp.size - 1) / 10));
            int left = x - sz;
            int top = y - sz;
            int right = x + sz;
            int bottom = y + sz;

            canvas.drawRect(left, top, right, bottom, getIconPaint(tp.color));

        }

    }

    private void prepareData(Projection projection) {
        ArrayList<TowerNetwork> networks = data().towers().selectNetworks(minLat, minLng, maxLat, maxLng);
        ArrayList<Tower> towers = data().towers().select(networks);
        SparseArray<TowerCircle> features = new SparseArray<>();
        TowersMap.TowerPoint[] points = new TowersMap.TowerPoint[towers.size()];
        if (scale > SCALE_DETAILED) {
            int idx = 0;
            for (Tower tower : towers) {
                LatLng latLng = new LatLng(tower.lat, tower.lng);
                TowerCircle circle = features.get(tower.color);
                if (circle == null) {
                    circle = new TowerCircle(getCirclePaint(tower.color));
                    features.put(tower.color, circle);
                }

                Point center = projection.toScreenLocation(latLng);
                circle.clipPath.addCircle((float) center.x,
                        (float) center.y,
                        (float) (tower.radius * scale),
                        Path.Direction.CCW);

                TowerPoint tp = new TowerPoint(center, tower, scale);
                points[idx++] = tp;
            }
            this.circles = features;

        } else if (scale > SCALE_MIDDLE) {

        } else {

        }
    }

    private Paint getIconPaint(int color) {
        return getPaint(0xFF000000 | color);

    }

    private Paint getCirclePaint(int color) {
        return getPaint(0x66000000 + (color & 0x00ffffff));
    }

    private Paint getPaint(int color) {
        Paint paint = paints.get(color);
        if (paint == null) {
            paints.put(color, paint = new Paint());
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }
        return paint;
    }


    public static class TowerCircle {
        public final Path clipPath;
        public final Paint paint;

        public TowerCircle(Paint paint) {
            clipPath = new Path();
            this.paint = paint;


        }
    }

    public static class TowerPoint {
        private final int color;
        private final Tower tower;
        private final Rect rect;
        int size = 1;

        public TowerPoint(Point center, Tower tower, double scale) {
            this.color = tower.color | 0xFF000000;
            this.tower = tower;
            int radius = (int) (tower.radius * scale);
            rect = new Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
        }
    }
}
