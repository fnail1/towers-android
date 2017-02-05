package ru.mail.my.towers.service;

import android.location.Location;
import android.util.LongSparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import ru.mail.my.towers.api.model.GsonBattleInfo;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonUserProfile;
import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.gis.GisUtils;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.Notification;
import ru.mail.my.towers.model.NotificationType;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.model.db.AppData;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.prefs;

public class GameService {
    public static final int BASE_GOLD_GAIN = 10;
    public static final int BASE_TOWER_HEALTH = 100;
    public static final int BASE_TOWER_RADIUS = 10;
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
            userInfo.health.current = 100;
            userInfo.health.regeneration = 1;
            userInfo.health.max = 100;
            userInfo.towersCount = 0;
            userInfo.gold.frequency = 30;
            userInfo.gold.current = 1000;
            userInfo.gold.gain = 1;
            userInfo.area = 0;
            userInfo.color = 0xffff0000;
            userInfo.createCost = 10;
            userInfo.currentLevel = 1;
            userInfo.exp = 0;
            userInfo.serverId = 100500;

        }
        me = userInfo;
    }

    public void start() {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(this::startSync);
    }

    private void startSync() {

    }

    public void updateMyProfile(GsonUserProfile data) {
        this.me.merge(data);

        if (me.serverId != 0) {
            data().users.save(me);
            myProfileEvent.fire(me);
        }
    }

    public void updateMyProfile(String newName, int newColor, UpdateMyProfileCallback callback) {
        me.color = newColor;
        me.name = newName;
        if (me.serverId != 0) {
            data().users.save(me);
            myProfileEvent.fire(me);
        }
        callback.onUpdateMyProfileComplete();
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

    public void createTower(Location location, String name) {
        createTower(location.getLatitude(), location.getLongitude(), name);
    }

    public void createTower(double latitude, double longitude, String name) {
        ThreadPool.DB.execute(() -> {
            me.towersCount++;

            Tower tower = new Tower();
            tower.lng = longitude;
            tower.lat = latitude;
            tower.my = true;
            tower.level = me.currentLevel;
            fillWithDefaults(tower);
            tower.health = 75;
            tower.title = name;
            tower.owner = me._id;

            LatLng p0 = new LatLng(latitude, longitude);
            LatLng[] points = new LatLng[4];
            points[0] = SphericalUtil.computeOffset(p0, tower.radius, 0);
            points[1] = SphericalUtil.computeOffset(p0, tower.radius, 90);
            points[2] = SphericalUtil.computeOffset(p0, tower.radius, 180);
            points[3] = SphericalUtil.computeOffset(p0, tower.radius, 270);
            MapExtent extent = new MapExtent(points);
            tower.extLatMin = extent.lat1;
            tower.extLngMin = extent.lng1;
            tower.extLatMax = extent.lat2;
            tower.extLngMax = extent.lng2;

            TowerNetwork network;
            CursorWrapper<Tower> cursor = data().towers.select(tower.extLatMin, tower.extLngMin, tower.extLatMax, tower.extLngMax);
            try {
                if (cursor.moveToFirst()) {
                    long netId = -1;
                    LongSparseArray<Boolean> toUnion = null;
                    do {
                        Tower t = cursor.get();
                        if (!t.my) {
                            onGameNotification("Здесь строить нельзя", NotificationType.ERROR);
                            return;
                        }

                        LatLng p1 = new LatLng(t.lat, t.lng);
                        if (SphericalUtil.computeDistanceBetween(p0, p1) < (tower.radius + t.radius)) {
                            if (netId < 0) {
                                netId = t.network;
                            } else if (t.network != netId) {
                                if (toUnion == null)
                                    toUnion = new LongSparseArray<>(4);
                                toUnion.put(t.network, Boolean.TRUE);
                            }
                        }
                    } while (cursor.moveToNext());
                    if (toUnion != null) {
                        for (int i = 0; i < toUnion.size(); i++) {
                            data().towers.unionNetworks(netId, toUnion.keyAt(i));
                        }
                        data().networks.deleteEmpty();
                    }
                    network = data().networks.selectById(netId);
                } else {
                    network = new TowerNetwork();
                    network.maxHealth = tower.maxHealth;
                    network.level = tower.level;
                    network.my = true;
                    network.area = (float) (Math.PI * tower.radius * tower.radius);
                }
            } finally {
                cursor.close();
            }

            cursor = data().towers.selectByNetwork(network._id);

            ArrayList<Tower> towers;
            try {
                towers = DbUtils.readToList(cursor);
            } finally {
                cursor.close();
            }

            towers.add(tower);
            network.area = GisUtils.calcArea(towers);
            double sumLat = 0, sumLng = 0;
            int sumLvl = 0, sumHP = 0, sumMaxHP = 0;
            for (Tower t : towers) {
                sumLat += t.lat;
                sumLng += t.lng;
                sumLvl += t.level;
                sumHP += t.health;
                sumMaxHP += t.maxHealth;
            }
            network.color = me.color;

            double centerLat = sumLat / towers.size();
            double centerLng = sumLng / towers.size();
            double dmin = Double.MAX_VALUE;
            Tower closest = null;
            for (Tower t : towers) {
                double dlat = t.lat - centerLat;
                double dlng = t.lng - centerLng;
                double d = dlat * dlat + dlng * dlng;
                if (d < dmin) {
                    dmin = d;
                    closest = t;
                }
            }

            data().networks.save(network);
            tower.network = network._id;
            data().towers.save(tower, prefs().getMyTowersGeneration());
            geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));

            onGameNotification("Башня \'" + tower.title + "\' построена", NotificationType.SUCCESS);
        });
    }

    public void fillWithDefaults(Tower tower) {
        tower.goldGain = BASE_GOLD_GAIN;
        tower.maxHealth = BASE_TOWER_HEALTH;
        tower.radius = BASE_TOWER_RADIUS * tower.level;
    }

    public void destroyTower(Tower tower) {
        me.towersCount--;

        TowerNetwork network = data().networks.selectById(tower.network);
        network.area = GisUtils.substractTowerArea(network, tower);
        data().towers.delete(tower._id);
        deleteTowerEvent.fire(tower);
        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));
        onGameNotification("Башня \'" + tower.title + "\' удалена", NotificationType.SUCCESS);
    }

    public void upgradeTower(Tower tower) {
        if (tower.level == me.currentLevel)
            return;
        tower.level++;
        fillWithDefaults(tower);
        data().towers.save(tower);

        TowerNetwork network = data().networks.selectById(tower.network);
        network.area = GisUtils.calcArea(network);
        data().networks.save(network);

        geoDataChangedEvent.fire(new MapExtent(tower.lat, tower.lng));

        onGameNotification("Башня \'" + tower.title + "\' обновлена", NotificationType.SUCCESS);
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
        battleInfo = new GsonBattleInfo();
        battleInfo.towerAttack = 5 * tower.level;
        battleInfo.playerAttack = me.currentLevel;
        battleInfo.towerAttackFrequency = 3;
        battleInfo.towerHealth = tower.health;

        synchronized (towersUnderAttack) {
            towersUnderAttack.put(tower._id, battleInfo);
        }
        hitTower(tower, battleInfo);
        BattleTowerAttackTask battleTask = new BattleTowerAttackTask(tower);
        battleTask.run();

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
