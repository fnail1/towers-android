package ru.mail.my.towers.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AppData {
    public static final String ANONYMOUS = "anonymous";

    public static final String TABLE_TOWER_NETWORKS = "TowerNetworks";
    public static final String TABLE_TOWERS = "Towers";
    public static final String TABLE_USER_INFOS = "UserInfos";
    public static final String TABLE_NOTIFICATIONS = "Notifications";

    private final SQLiteDatabase db;
    public final TowersTable towers;
    public final UsersTable users;
    public final NetworksTable networks;
    public final MyTowersCommands myTower;
    public final NotificationsTable notifications;

    public AppData(Context context, String userId) {
        SQLiteOpenHelper helper = new AppDataSQLiteOpenHelper(context, normalizeDbName(userId));
        db = helper.getWritableDatabase();
        towers = new TowersTable(db);
        users = new UsersTable(db);
        networks = new NetworksTable(db);
        myTower = new MyTowersCommands(db);
        notifications = new NotificationsTable(db);
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

    public String getDbPath() {
        return db.getPath();
    }

//    public void test() {
//
//        db.execSQL("create table if not exists IntegerPoints (x integer, y integer)");
//        db.execSQL("create table if not exists DoublePoints (x real, y real)");
//
//        db.execSQL("delete from IntegerPoints");
//        db.execSQL("delete from DoublePoints");
//
//        Random rnd = new Random();
//        long t0 = SystemClock.elapsedRealtime();
//
//        for (int i = 0; i < 100; i++) {
//            for (int j = 0; j < 100; j++) {
//                int x = rnd.nextInt(180 * Tower2.COORD_MULTIPLIER);
//                int y = rnd.nextInt(180 * Tower2.COORD_MULTIPLIER);
//                db.execSQL("insert into IntegerPoints (x, y) values (" + x + ", " + y + ")");
//            }
//        }
//        long t1 = SystemClock.elapsedRealtime();
//
//        for (int i = 0; i < 100; i++) {
//            for (int j = 0; j < 100; j++) {
//                double x = 360 * rnd.nextDouble() - 180;
//                double y = 360 * rnd.nextDouble() - 180;
//                db.execSQL("insert into DoublePoints (x, y) values (" + x + ", " + y + ")");
//            }
//        }
//
//        long t2 = SystemClock.elapsedRealtime();
//
//        int count1 = 0;
//        int count2 = 0;
//
//        Cursor cursor = db.rawQuery("select x, y from IntegerPoints where x between " +
//                (-90 * Tower2.COORD_MULTIPLIER) + " and " + (90 * Tower2.COORD_MULTIPLIER) + " ", null);
//        try {
//            int v = 0;
//            if (cursor.moveToFirst()) {
//                do {
//                    v += cursor.getInt(0) + cursor.getInt(1);
//                    count1++;
//                } while (cursor.moveToNext());
//            }
//        } finally {
//            cursor.close();
//        }
//
//        long t3 = SystemClock.elapsedRealtime();
//
//        cursor = db.rawQuery("select x, y from DoublePoints where x between -90.0 and 90.0 ", null);
//        try {
//            double v = 0;
//            if (cursor.moveToFirst()) {
//                do {
//                    v += cursor.getDouble(0) + cursor.getDouble(1);
//                    count2++;
//                } while (cursor.moveToNext());
//            }
//        } finally {
//            cursor.close();
//        }
//
//        long t4 = SystemClock.elapsedRealtime();
//
//        Logger.logV("TEST", "insert ints: " + (t1 - t0));
//        Logger.logV("TEST", "insert doubles: " + (t2 - t1));
//        Logger.logV("TEST", "select ints " + count1 + ": " + (t3 - t2));
//        Logger.logV("TEST", "select doubles " + count2 + ": " + (t4 - t3));
//
//    }
}
