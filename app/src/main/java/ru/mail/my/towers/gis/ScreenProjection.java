package ru.mail.my.towers.gis;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class ScreenProjection {
    private final Location locationBuffer1 = new Location("");
    private final Location locationBuffer2 = new Location("");

    private double a1, b1, d1;
    private double a2, b2, d2;
    public double scale;
    public double screenWidth;
    public double screenHeight;
    public int screenRequestCounter;

    public void build(LatLng[] screenPoints, double screenWidth, double screenHeight, int screenRequestCounter) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenRequestCounter = screenRequestCounter;
        double w1 = screenPoints[0].latitude;
        double u1 = screenPoints[0].longitude;
        double w2 = screenPoints[1].latitude;
        double u2 = screenPoints[1].longitude;
        double w3 = screenPoints[2].latitude;
        double u3 = screenPoints[2].longitude;
        double x1 = 0;
        double y1 = 0;
        double x2 = screenWidth;
        double y2 = 0;
        double x3 = 0;
        double y3 = screenHeight;

        double det = w1 * u2 - w1 * u3 - w2 * u1 + w2 * u3 + w3 * u1 - w3 * u2;

        a1 = ((u2 - u3) * x1 - (u1 - u3) * x2 + (u1 - u2) * x3) / det;
        b1 = (-(w2 - w3) * x1 + (w1 - w3) * x2 - (w1 - w2) * x3) / det;
        d1 = ((w2 * u3 - w3 * u2) * x1 - (w1 * u3 - w3 * u1) * x2 + (w1 * u2 - w2 * u1) * x3) / det;

        a2 = ((u2 - u3) * y1 - (u1 - u3) * y2 + (u1 - u2) * y3) / det;
        b2 = (-(w2 - w3) * y1 + (w1 - w3) * y2 - (w1 - w2) * y3) / det;
        d2 = ((w2 * u3 - w3 * u2) * y1 - (w1 * u3 - w3 * u1) * y2 + (w1 * u2 - w2 * u1) * y3) / det;

        locationBuffer1.setLatitude(screenPoints[0].latitude);
        locationBuffer1.setLongitude(screenPoints[0].longitude);

        locationBuffer2.setLatitude(screenPoints[3].latitude);
        locationBuffer2.setLongitude(screenPoints[3].longitude);

        scale = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) / locationBuffer1.distanceTo(locationBuffer2);

    }

    public int xi(double lat, double lng) {
        return (int) Math.round(a1 * lat + b1 * lng + d1);
    }

    public int yi(double lat, double lng) {
        return (int) Math.round(a2 * lat + b2 * lng + d2);
    }

    public float xf(double lat, double lng) {
        return Math.round(a1 * lat + b1 * lng + d1);
    }

    public float yf(double lat, double lng) {
        return Math.round(a2 * lat + b2 * lng + d2);
    }
}
