package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonGameInfo;
import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.data.DbTable;
import ru.mail.my.towers.data.IDbSerializationHandlers;
import ru.mail.my.towers.model.db.AppData;

@DbTable(name = AppData.TABLE_USER_INFOS)
public class UserInfo implements IDbSerializationHandlers {
    public static final int COLOR_ALPHA = 0x33000000;
    public static final int DEFAULT_COLOR = COLOR_ALPHA + 0xff0000;

    /**
     * имя
     */
    public String name;

    /**
     * цвет кругов
     */
    public int color;

    /**
     * XP пользователя
     */
    public int exp;

    /**
     * текущая роль
     */
    public UserRole role;

    /**
     * занимаемая площадь, м²
     */
    public double area;

    /**
     * текущий уровень, начиная с 0
     */
    public int currentLevel;


    /**
     * требуемый XP для следующего уровня
     */
    public int nextExp;

    /**
     * плата золотом для создания башни
     */
    public int createCost;

    /**
     * информация о здоровье
     */
    public transient UserHealthInfo health;

    /**
     * информация о золоте
     */
    public transient UserGoldInfo gold;

    private int _gold_frequency;
    private int _gold_gain;
    private int _gold_current;
    private int _health_current;
    private int _health_regeneration;
    private int _health_max;

    public void merge(GsonUserInfo gson) {
        name = gson.name;
        color = COLOR_ALPHA + Integer.parseInt(gson.color, 16);
        exp = gson.exp;

        if (health == null)
            health = new UserHealthInfo();
        health.current = gson.health;

        role = UserRole.valueOf(gson.role);
    }

    public void merge(GsonGameInfo gson) {
        area = gson.area;
        createCost = gson.createCost;
        currentLevel = gson.currentLevel;
        exp = gson.exp;
        gold = new UserGoldInfo(gson.gold);
        health = new UserHealthInfo(gson.health);
        nextExp = gson.nextExp;

    }

    @Override
    public void onBeforeSerialization() {
        _gold_frequency = gold.frequency;
        _gold_gain = gold.gain;
        _gold_current = gold.current;

        _health_current = health.current;
        _health_regeneration = health.regeneration;
        _health_max = health.max;
    }

    @Override
    public void onAfterDeserialization() {
        gold = new UserGoldInfo();
        gold.frequency = _gold_frequency;
        gold.gain = _gold_gain;
        gold.current = _gold_current;


        health = new UserHealthInfo();
        health.current = _health_current;
        health.regeneration = _health_regeneration;
        health.max = _health_max;
    }
}
