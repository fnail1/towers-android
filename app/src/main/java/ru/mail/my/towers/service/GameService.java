package ru.mail.my.towers.service;

import android.location.Location;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonCreateTowerResponse;
import ru.mail.my.towers.api.model.GsonDestroyTowerResponse;
import ru.mail.my.towers.api.model.GsonGameInfoResponse;
import ru.mail.my.towers.api.model.GsonGetProfileResponse;
import ru.mail.my.towers.api.model.GsonMyTowersResponse;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.api.model.GsonTowersNetworkInfo;
import ru.mail.my.towers.api.model.GsonUpdateTowerResponse;
import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.api.model.GsonUserProfile;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.TowerUpdateAction;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.model.db.AppData;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.TowersApp.prefs;

public class GameService {
    public final UserInfo me;

    public final ObservableEvent<MyProfileEventHandler, GameService, UserInfo> myProfileEvent = new ObservableEvent<MyProfileEventHandler, GameService, UserInfo>(this) {
        @Override
        protected void notifyHandler(MyProfileEventHandler handler, GameService sender, UserInfo args) {
            handler.onMyProfileChanged(args);
        }
    };

    public final ObservableEvent<GameMessageEventHandler, GameService, String> gameMessageEvent = new ObservableEvent<GameMessageEventHandler, GameService, String>(this) {
        @Override
        protected void notifyHandler(GameMessageEventHandler handler, GameService sender, String args) {
            handler.onGameNewMessage(args);
        }
    };

    public final ObservableEvent<TowersGeoDataChanged, GameService, MapExtent> geoDataChangedEvent = new ObservableEvent<TowersGeoDataChanged, GameService, MapExtent>(this) {
        @Override
        protected void notifyHandler(TowersGeoDataChanged handler, GameService sender, MapExtent args) {
            handler.onTowersGeoDataChanged(args);
        }
    };

    public GameService(Preferences preferences, AppData data) {
        UserInfo userInfo = data.users.select(preferences.getMeDbId());
        if (userInfo == null) {
            userInfo = new UserInfo();
        }
        me = userInfo;
    }

    public void updateMyProfile(GsonUserInfo data) {
        boolean exist = me._id > 0;

        this.me.merge(data);

        if (exist) {
            data().users.save(me);
            myProfileEvent.fire(me);
        }
    }


    public void updateMyProfile(GsonUserProfile data) {
        this.me.merge(data);

        if (me.serverId != 0) {
            data().users.save(me);
            myProfileEvent.fire(me);
        }
    }

