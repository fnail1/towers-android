package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;

import ru.mail.my.towers.gdb.TowersMap;
import ru.mail.my.towers.model.Tower;


public class MapObjectsView extends View implements TowersMap.TowersMapReadyToDrawListener {

    private final TowersMap towersMap;
    private TowersMap.TowerPoint pressedTowerPoint;
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        towersMap.onResize(w, h);
    }

    public void onCameraMove(GoogleMap map) {
        towersMap.onCameraMove(map);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        towersMap.onDraw(canvas);

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//                for (TowersMap.TowerPoint towerPoint : points) {
//                    if (towerPoint.rect.contains(((int) event.getX()), (int) event.getY())) {
//                        pressedTowerPoint = towerPoint;
//                        return true;
//                    }
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                if (pressedTowerPoint != null && pressedTowerPoint.rect.contains((int) event.getX(), (int) event.getY())) {
//                    if (mapObjectClickListener != null) {
//                        mapObjectClickListener.onMapObjectClick(pressedTowerPoint.tower, pressedTowerPoint.rect);
//                    }
//                    pressedTowerPoint = null;
//                    return true;
//                }
//                // no break;
//            case MotionEvent.ACTION_CANCEL:
//                pressedTowerPoint = null;
//                break;
//        }

        return super.onTouchEvent(event);
    }

//    private TowersMap.TowerPoint[] generalize(TowersMap.TowerPoint[] points) {
//        if (points.length == 0)
//            return points;
//
//        Arrays.sort(points, (o1, o2) -> o1.rect.left - o2.rect.left);
//        int cnt1 = unionIfClose(points);
//        Arrays.sort(points, (o1, o2) -> {
//            if (o1 == null)
//                return o2 == null ? 0 : 1;
//            if (o2 == null)
//                return -1;
//            return o1.rect.top - o2.rect.top;
//        });
//        int cnt2 = unionIfClose(points);
//        TowersMap.TowerPoint[] r = new TowersMap.TowerPoint[points.length - cnt1 - cnt2];
//        int idx = 0;
//        for (TowersMap.TowerPoint p : points) {
//            if (p != null)
//                r[idx++] = p;
//        }
//        return r;
//    }
//
//    private int unionIfClose(TowersMap.TowerPoint[] points) {
//        TowersMap.TowerPoint p0 = points[0];
//        int intersection = 0;
//        for (int i = 1; i < points.length; i++) {
//            TowersMap.TowerPoint p1 = points[i];
//            if (p1 == null)
//                continue;
//            if (p0.color == p1.color && p0.rect.intersect(p1.rect)) {
//                p0.rect.union(p1.rect);
//                p0.size++;
//                points[i] = null;
//                intersection++;
//            } else
//                p0 = p1;
//        }
//        return intersection;
//    }


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
