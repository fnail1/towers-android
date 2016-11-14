package ru.mail.my.towers.service;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit2.Response;
import ru.mail.my.towers.api.model.GsonGameInfoResponse;
import ru.mail.my.towers.api.model.GsonMyTowersResponse;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.api.model.GsonTowersNetworkInfo;
import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.prefs;

public class GameService {
    public final UserInfo me;

    public final ObservableEvent<MyProfileEventHandler, GameService, UserInfo> myProfileEvent = new ObservableEvent<MyProfileEventHandler, GameService, UserInfo>(this) {
        @Override
        protected void notifyHandler(MyProfileEventHandler handler, GameService sender, UserInfo args) {
            handler.onMyProfileChanged(args);
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

    public void updateMyProfile(String newName, int newColor, UpdateMyProfileCallback callback) {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH).execute(() -> {
            try {
                Response<GsonPutProfileResponse> response = api().setMyProfile(newName, Integer.toHexString(newColor)).execute();
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

    public interface MyProfileEventHandler {
        void onMyProfileChanged(UserInfo args);
    }

    public interface UpdateMyProfileCallback {
        void onUpdateMyProfileServerError(int code);

        void onUpdateMyProfileCommonServerError();

        void onUpdateMyProfileComplete();

        void onUpdateMyProfileNetworkError();
    }
}
