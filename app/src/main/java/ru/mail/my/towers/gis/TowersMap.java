package ru.mail.my.towers.gis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;


import java.util.ArrayList;

import ru.mail.my.towers.gis.layers.Layer;
import ru.mail.my.towers.gis.layers.NetworksPointLayer;
import ru.mail.my.towers.gis.layers.TowersPolygonLayer;
import ru.mail.my.towers.gis.layers.TowersPointLayer;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;

public class TowersMap implements TowersDataLoader.TowersDataLoaderCallback, IMapEngine {
    private static final double SCALE_DETAILED = 5.0;
    private static final double SCALE_MIDDLE = 2.0;

    private static final ScreenDataObjects EMPTY_SCREEN_DATA;

    static {
        EMPTY_SCREEN_DATA = new ScreenDataObjects(-1);
        EMPTY_SCREEN_DATA.towers = new ArrayList<>();
        EMPTY_SCREEN_DATA.networks = new ArrayList<>();
    }

    private final ExclusiveExecutor2 buildDataExecutor = new ExclusiveExecutor2(0, ThreadPool.SCHEDULER, this::prepareData);
    private final TowersDataLoader dataLoader = new TowersDataLoader(this);


    private final Point screenPointBuffer = new Point();
    private final LatLng[] mapPointsBuffer = new LatLng[4];
    private final SparseArray<Paint> paints = new SparseArray<>();
    private final TextPaint primaryTextPaint;
    private final TextPaint selectionTextPaint;
    private final TowersMapReadyToDrawListener listener;
    private final Runnable callListenerTask = this::notifyReadyToDraw;
    private final Layer[] layers;
    private final ScreenProjection screenProjection = new ScreenProjection();
    private final LongSparseArray<Tower> selectedTowers = new LongSparseArray<>();
    private final LongSparseArray<TowerNetwork> selectedNetworks = new LongSparseArray<>();

    private MapExtent mapExtent;
    private volatile int generation = 0;
    private volatile ScreenDataObjects screenDataObjects = EMPTY_SCREEN_DATA;

    public TowersMap(Context context, TowersMapReadyToDrawListener listener) {
        this.listener = listener;
        primaryTextPaint = new TextPaint();
        primaryTextPaint.setColor(0xffffffff);
        primaryTextPaint.setTextSize(48);
        primaryTextPaint.setTypeface(Typeface.DEFAULT);
        primaryTextPaint.setStyle(Paint.Style.STROKE);

        selectionTextPaint = new TextPaint();
        selectionTextPaint.setColor(0xff000000);
        selectionTextPaint.setTextSize(48);
        selectionTextPaint.setTypeface(Typeface.DEFAULT);
        selectionTextPaint.setStyle(Paint.Style.STROKE);

        layers = new Layer[3];

        layers[0] = new TowersPolygonLayer(context);
        layers[0].maxVisibleScale = Double.POSITIVE_INFINITY;
        layers[0].minVisibleScale = SCALE_MIDDLE;

        layers[1] = new TowersPointLayer(context);
        layers[1].maxVisibleScale = Double.POSITIVE_INFINITY;
        layers[1].minVisibleScale = SCALE_DETAILED;

        layers[2] = new NetworksPointLayer(context);
        layers[2].maxVisibleScale = SCALE_MIDDLE;
        layers[2].minVisibleScale = 0;
    }

    public void onCameraMove(GoogleMap map, int screenWidth, int screenHeight) {
        generation++;

        Projection projection = map.getProjection();
        screenPointBuffer.set(0, 0);
        mapPointsBuffer[0] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(screenWidth, 0);
        mapPointsBuffer[1] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(0, screenHeight);
        mapPointsBuffer[2] = projection.fromScreenLocation(screenPointBuffer);
        screenPointBuffer.set(screenWidth, screenHeight);
        mapPointsBuffer[3] = projection.fromScreenLocation(screenPointBuffer);

        screenProjection.build(mapPointsBuffer, screenWidth, screenHeight, generation);

        mapExtent = new MapExtent(mapPointsBuffer);
        mapExtent.expand(1);

        buildScreenData();

        dataLoader.requestData(mapExtent);
        buildDataExecutor.execute(false);
    }

    public void onDraw(Canvas canvas) {
        if (mapExtent == null)
            return;

        for (Layer layer : layers) {
            if (layer.isVisible(screenProjection.scale)) {
                layer.draw(this, canvas);
            }
        }
    }

    private void prepareData() {
        int generation = this.generation;
        ScreenDataObjects dataObjects = new ScreenDataObjects(generation);
        for (Layer layer : layers) {
            if (layer.isVisible(screenProjection.scale)) {
                layer.requestData(this, mapExtent, dataObjects);
                if (this.generation != generation) {
                    Log.d("TowersMap.Concurrent", "cancel 2");
                    return;
                }
            }
        }

        this.screenDataObjects = dataObjects;

        buildScreenData();

        if (this.generation != this.generation) {
            Log.d("TowersMap.Concurrent", "cancel 4");
            return;
        }

        ThreadPool.UI.post(callListenerTask);
    }


    @NonNull
    private void buildScreenData() {
        int generation = this.generation;

        ScreenDataObjects screenDataObjects = this.screenDataObjects;
        for (Layer layer : layers) {
            ScreenDataObjects dataObjects = layer.isVisible(screenProjection.scale)
                    ? screenDataObjects : EMPTY_SCREEN_DATA;

            layer.buildScreenData(this, dataObjects, screenProjection, generation);

            if (screenProjection.screenRequestCounter != generation)
                return;
        }
    }

    @Override
    public Paint getPaint(int color) {
        Paint paint = paints.get(color);
        if (paint == null) {
            paints.put(color, paint = new Paint());
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }
        return paint;
    }

    @Override
    public TextPaint getPrimaryTextPaint() {
        return primaryTextPaint;
    }

    @Override
    public TextPaint getSelectionTextPaint() {
        return selectionTextPaint;
    }

    @Override
    public LongSparseArray<Tower> getSelectedTowers() {
        return selectedTowers;
    }

    @Override
    public LongSparseArray<TowerNetwork> getSelectedNetworks() {
        return selectedNetworks;
    }

    @Override
    public void onTowersDataLoaded(MapExtent extent) {
        if (mapExtent == extent || mapExtent.intersect(extent))
            buildDataExecutor.execute(false);
    }

    private void notifyReadyToDraw() {
        listener.onTowersMapReadyToDraw();
    }

    @NonNull
    public GeoRequestResult requestObjectsAt(int x, int y, boolean visibleOnly) {
        GeoRequestResult out = new GeoRequestResult();
        for (Layer layer : layers) {
            if (!layer.isVisible(screenProjection.scale)) {
                if (visibleOnly)
                    continue;

                layer.buildScreenData(this, screenDataObjects, screenProjection, generation);
            }

            layer.requestObjectsAt(this, x, y, out);
        }
        return out;
    }

    public void setSelection(long towerId, long networkId) {
        selectedTowers.clear();
        selectedNetworks.clear();
        if (networkId > 0) {
            selectedNetworks.put(networkId, TowerNetwork.FAKE_INSTANCE);
        }
        if (towerId > 0) {
            selectedTowers.put(towerId, Tower.FAKE_INSTANCE);
        }
        buildScreenData();
    }

    public interface TowersMapReadyToDrawListener {
        void onTowersMapReadyToDraw();
    }

    public static final class GeoRequestResult {
        public LongSparseArray<Double> towers = new LongSparseArray<>();
        public LongSparseArray<Double> networks = new LongSparseArray<>();

        private GeoRequestResult() {
        }
    }

}
