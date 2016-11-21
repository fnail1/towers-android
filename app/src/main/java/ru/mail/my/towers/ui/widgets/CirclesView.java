package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;

import ru.mail.my.towers.R;
import ru.mail.my.towers.model.Tower;


public class CirclesView extends View {

    private int iconWidth;
    private int iconHeight;

    private static Drawable loadIcon(Context context, int id) {
        Drawable icon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            icon = context.getDrawable(id);
        } else {
            icon = context.getResources().getDrawable(id);
        }
        return icon;
    }

    private final SparseArray<TowerCircle> features = new SparseArray<>();
    private final SparseArray<Paint> paints = new SparseArray<>();
    private final Point point = new Point();
    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Drawable icon;
    private TowerPoint points[] = {};


    public CirclesView(Context context) {
        super(context);
        icon = loadIcon(context, R.drawable.ic_tower);
        iconWidth = icon.getIntrinsicWidth();
        iconHeight = icon.getIntrinsicHeight();
    }


    public CirclesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        icon = loadIcon(context, R.drawable.ic_tower);
        iconWidth = icon.getIntrinsicWidth();
        iconHeight = icon.getIntrinsicHeight();
    }

    public CirclesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        icon = loadIcon(context, R.drawable.ic_tower);
        iconWidth = icon.getIntrinsicWidth();
        iconHeight = icon.getIntrinsicHeight();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CirclesView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        icon = loadIcon(context, R.drawable.ic_tower);
        iconWidth = icon.getIntrinsicWidth();
        iconHeight = icon.getIntrinsicHeight();
    }

    /**
     * Готовит данные по башням для отрисовки
     *
     * @param map    обект GoogleMap
     * @param towers коллекция башен, для отрисовки
     */
    public void onCameraMove(GoogleMap map, Tower[] towers) {
        Projection projection = map.getProjection();

        point.set(0, 0);
        LatLng latLng = projection.fromScreenLocation(point);
        leftTopCorner.setLongitude(latLng.longitude);
        leftTopCorner.setLatitude(latLng.latitude);

        int width = getWidth();
        int height = getHeight();
        point.set(width, height);
        latLng = projection.fromScreenLocation(point);
        rightBottomCorner.setLongitude(latLng.longitude);
        rightBottomCorner.setLatitude(latLng.latitude);

        double scale = Math.sqrt(width * width + height * height) / leftTopCorner.distanceTo(rightBottomCorner);

        features.clear();

        TowerPoint[] points = new TowerPoint[towers.length];
        int idx = 0;


        if (scale > 2) {
            for (Tower tower : towers) {
                latLng = new LatLng(tower.lat, tower.lng);
                TowerCircle circle = features.get(tower.color);
                if (circle == null) {
                    circle = new TowerCircle(getPaint(tower.color));
                    features.put(tower.color, circle);
                }

                Point center = projection.toScreenLocation(latLng);
                circle.clipPath.addCircle((float) center.x,
                        (float) center.y,
                        (float) (tower.radius * scale),
                        Path.Direction.CCW);

                TowerPoint tp = new TowerPoint(center, tower.color | 0xFF000000, (int) (tower.radius * scale));
                points[idx++] = tp;
            }
        } else {
            for (Tower tower : towers) {
                latLng = new LatLng(tower.lat, tower.lng);
                Point center = projection.toScreenLocation(latLng);
                TowerPoint tp = new TowerPoint(center, tower.color | 0xFF000000, (int) (tower.radius * scale));
                points[idx++] = tp;
            }
        }

        this.points = points;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < features.size(); i++) {
            TowerCircle circle = features.valueAt(i);
            canvas.save();
            canvas.clipPath(circle.clipPath);
            canvas.drawRect(0, 0, getWidth(), getHeight(), circle.paint);
            canvas.restore();
        }


        for (int i = 0; i < points.length; i++) {
            TowerPoint tp = points[i];
            Drawable d = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(d.mutate(), tp.color);
            int x = tp.rect.centerX();
            int y = tp.rect.centerY();
            int sz = (int) ((iconWidth / 2) * (1 + (float) (tp.size - 1) / 10));
            int left = x - sz;
            int top = y - sz;
            int right = x + sz;
            int bottom = y + sz;
            d.setBounds(left, top, right, bottom);

            canvas.drawRect(left, top, right, bottom, getPaint(0xFFFFFF));
            d.draw(canvas);

        }

        super.onDraw(canvas);
    }

    private TowerPoint[] generalize(TowerPoint[] points) {
        if (points.length == 0)
            return points;

        Arrays.sort(points, (o1, o2) -> o1.rect.left - o2.rect.left);
        int cnt1 = unionIfClose(points);
        Arrays.sort(points, (o1, o2) -> {
            if (o1 == null)
                return o2 == null ? 0 : 1;
            if (o2 == null)
                return -1;
            return o1.rect.top - o2.rect.top;
        });
        int cnt2 = unionIfClose(points);
        TowerPoint[] r = new TowerPoint[points.length - cnt1 - cnt2];
        int idx = 0;
        for (TowerPoint p : points) {
            if (p != null)
                r[idx++] = p;
        }
        return r;
    }

    private int unionIfClose(TowerPoint[] points) {
        TowerPoint p0 = points[0];
        int intersection = 0;
        for (int i = 1; i < points.length; i++) {
            TowerPoint p1 = points[i];
            if (p1 == null)
                continue;
            if (p0.color == p1.color && p0.rect.intersect(p1.rect)) {
                p0.rect.union(p1.rect);
                p0.size++;
                points[i] = null;
                intersection++;
            } else
                p0 = p1;
        }
        return intersection;
    }


    private Paint getPaint(int color) {
        Paint paint = paints.get(color);
        if (paint == null) {
            paints.put(color, paint = new Paint());
            paint.setColor(0x66000000 + (color & 0x00ffffff));
            paint.setStyle(Paint.Style.FILL);
        }
        return paint;
    }

    private static class TowerCircle {
        public final Path clipPath;
        public final Paint paint;

        private TowerCircle(Paint paint) {
            clipPath = new Path();
            this.paint = paint;


        }
    }

    private static class TowerPoint {
        int color;
        Rect rect;
        int size = 1;

        public TowerPoint(Point center, int color, int radius) {
            this.color = color;
            rect = new Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
        }
    }
}
