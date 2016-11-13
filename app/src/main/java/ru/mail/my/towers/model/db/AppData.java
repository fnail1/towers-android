package ru.mail.my.towers.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.service.Preferences;

public class AppData {
    public static final String ANONYMOUS = "anonymous";

    public static final String TABLE_TOWER_NETWORKS = "TowerNetworks";
    public static final String TABLE_TOWERS = "Towers";
    public static final String TABLE_USER_INFOS = "UserInfos";

    private final SQLiteDatabase db;
    private final TowersTable towers;

    public AppData(Context context, String userId) {
        SQLiteOpenHelper helper = new AppDataSQLiteOpenHelper(context, normalizeDbName(userId));
        db = helper.getWritableDatabase();
        towers = new TowersTable(db);
    }

    public TowersTable towers() {
        return towers;
    }

    public static String normalizeDbName(String userId) {
        if (TextUtils.isEmpty(userId))
            return ANONYMOUS;

        if (ANONYMOUS.equals(userId))
            throw new RuntimeException("WTF?");

        try {
            return URLEncoder.encode(userId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return String.valueOf(userId.hashCode());
        }
    }
}
