package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

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

    private final SparseArray<Feature> features = new SparseArray<>();
    private final Point point = new Point();
    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Drawable icon;
    private int xs[] = {};
    private int ys[] = {};
    private int cs[] = {};


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

        xs = new int[towers.length];
        ys = new int[towers.length];
        cs = new int[towers.length];
        int idx = 0;

        for (Tower tower : towers) {
            latLng = new LatLng(tower.lat, tower.lng);
            Feature feature = features.get(tower.color);
            if (feature == null) {
                feature = new Feature(tower.color);
                features.put(tower.color, feature);
            }

            Point center = projection.toScreenLocation(latLng);
            feature.clipPath.addCircle((float) center.x,
                    (float) center.y,
                    (float) (tower.radius * scale),
                    Path.Direction.CCW);
            xs[idx] = center.x;
            ys[idx] = center.y;
            cs[idx] = tower.color;
            idx++;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.valueAt(i);
            canvas.save();
            canvas.clipPath(feature.clipPath);
            canvas.drawRect(0, 0, getWidth(), getHeight(), feature.paint);
            canvas.restore();
        }


        for (int i = 0; i < xs.length; i++) {
            icon.setBounds(xs[i] - iconWidth / 2, ys[i] - iconHeight / 2, xs[i] + iconWidth / 2, ys[i] + iconHeight / 2);
            icon.setTint(cs[i]);
            icon.setTintMode(PorterDuff.Mode.ADD);
            icon.draw(canvas);
        }

        super.onDraw(canvas);
    }

    private static class Feature {
        public final Path clipPath;
        public final Paint paint;

        private Feature(int color) {
            clipPath = new Path();

            paint = new Paint();
            paint.setColor(0x66000000 + (color & 0x00ffffff));
//            paint.setColor(0x66000000 + 0x00ffffff);
            paint.setStyle(Paint.Style.FILL);

        }
    }
}
