package ru.mail.my.towers.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.prefs;
import static ru.mail.my.towers.toolkit.collections.Query.query;

public class MapObjectsService {
    public final ObservableEvent<MapObjectsLoadingCompleteEventHandler, MapObjectsService, MapObjectsLoadingCompleteEventArgs> loadingCompleteEvent = new ObservableEvent<MapObjectsLoadingCompleteEventHandler, MapObjectsService, MapObjectsLoadingCompleteEventArgs>(this) {
        @Override
        protected void notifyHandler(MapObjectsLoadingCompleteEventHandler handler, MapObjectsService sender, MapObjectsLoadingCompleteEventArgs args) {
            handler.onMapObjectsLoadingComplete(args);
        }
    };

    private final ExclusiveExecutor2 executor = new ExclusiveExecutor2(3000, ThreadPool.SCHEDULER, this::loadMapObjectsSync);

    private final HashMap<Envelop, Tower[]> cache = new HashMap<>();
    private Envelop envelop;

    public void loadMapObjects(double lat1, double lng1, double lat2, double lng2) {
        envelop = new Envelop(lat1, lng1, lat2, lng2);
        executor.execute(false);
        List<Tower> cached;
        synchronized (cache) {
            cached = query(cache.entrySet())
                    .where(e -> e.getKey().intersect(envelop))
                    .extract(e -> query(e.getValue()))
                    .where(envelop::inside)
                    .toList();
        }


        if (!cached.isEmpty()) {
            MapObjectsLoadingCompleteEventArgs args = new MapObjectsLoadingCompleteEventArgs(cached.toArray(new Tower[cached.size()]), envelop);
            loadingCompleteEvent.fire(args);
        }
    }

    private void loadMapObjectsSync() {

        CursorWrapper<Tower> towersCursor = data().towers().select(envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
        if (towersCursor.moveToFirst()) {
            Tower[] known = new Tower[towersCursor.getCount()];
            int idx = 0;

            do {
                known[idx++] = towersCursor.get();
            } while (towersCursor.moveToNext());

            synchronized (cache) {
                cache.put(envelop, known);
            }
            MapObjectsLoadingCompleteEventArgs args = new MapObjectsLoadingCompleteEventArgs(known, envelop);
            loadingCompleteEvent.fire(args);
        }

        try {
            Response<GsonTowersInfoResponse> response = api().getTowersInfo((envelop.lat1 + envelop.lat2) / 2, (envelop.lng1 + envelop.lng2) / 2).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;

            GsonTowersInfoResponse towersInfo = response.body();
            if (!towersInfo.success)
                return;

            int generation = prefs().getTowersGeneration();
            Tower[] towers = new Tower[towersInfo.towers.length];
            int idx = 0;
            for (GsonTowerInfo tower : towersInfo.towers) {
                Tower t = new Tower(tower);
                data().towers().save(t, generation);
                towers[idx++] = t;
            }
            data().towers().deleteDeprecated(generation, false, envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
            synchronized (cache) {
                cache.put(envelop, towers);
            }

            MapObjectsLoadingCompleteEventArgs args = new MapObjectsLoadingCompleteEventArgs(towers, envelop);
            loadingCompleteEvent.fire(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface MapObjectsLoadingCompleteEventHandler {
        void onMapObjectsLoadingComplete(MapObjectsLoadingCompleteEventArgs args);
    }

    public static class MapObjectsLoadingCompleteEventArgs {

        public final Envelop envelop;
        public final Tower[] towers;

        public MapObjectsLoadingCompleteEventArgs(Tower[] towers, Envelop envelop) {
            this.envelop = envelop;
            this.towers = towers;
        }
    }
}
