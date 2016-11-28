package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;

import ru.mail.my.towers.gis.POI;
import ru.mail.my.towers.gis.TowersMap;
import ru.mail.my.towers.model.Tower;

import static ru.mail.my.towers.TowersApp.data;


public class MapObjectsView extends View implements TowersMap.TowersMapReadyToDrawListener {

    private final TowersMap towersMap;
    private MapObjectClickListener mapObjectClickListener;

    public static TowersMap init(Context context, MapObjectsView view) {
        TowersMap towersMap = new TowersMap(context, view);
        return towersMap;
    }

    public MapObjectsView(Context context) {
        super(context);
        towersMap = init(context, this);
    }


    public MapObjectsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        towersMap = init(context, this);
    }

    public MapObjectsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        towersMap = init(context, this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MapObjectsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        towersMap = init(context, this);
    }

    public void onCameraMove(GoogleMap map) {
        towersMap.onCameraMove(map, getWidth(), getHeight());

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        towersMap.onDraw(canvas);

        super.onDraw(canvas);
    }

    float gestureStartX;
    float gestureStartY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d("TOUCH", MotionEvent.actionToString(event.getAction()));
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureStartX = event.getX();
                gestureStartY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(gestureStartX - event.getX()) <= 5 &&
                        Math.abs(gestureStartY - event.getY()) <= 5) {
                    onTowersMapClick();
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    public void onTowersMapClick() {
        TowersMap.GeoRequestResult found = towersMap.requestObjectsAt((int) gestureStartX, (int) gestureStartY, true);

        long tid = 0, nid = 0;
        double tdis = Double.POSITIVE_INFINITY;
        POI tpoi = null;
        if (found.towers.size() > 0) {
            for (int i = 0; i < found.towers.size(); i++) {
                POI poi = found.towers.valueAt(i);
                double dx = poi.x - gestureStartX;
                double dy = poi.y - gestureStartY;
                double d2 = dx * dx + dy * dy;
                if (tdis > d2) {
                    tdis = d2;
                    tid = found.towers.keyAt(i);
                    tpoi = poi;
                }
            }
            Tower tower = data().towers().selectById(tid);
            towersMap.setSelection(tower._id, tower.network);

            if (mapObjectClickListener != null) {
                Rect rect = new Rect(
                        (int) (tpoi.x - tpoi.radius),
                        (int) (tpoi.y - tpoi.radius),
                        (int) (tpoi.x - tpoi.radius),
                        (int) (tpoi.y - tpoi.radius));
                mapObjectClickListener.onMapObjectClick(tower, rect);
            }
        } else if (found.networks.size() > 0) {
            double ndis = Double.POSITIVE_INFINITY;
            for (int i = 0; i < found.networks.size(); i++) {
                POI poi = found.networks.valueAt(i);
                float dx = poi.x - gestureStartX;
                float dy = poi.y - gestureStartY;
                double d = dx * dx + dy * dy;
                if (ndis > d) {
                    ndis = d;
                    nid = found.networks.keyAt(i);
                }
            }
            towersMap.setSelection(0, nid);
        } else {
            towersMap.setSelection(0, 0);
        }
        invalidate();
    }


    public void setMapObjectClickListener(MapObjectClickListener mapObjectClickListener) {
        this.mapObjectClickListener = mapObjectClickListener;
    }

    @Override
    public void onTowersMapReadyToDraw() {
        invalidate();
    }

    public interface MapObjectClickListener {
        void onMapObjectClick(Tower tower, Rect rect);
    }
}
