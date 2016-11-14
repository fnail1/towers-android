package ru.mail.my.towers.model.db;

import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.lang.reflect.Field;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.ColumnNames;
import ru.mail.my.towers.model.Tower;

public class TowersTable {
    private final ThreadLocal<SQLiteStatement> insert;
    private final ThreadLocal<SQLiteStatement> selectByServerId;
    private final ThreadLocal<SQLiteStatement> update;


    private final SQLiteDatabase db;

    public TowersTable(SQLiteDatabase db) {
        this.db = db;
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(Tower.class));
        selectByServerId = new SQLiteStatementSimpleBuilder(db, DbUtils.buildSelectAll(Tower.class) +
                "\nwhere " + ColumnNames.SERVER_ID + " = ?");
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(Tower.class));
    }

    public long save(Tower tower, int generation) {
        tower._generation = generation;
        if (tower._id == 0) {
            SQLiteStatement insertCmd = insert.get();
            DbUtils.bindAllArgsAsStrings(insertCmd, DbUtils.buildInsertArgs(tower));
            try {
                tower._id = insertCmd.executeInsert();
                if (tower._id != 0)
                    return tower._id;
            } catch (SQLiteConstraintException ignored) {
                // обработка ниже
            }

            SQLiteStatement selectCmd = selectByServerId.get();
            selectCmd.bindLong(1, tower.serverId);
            tower._id = selectCmd.simpleQueryForLong();
        }
        SQLiteStatement updateCmd = update.get();
        DbUtils.bindAllArgsAsStrings(updateCmd, DbUtils.buildUpdateArgs(tower));
        updateCmd.executeUpdateDelete();
        return tower._id;
    }

    public int deleteDeprecated(int generation, boolean my) {
        StringBuilder sb = new StringBuilder();

        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterMy(sb, my);
        return db.delete(AppData.TABLE_TOWERS,sb.toString(), null);
    }

    public int deleteDeprecated(int generation, boolean my, double lat1, double lng1, double lat2, double lng2) {
        StringBuilder sb = new StringBuilder();
        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterMy(sb, my);
        sb.append("\n\t and ");
        filterLocation(sb, lat1, lng1, lat2, lng2);
        return db.delete(AppData.TABLE_TOWERS,sb.toString(), null);
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

    private void filterLocation(StringBuilder sb, String tableAlias, double lat1, double lng1, double lat2, double lng2) {
        sb.append(tableAlias).append(".").append(ColumnNames.LAT).append(" between ").append(lat1).append(" and ").append(lat2).append(" ");
        sb.append(" and ");
        sb.append(tableAlias).append(".").append(ColumnNames.LNG).append(" between ").append(lng1).append(" and ").append(lng2).append(" ");
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
            public Tower get() {
                return DbUtils.readObjectFromCursor(cursor, new Tower(), towersCursorMap);
            }
        };
    }
}
