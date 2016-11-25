package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonTowersNetworkInfo;
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

    /**
     * уровень сети (кластера) - среднее арифметическое уровня всех башен в сети
     */
    public int level;

    public long serverId;


    public TowerNetwork() {
    }

    public TowerNetwork(GsonTowersNetworkInfo towersNet) {
        merge(towersNet);
    }

    public void merge(GsonTowersNetworkInfo towersNet) {
        goldGain = towersNet.goldGain;
        area = towersNet.area;
    }

}
