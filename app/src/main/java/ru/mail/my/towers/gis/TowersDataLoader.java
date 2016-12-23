package ru.mail.my.towers.gis;

import java.io.IOException;

import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;

import static ru.mail.my.towers.TowersApp.game;

public class TowersDataLoader {
    private final ExclusiveExecutor2 executor = new ExclusiveExecutor2(200, ThreadPool.SCHEDULER, this::loadSync);
    private final TowersDataLoaderCallback callback;
    private MapExtent extent;

    public TowersDataLoader(TowersDataLoaderCallback callback) {
        this.callback = callback;
    }

    public void requestData(MapExtent extent) {
        this.extent = extent;
        executor.execute(false);
    }

    private void loadSync() {
        MapExtent mapExtent = extent;
        try {
            if (!game().loadTowers(mapExtent))
                return;
        } catch (IOException e) {
            e.printStackTrace();
        }

        callback.onTowersDataLoaded(mapExtent);
    }

    public interface TowersDataLoaderCallback {
        void onTowersDataLoaded(MapExtent extent);
    }
}
