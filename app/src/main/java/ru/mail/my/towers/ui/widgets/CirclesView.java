package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.ui.MapRect;


public class CirclesView extends View {

    private final SparseArray<Feature> features = new SparseArray<>();
    private final Point point = new Point();
    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");


    public CirclesView(Context context) {
        super(context);
    }

    public CirclesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CirclesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CirclesView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.valueAt(i);
            canvas.clipPath(feature.clipPath);
            canvas.drawRect(0, 0, getWidth(), getHeight(), feature.paint);
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
            paint.setStyle(Paint.Style.FILL);

        }
    }
}
