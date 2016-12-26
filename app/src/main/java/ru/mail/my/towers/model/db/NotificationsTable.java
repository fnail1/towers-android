package ru.mail.my.towers.model.db;

import android.database.sqlite.SQLiteDatabase;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.Notification;

import static ru.mail.my.towers.diagnostics.Logger.logDb;

public class NotificationsTable extends SQLiteCommands<Notification> {
    private final String selectRange;

    public NotificationsTable(SQLiteDatabase db) {
        super(db, Notification.class);
        selectRange = DbUtils.buildSelectAll(rowType)
                + "\n order by " + ColumnNames.ID + " desc "
                + "\n limit ? offset ? ";

    }

    @Override
    public CursorWrapper<Notification> select(int skip, int limit) {

        logDb("SELECT RANGE(%d, %d) FROM %s", skip, limit, rawTypeSimpleName);

        String[] args = {String.valueOf(limit), String.valueOf(skip)};
        return new SimpleCursorWrapper(db.rawQuery(selectRange, args));
    }
}
