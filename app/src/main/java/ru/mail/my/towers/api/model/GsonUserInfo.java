package ru.mail.my.towers.api.model;

public class GsonUserInfo {
    /**
     * занимаемая площадь, м²
     */
    public double area;

    /**
     * текущий уровень, начиная с 0
     */
    public int currentLevel;

    /**
     * текущий XP
     */
    public int exp;

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
    public GsonHealthInfo health;

    /**
     * информация о золоте
     */
    public GsonGoldInfo gold;

}
