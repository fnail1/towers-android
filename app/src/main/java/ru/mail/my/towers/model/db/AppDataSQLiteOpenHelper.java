package ru.mail.my.towers.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import ru.mail.my.towers.BuildConfig;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.model.UserInfo;

import static ru.mail.my.towers.diagnostics.Logger.logDb;

public class AppDataSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;

    public AppDataSQLiteOpenHelper(Context context, String name) {
        super(context, name, getCursorFactory(), VERSION);
    }


    @Nullable
    private static SQLiteDatabase.CursorFactory getCursorFactory() {
        if (!BuildConfig.DEBUG)
            return null;

        return (db, masterQuery, editTable, query) -> {
            logDb(query.toString());
            return new SQLiteCursor(masterQuery, editTable, query);
        };
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
