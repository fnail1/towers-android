package ru.mail.my.towers.model.db;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import ru.mail.my.towers.data.ConflictAction;
import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbColumn;
import ru.mail.my.towers.data.DbUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static ru.mail.my.towers.diagnostics.DebugUtils.safeThrow;
import static ru.mail.my.towers.diagnostics.Logger.logDb;

public abstract class SQLiteCommands<T> {

    private final ThreadLocal<SQLiteStatement> insert;
    private final ThreadLocal<SQLiteStatement> update;
    private final ThreadLocal<SQLiteStatement> delete;
    protected final String selectAll;
    private final String selectById;
    protected final SQLiteDatabase db;
    protected final Class<T> rowType;
    private final Field primaryKey;
    protected final String rawTypeSimpleName;
    private final String selectRange;

    public SQLiteCommands(SQLiteDatabase db, Class<T> rowType) {
        this.db = db;
        this.rowType = rowType;
        primaryKey = DbUtils.iterateFields(rowType)
                .first(f -> {
                    DbColumn a = f.getAnnotation(DbColumn.class);
                    return a != null && a.primaryKey();
                });
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(rowType, ConflictAction.IGNORE));
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(rowType));
        delete = new SQLiteStatementSimpleBuilder(db, DbUtils.buildDelete(rowType));
        selectAll = DbUtils.buildSelectAll(rowType);
        selectRange = DbUtils.buildSelectAll(rowType) + "\n limit ? offset ? ";
        selectById = DbUtils.buildSelectById(rowType);
        rawTypeSimpleName = rowType.getSimpleName();
    }

    public long insert(T raw) {
        SQLiteStatement statement = insert.get();
        String[] args = DbUtils.buildInsertArgs(raw);
        DbUtils.bindAllArgsAsStrings(statement, args);
        long r = statement.executeInsert();
        logDb("INSERT %s %s returns %d", rawTypeSimpleName, raw, r);
        return r;
    }

    public int update(T raw) {
        SQLiteStatement statement = update.get();
        String[] args = DbUtils.buildUpdateArgs(raw);
        DbUtils.bindAllArgsAsStrings(statement, args);
        int r = statement.executeUpdateDelete();
        logDb("UPDATE %s %s returns %d", rawTypeSimpleName, raw, r);
        return r;
    }

    public int delete(long id) {
        SQLiteStatement statement = delete.get();
        statement.bindLong(1, id);
        int r = statement.executeUpdateDelete();
        logDb("DELETE %s %d returns %d", rawTypeSimpleName, id, r);
        return r;
    }

    public T selectById(long id) {
        String[] args = {String.valueOf(id)};
        return DbUtils.readSingle(db, rowType, selectById, args);
    }

    public final CursorWrapper<T> selectAll() {
        logDb("SELECT ALL FROM %s", rawTypeSimpleName);

        return new SimpleCursorWrapper(db.rawQuery(selectAll, null));
    }

    public ArrayList toList() {
        CursorWrapper<T> cursor = selectAll();
        try {
            return DbUtils.readToList(cursor);
        } finally {
            cursor.close();
        }
    }

    public long save(T obj) {
        logDb("WARNING! Avoid using default save for %s ", rawTypeSimpleName);

        long id = getId(obj);
        if (id == 0) {
            return setId(obj, insert(obj));
        } else {
            if (update(obj) != 1)
                safeThrow(new Exception("update failed " + rawTypeSimpleName + ".id = " + id));
            return id;
        }
    }

    protected long getId(T obj) {
        if (primaryKey == null)
            throw new IllegalArgumentException();
        try {
            return primaryKey.getLong(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected long setId(T obj, long id) {
        try {
            primaryKey.setLong(obj, id);
            return id;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int count() {
        return DbUtils.count(db, DbUtils.getTableName(rowType));
    }

    public CursorWrapper<T> select(int skip, int limit) {

        logDb("SELECT RANGE(%d, %d) FROM %s", skip, limit, rawTypeSimpleName);

        String[] args = {String.valueOf(limit), String.valueOf(skip)};
        return new SimpleCursorWrapper(db.rawQuery(selectRange, args));
    }

    protected class SimpleCursorWrapper extends CursorWrapper<T> {
        final Field[] map;

        public SimpleCursorWrapper(Cursor cursor) {
            super(cursor);
            map = DbUtils.mapCursorForRowType(this.cursor, rowType, null);
        }

        @Override
        protected T get(Cursor cursor) {
            try {
                return DbUtils.readObjectFromCursor(cursor, rowType.newInstance(), map);
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }
}
