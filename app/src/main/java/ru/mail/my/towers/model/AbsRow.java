package ru.mail.my.towers.model;

import ru.mail.my.towers.data.DbColumn;
import ru.mail.my.towers.model.db.ColumnNames;

public class AbsRow {
    @DbColumn(name = ColumnNames.ID, primaryKey = true)
    public long _id;

    public int _generation;
}
