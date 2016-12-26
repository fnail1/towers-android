package ru.mail.my.towers.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.toolkit.Flags32;
import ru.mail.my.towers.toolkit.collections.Query;

import static ru.mail.my.towers.diagnostics.DebugUtils.safeThrow;
import static ru.mail.my.towers.toolkit.collections.Query.query;


public class DbUtils {

    public static final String COLUMN_ID = "_id";

    private static final HashMap<Class<?>, SQLiteType> TYPE_MAP = new HashMap<Class<?>, SQLiteType>() {
        {
            put(byte.class, SQLiteType.INTEGER);
            put(short.class, SQLiteType.INTEGER);
            put(int.class, SQLiteType.INTEGER);
            put(long.class, SQLiteType.INTEGER);
            put(float.class, SQLiteType.REAL);
            put(double.class, SQLiteType.REAL);
            put(boolean.class, SQLiteType.INTEGER);
            put(char.class, SQLiteType.TEXT);
            put(byte[].class, SQLiteType.BLOB);
            put(Byte.class, SQLiteType.INTEGER);
            put(Short.class, SQLiteType.INTEGER);
            put(Integer.class, SQLiteType.INTEGER);
            put(Long.class, SQLiteType.INTEGER);
            put(Float.class, SQLiteType.REAL);
            put(Double.class, SQLiteType.REAL);
            put(Boolean.class, SQLiteType.INTEGER);
            put(Character.class, SQLiteType.TEXT);
            put(String.class, SQLiteType.TEXT);
            put(Byte[].class, SQLiteType.BLOB);
        }
    };

    public static Query<Field> iterateFields(Class<?> t) {
        Query<Field> query = query(t.getDeclaredFields());
        while ((t = t.getSuperclass()) != Object.class)
            query = query.concat(t.getDeclaredFields());
        return query.where(DbUtils::checkTransient).select(f -> {
            f.setAccessible(true);
            return f;
        });
    }

    @NonNull
    public static String buildCreateScript(@NonNull Class<?> rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ");
        String tableName = getTableName(rowType);
        sb.append(tableName);
        sb.append(" (");

        for (Field field : iterateFields(rowType)) {
            createColumnDefinition(field, sb);
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());
        sb.append(") ");

        String s = sb.toString();
        Logger.logDb("buildCreateScript %s", s);
        return s;
    }


    @NonNull
    public static String getTableName(@NonNull Class<?> rowType) {
        DbTable meta = rowType.getAnnotation(DbTable.class);
        return meta != null && !TextUtils.isEmpty(meta.name()) ? meta.name() : rowType.getSimpleName();
    }

    public static void createColumnDefinition(@NonNull Field field, @NonNull StringBuilder out) {
        Class<?> type = field.getType();
        DbColumn column = field.getAnnotation(DbColumn.class);
        final String name = getColumnName(field, column);

        SQLiteType sqLiteType = TYPE_MAP.get(type);
        if (sqLiteType == null) {
            if (type.isEnum())
                sqLiteType = SQLiteType.INTEGER;
            else if (type == Flags32.class)
                sqLiteType = SQLiteType.INTEGER;
            else
                throw new IllegalArgumentException("Can't serialize field " + name);
        }

        out.append(name);
        out.append(" ");
        out.append(sqLiteType.toString());

        if (column != null) {
            if (column.primaryKey()) {
                out.append(" PRIMARY KEY AUTOINCREMENT");
            }

            if (column.length() > -1) {
                out.append(" (").append(column.length()).append(")");
            }

            if (column.notNull()) {
                out.append(" NOT NULL ON CONFLICT ").append(column.onNullConflict());
            }

            if (column.unique()) {
                out.append(" UNIQUE ON CONFLICT ").append(column.onUniqueConflict().toString());
            }
        }

        DbForeignKey fk = field.getAnnotation(DbForeignKey.class);
        if (fk != null) {
            out.append(" REFERENCES ");
            out.append(fk.table());
            out.append('(').append(fk.column()).append(')');
            out.append(" ON DELETE ");
            out.append(fk.onDelete().toString().replace("_", " "));
            out.append(" ON UPDATE ");
            out.append(fk.onUpdate().toString().replace("_", " "));
        }
    }

    @NonNull
    public static String getColumnName(@NonNull Field field) {
        return getColumnName(field, field.getAnnotation(DbColumn.class));
    }

