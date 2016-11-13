package ru.mail.my.towers.model.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class SQLiteStatementSimpleBuilder extends ThreadLocal<SQLiteStatement> {
    private final SQLiteDatabase db;
    private final String sql;


    public SQLiteStatementSimpleBuilder(SQLiteDatabase db, String sql) {
        this.db = db;
        this.sql = sql;
    }

    @Override
    protected SQLiteStatement initialValue() {
        return db.compileStatement(sql);
    }

}
