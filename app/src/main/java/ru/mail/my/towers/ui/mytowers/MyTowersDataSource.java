package ru.mail.my.towers.ui.mytowers;

import java.util.Arrays;
import java.util.List;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.ui.PagedDataSource;

import static ru.mail.my.towers.TowersApp.data;

public class MyTowersDataSource extends PagedDataSource<MyTowersListItem> {
    private final int count;

    MyTowersDataSource() {
        super();
        count = data().myTower.count();
    }

    @Override
    protected List<MyTowersListItem> prepareDataSync(int skip, int limit) {

        CursorWrapper<MyTowersListItem> cursor = data().myTower.select(skip, limit);

        try {
            return DbUtils.readToList(cursor);
        } finally {
            cursor.close();
        }
    }

    @Override
    public int count() {
        return count;
    }
}
