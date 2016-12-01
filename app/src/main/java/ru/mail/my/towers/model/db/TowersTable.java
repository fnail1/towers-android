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

import static ru.mail.my.towers.diagnostics.DebugUtils.safeThrow;
import static ru.mail.my.towers.toolkit.collections.Query.query;

public class TowersTable {
    private final ThreadLocal<SQLiteStatement> insert;
    private final ThreadLocal<SQLiteStatement> update;
    private final ThreadLocal<SQLiteStatement> selectIdByServerId;

    private final ThreadLocal<SQLiteStatement> insertNetwork;
    private final ThreadLocal<SQLiteStatement> updateNetwork;


    private final SQLiteDatabase db;

    public TowersTable(SQLiteDatabase db) {
        this.db = db;
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(Tower.class));
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(Tower.class));
        selectIdByServerId = new SQLiteStatementSimpleBuilder(db, "select " + ColumnNames.ID + " from " + AppData.TABLE_TOWERS + " where " + ColumnNames.SERVER_ID + " = ?");

        insertNetwork = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(TowerNetwork.class));
        updateNetwork = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(TowerNetwork.class));
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

            SQLiteStatement selectCmd = selectIdByServerId.get();
            selectCmd.bindLong(1, tower.serverId);
            try {
                tower._id = selectCmd.simpleQueryForLong();
            } catch (Exception ignored) {
            }
        }
        SQLiteStatement updateCmd = update.get();
        DbUtils.bindAllArgsAsStrings(updateCmd, DbUtils.buildUpdateArgs(tower));
        if (updateCmd.executeUpdateDelete() != 1)
            safeThrow(new Exception("update failed id = " + tower._id));
        return tower._id;
    }

    public long save(TowerNetwork network, int generation) {
        network._generation = generation;
        if (network._id == 0) {
            SQLiteStatement insertCmd = insertNetwork.get();
            DbUtils.bindAllArgsAsStrings(insertCmd, DbUtils.buildInsertArgs(network));
            return network._id = insertCmd.executeInsert();
        } else {
            SQLiteStatement updateCmd = updateNetwork.get();
            DbUtils.bindAllArgsAsStrings(updateCmd, DbUtils.buildUpdateArgs(network));
            if (updateCmd.executeUpdateDelete() != 1)
                safeThrow(new Exception("update failed id = " + network._id));
            return network._id;
        }
    }

    public boolean deleteDeprecated(int generation, boolean my) {
        StringBuilder sb = new StringBuilder();

        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterMy(sb, my);
        int delete = db.delete(AppData.TABLE_TOWERS, sb.toString(), null);

        deleteEmptyNetworks();
        int delete1 = 0;

        return delete > 0 || delete1 > 0;
    }

    public int deleteEmptyNetworks() {
        String whereClause =
                " (select count(*) " +
                        "       from " + AppData.TABLE_TOWERS + " t " +
                        "       where t." + ColumnNames.NETWORK + "=" + AppData.TABLE_TOWER_NETWORKS + "." + ColumnNames.ID + ") = 0\n";
        return db.delete(AppData.TABLE_TOWER_NETWORKS, whereClause, null);
    }

    public boolean deleteDeprecated(int generation, double lat1, double lng1, double lat2, double lng2) {
        StringBuilder sb = new StringBuilder();
        sb.append(ColumnNames.GENERATION).append("<>").append(generation).append(" and \n\t");
        filterLocation(sb, lat1, lng1, lat2, lng2);
        int delete = db.delete(AppData.TABLE_TOWERS, sb.toString(), null);

        int delete1 = deleteEmptyNetworks();

        return delete > 0 || delete1 > 0;
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

    public Tower selectByServerId(long serverId) {
        return DbUtils.readSingle(db, Tower.class, DbUtils.buildSelectById(Tower.class, ColumnNames.SERVER_ID), String.valueOf(serverId));
    }

    public ArrayList<TowerNetwork> selectNetworks(MapExtent extent) {
        StringBuilder sb = new StringBuilder();
        sb.append("select distinct ");
        DbUtils.buildComplexColumnNames(TowerNetwork.class, "n", sb);
        sb.append("\n");
        sb.append("from ").append(AppData.TABLE_TOWER_NETWORKS).append(" n \n");
        sb.append("join ").append(AppData.TABLE_TOWERS).append(" t on t.").append(ColumnNames.NETWORK).append(" = n.").append(ColumnNames.ID).append("\n");
        sb.append("where ");
        filterLocation(sb, "t", extent.lat1, extent.lng1, extent.lat2, extent.lng2);
        Cursor cursor = db.rawQuery(sb.toString(), null);
        try {
            return DbUtils.readToList(cursor, TowerNetwork.class, "n");
        } finally {
            cursor.close();
        }
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

    public TowerNetwork selectNetworkByTowers(long[] serverIds) {
        if (serverIds.length == 0)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append("select n.* \n");
        sb.append("from ").append(AppData.TABLE_TOWER_NETWORKS).append(" n \n");
        sb.append("join ").append(AppData.TABLE_TOWERS).append(" t on t.").append(ColumnNames.NETWORK).append(" = n.").append(ColumnNames.ID).append(" \n");
        sb.append("where t.").append(ColumnNames.SERVER_ID).append(" in (");
        for (long serverId : serverIds) {
            sb.append(serverId).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(")\n");
        sb.append("limit 1");

        return DbUtils.readSingle(db, TowerNetwork.class, sb.toString());
    }

    public TowerNetwork selectNetworkByServerId(long netId) {
        String sql = DbUtils.buildSelectAll(TowerNetwork.class) + " where " + ColumnNames.SERVER_ID + " = ?";
        return DbUtils.readSingle(db, TowerNetwork.class, sql, String.valueOf(netId));
    }

    public TowerNetwork selectNetworkById(long network) {
        return DbUtils.readSingle(db, TowerNetwork.class, DbUtils.buildSelectById(TowerNetwork.class), String.valueOf(network));
    }

    public Tower selectById(long towerId) {
        return DbUtils.readSingle(db, Tower.class, DbUtils.buildSelectById(Tower.class), String.valueOf(towerId));
    }

    public void delete(long id) {
        db.delete(AppData.TABLE_TOWERS, ColumnNames.ID + "=?", new String[]{String.valueOf(id)});
        deleteEmptyNetworks();
    }


}
