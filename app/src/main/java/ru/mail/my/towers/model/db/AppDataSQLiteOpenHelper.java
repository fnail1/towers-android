package ru.mail.my.towers.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.UserInfo;

public class AppDataSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;

    public AppDataSQLiteOpenHelper(Context context, String name) {
        super(context, name, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbUtils.buildCreateScript(Tower.class));
        db.execSQL(DbUtils.buildCreateScript(TowerNetwork.class));
        db.execSQL(DbUtils.buildCreateScript(UserInfo.class));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
