package ru.mail.my.towers.api.model;

public class GsonTowersInfoResponse extends GsonBaseResponse {
    public GsonTowerInfo[] towers;
    public GsonTowersCollection[] towersNew;

    public static class GsonTowersCollection {
        public GsonTowerInfo[] towers;
    }
}
