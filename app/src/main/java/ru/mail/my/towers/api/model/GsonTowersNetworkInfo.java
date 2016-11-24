package ru.mail.my.towers.api.model;

public class GsonTowersNetworkInfo {
    /**
     * уровень сети (кластера) - среднее арифметическое уровня всех башен в сети
     */
    public float level;

    /**
     * сколько сеть добывает золота
     */
    public float goldGain;

    /**
     * площадь сети
     */
    public float area;

    /**
     * массив объектов Tower в этой сети
     */
    public GsonTowerInfo[] inside;
}
