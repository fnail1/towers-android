package ru.mail.my.towers.model;

import ru.mail.my.towers.data.DbColumn;

public class AbsRow {
    @DbColumn(name = ColumnNames.ID)
    public long _id;

    public int _generation;
}
