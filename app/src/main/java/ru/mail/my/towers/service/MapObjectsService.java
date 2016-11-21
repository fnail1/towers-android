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
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.prefs;

public class MapObjectsService {
    public final ObservableEvent<MapObjectsLoadingCompleteEventHandler, MapObjectsService, MapObjectsLoadingCompleteEventArgs> loadingCompleteEvent = new ObservableEvent<MapObjectsLoadingCompleteEventHandler, MapObjectsService, MapObjectsLoadingCompleteEventArgs>(this) {
        @Override
        protected void notifyHandler(MapObjectsLoadingCompleteEventHandler handler, MapObjectsService sender, MapObjectsLoadingCompleteEventArgs args) {
            handler.onMapObjectsLoadingComplete(args);
        }
    };

    private final ExclusiveExecutor2 executor = new ExclusiveExecutor2(3000, ThreadPool.SCHEDULER, this::loadMapObjectsSync);

    private final HashSet<Tower> cache = new HashSet<>();
    private Envelop envelop;
    private volatile MapObjectsLoadingCompleteEventArgs lastDeliveredArgs;

    public void loadMapObjects(double lat1, double lng1, double lat2, double lng2) {
        envelop = new Envelop(lat1, lng1, lat2, lng2);
        lookupInMemoryCache();

        executor.execute(false);
    }

    private void lookupInMemoryCache() {
//        Logger.logV("selection", ">");
        HashSet<Tower> cached = new HashSet<>();

        synchronized (cache) {
            for (Tower tower : cache) {
                if (envelop.inside(tower)) {
                    cached.add(tower);
                }
            }
        }

//        Logger.logV("selection", "MEM: " + envelop + " -> " + cached.size());
        lastDeliveredArgs = new MapObjectsLoadingCompleteEventArgs(envelop, cached);
        loadingCompleteEvent.fire(lastDeliveredArgs);
    }

    private void loadMapObjectsSync() {
        lookupInDb();

        int generation = prefs().getTowersGeneration() + 1;
        boolean hasNewObjects = false;

        try {
            Response<GsonTowersInfoResponse> response = api().getTowersInfo(envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;

            GsonTowersInfoResponse towersInfo = response.body();
            if (!towersInfo.success)
                return;

//            Logger.logV("selection", ">");
//            Logger.logV("selection", "response contains " + towersInfo.towers.length + " objects");
            HashSet<Tower> towers = new HashSet<>(towersInfo.towers.length);
            for (GsonTowerInfo towerInfo : towersInfo.towers) {
//                Logger.logV("selection", "FROM SERVER " + tower.id);
                UserInfo owner = new UserInfo();
                owner.merge(towerInfo.user);
                data().users().save(owner);

                Tower tower = new Tower(towerInfo, owner);
                data().towers().save(tower, generation);

                towers.add(tower);
            }
            prefs().setTowersGeneration(generation);
            synchronized (cache) {
                hasNewObjects = cache.addAll(towers);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Logger.logV("selection", ">");
//        CursorWrapper<Tower> select = data().towers().select(envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
//        try {
//            if (select.moveToFirst()) {
//                do {
//                    Tower tower = select.get();
//                    if (!tower.my) {
//                        Logger.logV("selection", "DELETE " + tower.serverId);
//                    }
//                } while (select.moveToNext());
//            }
//        } finally {
//            select.close();
//        }
        int deleted = data().towers().deleteDeprecated(generation, false, envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
//        Logger.logV("selection", "" + deleted + " objects deleted");


        if (deleted > 0) {
            lookupInDb();
        } else if (hasNewObjects) {
            lookupInMemoryCache();
        }
    }

    private void lookupInDb() {
//        Logger.logV("selection", ">");
        CursorWrapper<Tower> towersCursor = data().towers().select(envelop.lat1, envelop.lng1, envelop.lat2, envelop.lng2);
        try {
            if (towersCursor.moveToFirst()) {
                HashSet<Tower> known = new HashSet<>(towersCursor.getCount());
                boolean hasNewObjects = false;
                do {
                    Tower tower = towersCursor.get();
                    if (!hasNewObjects && (lastDeliveredArgs == null || !lastDeliveredArgs.towers.contains(tower)))
                        hasNewObjects = true;
                    known.add(tower);

//                    Logger.logV("selection", "FROM DB " + tower.serverId);
                } while (towersCursor.moveToNext());

                synchronized (cache) {
                    cache.addAll(known);
                }

                if (hasNewObjects) {
//                    Logger.logV("selection", "DB: " + envelop + " -> " + known.size());
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
