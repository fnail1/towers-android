package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonUserInfo;
import ru.mail.my.towers.api.model.GsonUserProfile;
import ru.mail.my.towers.data.DbColumn;
import ru.mail.my.towers.data.DbTable;
import ru.mail.my.towers.data.IDbSerializationHandlers;
import ru.mail.my.towers.model.db.AppData;

import static ru.mail.my.towers.TowersApp.appState;

@DbTable(name = AppData.TABLE_USER_INFOS)
public class UserInfo implements IDbSerializationHandlers {
    public static final int COLOR_ALPHA = 0x33000000;
    public static final int DEFAULT_COLOR = COLOR_ALPHA + 0xff0000;
    public static final int INVALID_COLOR = 0x33FFFFFF;

    @DbColumn(primaryKey = true)
    public long _id;

    @DbColumn(unique = true)
    public long serverId;

    /**
     * имя
     */
    public String name;

    /**
     * цвет кругов
     */
    public int color = INVALID_COLOR;

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
    public transient final UserHealthInfo health = new UserHealthInfo();

    /**
     * информация о золоте
     */
    public transient final UserGoldInfo gold = new UserGoldInfo();

    public int towersCount;


    public int _gold_frequency;
    public int _gold_gain;
    public int _gold_current;
    public int _health_current;
    public int _health_regeneration;
    public int _health_max;
    public long syncTs;

    public void merge(GsonUserProfile gson) {
        serverId = gson.id;

        if (gson.name != null) {
            name = gson.name;
        }

        color = parseColor(gson.color);

        if (gson.role != null) {
            role = UserRole.valueOf(gson.role);
        }
    }


    public void merge(GsonUserInfo gson) {
        area = gson.area;
        createCost = gson.createCost;
        currentLevel = gson.currentLevel;
        exp = gson.exp;
        gold.merge(gson.gold);
        health.merge(gson.health);
        nextExp = gson.nextExp;
        syncTs = appState().getServerTime();
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
        gold.frequency = _gold_frequency;
        gold.gain = _gold_gain;
        gold.current = _gold_current;


        health.current = _health_current;
        health.regeneration = _health_regeneration;
        health.max = _health_max;
    }


    public static int parseColor(String color) {
        if (color == null)
            return INVALID_COLOR;

        try {
            return COLOR_ALPHA + Integer.parseInt(color, 16);
        } catch (NumberFormatException e) {
            return INVALID_COLOR;
        }
    }

    public int currentHealth() {
        long dt = appState().getServerTime() - syncTs;
        if (dt < 0)
            return health.current;
        return Math.min(health.current + health.regeneration * ((int) (dt / 1000)), health.max);
    }

    public int currentGold() {
        long dt = appState().getServerTime() - syncTs;
        if (dt < 0 || gold.frequency == 0)
            return gold.current;
        int gain = gold.gain * (int) (dt / (1000 * gold.frequency));
        return gold.current + gain;
    }
}
