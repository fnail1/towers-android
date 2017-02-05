package ru.mail.my.towers.gis;

import android.location.Location;

import com.google.android.gms.maps.Projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

import static ru.mail.my.towers.TowersApp.data;

public class GisUtils {

    public static float addTowerArea(TowerNetwork network, Tower tower) {
        CursorWrapper<Tower> cursor = data().towers.selectByNetwork(network._id);

        try {
            ArrayList<Tower> towers = DbUtils.readToList(cursor);
            towers.add(tower);
            return calcArea(towers);
        } finally {
            cursor.close();
        }
    }


    public static float substractTowerArea(TowerNetwork network, Tower tower) {
        CursorWrapper<Tower> cursor = data().towers.selectByNetwork(network._id);

        try {
            ArrayList<Tower> towers = DbUtils.readToList(cursor);
            for (Iterator<Tower> iterator = towers.iterator(); iterator.hasNext(); ) {
                Tower t = iterator.next();
                if (t._id == tower._id) {
                    iterator.remove();
                    break;
                }
            }

            towers.add(tower);
            return calcArea(towers);
        } finally {
            cursor.close();
        }
    }


    public static float calcArea(TowerNetwork network) {
        return calcNetworkArea(network._id);
    }

    public static float calcNetworkArea(long netId) {
        CursorWrapper<Tower> cursor = data().towers.selectByNetwork(netId);

        try {
            return calcArea(DbUtils.readToList(cursor));
        } finally {
            cursor.close();
        }
    }

    public static float calcArea(ArrayList<Tower> towers) {
        if (towers.isEmpty()) {
            return 0;
        }

        double[] lats = new double[towers.size() * 2];
        double[] lons = new double[towers.size() * 2];

        for (int i = 0; i < towers.size(); i++) {
            Tower rect = towers.get(i);
            lats[i * 2] = rect.extLatMin;
            lats[i * 2 + 1] = rect.extLatMax;
            lons[i * 2] = rect.extLngMin;
            lons[i * 2 + 1] = rect.extLngMax;
        }

        Arrays.sort(lats);
        Arrays.sort(lons);

        float area = 0.0F;
        for (int ix = 1; ix < lats.length; ix++) {
            for (int iy = 1; iy < lons.length; iy++) {
                for (Tower rect : towers) {
                    double lat1 = rect.extLatMin;
                    double lon1 = rect.extLngMin;
                    double lat2 = rect.extLatMax;
                    double lon2 = rect.extLngMax;

                    if (!(lat1 > lats[ix] || lon1 > lons[iy] ||
                            lat2 < lats[ix - 1] || lon2 < lons[iy - 1])) {
                        area += calcArea(lats[ix - 1], lons[iy - 1], lats[ix], lons[iy]);
                        break;
                    }
                }
            }
        }
        return area;
    }

    public static float calcArea(double lat0, double lon0, double lat1, double lon1) {
        if (lat0 > lat1)
            throw new IllegalArgumentException();
        if (lon0 > lon1)
            throw new IllegalArgumentException();

        Location lt = new Location("");
        lt.setLatitude(lat0);
        lt.setLongitude(lon0);

        Location rt = new Location("");
        rt.setLatitude(lat0);
        rt.setLongitude(lon1);

        Location lb = new Location("");
        lb.setLatitude(lat1);
        lb.setLongitude(lon0);

        Location rb = new Location("");
        rb.setLatitude(lat1);
        rb.setLongitude(lon1);


        float wt = lt.distanceTo(rt);
        float wb = lb.distanceTo(rb);
        float hl = lt.distanceTo(lb);
        float hr = rt.distanceTo(rb);

        return (wt + wb) * (hl + hr) / 4;
    }

}
