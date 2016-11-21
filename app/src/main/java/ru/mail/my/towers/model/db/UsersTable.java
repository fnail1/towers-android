package ru.mail.my.towers.model.db;

import android.database.sqlite.SQLiteDatabase;
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

    public UsersTable(SQLiteDatabase db) {
        this.db = db;
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(UserInfo.class));
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(UserInfo.class));
        selectByServerId = new SQLiteStatementSimpleBuilder(db, DbUtils.buildSelectAll(UserInfo.class) +
                "\n where " + ColumnNames.SERVER_ID + " = ? ");
        updateByServerId = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(UserInfo.class, ColumnNames.SERVER_ID));
    }

    public long save(UserInfo owner) {
        if (owner._id != 0) {
            SQLiteStatement sql = update.get();
            DbUtils.bindAllArgsAsStrings(sql, DbUtils.buildUpdateArgs(owner));
            if (sql.executeUpdateDelete() == 1)
                return owner._id;
        } else if (owner.serverId > 0) {
            SQLiteStatement sql = updateByServerId.get();
            DbUtils.bindAllArgsAsStrings(sql, DbUtils.buildUpdateArgs(owner, ColumnNames.SERVER_ID, String.valueOf(owner.serverId)));
            if (sql.executeUpdateDelete() == 1)
                return owner._id;
        }
        SQLiteStatement sql = insert.get();
        DbUtils.bindAllArgsAsStrings(sql, DbUtils.buildInsertArgs(owner));
        return owner._id = sql.executeInsert();
    }

    public UserInfo select(long meDbId) {
        return DbUtils.readSingle(db, UserInfo.class, DbUtils.buildSelectById(UserInfo.class), String.valueOf(meDbId));
    }
}
