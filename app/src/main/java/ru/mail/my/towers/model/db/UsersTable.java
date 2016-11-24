package ru.mail.my.towers.model.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;

import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.ColumnNames;
import ru.mail.my.towers.model.UserInfo;

public class UsersTable {
    private final SQLiteDatabase db;
    private final ThreadLocal<SQLiteStatement> insert;
    private final ThreadLocal<SQLiteStatement> update;
    private final ThreadLocal<SQLiteStatement> selectByServerId;
    private final ThreadLocal<SQLiteStatement> updateByServerId;
    private final ThreadLocal<SQLiteStatement> serverIdToLocalId;

    public UsersTable(SQLiteDatabase db) {
        this.db = db;
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(UserInfo.class));
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(UserInfo.class));
        selectByServerId = new SQLiteStatementSimpleBuilder(db, DbUtils.buildSelectAll(UserInfo.class) +
                "\n where " + ColumnNames.SERVER_ID + " = ? ");
        updateByServerId = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(UserInfo.class, ColumnNames.SERVER_ID));
        serverIdToLocalId = new SQLiteStatementSimpleBuilder(db,
                " select " + ColumnNames.ID +
                        " from " + AppData.TABLE_USER_INFOS +
                        " where " + ColumnNames.SERVER_ID + " = ?");
    }

    public long save(UserInfo owner) {
        if (owner._id == 0) {
            if (owner.serverId != 0) {
                SQLiteStatement sql = serverIdToLocalId.get();
                sql.bindLong(1, owner.serverId);
                try {
                    owner._id = sql.simpleQueryForLong();
                } catch (SQLiteDoneException ignored) {
                }
            }
        }

        if (owner._id != 0) {
            SQLiteStatement sql = update.get();
            DbUtils.bindAllArgsAsStrings(sql, DbUtils.buildUpdateArgs(owner));
            if (sql.executeUpdateDelete() == 1)
                return owner._id;
        }

        SQLiteStatement sql = insert.get();
        DbUtils.bindAllArgsAsStrings(sql, DbUtils.buildInsertArgs(owner));
        return owner._id = sql.executeInsert();
    }

    public UserInfo select(long id) {
        return DbUtils.readSingle(db, UserInfo.class, DbUtils.buildSelectById(UserInfo.class), String.valueOf(id));
    }
}
