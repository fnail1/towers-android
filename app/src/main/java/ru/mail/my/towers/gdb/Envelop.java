package ru.mail.my.towers.gdb;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Envelop envelop = (Envelop) o;

        if (Double.compare(envelop.lat1, lat1) != 0) return false;
        if (Double.compare(envelop.lng1, lng1) != 0) return false;
        if (Double.compare(envelop.lat2, lat2) != 0) return false;
        return Double.compare(envelop.lng2, lng2) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat1);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng1);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lat2);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng2);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Envelop{" +
                "lat1=" + lat1 +
                ", lng1=" + lng1 +
                ", lat2=" + lat2 +
                ", lng2=" + lng2 +
                '}';
    }
}
