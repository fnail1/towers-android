package ru.mail.my.towers.gis;

import com.google.android.gms.maps.model.LatLng;

import ru.mail.my.towers.model.Tower;

public class MapExtent {
    public double lat1;
    public double lng1;
    public double lat2;
    public double lng2;

    public MapExtent(double lat, double lng) {
        this(lat - Double.MIN_NORMAL, lng - Double.MIN_NORMAL, lat + Double.MIN_NORMAL, lng + Double.MIN_NORMAL);
    }

    public MapExtent(double lat1, double lng1, double lat2, double lng2) {
        set(lat1, lng1, lat2, lng2);
    }

    public MapExtent(LatLng[] points) {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLng = Double.POSITIVE_INFINITY;
        double maxLng = Double.NEGATIVE_INFINITY;

        for (LatLng pt : points) {
            if (minLat > pt.latitude)
                minLat = pt.latitude;
            else if (maxLat < pt.latitude)
                maxLat = pt.latitude;

            if (minLng > pt.longitude)
                minLng = pt.longitude;
            else if (maxLng < pt.longitude)
                maxLng = pt.longitude;
        }

        lat1 = minLat;
        lng1 = minLng;
        lat2 = maxLat;
        lng2 = maxLng;
    }

    public void expand(double factor) {
        double dlat = (lat2 - lat1) * factor;
        double dblng = (lng2 - lng1) * factor;

        lat1 -= dlat;
        lng1 -= dblng;
        lat2 += dlat;
        lng2 += dblng;
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

    public boolean intersect(MapExtent env) {
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

        MapExtent mapExtent = (MapExtent) o;

        if (Double.compare(mapExtent.lat1, lat1) != 0) return false;
        if (Double.compare(mapExtent.lng1, lng1) != 0) return false;
        if (Double.compare(mapExtent.lat2, lat2) != 0) return false;
        return Double.compare(mapExtent.lng2, lng2) == 0;

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
        return "MapExtent{" +
                "lat1=" + lat1 +
                ", lng1=" + lng1 +
                ", lat2=" + lat2 +
                ", lng2=" + lng2 +
                '}';
    }
}
