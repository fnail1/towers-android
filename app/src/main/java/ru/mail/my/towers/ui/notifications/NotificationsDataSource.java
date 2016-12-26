package ru.mail.my.towers.ui.notifications;

import java.util.List;

import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.Notification;
import ru.mail.my.towers.ui.PagedDataSource;

import static ru.mail.my.towers.TowersApp.data;

public class NotificationsDataSource extends PagedDataSource<Notification> {

    private final int count;

    public NotificationsDataSource() {
        count = data().notifications.count();
    }

    @Override
    protected List<Notification> prepareDataSync(int skip, int limit) {
        return DbUtils.readToList(data().notifications.select(skip, limit));
    }

    @Override
    public int count() {
        return count;
    }
}
