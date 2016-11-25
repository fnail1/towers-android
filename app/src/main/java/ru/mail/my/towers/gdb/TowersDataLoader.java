package ru.mail.my.towers.gdb;

import android.support.v4.util.LongSparseArray;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.toolkit.ExclusiveExecutor2;
import ru.mail.my.towers.toolkit.ThreadPool;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.prefs;

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
        int generation = prefs().getTowersGeneration() + 1;
        MapExtent mapExtent = extent;
        try {
            Response<GsonTowersInfoResponse> response = api().getTowersInfo(mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;

            GsonTowersInfoResponse towersInfo = response.body();
            if (!towersInfo.success)
                return;

            LongSparseArray<TowerNetwork> networks = new LongSparseArray<>();
            for (GsonTowerInfo towerInfo : towersInfo.towers) {
                TowerNetwork network = networks.get(towerInfo.netId);
                if (network == null) {
                    network = data().towers().selectNetworkByServerId(towerInfo.netId);
                    if (network == null) {
                        network = new TowerNetwork();
                        network.serverId = towerInfo.netId;
                        data().towers().save(network, generation);
                    }
                }
                UserInfo owner = new UserInfo();
                owner.merge(towerInfo.user);
                data().users().save(owner);

                Tower tower = new Tower(towerInfo, owner);
                tower.network = network._id;
                data().towers().save(tower, generation);

            }
            prefs().setTowersGeneration(generation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        data().towers().deleteDeprecated(generation, false, mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);

        callback.onTowersDataLoaded(extent);
    }

    public interface TowersDataLoaderCallback {
        void onTowersDataLoaded(MapExtent extent);
    }
}
