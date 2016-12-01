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
    private final Runnable longClickTask = this::performMapLongClick;


    private MapObjectClickListener mapObjectClickListener;
    private MapObjectLongClickListener mapObjectLongClickListener;
    private float gestureStartX;
    private float gestureStartY;
    private boolean gestureIgnored;
    private boolean longClickTaskScheduled;

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d("TOUCH", MotionEvent.actionToString(event.getAction()));
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureStartX = event.getX();
                gestureStartY = event.getY();
                gestureIgnored = false;
                if (!longClickTaskScheduled) {
                    Log.d("TOUCH", "POST longClickTask");
                    longClickTaskScheduled = true;
                    postDelayed(longClickTask, 1500);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(gestureStartX - event.getX()) > 5 ||
                        Math.abs(gestureStartY - event.getY()) > 5) {
                    Log.d("TOUCH", "REMOVE longClickTask");
                    removeCallbacks(longClickTask);
                    gestureIgnored = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!gestureIgnored &&
                        gestureStartX >= 0 && gestureStartY >= 0 &&
                        Math.abs(gestureStartX - event.getX()) <= 5 &&
                        Math.abs(gestureStartY - event.getY()) <= 5) {
                    onTowersMapClick();
                }
                // no break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("TOUCH", "REMOVE longClickTask");
                removeCallbacks(longClickTask);
                longClickTaskScheduled = false;
                gestureStartX = gestureStartY = -1;
                break;
        }

        return super.onTouchEvent(event);
    }

    public void onTowersMapClick() {
        TowersMap.GeoRequestResult found = towersMap.requestObjectsAt((int) gestureStartX, (int) gestureStartY, true);

        Tower tower;
        long tid = 0, nid = 0;
        double tdis = Double.POSITIVE_INFINITY;
        if (found.towers.size() > 0) {
            for (int i = 0; i < found.towers.size(); i++) {
                POI poi = found.towers.valueAt(i);
                double dx = poi.x - gestureStartX;
                double dy = poi.y - gestureStartY;
                double d2 = dx * dx + dy * dy;
                if (tdis > d2) {
                    tdis = d2;
                    tid = found.towers.keyAt(i);
                }
            }
            tower = data().towers().selectById(tid);
            towersMap.setSelection(tower._id, tower.network);
        } else {
            tower = null;

            if (found.networks.size() > 0) {
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
        }

        if (mapObjectClickListener != null) {
            mapObjectClickListener.onMapSelectionChanged(tower);
        }
        invalidate();
    }


    private void performMapLongClick() {
        Log.d("TOUCH", "EXECUTE longClickTask");
        if (!gestureIgnored && mapObjectLongClickListener != null)
            mapObjectLongClickListener.onMapObjectLongClick(Math.round(gestureStartX), Math.round(gestureStartY));
        gestureStartX = gestureStartY = -1;
        longClickTaskScheduled = false;
    }

    public void setMapObjectClickListener(MapObjectClickListener mapObjectClickListener) {
        this.mapObjectClickListener = mapObjectClickListener;
    }

    public void setMapObjectLongClickListener(MapObjectLongClickListener listener) {
        this.mapObjectLongClickListener = listener;
    }

    @Override
    public void onTowersMapReadyToDraw() {
        invalidate();
    }

    public interface MapObjectClickListener {
        void onMapSelectionChanged(Tower tower);
    }

    public interface MapObjectLongClickListener {
        void onMapObjectLongClick(int x, int y);
    }
}
