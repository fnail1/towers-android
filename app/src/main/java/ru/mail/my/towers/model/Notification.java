package ru.mail.my.towers.model;

import java.util.Calendar;

import ru.mail.my.towers.data.DbColumn;
import ru.mail.my.towers.data.DbTable;
import ru.mail.my.towers.model.db.AppData;

@DbTable(name = AppData.TABLE_NOTIFICATIONS)
public class Notification {
    @DbColumn(primaryKey = true)
    public long _id;
    public String message;
    public long ts;
    public NotificationType type;

    private transient Calendar time;

    public Calendar getTime() {
        if (time == null) {
            time = Calendar.getInstance();
            time.setTimeInMillis(ts);
        }
        return time;
    }
}
