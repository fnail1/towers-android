package ru.mail.my.towers.model;

import ru.mail.my.towers.api.model.GsonHealthInfo;

public class UserHealthInfo {

    /**
     * текущее здоровье
     */
    public int current;

    /**
     * максимальное здоровье
     */
    public int max;

    /**
     * регенерация здоровья в секунду
     */
    public int regeneration;

    public UserHealthInfo(GsonHealthInfo gson) {
        merge(gson);
    }

    public void merge(GsonHealthInfo gson) {
        current = gson.current;
        max = gson.max;
        regeneration = gson.regeneration;
    }

    public UserHealthInfo() {

    }
}
