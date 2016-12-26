package ru.mail.my.towers.model.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.util.ArrayList;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.diagnostics.DebugUtils.safeThrow;
import static ru.mail.my.towers.diagnostics.Logger.logDb;

public class NetworksTable extends SQLiteCommands<TowerNetwork> {

    private final String selectById;

    public NetworksTable(SQLiteDatabase db) {
        super(db, TowerNetwork.class);
        selectById = DbUtils.buildSelectById(TowerNetwork.class, ColumnNames.IS_MY);
    }

    public long save(TowerNetwork network, int generation) {
        network._generation = generation;
        return save(network);
    }


    public TowerNetwork selectByTowers(long[] serverIds) {
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

    public TowerNetwork selectByServerId(long netId) {
        String sql = DbUtils.buildSelectAll(TowerNetwork.class) + " where " + ColumnNames.SERVER_ID + " = ?";
        return DbUtils.readSingle(db, TowerNetwork.class, sql, String.valueOf(netId));
    }

    public ArrayList<TowerNetwork> select(MapExtent extent) {
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

    private void filterLocation(StringBuilder sb, String tableAlias, double lat1, double lng1, double lat2, double lng2) {
        sb.append(tableAlias).append(".").append(ColumnNames.LAT).append(" between ").append(lat1).append(" and ").append(lat2).append(" ");
        sb.append(" and ");
        sb.append(tableAlias).append(".").append(ColumnNames.LNG).append(" between ").append(lng1).append(" and ").append(lng2).append(" ");
    }

    public int deleteEmpty() {
        String whereClause =
                " (select count(*) " +
                        "       from " + AppData.TABLE_TOWERS + " t " +
                        "       where t." + ColumnNames.NETWORK + "=" + AppData.TABLE_TOWER_NETWORKS + "." + ColumnNames.ID + ") = 0\n";
        int delete = db.delete(AppData.TABLE_TOWER_NETWORKS, whereClause, null);

        logDb("deleteEmptyNetworks where %s: %d objects deleted", whereClause, delete);

        return delete;
    }

    @Override
    public long save(TowerNetwork obj) {

        long id = obj._id;
        if (id == 0) {
            return obj._id = insert(obj);
        } else {
            if (update(obj) != 1)
                safeThrow(new Exception("update failed TowerNetwork.id = " + id));
            return id;
        }
    }

    public ArrayList<TowerNetwork> selectMy() {
        Cursor cursor = db.rawQuery(selectById, new String[]{"1"});
        CursorWrapper<TowerNetwork> wrapper = new CursorWrapper<TowerNetwork>(cursor) {

            private Field[] map = DbUtils.mapCursorForRowType(cursor, TowerNetwork.class, null);

            @Override
            protected TowerNetwork get(Cursor cursor) {
                return DbUtils.readObjectFromCursor(cursor, new TowerNetwork(), map);
            }
        };

        try {
            return DbUtils.readToList(wrapper);
        } finally {
            wrapper.close();
        }
    }
}
