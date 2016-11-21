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
    //    private static final String PROFILE_SERVER_ID = "profile_server_id";
//    private static final String PROFILE_NAME = "profile_name";
//    private static final String PROFILE_COLOR = "profile_color";
//    private static final String PROFILE_EXP = "profile_exp";
//    private static final String PROFILE_HEALTH = "profile_health";
//    private static final String PROFILE_ROLE = "profile_role";
//    private static final String PROFILE_HEALTH_MAX = "profile_health_max";
//    private static final String PROFILE_HEALTH_REGEN = "profile_health_regen";
//    private static final String PROFILE_AREA = "profile_area";
//    private static final String PROFILE_TOWER_COST = "profile_tower_cost";
//    private static final String PROFILE_LEVEL = "profile_level";
//    private static final String PROFILE_GOLD = "profile_gold";
//    private static final String PROFILE_GOLD_GAIN = "profile_gold_gain";
//    private static final String PROFILE_GOLD_FREQUENCY = "profile_gold_frequency";
//    private static final String PROFILE_EXP_NEXT = "profile_exp_next";
    private static final String GENERATION_MY_TOWERS = "generation_my_towers";
    private static final String GENERATION_TOWERS = "generation_towers";


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
//    public UserInfo getMyProfile() {
//        UserInfo profile = new UserInfo();
//        profile.name = personal.getString(PROFILE_NAME, "");
//        profile.serverId = personal.getLong(PROFILE_SERVER_ID, -1);
//        profile.color = personal.getInt(PROFILE_COLOR, UserInfo.DEFAULT_COLOR);
//        profile.exp = personal.getInt(PROFILE_EXP, 0);
//        profile.role = UserRole.values()[personal.getInt(PROFILE_ROLE, UserRole.none.ordinal())];
//        profile.health.current = personal.getInt(PROFILE_HEALTH, 0);
//        profile.health.max = personal.getInt(PROFILE_HEALTH_MAX, 0);
//        profile.health.regeneration = personal.getInt(PROFILE_HEALTH_REGEN, 0);
//        profile.area = Double.longBitsToDouble(personal.getLong(PROFILE_AREA, 0));
//        profile.createCost = personal.getInt(PROFILE_TOWER_COST, 0);
//        profile.currentLevel = personal.getInt(PROFILE_LEVEL, 0);
//        profile.gold.current = personal.getInt(PROFILE_GOLD, 0);
//        profile.gold.gain = personal.getInt(PROFILE_GOLD_GAIN, 0);
//        profile.gold.frequency = personal.getInt(PROFILE_GOLD_FREQUENCY, 0);
//        profile.nextExp = personal.getInt(PROFILE_EXP_NEXT, 0);
//
//
//        return profile;
//    }
//
//
//    public void setMyProfile(UserInfo p) {
//        personal.edit()
//                .putString(PROFILE_NAME, p.name)
//                .putLong(PROFILE_SERVER_ID, p.serverId)
//                .putInt(PROFILE_COLOR, p.color)
//                .putInt(PROFILE_EXP, p.exp)
//                .putInt(PROFILE_ROLE, p.role.ordinal())
//                .putInt(PROFILE_HEALTH, p.health.current)
//                .putInt(PROFILE_HEALTH_MAX, p.health.max)
//                .putInt(PROFILE_HEALTH_REGEN, p.health.regeneration)
//                .putLong(PROFILE_AREA, Double.doubleToLongBits(p.area))
//                .putInt(PROFILE_TOWER_COST, p.createCost)
//                .putInt(PROFILE_LEVEL, p.currentLevel)
//                .putInt(PROFILE_GOLD, p.gold.current)
//                .putInt(PROFILE_GOLD_GAIN, p.gold.gain)
//                .putInt(PROFILE_GOLD_FREQUENCY, p.gold.frequency)
//                .putInt(PROFILE_EXP_NEXT, p.nextExp)
//                .apply();
//    }

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
}
