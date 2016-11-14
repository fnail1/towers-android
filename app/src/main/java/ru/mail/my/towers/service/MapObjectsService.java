package ru.mail.my.towers.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.collections.Query;
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

    private final HashMap<Envelop, HashSet<Tower>> cache = new HashMap<>();
    private Envelop envelop;
    private volatile MapObjectsLoadingCompleteEventArgs lastDeliveredArgs;

    public void loadMapObjects(double lat1, double lng1, double lat2, double lng2) {
        envelop = new Envelop(lat1, lng1, lat2, lng2);
        lookupInMemoryCache();

        executor.execute(false);
    }

    private void lookupInMemoryCache() {
        HashSet<Tower> cached = new HashSet<>();

        synchronized (cache) {
            for (Iterator<Map.Entry<Envelop, HashSet<Tower>>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Envelop, HashSet<Tower>> entry = iterator.next();
                if (entry.getKey().intersect(envelop)) {
                    for (Tower tower : entry.getValue()) {
                        if (envelop.inside(tower))
                            cached.add(tower);
                    }
                } else {
                    iterator.remove();
                }
            }
        }

        if (!cached.isEmpty()) {
            Logger.logV("selection", "MEM: " + envelop + " -> " + cached.size());
            lastDeliveredArgs = new MapObjectsLoadingCompleteEventArgs(envelop, cached);
            loadingCompleteEvent.fire(lastDeliveredArgs);
        }
    }

    private void loadMapObjectsSync() {
        lookupInDb();

        try {
            Response<GsonTowersInfoResponse> response = api().getTowersInfo((envelop.lat1 + envelop.lat2) / 2, (envelop.lng1 + envelop.lng2) / 2).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;

            GsonTowersInfoResponse towersInfo = response.body();
            if (!towersInfo.success)
                return;

            Logger.logV("selection", "response contains " + towersInfo.towers.length + " objects");
            int generation = prefs().getTowersGeneration() + 1;
            HashSet<Tower> towers = new HashSet<>(towersInfo.towers.length);
            boolean hasNewObjects = false;
            for (GsonTowerInfo tower : towersInfo.towers) {
                Tower t = new Tower(tower);
                data().towers().save(t, generation);
                towers.add(t);
                if (!hasNewObjects && lastDeliveredArgs != null && lastDeliveredArgs.towers.contains(t))
                    hasNewObjects = true;
            }
            prefs().setTowersGeneration(generation);
            int deleted = data().towers().deleteDeprecated(generation, false, envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
            Logger.logV("selection", "" + deleted + " objects deleted");

            synchronized (cache) {
                cache.put(envelop, towers);
            }

            if (deleted > 0) {
                lookupInDb();
            } else if (hasNewObjects) {
                lookupInMemoryCache();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void lookupInDb() {
        CursorWrapper<Tower> towersCursor = data().towers().select(envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
        try {
            if (towersCursor.moveToFirst()) {
                HashSet<Tower> known = new HashSet<>(towersCursor.getCount());
                boolean hasNewObjects = false;
                do {
                    Tower tower = towersCursor.get();
                    if (!hasNewObjects && lastDeliveredArgs != null && lastDeliveredArgs.towers.contains(tower))
                        hasNewObjects = true;
                    known.add(tower);
                } while (towersCursor.moveToNext());

                synchronized (cache) {
                    cache.put(envelop, known);
                }

                if (hasNewObjects) {
                    Logger.logV("selection", "DB: " + envelop + " -> " + known.size());
                    lastDeliveredArgs = new MapObjectsLoadingCompleteEventArgs(envelop, known);
                    loadingCompleteEvent.fire(lastDeliveredArgs);
                }
            }
        } finally {
            towersCursor.close();
        }
    }

    public interface MapObjectsLoadingCompleteEventHandler {
        void onMapObjectsLoadingComplete(MapObjectsLoadingCompleteEventArgs args);
    }

    public static class MapObjectsLoadingCompleteEventArgs {

        public final Envelop envelop;
        public final HashSet<Tower> towers;

        public MapObjectsLoadingCompleteEventArgs(Envelop envelop, HashSet<Tower> towers) {
            this.envelop = envelop;
            this.towers = towers;
        }
    }
}
