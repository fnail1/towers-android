package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonTowerInfo;
import ru.mail.my.towers.data.DbColumn;
import ru.mail.my.towers.data.DbForeignKey;
import ru.mail.my.towers.data.DbTable;
import ru.mail.my.towers.model.db.AppData;

import static ru.mail.my.towers.TowersApp.game;

@DbTable(name = AppData.TABLE_TOWERS)
public class Tower extends AbsRow {
    @DbColumn(name = ColumnNames.SERVER_ID, unique = true)
    public long serverId;

    @DbForeignKey(table = AppData.TABLE_TOWER_NETWORKS, column = ColumnNames.ID)
    public long network;

    public double lat;
    public double lng;
    public float radius;
    public String title;

    /**
     * уровень башни, начиная с 0
     */
    public int level;

    /**
     * текущее здоровье
     */
    public int health;

    /**
     * максимальное здоровье
     */
    public int maxHealth;

    /**
     * цвет круга
     */
    public int color;

    /**
     * сколько приносит золота
     */
    public int goldGain;

    /**
     * цена апдейта
     */
    public int updateCost;

    /**
     * цена починки
     */
    public int repairCost;

    /**
     * моя башня или нет
     */
    public boolean my;

    public Tower(GsonTowerInfo towerInfo) {
        this.serverId = towerInfo.id;
        this.lat = towerInfo.lat;
        this.lng = towerInfo.lng;
        this.radius = towerInfo.radius;
        this.title = towerInfo.title;
        this.level = towerInfo.level;
        this.health = towerInfo.health;
        this.maxHealth = towerInfo.maxHealth;
        this.color = Integer.parseInt(towerInfo.user.color, 16);
        this.goldGain = towerInfo.goldGain;
        this.updateCost = towerInfo.updateCost;
        this.repairCost = towerInfo.repairCost;
        this.my = towerInfo.my;

    }

    public Tower() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tower tower = (Tower) o;

        return serverId == tower.serverId;

    }

    @Override
    public int hashCode() {
        return (int) (serverId ^ (serverId >>> 32));
    }
}
