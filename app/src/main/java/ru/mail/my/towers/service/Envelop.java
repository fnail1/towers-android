package ru.mail.my.towers.service;

import ru.mail.my.towers.model.Tower;

public class Envelop {
    public double lat1;
    public double lng1;
    public double lat2;
    public double lng2;

    public Envelop(double lat1, double lng1, double lat2, double lng2) {
        set(lat1, lng1, lat2, lng2);
    }

    public void set(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 > lat2) {
            this.lat1 = lat2;
            this.lat2 = lat1;
        } else {
            this.lat1 = lat1;
            this.lat2 = lat2;
        }

        if (lng1 > lng2) {
            this.lng1 = lng2;
            this.lng2 = lng1;
        } else {
            this.lng1 = lng1;
            this.lng2 = lng2;
        }
    }

    public boolean intersect(Envelop env) {
        return !(lat2 < env.lat1 || lat1 > env.lat2
                || lng2 < env.lng1 || lng1 > env.lng2);
    }

    boolean inside(Tower tower) {
        return inside(tower.lat, tower.lng);
    }

    boolean inside(double lat, double lng) {
        return lat1 <= lat && lat <= lat2 &&
                lng1 <= lng && lng <= lng2;
    }
}
