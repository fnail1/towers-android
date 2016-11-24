package ru.mail.my.towers.gdb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.data.CursorWrapper;
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
    private MapExtent mapExtent;
    private volatile MapObjectsLoadingCompleteEventArgs lastDeliveredArgs;

    public void loadMapObjects(double lat1, double lng1, double lat2, double lng2) {
        mapExtent = new MapExtent(lat1, lng1, lat2, lng2);
        lookupInMemoryCache();

        executor.execute(false);
    }

    private void lookupInMemoryCache() {
//        Logger.logV("selection", ">");
        HashSet<Tower> cached = new HashSet<>();

        synchronized (cache) {
            for (Tower tower : cache) {
                if (mapExtent.inside(tower)) {
                    cached.add(tower);
                }
            }
        }

//        Logger.logV("selection", "MEM: " + mapExtent + " -> " + cached.size());
        lastDeliveredArgs = new MapObjectsLoadingCompleteEventArgs(mapExtent, cached);
        loadingCompleteEvent.fire(lastDeliveredArgs);
    }

    private void loadMapObjectsSync() {
        lookupInDb();

        int generation = prefs().getTowersGeneration() + 1;
        boolean hasNewObjects = false;

        try {
            Response<GsonTowersInfoResponse> response = api().getTowersInfo(mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2).execute();
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
//        CursorWrapper<Tower> select = data().towers().select(mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);
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
        boolean deleted = data().towers().deleteDeprecated(generation, false, mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);
//        Logger.logV("selection", "" + deleted + " objects deleted");


        if (deleted) {
            lookupInDb();
        } else if (hasNewObjects) {
            lookupInMemoryCache();
        }
    }

    private void lookupInDb() {
//        Logger.logV("selection", ">");
        CursorWrapper<Tower> towersCursor = data().towers().select(mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);
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
//                    Logger.logV("selection", "DB: " + mapExtent + " -> " + known.size());
                    lastDeliveredArgs = new MapObjectsLoadingCompleteEventArgs(mapExtent, known);
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

        public final MapExtent mapExtent;
        public final HashSet<Tower> towers;

        public MapObjectsLoadingCompleteEventArgs(MapExtent mapExtent, HashSet<Tower> towers) {
            this.mapExtent = mapExtent;
            this.towers = towers;
        }
    }
}
