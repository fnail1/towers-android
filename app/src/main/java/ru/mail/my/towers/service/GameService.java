package ru.mail.my.towers.service;

import android.location.Location;
import android.util.LongSparseArray;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;
import ru.mail.my.towers.api.TowersGameApi;
import ru.mail.my.towers.api.model.GsonAttackResponse;
import ru.mail.my.towers.api.model.GsonBattleInfo;
import ru.mail.my.towers.api.model.GsonCreateTowerResponse;
import ru.mail.my.towers.api.model.GsonDestroyTowerResponse;
import ru.mail.my.towers.api.model.GsonGameInfoResponse;
import ru.mail.my.towers.api.model.GsonGetProfileResponse;
import ru.mail.my.towers.api.model.GsonMyTowersResponse;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.api.model.GsonButtleResultsResponse;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.api.model.GsonTowersNetworkInfo;
import ru.mail.my.towers.api.model.GsonUpdateTowerResponse;
import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.api.model.GsonUserProfile;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.Notification;
import ru.mail.my.towers.model.NotificationType;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.TowerUpdateAction;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.model.db.AppData;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.appState;
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

    public final ObservableEvent<GameMessageEventHandler, GameService, Notification> gameMessageEvent = new ObservableEvent<GameMessageEventHandler, GameService, Notification>(this) {
        @Override
        protected void notifyHandler(GameMessageEventHandler handler, GameService sender, Notification args) {
            handler.onGameNewMessage(args);
        }
    };

    public final ObservableEvent<TowersGeoDataChangedEventHandler, GameService, MapExtent> geoDataChangedEvent = new ObservableEvent<TowersGeoDataChangedEventHandler, GameService, MapExtent>(this) {
        @Override
        protected void notifyHandler(TowersGeoDataChangedEventHandler handler, GameService sender, MapExtent args) {
            handler.onTowersGeoDataChanged(args);
        }
    };

    public final ObservableEvent<TowerDeleteEventHandler, GameService, Tower> deleteTowerEvent = new ObservableEvent<TowerDeleteEventHandler, GameService, Tower>(this) {
        @Override
        protected void notifyHandler(TowerDeleteEventHandler handler, GameService sender, Tower args) {
            handler.onTowerDelete(args);
        }
    };

    private final LongSparseArray<GsonBattleInfo> towersUnderAttack = new LongSparseArray<>();

    public GameService(Preferences preferences, AppData data) {
        UserInfo userInfo = data.users.select(preferences.getMeDbId());
        if (userInfo == null) {
            userInfo = new UserInfo();
        }
        me = userInfo;
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

    public void updateMyProfile(GsonUserInfo data) {
        synchronized (me) {
            me.merge(data);
        }

        if (me._id > 0) {
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

    public void onGameNotification(String message, NotificationType type) {
        Logger.logNotification(message, type);

        Notification raw = new Notification();
        raw.message = message;
        raw.type = type;
        raw.ts = appState().getServerTime();
        data().notifications.insert(raw);
        gameMessageEvent.fire(raw);
    }


    public boolean loadTowers(MapExtent mapExtent) throws IOException {
        if (towersUnderAttack.size() > 0)
            return false;

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
                    onGameNotification("Башня не постоена. Сервер вернул " + response.code(), NotificationType.ERROR);
                } else {
                    GsonCreateTowerResponse body = response.body();
                    if (!body.success) {
                        onGameNotification("Башня не построена: " + body.error.message, NotificationType.ERROR);
                    } else {
                        me.towersCount++;
                        me.merge(body.tower.user);

                        Tower tower = new Tower(body.tower, me);
                        data().towers.save(tower, prefs().getMyTowersGeneration());
                        updateMyProfile(body.userInfo);
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));

                        onGameNotification("Башня \'" + tower.title + "\' построена", NotificationType.SUCCESS);
                    }
                }
            } catch (IOException e) {
                onGameNotification("Не удалось построить башню из-за сетевой ошибки.", NotificationType.ERROR);
            }
        });
    }

    public void destroyTower(Tower tower) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            try {
                Response<GsonDestroyTowerResponse> response = api().destroyTower(tower.serverId, TowerUpdateAction.destroy).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    onGameNotification("Башня устояла. Сервер вернул " + response.code(), NotificationType.ERROR);
                } else {
                    GsonDestroyTowerResponse body = response.body();
                    if (!body.success) {
                        onGameNotification("Башня устояла: " + body.error.message, NotificationType.ERROR);
                    } else {
                        me.towersCount--;

                        data().towers.delete(tower._id);
                        deleteTowerEvent.fire(tower);
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
                        onGameNotification("Башня \'" + tower.title + "\' удалена", NotificationType.SUCCESS);

                        startSync();
                    }
                }
            } catch (IOException e) {
                onGameNotification("Не удалось удалить башню из-за сетевой ошибки.", NotificationType.ERROR);
            }
        });
    }

    public void upgradeTower(Tower tower) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            try {
                Response<GsonUpdateTowerResponse> response = api().updateTower(tower.serverId, TowerUpdateAction.upgrade).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    onGameNotification("Ошибка. Сервер вернул " + response.code(), NotificationType.ERROR);
                } else {
                    GsonUpdateTowerResponse body = response.body();
                    if (!body.success) {
                        onGameNotification("Ошибка: " + body.error.message, NotificationType.ERROR);
                    } else {
                        tower.merge(body.tower);
                        data().towers.save(tower);
                        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));

                        updateMyProfile(body.userInfo);
                        onGameNotification("Башня \'" + tower.title + "\' обновлена", NotificationType.SUCCESS);
                    }
                }
            } catch (IOException e) {
                onGameNotification("Не удалось прокачать башню из-за сетевой ошибки.", NotificationType.ERROR);
            }
        });
    }


    public void attackTower(Tower tower) {
        int i;
        GsonBattleInfo battleInfo = null;
        synchronized (towersUnderAttack) {
            i = towersUnderAttack.indexOfKey(tower._id);
            if (i >= 0)
                battleInfo = towersUnderAttack.valueAt(i);
        }

        if (battleInfo != null) {
            hitTower(tower, battleInfo);
            return;
        }

        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH).execute(() -> {
            try {
                Response<GsonAttackResponse> response = api().attack(tower.serverId, TowersGameApi.START).execute();
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    onGameNotification("Атака захлебнулась из-за ошибки сервера: " + response.code(), NotificationType.ERROR);
                    return;
                }

                GsonAttackResponse body = response.body();
                if (!body.success) {
                    onGameNotification("Атака захлебнулась: " + body.error, NotificationType.ERROR);
                    return;
                }

                synchronized (towersUnderAttack) {
                    towersUnderAttack.put(tower._id, body.battleInfo);
                }
                hitTower(tower, body.battleInfo);
                BattleTowerAttackTask battleTask = new BattleTowerAttackTask(tower);
                battleTask.run();
            } catch (IOException e) {
                onGameNotification("Атака захлебнулась из-за сетевой ошибки", NotificationType.ERROR);
            }
        });

    }

    private void hitTower(Tower tower, GsonBattleInfo battleInfo) {
        if (tower.health <= 0)
            return;


        if (tower.health <= battleInfo.playerAttack) {
            onGameNotification("Побееда ", NotificationType.INFO);
            onGameNotification("Атака наносит башне урон " + tower.health + " (0)", NotificationType.INFO);
            tower.health = 0;

            synchronized (towersUnderAttack) {
                int i = towersUnderAttack.indexOfKey(tower._id);
                if (i < 0)
                    return;
                towersUnderAttack.removeAt(i);
            }

            data().towers.delete(tower._id);
            deleteTowerEvent.fire(tower);
            geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));

            ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
                try {
                    Response<GsonButtleResultsResponse> response = api().win(tower.serverId, TowersGameApi.STOP, true).execute();
                    if (response.code() != HttpURLConnection.HTTP_OK) {
                        onGameNotification("Похоже, мы победили, но результат неизвестен из-за ошибки сервера: " + response.code(), NotificationType.ERROR);
                    } else if (!response.body().success) {
                        onGameNotification("Похоже, мы победили, но результат неизвестен из-за ошибки сервера: " + response.body().error.message, NotificationType.ERROR);
                    } else {
                        onGameNotification("Мы победили", NotificationType.GOOD_NEWS);
                        updateMyProfile(response.body().info);
                    }
                } catch (IOException e) {
                    onGameNotification("Похоже, мы победили, но результат неизвестен из-за сетевой ошибки", NotificationType.ERROR);
                }
            });
        } else {
            tower.health -= battleInfo.playerAttack;
            onGameNotification("Атака наносит башне урон " + battleInfo.playerAttack + " (" + tower.health + ")", NotificationType.INFO);
            data().towers.save(tower, prefs().getTowersGeneration());
            geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
        }

    }

    private class BattleTowerAttackTask implements Runnable {
        private final Tower tower;

        private BattleTowerAttackTask(Tower tower) {
            this.tower = tower;
        }

        @Override
        public void run() {
            GsonBattleInfo battleInfo;
            synchronized (towersUnderAttack) {
                int i = towersUnderAttack.indexOfKey(tower._id);
                if (i < 0)
                    return;
                battleInfo = towersUnderAttack.valueAt(i);
            }

            onGameNotification("Башня наносит " + battleInfo.towerAttack + " урона", NotificationType.INFO);

            synchronized (me) {
                if (me.health.current <= battleInfo.towerAttack) {
                    me.health.current = 0;
                } else {
                    me.health.current -= battleInfo.towerAttack;
                }
            }

            if (me.health.current <= 0) {
                synchronized (towersUnderAttack) {
                    towersUnderAttack.remove(tower._id);
                }
                onGameNotification("Бой проигран", NotificationType.BAD_NEWS);

//                try {
//                    Response<GsonRetreatResponse> response = api().lose(tower.serverId, TowersGameApi.STOP, false).execute();
//                    if (response.code() != HttpURLConnection.HTTP_OK) {
//                        onGameNotification("Бой, похоже, проигран, но результат неизвестен из-за ошибки сервера: " + response.code(), NotificationType.ERROR);
//                    } else if (!response.body().success) {
//                        onGameNotification("Бой, похоже, проигран, но результат неизвестен из-за ошибки сервера: " + response.body().error.message, NotificationType.ERROR);
//                    } else {
//                        onGameNotification("Бой проигран", NotificationType.BAD_NEWS);
//                        updateMyProfile(response.body().userInfo);
//                    }
//                } catch (IOException e) {
//                    onGameNotification("Бой, похоже, проигран, но результат неизвестен из-за сетевой ошибки", NotificationType.ERROR);
//                }
            } else {
                ThreadPool.SCHEDULER.schedule(this, battleInfo.towerAttackFrequency, TimeUnit.SECONDS);
            }
            data().users.save(me);
            myProfileEvent.fire(me);
        }
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
        void onGameNewMessage(Notification args);
    }

    public interface TowersGeoDataChangedEventHandler {
        void onTowersGeoDataChanged(MapExtent extent);
    }

    public interface TowerDeleteEventHandler {
        void onTowerDelete(Tower tower);
    }
}
