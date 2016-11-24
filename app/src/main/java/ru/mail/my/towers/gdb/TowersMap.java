package ru.mail.my.towers.gdb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ru.mail.my.towers.R;
import ru.mail.my.towers.gdb.layers.TowerNetworksLayer;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;

import static ru.mail.my.towers.TowersApp.data;

public class TowersMap {
    private static final double SCALE_DETAILED = 5.0;
    private static final double SCALE_MIDDLE = 2.0;
    private static final TowerPoint[] TOWER_POINTS_EMPTY = new TowerPoint[]{};

    private final ExclusiveExecutor2 buildDataExecutor = new ExclusiveExecutor2(0, ThreadPool.SCHEDULER, this::prepareData);
    private final Point screenPointBuffer = new Point();
    private final LatLng[] mapPointsBuffer = new LatLng[4];
    private final TowerNetworksLayer towerNetworksLayer = new TowerNetworksLayer();
    private final Location locationBuffer1 = new Location("");
    private final Location locationBuffer2 = new Location("");
    private final SparseArray<Paint> paints = new SparseArray<>();
    private final TextPaint primaryTextPaint;
    private final int iconWidth;
    private final TowersMapReadyToDrawListener listener;
    private final Runnable callListenerTask = new Runnable() {
        @Override
        public void run() {
            projection = null;
            listener.onTowersMapReadyToDraw();
        }
    };

    private int screenWidth;
    private int screenHeight;
    private MapExtent mapExtent;
    private double scale;
    private Projection projection;
    private volatile int extentRequestCounter = 0;
    private volatile ScreenDrawObjects screenDrawObjects = new ScreenDrawObjects(new TowerPoint[]{}, new SparseArray<>());
    private volatile ScreenDataObjects screenDataObjects = new ScreenDataObjects(new ArrayList<TowerNetwork>(), new ArrayList<Tower>());


    public TowersMap(Context context, TowersMapReadyToDrawListener listener) {
        iconWidth = context.getResources().getDimensionPixelOffset(R.dimen.tower_icon_size);
        this.listener = listener;
        primaryTextPaint = new TextPaint();
        primaryTextPaint.setColor(0xffffffff);
        primaryTextPaint.setTextSize(48);
        primaryTextPaint.setTypeface(Typeface.DEFAULT);
        primaryTextPaint.setStyle(Paint.Style.STROKE);
    }


    public void onCameraMove(GoogleMap map) {
        extentRequestCounter++;

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
        mapExtent = new MapExtent(minLat, minLng, maxLat, maxLng);

        locationBuffer1.setLatitude(minLat);
        locationBuffer1.setLongitude(minLng);

        locationBuffer2.setLatitude(maxLat);
        locationBuffer2.setLongitude(maxLng);


        scale = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) / locationBuffer1.distanceTo(locationBuffer2);
//        Log.d("SCALE", "" + scale);

