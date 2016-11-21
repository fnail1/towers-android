package ru.mail.my.towers.service;

import android.location.Location;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonCreateTowerResponse;
import ru.mail.my.towers.api.model.GsonGameInfoResponse;
import ru.mail.my.towers.api.model.GsonGetProfileResponse;
import ru.mail.my.towers.api.model.GsonMyTowersResponse;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersNetworkInfo;
import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.api.model.GsonUserProfile;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.mapObjects;
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

    public GameService(Preferences preferences) {
        me = preferences.getMyProfile();

    }

    public void updateMyProfile(GsonUserInfo data) {
        this.me.merge(data);
        prefs().setMyProfile(me);
        myProfileEvent.fire(me);
    }


    public void updateMyProfile(GsonUserProfile data) {
        this.me.merge(data);
        prefs().setMyProfile(me);
        myProfileEvent.fire(me);
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
            if (gameInfoResponse.isSuccessful()) {
                GsonGameInfoResponse gameInfo = gameInfoResponse.body();
                me.merge(gameInfo.info);
                prefs().setMyProfile(me);
                myProfileEvent.fire(me);
            }

            Response<GsonGetProfileResponse> profileResponse = api().getMyProfile().execute();
            if(profileResponse.code() == HttpURLConnection.HTTP_OK){
                GsonGetProfileResponse body = profileResponse.body();
                if (body.success) {
                    updateMyProfile(body.profile);
                }
            }

            int generation = prefs().getMyTowersGeneration() + 1;

            Response<GsonMyTowersResponse> myTowersResponse = api().getMyTowers().execute();
            if (myTowersResponse.isSuccessful()) {
                GsonMyTowersResponse myTowers = myTowersResponse.body();
                if (myTowers.success) {
                    for (GsonTowersNetworkInfo towersNet : myTowers.towersNets) {
                        for (GsonTowerInfo towerInfo : towersNet.inside) {
                            Tower tower = new Tower(towerInfo);
                            data().towers().save(tower, generation);
                        }
                    }
                    prefs().setMyTowersGeneration(generation);
                    int deleted = data().towers().deleteDeprecated(generation, true);
                    Logger.logV("selection", "" + deleted + " objects deleted");

                }
            }
//
//            Response<GsonCreateTowerResponse> createTowerResponse = api().createTower(55.874765, 37.912708, "Моя первая башня").execute();
//            GsonCreateTowerResponse body = createTowerResponse.body();
//            Tower t = new Tower(body.tower);
//            data().towers().save(t, generation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTower(Location location, String name) {
        createTower(location.getLatitude(), location.getLongitude(), name);
    }

    private void createTower(double latitude, double longitude, String name) {
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
                        updateMyProfile(body.userInfo);
                        Tower tower = new Tower(body.tower);
                        data().towers().save(tower, prefs().getMyTowersGeneration());
                        mapObjects().loadMapObjects(latitude - .1, longitude - .1, latitude + .1, longitude + .1);
                        gameMessageEvent.fire("Башня \'" + tower.title + "\' построена");
                    }
                }
            } catch (IOException e) {
                gameMessageEvent.fire("Не удалось построить башню из-за сетевой ошибки.");
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
}
