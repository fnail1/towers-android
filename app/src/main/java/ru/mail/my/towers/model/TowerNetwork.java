package ru.mail.my.towers.model;

import ru.mail.my.towers.data.DbTable;
import ru.mail.my.towers.model.db.AppData;

@DbTable(name = AppData.TABLE_TOWER_NETWORKS)
public class TowerNetwork extends AbsRow {
    /**
     * сколько сеть добывает золота
     */
    public float goldGain;

    /**
     * площадь сети
     */
    public float area;
}
