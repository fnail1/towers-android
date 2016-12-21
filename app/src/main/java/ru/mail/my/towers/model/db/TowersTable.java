package ru.mail.my.towers.model.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.reflect.Field;
import java.util.ArrayList;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.ColumnNames;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.diagnostics.DebugUtils.safeThrow;
import static ru.mail.my.towers.diagnostics.Logger.logDb;

public class TowersTable extends SQLiteCommands<Tower> {
    private final ThreadLocal<SQLiteStatement> selectIdByServerId;

    public TowersTable(SQLiteDatabase db) {
        super(db, Tower.class);
        selectIdByServerId = new SQLiteStatementSimpleBuilder(db, "select " + ColumnNames.ID + " from " + AppData.TABLE_TOWERS + " where " + ColumnNames.SERVER_ID + " = ?");
    }

    public long save(Tower tower, int generation) {
        tower._generation = generation;
        if (tower._id == 0) {
            tower._id = insert(tower);
            if (tower._id > 0)
                return tower._id;

            SQLiteStatement selectCmd = selectIdByServerId.get();
            selectCmd.bindLong(1, tower.serverId);
            try {
                tower._id = selectCmd.simpleQueryForLong();
            } catch (Exception ignored) {
            }
        }
        update(tower);
        return tower._id;
    }

    public boolean deleteDeprecated(int generation, boolean my) {
        StringBuilder sb = new StringBuilder();

        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterMy(sb, my);
        int delete = db.delete(AppData.TABLE_TOWERS, sb.toString(), null);

        if (delete > 0) {
            data().networks().deleteEmpty();
        }

        logDb("deleteDeprecated %d %s: %d objects deleted", generation, my ? "my" : "their", delete);

        return delete > 0;
    }


    public boolean deleteDeprecated(int generation, double lat1, double lng1, double lat2, double lng2) {
        StringBuilder sb = new StringBuilder();
        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterLocation(sb, lat1, lng1, lat2, lng2);
        int delete = db.delete(AppData.TABLE_TOWERS, sb.toString(), null);

        if (delete > 0) {
            data().networks().deleteEmpty();
        }

        logDb("deleteDeprecated %f;%f - %f;%f: %d objects deleted", lat1, lng1, lat2, lng2, delete);

        return delete > 0;
    }

    private void filterMy(StringBuilder sb, boolean my) {
        sb.append(ColumnNames.IS_MY).append(" = ").append(my ? 1 : 0);
    }

    private void filterMy(StringBuilder sb, String tableAlias, boolean my) {
        sb.append(tableAlias).append(".");
        sb.append(ColumnNames.IS_MY).append(" = ").append(my ? 1 : 0);
    }

    private void filterLocation(StringBuilder sb, double lat1, double lng1, double lat2, double lng2) {
        sb.append(ColumnNames.LAT).append(" between ").append(lat1).append(" and ").append(lat2).append(" ");
        sb.append(" and ");
        sb.append(ColumnNames.LNG).append(" between ").append(lng1).append(" and ").append(lng2).append(" ");
    }

    public CursorWrapper<Tower> select(double lat1, double lng1, double lat2, double lng2) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        DbUtils.buildComplexColumnNames(Tower.class, "t", sb);
        sb.append("\n from ").append(AppData.TABLE_TOWERS).append(" t \n");
        sb.append("where ");
        filterLocation(sb, lat1, lng1, lat2, lng2);

        return new CursorWrapper<Tower>(db.rawQuery(sb.toString(), null)) {
            Field[] towersCursorMap = DbUtils.mapCursorForRawType(cursor, Tower.class, "t");

            @Override
            public Tower get(Cursor cursor) {
                return DbUtils.readObjectFromCursor(cursor, new Tower(), towersCursorMap);
            }
        };
    }

    public Tower selectByServerId(long serverId) {
        return DbUtils.readSingle(db, Tower.class, DbUtils.buildSelectById(Tower.class, ColumnNames.SERVER_ID), String.valueOf(serverId));
    }

    @NonNull
    public ArrayList<Tower> select(ArrayList<TowerNetwork> networks) {
        if (networks.isEmpty())
            return new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        sb.append("select * \n");
        sb.append("from ").append(AppData.TABLE_TOWERS).append(" n \n");
        sb.append("where ").append(ColumnNames.NETWORK).append(" in (");
        for (TowerNetwork network : networks)
            sb.append(network._id).append(", ");
        sb.delete(sb.length() - 2, sb.length());
        sb.append(")");

        Cursor cursor = db.rawQuery(sb.toString(), null);
        try {
            return DbUtils.readToList(cursor, Tower.class, null);
        } finally {
            cursor.close();
        }
    }

    public int countOfMy() {
        return DbUtils.count(db, "select count (*) from " + AppData.TABLE_TOWERS + " where " + ColumnNames.IS_MY + " = 1", (String[]) null);
    }


}
