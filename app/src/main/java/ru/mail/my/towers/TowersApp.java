package ru.mail.my.towers;

import android.app.Application;

import ru.mail.my.towers.api.TowersGameApi;
import ru.mail.my.towers.model.db.AppData;
import ru.mail.my.towers.service.AppStateService;
import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.service.Preferences;
import ru.mail.my.towers.toolkit.ThreadPool;

public class TowersApp extends Application {

    private static TowersApp instance;
    private LocationAppService locationService;
    private Preferences preferences;
    private AppStateService appStateService;
    private TowersGameApi api;
    private GameService gameService;
    private AppData data;

    public static TowersApp app() {
        return instance;
    }

    public static AppData data() {
        return instance.data;
    }

    public static Preferences prefs() {
        return instance.preferences;
    }

    public static LocationAppService location() {
        return instance.locationService;
    }

    public static AppStateService appState() {
        return instance.appStateService;
    }

    public static TowersGameApi api() {
        return instance.api;
    }

    public static GameService game() {
        return instance.gameService;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = new Preferences(this);
        data = new AppData(this, preferences.getUserId());
        appStateService = new AppStateService(this, preferences);
        locationService = new LocationAppService(this, appStateService);
        api = TowersGameApi.Builder.createInstance(TowersGameApi.BASE_URL);
        gameService = new GameService(preferences, data);

        instance = this;

        gameService.start();
    }

    public void onLogin(String number, String token) {
        preferences.onLogin(this, number, token);
        data = new AppData(this, preferences.getUserId());
        gameService = new GameService(preferences, data);
    }

    public void onLogout() {
        onLogin(null, null);
    }
}
