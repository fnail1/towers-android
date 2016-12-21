package ru.mail.my.towers.gis;

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
import static ru.mail.my.towers.TowersApp.game;
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

            for (GsonTowersInfoResponse.GsonTowersCollection networkInfo : towersInfo.towersNew) {
                if (networkInfo.towers.length == 0)
                    continue;

                TowerNetwork network = null;
                for (GsonTowerInfo towerInfo : networkInfo.towers) {
                    Tower tower = data().towers().selectByServerId(towerInfo.id);
                    if (tower != null) {
                        network = data().networks().selectById(tower.network);
                        break;
                    }
                }

                if (network == null) {
                    network = new TowerNetwork();
                    data().networks().save(network, generation);
                }

                double lat = 0, lng = 0;
                float level = 0;

                for (GsonTowerInfo towerInfo : networkInfo.towers) {
                    lat += towerInfo.lat;
                    lng += towerInfo.lng;
                    level += towerInfo.level;
                    network.color = UserInfo.parseColor(towerInfo.user.color);
                }

                double centerLat = lat / networkInfo.towers.length;
                double centerLng = lng / networkInfo.towers.length;
                double dmin = Double.MAX_VALUE;
                GsonTowerInfo closest = null;
                for (GsonTowerInfo towerInfo : networkInfo.towers) {
                    double dlat = towerInfo.lat - centerLat;
                    double dlng = towerInfo.lng - centerLng;
                    double d = dlat * dlat + dlng * dlng;
                    if (d < dmin) {
                        dmin = d;
                        closest = towerInfo;
                    }
                }
                //noinspection ConstantConditions
                network.lat = closest.lat;
                network.lng = closest.lng;
                network.level = level / networkInfo.towers.length;
                network.count = networkInfo.towers.length;
                network.my = networkInfo.towers[0].user.id == game().me.serverId;
                data().networks().save(network, generation);

                for (GsonTowerInfo towerInfo : networkInfo.towers) {
                    UserInfo owner = new UserInfo();
                    owner.merge(towerInfo.user);
                    data().users().save(owner);

                    Tower tower = new Tower(towerInfo, owner);
                    tower.network = network._id;
                    data().towers().save(tower, generation);
                }
            }

            prefs().setTowersGeneration(generation);
            data().towers().deleteDeprecated(generation, mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        callback.onTowersDataLoaded(mapExtent);
    }

    public interface TowersDataLoaderCallback {
        void onTowersDataLoaded(MapExtent extent);
    }
}