    @NonNull
    private static String getColumnName(@NonNull Field field, @Nullable DbColumn column) {
        return column != null && !TextUtils.isEmpty(column.name()) ? column.name() : field.getName();
    }

    @NonNull
    public static String getColumnAlias(@NonNull Field field, String tableAlias) {
        return getColumnAlias(field, field.getAnnotation(DbColumn.class), tableAlias);
    }

    @NonNull
    private static String getColumnAlias(@NonNull Field field, @Nullable DbColumn column, String tableAlias) {
        return tableAlias + "_" + (column != null && !TextUtils.isEmpty(column.name()) ? column.name() : field.getName());
    }

    @NonNull
    public static String buildInsert(@NonNull Class<?> rowType) {
        return buildInsert(rowType, null);
    }

    @NonNull
    public static String buildInsert(@NonNull Class<?> rowType, @Nullable ConflictAction onConflict) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert");
        if (onConflict != null)
            sb.append(" or ").append(onConflict.name());
        sb.append(" into ");
        sb.append(getTableName(rowType));
        sb.append(" (");

        int fieldCount = 0;
        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                continue;
            sb.append(getColumnName(field, column));
            fieldCount++;
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());

        sb.append(") values (");
        sb.append("?");
        for (int i = 1; i < fieldCount; i++)
            sb.append(", ?");
        sb.append(")");

        return sb.toString();
    }

    @NonNull
    public static String[] buildInsertArgs(@NonNull Object row) {
        if (row instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) row).onBeforeSerialization();

        ArrayList<String> list = new ArrayList<>(row.getClass().getDeclaredFields().length);
        for (Field field : iterateFields(row.getClass())) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                continue;

            String value = getFieldValueAsString(row, field, column);
            list.add(value);
        }
        return list.toArray(new String[list.size()]);
    }

    @NonNull
    public static String buildUpdate(@NonNull Class<?> rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getTableName(rowType));
        sb.append(" set ");
        String pk = null;

        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey()) {
                pk = getColumnName(field, column);
                continue;
            }
            sb.append(getColumnName(field, column));
            sb.append(" = ?, ");
        }

        sb.delete(sb.length() - 2, sb.length());

        sb.append("\nwhere ").append(pk).append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static String buildUpdate(@NonNull Class<?> rowType, String keyColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getTableName(rowType));
        sb.append(" set ");
        String pk = null;

        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && (column.primaryKey() || column.equals(keyColumn)))
                continue;
            sb.append(getColumnName(field, column));
            sb.append(" = ?, ");
        }

        sb.delete(sb.length() - 2, sb.length());

        sb.append("\n where ").append(keyColumn).append(" = ?");

        return sb.toString();
    }

    public static <T> String buildDelete(Class<T> rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from  ");
        sb.append(getTableName(rowType));
        sb.append(" where ");

        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey()) {
                sb.append(getColumnName(field, column));
                sb.append(" = ? ");
                return sb.toString();
            }
        }
        sb.append("_id = ? ");
        return sb.toString();
    }

    @NonNull
    public static String[] buildUpdateArgs(@NonNull Object row) {
        if (row instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) row).onBeforeSerialization();

        try {
            ArrayList<String> values = new ArrayList<>();
            String pk = null;
            for (Field field : iterateFields(row.getClass())) {
                DbColumn column = field.getAnnotation(DbColumn.class);
                if (column != null && column.primaryKey()) {
                    pk = field.get(row).toString();
                    continue;
                }
                String value = getFieldValueAsString(row, field, column);
                values.add(value);
            }
            values.add(pk);
            return values.toArray(new String[values.size()]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static String getFieldValueAsString(@NonNull Object row, Field field, DbColumn column) {
        try {
            String value;
            if (field.getType().isEnum()) {
                value = String.valueOf(((Enum<?>) field.get(row)).ordinal());
            } else if (field.getType() == Flags32.class) {
                value = String.valueOf(((Flags32) field.get(row)).get());
            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                value = field.getBoolean(row) ? "1" : "0";
            } else if (shouldReplaceZeroWithNull(row, field, column))
                value = null;
            else {
                Object obj = field.get(row);
                value = obj == null ? null : obj.toString();
            }

            return value;
        } catch (IllegalAccessException e) {
            return "error";
        }
    }

    private static boolean shouldReplaceZeroWithNull(@NonNull Object row, Field field, DbColumn column) throws IllegalAccessException {
        return (column != null && column.unique() || field.getAnnotation(DbForeignKey.class) != null)
                && ((field.getType() == long.class && field.getLong(row) == 0L)
                || (field.getType() == int.class && field.getInt(row) == 0));
    }

    public static void buildComplexColumnNames(@NonNull Class<?> rowType, String tableAlias, @NonNull StringBuilder out) {
        for (Field field : iterateFields(rowType)) {
            out.append(tableAlias)
                    .append('.')
                    .append(getColumnName(field))
                    .append(" as ")
                    .append(getColumnAlias(field, tableAlias))
                    .append(", ");
        }
        out.delete(out.length() - 2, out.length());
    }

    @NonNull
    public static String buildSelectAll(@NonNull Class<?> rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\nfrom ");
        sb.append(getTableName(rowType));
        sb.append('\n');
        return sb.toString();
    }

    @NonNull
    public static String buildSelectById(@NonNull Class<?> rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");

        String pk = null;
        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            if (column != null && column.primaryKey()) {
                pk = name;
            }
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(" from ");
        sb.append(getTableName(rowType));
        sb.append(" where ");
        sb.append(pk);
        sb.append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static String buildSelectById(@NonNull Class<?> rowType, String keyColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");

        for (Field field : iterateFields(rowType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(" from ");
        sb.append(getTableName(rowType));
        sb.append(" where ");
        sb.append(keyColumn);
        sb.append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static <T> ArrayList<T> readToList(@NonNull Cursor cursor, @NonNull Class<T> rowType, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rowType, mapCursorForRowType(cursor, rowType, tableAlias));
        ArrayList<T> list = new ArrayList<>(cursor.getCount());
        for (T item : reader) {
            list.add(item);
        }

        return list;
    }

    @NonNull
    public static <K, T> HashMap<K, T> readToMap(@NonNull Cursor cursor, @NonNull Class<T> rowType) {
        Field[] fields = rowType.getFields();
        for (Field field : fields) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                return readToMap(cursor, rowType, field, null);
        }

        throw new RuntimeException();
    }

    @NonNull
    public static <K, T> HashMap<K, T> readToMap(@NonNull Cursor cursor, @NonNull Class<T> rowType, @NonNull Field keyColumn, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rowType, mapCursorForRowType(cursor, rowType, tableAlias));
        HashMap<K, T> list = new HashMap<>(cursor.getCount());
        for (T item : reader) {
            try {
                @SuppressWarnings("unchecked")
                K key = (K) keyColumn.get(item);
                list.put(key, item);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return list;
    }

    public static <T> T readSingle(SQLiteDatabase db, Class<T> rowType, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            if (cursor.moveToFirst())
                return readObjectFromCursor(cursor, rowType.newInstance(), mapCursorForRowType(cursor, rowType, null));
            else
                return null;
        } catch (InstantiationException e) {
            safeThrow(e);
            return null;
        } catch (IllegalAccessException e) {
            safeThrow(e);
            return null;
        } finally {
            cursor.close();
        }
    }

    @NonNull
    public static <T> LongSparseArray<T> readToLongSparseArray(@NonNull Cursor cursor, @NonNull Class<T> rowType, @NonNull Field keyColumn, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rowType, mapCursorForRowType(cursor, rowType, tableAlias));
        LongSparseArray<T> list = new LongSparseArray<>(cursor.getCount());
        for (T item : reader) {
            try {
                @SuppressWarnings("unchecked")
                long key = keyColumn.getLong(item);
                list.put(key, item);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return list;
    }


    public static <T> LongSparseArray<T> readToLongSparseArray(CursorWrapper<T> cursor, Field keyColumn) {
        LongSparseArray<T> list = new LongSparseArray<>(cursor.getCount());
        try {
            if (cursor.moveToFirst()) {
                do {
                    T obj = cursor.get();
                    long key = keyColumn.getLong(obj);
                    list.put(key, obj);
                } while (cursor.moveToNext());
            }
        } catch (IllegalAccessException e) {
            safeThrow(e);
        } finally {
            cursor.close();
        }
        return list;
    }

    @NonNull
    public static <T, R extends T> ArrayList<T> readToList(CursorWrapper<R> cursor) {
        ArrayList<T> list = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.get());
            } while (cursor.moveToNext());
        }
        return list;
    }

    public static int count(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(String.format("select count(*) from %s", tableName), null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static int count(SQLiteDatabase db, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static long longCount(SQLiteDatabase db, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static Field[] mapCursorForRowType(Cursor cursor, Class<?> rowType, String tableAlias) {
        Field[] fields = new Field[cursor.getColumnCount()];
        if (tableAlias != null)
            for (Field field : iterateFields(rowType)) {
                field.setAccessible(true);
                int columnIndex = cursor.getColumnIndex(getColumnAlias(field, tableAlias));
                if (columnIndex >= 0) {
                    fields[columnIndex] = field;
                }
            }
        else
            for (Field field : iterateFields(rowType)) {
                field.setAccessible(true);
                int columnIndex = cursor.getColumnIndex(getColumnName(field));
                if (columnIndex >= 0) {
                    fields[columnIndex] = field;
                }
            }
        return fields;
    }

    @NonNull
    public static <T> T readObjectFromCursor(Cursor cursor, T next, Field[] cursorMap) {
        try {
            for (int i = 0; i < cursorMap.length; i++) {
                Field field = cursorMap[i];
                if (field == null)
                    continue;

                if (cursor.isNull(i))
                    continue;

                Class<?> fieldType = field.getType();
                if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                    field.set(next, (byte) cursor.getInt(i));
                } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                    field.set(next, (short) cursor.getInt(i));
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    field.set(next, cursor.getInt(i));
                } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    field.set(next, cursor.getLong(i));
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    field.set(next, cursor.getFloat(i));
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    field.set(next, cursor.getDouble(i));
                } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    field.set(next, cursor.getInt(i) != 0);
                } else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
                    field.set(next, cursor.getString(i).charAt(0));
                } else if (fieldType.equals(String.class)) {
                    field.set(next, cursor.getString(i));
                } else if (fieldType.equals(Byte[].class) || fieldType.equals(byte[].class)) {
                    field.set(next, cursor.getBlob(i));
                } else if (fieldType.isEnum()) {
                    field.set(next, fieldType.getEnumConstants()[cursor.getInt(i)]);
                } else if (fieldType == Flags32.class) {
                    Object value = field.get(next);
                    if (value == null) {
                        field.set(next, new Flags32(cursor.getInt(i)));
                    } else {
                        ((Flags32) value).set(cursor.getInt(i));
                    }
                } else
                    throw new IllegalArgumentException();
            }

            if (next instanceof IDbSerializationHandlers)
                ((IDbSerializationHandlers) next).onAfterDeserialization();

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return next;
    }

    public static void bindAllArgsAsStrings(SQLiteStatement sql, String[] args) {
        if (args != null) {
            for (int i = args.length; i != 0; i--) {
                String arg = args[i - 1];
                if (arg != null) {
                    sql.bindString(i, arg);
                } else {
                    sql.bindNull(i);
                }
            }
        }
    }

    public static void join(StringBuilder sb, String fktable, final String fkalias, String fkcolumn, final String pktable, String pkcolumn) {
        sb.append("join ").append(fktable).append(" " + fkalias + " on " + fkalias + ".").append(fkcolumn).append("=" + pktable + ".").append(pkcolumn).append("\n");
    }

    private static boolean checkTransient(Field field) {
        int modifiers = field.getModifiers();

        if (Modifier.isTransient(modifiers))
            return false;

        if (Modifier.isStatic(modifiers))
            return false;

        return !Modifier.isFinal(modifiers) || field.getType() == Flags32.class;
    }

    public static class CursorReader<T> implements Iterable<T> {
        private final Cursor cursor;
        private final Field[] fields;
        private final Class<T> rowType;

        public CursorReader(Cursor cursor, Class<T> rowType, Field[] cursorMap) {
            this.cursor = cursor;
            this.rowType = rowType;
            this.fields = cursorMap;
        }

        public CursorReader(Cursor cursor, Class<T> rowType, String tableAlias) {
            this(cursor, rowType, mapCursorForRowType(cursor, rowType, tableAlias));
        }

        public CursorReader(Cursor cursor, Class<T> rowType) {
            this(cursor, rowType, mapCursorForRowType(cursor, rowType, null));
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private boolean hasNext = cursor.moveToFirst();

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public T next() {
                    //noinspection TryWithIdenticalCatches
                    try {
                        return readObjectFromCursor(cursor, rowType.newInstance(), fields);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } finally {
                        hasNext = cursor.moveToNext();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    public enum SQLiteType {
        INTEGER, REAL, TEXT, BLOB
    }

}
