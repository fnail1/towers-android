package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonGoldInfo;

public class UserGoldInfo {

    /**
     * текущее золото
     */
    public int current;

    /**
     * количество добываемого золота
     */
    public int gain;

    /**
     * частота пополнения золота (секунды)
     */
    public int frequency;

    public UserGoldInfo(GsonGoldInfo gson){
        current = gson.current;
        gain = gson.gain;
        frequency = gson.frequency;
    }

    public UserGoldInfo() {

    }
}
