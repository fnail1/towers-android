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

import ru.mail.my.towers.gis.TowersMap;
import ru.mail.my.towers.model.Tower;


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
                    TowersMap.GeoRequestResult found = towersMap.requestObjectsAt((int) gestureStartX, (int) gestureStartY, true);
                    long tid = 0, nid = 0;
                    double tdis = Double.POSITIVE_INFINITY;
                    double ndis = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < found.networks.size(); i++) {
                        Double d = found.networks.valueAt(i);
                        if (ndis > d) {
                            ndis = d;
                            nid = found.networks.keyAt(i);
                        }
                    }
                    for (int i = 0; i < found.towers.size(); i++) {
                        Double d = found.towers.valueAt(i);
                        if (tdis > d) {
                            tdis = d;
                            tid = found.towers.keyAt(i);
                        }
                    }
                    towersMap.setSelection(tid, nid);
                    invalidate();
                }
                break;
        }

        return super.onTouchEvent(event);
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
