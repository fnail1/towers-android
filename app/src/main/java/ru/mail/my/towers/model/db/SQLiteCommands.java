package ru.mail.my.towers.model.db;

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
    protected final Class<T> rawType;
    private final Field primaryKey;
    private final String rawTypeSimpleName;

    public SQLiteCommands(SQLiteDatabase db, Class<T> rawType) {
        this.db = db;
        this.rawType = rawType;
        primaryKey = DbUtils.iterateFields(rawType)
                .first(f -> {
                    DbColumn a = f.getAnnotation(DbColumn.class);
                    return a != null && a.primaryKey();
                });
        insert = new SQLiteStatementSimpleBuilder(db, DbUtils.buildInsert(rawType, ConflictAction.IGNORE));
        update = new SQLiteStatementSimpleBuilder(db, DbUtils.buildUpdate(rawType));
        delete = new SQLiteStatementSimpleBuilder(db, DbUtils.buildDelete(rawType));
        selectAll = DbUtils.buildSelectAll(rawType);
        selectById = DbUtils.buildSelectById(rawType);
        rawTypeSimpleName = rawType.getSimpleName();
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
        return DbUtils.readSingle(db, rawType, selectById, args);
    }

    public final CursorWrapper<T> selectAll() {
        logDb("SELECT ALL FROM %s", rawTypeSimpleName);

        Cursor cursor = db.rawQuery(selectAll, null);
        return new CursorWrapper<T>(cursor) {
            Field[] map = DbUtils.mapCursorForRawType(cursor, rawType, null);

            @Override
            protected T get(Cursor cursor) {
                try {
                    return DbUtils.readObjectFromCursor(cursor, rawType.newInstance(), map);
                } catch (InstantiationException e) {
                    return null;
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
        };
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
}
