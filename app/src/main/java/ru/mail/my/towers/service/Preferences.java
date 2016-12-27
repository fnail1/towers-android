package ru.mail.my.towers.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import ru.mail.my.towers.model.db.AppData;
import ru.mail.my.towers.model.UserGoldInfo;
import ru.mail.my.towers.model.UserHealthInfo;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.model.UserRole;


public class Preferences {
    private static final String USER_ID = "USER_ID";

    private static final String SERVER_TIME_OFFSET = "server_time_offset";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String MY_DB_ID = "my_db_id";
    private static final String GENERATION_MY_TOWERS = "generation_my_towers";
    private static final String GENERATION_TOWERS = "generation_towers";
    private static final String FREE_SCROLL_ENABLED = "free_scroll_enabled";


    private final SharedPreferences common;
    SharedPreferences personal;

    public Preferences(Context context) {
        common = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = common.getString(USER_ID, null);
        personal = context.getSharedPreferences(AppData.normalizeDbName(userId), Context.MODE_PRIVATE);
    }


    public void onLogin(Context context, String userId, String token) {
        personal = context.getSharedPreferences(AppData.normalizeDbName(userId), Context.MODE_PRIVATE);
        personal.edit().putString(AUTH_TOKEN, token).apply();
        common.edit().putString(USER_ID, userId).apply();
    }

    @Nullable
    public String getUserId() {
        return common.getString(USER_ID, null);
    }

    public long getServerTimeOffset() {
        return common.getLong(SERVER_TIME_OFFSET, 0);
    }

    public void setServerTimeOffset(long serverTimeOffset) {
        common.edit().putLong(SERVER_TIME_OFFSET, serverTimeOffset).apply();
    }

    public String getAccessToken() {
        return personal.getString(AUTH_TOKEN, null);
    }

    public long getMeDbId() {
        return personal.getLong(MY_DB_ID, -1);
    }

    public void setMeDbId(long value) {
        personal.edit().putLong(MY_DB_ID, value).apply();
    }

    public int getMyTowersGeneration() {
        return personal.getInt(GENERATION_MY_TOWERS, 1);
    }


    public void setMyTowersGeneration(int value) {
        personal.edit().putInt(GENERATION_MY_TOWERS, value).apply();
    }

    public int getTowersGeneration() {
        return personal.getInt(GENERATION_TOWERS, 1);
    }


    public void setTowersGeneration(int value) {
        personal.edit().putInt(GENERATION_TOWERS, value).apply();
    }

    public boolean freeScrollEnabled() {
        return personal.getBoolean(FREE_SCROLL_ENABLED, false);
    }

    public void setFreeScrollEnabled(boolean value) {
        personal.edit().putBoolean(FREE_SCROLL_ENABLED, value).apply();
    }
}
