package ru.mail.my.towers.api.model;

public class GsonTowerInfo {
    public long id;
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

    public long netId;

    public GsonUserProfile user;
}
