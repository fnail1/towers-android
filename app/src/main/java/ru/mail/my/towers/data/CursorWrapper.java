package ru.mail.my.towers.data;

import android.database.Cursor;

import java.io.Closeable;

public abstract class CursorWrapper<T> implements Closeable {
    protected final Cursor cursor;

    public CursorWrapper(Cursor cursor) {
        this.cursor = cursor;
    }

    public int getCount() {
        return cursor.getCount();
    }

    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    public boolean moveToNext() {
        return cursor.moveToNext();
    }

    @Override
    public void close() {
        cursor.close();
    }

    public abstract T get();

}