        this.projection = projection;
        buildDataExecutor.execute(false);
    }

    public void onResize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void onDraw(Canvas canvas) {
        ScreenDrawObjects drawObjects = this.screenDrawObjects;
        if (drawObjects == null)
            return;

        for (int i = 0; i < drawObjects.circles.size(); i++) {
            drawObjects.circles.valueAt(i).draw(canvas, this.screenWidth, this.screenHeight);
        }

        for (int i = 0; i < drawObjects.points.length; i++) {
            drawObjects.points[i].draw(canvas, this);
        }
    }

    private void prepareData() {
        prepareData(projection);
    }

    private void prepareData(Projection projection) {
        if (projection == null)
            return;

        int extentRequestNumber = extentRequestCounter;

        ScreenDrawObjects drawObjects = buildScreenData(projection);
        this.screenDrawObjects = drawObjects;

        if (extentRequestCounter != extentRequestNumber) {
            Log.d("TowersMap.Concurrent", "cancel 3");
            return;
        }

        ThreadPool.UI.post(callListenerTask);

        ScreenDataObjects dataObjects = new ScreenDataObjects();
        dataObjects.networks = data().towers().selectNetworks(mapExtent);
        if (extentRequestCounter != extentRequestNumber) {
            Log.d("TowersMap.Concurrent", "cancel 1");
            return;
        }

        dataObjects.towers = data().towers().select(dataObjects.networks);
        if (extentRequestCounter != extentRequestNumber) {
            Log.d("TowersMap.Concurrent", "cancel 2");
            return;
        }

        this.screenDataObjects = dataObjects;
        drawObjects = buildScreenData(projection);

        if (extentRequestCounter != extentRequestNumber) {
            Log.d("TowersMap.Concurrent", "cancel 3");
            return;
        }

        this.screenDrawObjects = drawObjects;
        ThreadPool.UI.post(callListenerTask);
    }

    @NonNull
    private ScreenDrawObjects buildScreenData(Projection projection) {
        ScreenDataObjects screenDataObjects = this.screenDataObjects;
        ArrayList<TowerNetwork> networks = screenDataObjects.networks;
        ArrayList<Tower> towers = screenDataObjects.towers;

        SparseArray<TowerCircle> circles = new SparseArray<>();
        TowerPoint[] points;
        if (scale > SCALE_DETAILED) {
            int idx = 0;
            points = new TowerPoint[towers.size()];
            for (Tower tower : towers) {
                LatLng latLng = new LatLng(tower.lat, tower.lng);
                TowerCircle circle = circles.get(tower.color);
                if (circle == null) {
                    circle = new TowerCircle(getCirclePaint(tower.color));
                    circles.put(tower.color, circle);
                }

                Point center = projection.toScreenLocation(latLng);
                circle.clipPath.addCircle((float) center.x,
                        (float) center.y,
                        (float) (tower.radius * scale),
                        Path.Direction.CCW);

                TowerPoint tp = new TowerPoint(this, center, tower, scale);
                points[idx++] = tp;
            }

        } else if (scale > SCALE_MIDDLE) {
            points = TOWER_POINTS_EMPTY;
            for (Tower tower : towers) {
                LatLng latLng = new LatLng(tower.lat, tower.lng);
                TowerCircle circle = circles.get(tower.color);
                if (circle == null) {
                    circle = new TowerCircle(getCirclePaint(tower.color));
                    circles.put(tower.color, circle);
                }

                Point center = projection.toScreenLocation(latLng);
                circle.clipPath.addCircle((float) center.x,
                        (float) center.y,
                        (float) (tower.radius * scale),
                        Path.Direction.CCW);

            }
        } else {
            int idx = 0;
            points = new TowerPoint[towers.size()];
            for (Tower tower : towers) {
                LatLng latLng = new LatLng(tower.lat, tower.lng);
                Point center = projection.toScreenLocation(latLng);
                TowerPoint tp = new TowerPoint(this, center, tower, scale);
                points[idx++] = tp;
            }
        }

        return new ScreenDrawObjects(points, circles);
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

        public void draw(Canvas canvas, int screenWidth, int screenHeight) {
            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            canvas.restore();
        }
    }

    public static class TowerPoint {
        private final int color;
        private final Tower tower;
        private final Rect rect;
        private final Rect iconRect;
        private final Rect levelRect;
        private final String levelText;
        int size = 1;
        private final int levelLeft;
        private final int levelBottom;

        public TowerPoint(TowersMap towersMap, Point center, Tower tower, double scale) {
            this.color = tower.color | 0xFF000000;
            this.tower = tower;
            int radius = (int) (tower.radius * scale);
            rect = new Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
            levelText = String.valueOf(tower.level + 1);
            levelRect = new Rect();
            towersMap.primaryTextPaint.getTextBounds(levelText, 0, levelText.length(), levelRect);
            iconRect = new Rect();
            int x = rect.centerX();
            int y = rect.centerY();
            int sz = (int) ((towersMap.iconWidth / 2) * (1 + (float) (size - 1) / 10));
            iconRect.left = x - sz;
            iconRect.top = y - sz;
            iconRect.right = x + sz;
            iconRect.bottom = y + sz;
            levelLeft = iconRect.left + (sz - levelRect.width() / 2) - levelRect.left;
            levelBottom = iconRect.bottom - (sz - levelRect.height() / 2) + levelRect.bottom;
        }

        public void draw(Canvas canvas, TowersMap towersMap) {
            canvas.drawRect(iconRect, towersMap.getIconPaint(color));
            canvas.drawText(levelText, levelLeft, levelBottom, towersMap.primaryTextPaint);
        }
    }

    private static class ScreenDrawObjects {
        public final TowerPoint[] points;
        public final SparseArray<TowerCircle> circles;

        private ScreenDrawObjects(TowerPoint[] points, SparseArray<TowerCircle> circles) {
            this.points = points;
            this.circles = circles;
        }
    }

    private static class ScreenDataObjects {
        ArrayList<TowerNetwork> networks;
        ArrayList<Tower> towers;

        public ScreenDataObjects(ArrayList<TowerNetwork> towerNetworks, ArrayList<Tower> towers) {
            networks = towerNetworks;
            this.towers = towers;
        }

        public ScreenDataObjects() {

        }
    }

    public interface TowersMapReadyToDrawListener {
        void onTowersMapReadyToDraw();
    }
}