    public void updateMyProfile(String newName, int newColor, UpdateMyProfileCallback callback) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH).execute(() -> {
            try {
                StringBuilder sb = new StringBuilder("000000");
                String hexColor = Integer.toHexString(newColor).substring(0, 6);
                sb.replace(6 - hexColor.length(), 6, hexColor);
                hexColor = sb.toString();

                Response<GsonPutProfileResponse> response = api().setMyProfile(newName, hexColor).execute();
                if (HttpURLConnection.HTTP_OK != response.code()) {
                    callback.onUpdateMyProfileServerError(response.code());
                    return;
                }
                GsonPutProfileResponse data = response.body();
                if (!data.success) {
                    callback.onUpdateMyProfileCommonServerError();
                    return;
                }
                updateMyProfile(data.profile);
                callback.onUpdateMyProfileComplete();
            } catch (IOException e) {
                callback.onUpdateMyProfileNetworkError();
            }
        });

    }

    public void start() {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(this::startSync);
    }

    private void startSync() {
        try {
            Response<GsonGameInfoResponse> gameInfoResponse = api().getGameInfo().execute();
            if (gameInfoResponse.code() == HttpURLConnection.HTTP_OK) {
                GsonGameInfoResponse gameInfo = gameInfoResponse.body();
                if (gameInfo.success) {
                    updateMyProfile(gameInfo.info);
                }
            }

            Response<GsonGetProfileResponse> profileResponse = api().getMyProfile().execute();
            if (profileResponse.code() == HttpURLConnection.HTTP_OK) {
                GsonGetProfileResponse body = profileResponse.body();
                if (body.success) {
                    updateMyProfile(body.profile);
                }
            }

            loadMyTowers(me);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadTowers(MapExtent mapExtent) throws IOException {
        int generation = prefs().getTowersGeneration() + 1;
        Response<GsonTowersInfoResponse> response = api().getTowersInfo(mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2).execute();
        if (response.code() != HttpURLConnection.HTTP_OK)
            return false;

        GsonTowersInfoResponse towersInfo = response.body();
        if (!towersInfo.success)
            return false;

        for (GsonTowersInfoResponse.GsonTowersCollection networkInfo : towersInfo.towersNew) {
            GsonTowerInfo[] towers = networkInfo.towers;
            if (towers.length == 0)
                continue;

            updateTowersNetwork(towers, generation);
        }

        prefs().setTowersGeneration(generation);
        data().towers.deleteDeprecated(generation, mapExtent.lat1, mapExtent.lng1, mapExtent.lat2, mapExtent.lng2);
        return true;
    }

    private void loadMyTowers(UserInfo me) throws IOException {
        int generation = prefs().getMyTowersGeneration() + 1;

        Response<GsonMyTowersResponse> myTowersResponse = api().getMyTowers().execute();
        if (myTowersResponse.isSuccessful()) {
            GsonMyTowersResponse myTowers = myTowersResponse.body();
            if (myTowers.success) {
                int totalCount = 0;
                for (GsonTowersNetworkInfo towersNet : myTowers.towersNets) {
                    GsonTowerInfo[] towers = towersNet.inside;
                    if (towers.length == 0)
                        continue;
                    totalCount += towers.length;

                    updateTowersNetwork(towers, generation);
                }
                me.towersCount = totalCount;
                data().users.save(me);
                prefs().setMyTowersGeneration(generation);
                data().towers.deleteDeprecated(generation, true);
            }
        }
    }


    public void updateTowersNetwork(GsonTowerInfo[] towers, int generation) {
        TowerNetwork network = null;
        for (GsonTowerInfo towerInfo : towers) {
            Tower tower = data().towers.selectByServerId(towerInfo.id);
            if (tower != null) {
                network = data().networks.selectById(tower.network);
                break;
            }
        }

        if (network == null) {
            network = new TowerNetwork();
            data().networks.save(network, generation);
        }

        double lat = 0, lng = 0;
        float level = 0;
        int health = 0;
        int maxHealth = 0;


        for (GsonTowerInfo towerInfo : towers) {
            lat += towerInfo.lat;
            lng += towerInfo.lng;
            level += towerInfo.level;
            health += towerInfo.health;
            maxHealth += towerInfo.maxHealth;
            network.color = UserInfo.parseColor(towerInfo.user.color);
        }

        double centerLat = lat / towers.length;
        double centerLng = lng / towers.length;
        double dmin = Double.MAX_VALUE;
        GsonTowerInfo closest = null;
        for (GsonTowerInfo towerInfo : towers) {
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
        network.level = level / towers.length;
        network.count = towers.length;
        network.health = health;
        network.maxHealth = maxHealth;
        network.my = towers[0].user.id == game().me.serverId;
        data().networks.save(network, generation);

        for (GsonTowerInfo towerInfo : towers) {
            UserInfo owner = new UserInfo();
            owner.merge(towerInfo.user);
            data().users.save(owner);

            Tower tower = new Tower(towerInfo, owner);
            tower.network = network._id;
            data().towers.save(tower, generation);
        }
    }

    public void createTower(Location location, String name) {
        createTower(location.getLatitude(), location.getLongitude(), name);
    }

    public void createTower(double latitude, double longitude, String name) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            try {
                Response<GsonCreateTowerResponse> response = api().createTower(latitude, longitude, name).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    gameMessageEvent.fire("Башня не постоена. Сервер вернул " + response.code());
                } else {
                    GsonCreateTowerResponse body = response.body();
                    if (!body.success) {
                        gameMessageEvent.fire("Башня не построена: " + body.error.message);
                    } else {
                        me.towersCount++;
                        me.merge(body.userInfo);
                        me.merge(body.tower.user);
                        data().users.save(me);

                        Tower tower = new Tower(body.tower, me);
                        data().towers.save(tower, prefs().getMyTowersGeneration());
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
                        myProfileEvent.fire(me);
                        gameMessageEvent.fire("Башня \'" + tower.title + "\' построена");
                    }
                }
            } catch (IOException e) {
                gameMessageEvent.fire("Не удалось построить башню из-за сетевой ошибки.");
            }
        });
    }

    public void destroyTower(Tower tower) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            try {
                Response<GsonDestroyTowerResponse> response = api().destroyTower(tower.serverId, TowerUpdateAction.destroy).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    gameMessageEvent.fire("Башня устояла. Сервер вернул " + response.code());
                } else {
                    GsonDestroyTowerResponse body = response.body();
                    if (!body.success) {
                        gameMessageEvent.fire("Башня устояла: " + body.error.message);
                    } else {
                        me.towersCount--;

                        data().towers.delete(tower._id);
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
                        gameMessageEvent.fire("Башня \'" + tower.title + "\' удалена");

                        startSync();
                    }
                }
            } catch (IOException e) {
                gameMessageEvent.fire("Не удалось удалить башню из-за сетевой ошибки.");
            }
        });
    }

    public void upgradeTower(Tower tower) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            try {
                Response<GsonUpdateTowerResponse> response = api().updateTower(tower.serverId, TowerUpdateAction.upgrade).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    gameMessageEvent.fire("Ошибка. Сервер вернул " + response.code());
                } else {
                    GsonUpdateTowerResponse body = response.body();
                    if (!body.success) {
                        gameMessageEvent.fire("Ошибка: " + body.error.message);
                    } else {
                        me.towersCount--;

                        data().towers.delete(tower._id);
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
                        gameMessageEvent.fire("Башня \'" + tower.title + "\' обновлена");

                        startSync();
                    }
                }
            } catch (IOException e) {
                gameMessageEvent.fire("Не удалось прокачать башню из-за сетевой ошибки.");
            }
        });
    }

    public interface MyProfileEventHandler {
        void onMyProfileChanged(UserInfo args);
    }

    public interface UpdateMyProfileCallback {
        void onUpdateMyProfileServerError(int code);

        void onUpdateMyProfileCommonServerError();

        void onUpdateMyProfileComplete();

        void onUpdateMyProfileNetworkError();
    }

    public interface GameMessageEventHandler {
        void onGameNewMessage(String args);
    }

    public interface TowersGeoDataChanged {
        void onTowersGeoDataChanged(MapExtent extent);
    }
}
