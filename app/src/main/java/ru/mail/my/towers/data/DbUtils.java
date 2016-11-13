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
import java.util.List;

import ru.mail.my.towers.diagnostics.DebugUtils;
import ru.mail.my.towers.toolkit.Flags32;
import ru.mail.my.towers.toolkit.collections.Query;

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

    private static Query<Field> iterateFields(Class<?> t) {
        Query<Field> query = query(t.getDeclaredFields());
        while ((t = t.getSuperclass()) != Object.class)
            query = query.concat(t.getDeclaredFields());
        return query.where(DbUtils::checkTransient).select(f->{
            f.setAccessible(true);
            return f;
        });
    }

    @NonNull
    public static String buildCreateScript(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ");
        String tableName = getTableName(rawType);
        sb.append(tableName);
        sb.append(" (");

        for (Field field : iterateFields(rawType)) {
            createColumnDefinition(field, sb);
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());
        sb.append(") ");

        return sb.toString();
    }


    @NonNull
    public static String getTableName(@NonNull Class<?> rawType) {
        DbTable meta = rawType.getAnnotation(DbTable.class);
        return meta != null && !TextUtils.isEmpty(meta.name()) ? meta.name() : rawType.getSimpleName();
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
    public static String buildInsert(@NonNull Class<?> rawType) {
        return buildInsert(rawType, null);
    }

    @NonNull
    public static String buildInsert(@NonNull Class<?> rawType, @Nullable ConflictAction onConflict) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert");
        if (onConflict != null)
            sb.append(" or ").append(onConflict.name());
        sb.append(" into ");
        sb.append(getTableName(rawType));
        sb.append(" (");

        int fieldCount = 0;
        for (Field field : iterateFields(rawType)) {
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
    public static String[] buildInsertArgs(@NonNull Object raw) {
        if (raw instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) raw).onBeforeSerialization();

        Query<String> query = iterateFields(raw.getClass()).where(field -> {
            DbColumn column = field.getAnnotation(DbColumn.class);
            return column == null || !column.primaryKey();
        }).select(field -> {
            try {
                Object value = field.get(raw);

                if (value == null)
                    return null;

                if (field.getType().isEnum())
                    return String.valueOf(((Enum<?>) value).ordinal());

                if (field.getType() == Flags32.class)
                    return String.valueOf(((Flags32) value).getValue());

                if (field.getType() == boolean.class || field.getType() == Boolean.class)
                    return ((boolean) value) ? "1" : "0";


                return String.valueOf(value);
            } catch (IllegalAccessException e) {
                return "error";
            }
        });
        List<String> list = query.toList();
        return list.toArray(new String[list.size()]);
    }

    @NonNull
    public static String buildUpdate(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getTableName(rawType));
        sb.append(" set ");
        String pk = null;

        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey()) {
                pk = getColumnName(field, column);
                continue;
            }
            sb.append(getColumnName(field, column));
            sb.append(" = ?, ");
        }

        sb.delete(sb.length() - 2, sb.length());

        sb.append("where ").append(pk).append(" = ?");

        return sb.toString();
    }


    @NonNull
    public static String[] buildUpdateArgs(@NonNull Object raw) {
        if (raw instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) raw).onBeforeSerialization();

        try {
            ArrayList<String> values = new ArrayList<>();
            String pk = null;
            for (Field field : iterateFields(raw.getClass())) {
                DbColumn column = field.getAnnotation(DbColumn.class);
                Object v = field.get(raw);
                if (column != null && column.primaryKey()) {
                    pk = v.toString();
                    continue;
                }
                String value;
                if (v == null) {
                    value = null;
                } else if (field.getType().isEnum()) {
                    value = String.valueOf(((Enum<?>) v).ordinal());
                } else if (field.getType() == Flags32.class) {
                    value = String.valueOf(((Flags32) v).getValue());
                } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    value = ((boolean) v) ? "1" : "0";
                } else {
                    value = String.valueOf(v);
                }
                values.add(value);
            }
            values.add(pk);
            return values.toArray(new String[values.size()]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void buildComplexColumnNames(@NonNull Class<?> rawType, String tableAlias, @NonNull StringBuilder out) {
        for (Field field : iterateFields(rawType)) {
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
    public static String buildSelectAll(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\nfrom ");
        sb.append(getTableName(rawType));
        sb.append('\n');
        return sb.toString();
    }

    @NonNull
    public static String buildSelectById(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");

        String pk = null;
        for (Field field : iterateFields(rawType)) {
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
        sb.append(getTableName(rawType));
        sb.append(" where ");
        sb.append(pk);
        sb.append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static <T> ArrayList<T> readToList(@NonNull Cursor cursor, @NonNull Class<T> rawType, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rawType, mapCursorForRawType(cursor, rawType, tableAlias));
        ArrayList<T> list = new ArrayList<>(cursor.getCount());
        for (T item : reader) {
            list.add(item);
        }

        return list;
    }

    @NonNull
    public static <T> ArrayList<T> readToList(@NonNull CursorWrapper<T> cursor) {
        ArrayList<T> list = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.get());
            } while (cursor.moveToNext());
        }

        return list;
    }

    @NonNull
    public static <K, T> HashMap<K, T> readToMap(@NonNull Cursor cursor, @NonNull Class<T> rawType) {
        Field[] fields = rawType.getFields();
        for (Field field : fields) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                return readToMap(cursor, rawType, field, null);
        }

        throw new RuntimeException();
    }

    @NonNull
    public static <K, T> HashMap<K, T> readToMap(@NonNull Cursor cursor, @NonNull Class<T> rawType, @NonNull Field keyColumn, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rawType, mapCursorForRawType(cursor, rawType, tableAlias));
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

    public static <T> T readSingle(SQLiteDatabase db, Class<T> rawType, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            if (cursor.moveToFirst())
                return readObjectFromCursor(cursor, rawType.newInstance(), mapCursorForRawType(cursor, rawType, null));
            else
                return null;
        } catch (InstantiationException e) {
            DebugUtils.safeThrow(e);
            return null;
        } catch (IllegalAccessException e) {
            DebugUtils.safeThrow(e);
            return null;
        } finally {
            cursor.close();
        }
    }

    @NonNull
    public static <T> LongSparseArray<T> readToLongSparseArray(@NonNull Cursor cursor, @NonNull Class<T> rawType, @NonNull Field keyColumn, String tableAlias) {
        CursorReader<T> reader = new CursorReader<>(cursor, rawType, mapCursorForRawType(cursor, rawType, tableAlias));
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

    public static Field[] mapCursorForRawType(Cursor cursor, Class<?> rawType, String tableAlias) {
        Field[] fields = new Field[cursor.getColumnCount()];
        if (tableAlias != null)
            for (Field field : iterateFields(rawType)) {
                field.setAccessible(true);
                int columnIndex = cursor.getColumnIndex(getColumnAlias(field, tableAlias));
                if (columnIndex >= 0) {
                    fields[columnIndex] = field;
                }
            }
        else
            for (Field field : iterateFields(rawType)) {
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

    public static class CursorReader<T> implements Iterable<T> {
        private final Cursor cursor;
        private final Field[] fields;
        private final Class<T> rawType;

        public CursorReader(Cursor cursor, Class<T> rawType, Field[] cursorMap) {
            this.cursor = cursor;
            this.rawType = rawType;
            this.fields = cursorMap;
        }

        public CursorReader(Cursor cursor, Class<T> rawType, String tableAlias) {
            this(cursor, rawType, mapCursorForRawType(cursor, rawType, tableAlias));
        }

        public CursorReader(Cursor cursor, Class<T> rawType) {
            this(cursor, rawType, mapCursorForRawType(cursor, rawType, null));
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
                        return readObjectFromCursor(cursor, rawType.newInstance(), fields);
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


    private static boolean checkTransient(Field field) {
        int modifiers = field.getModifiers();

        if (Modifier.isTransient(modifiers))
            return false;

        if (Modifier.isStatic(modifiers))
            return false;

        return !Modifier.isFinal(modifiers) || field.getType() == Flags32.class;
    }

    public enum SQLiteType {
        INTEGER, REAL, TEXT, BLOB
    }


}
