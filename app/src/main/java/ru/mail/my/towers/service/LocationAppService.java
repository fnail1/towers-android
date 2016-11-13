package ru.mail.my.towers.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.LocationServices;

import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.diagnostics.Logger.logLocation;
import static ru.mail.my.towers.diagnostics.Logger.trace;


public class LocationAppService implements LocationListener, AppStateService.AppStateEventHandler {

    public final ObservableEvent<LocationChangedEventHandler, LocationAppService, Location> locationChangedEvent = new ObservableEvent<LocationChangedEventHandler, LocationAppService, Location>(this) {
        @Override
        protected void notifyHandler(LocationChangedEventHandler handler, LocationAppService sender, Location args) {
            handler.onLocationChanged(sender, args);
        }
    };

    private final LocationManager systemService;
    private boolean subscribed = false;

    public LocationAppService(Context context, AppStateService appStateService) {
        systemService = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        appStateService.stateEvent.add(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.logLocation(location);
        locationChangedEvent.fire(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        trace();
    }

    @Override
    public void onProviderEnabled(String s) {
        trace();
    }

    @Override
    public void onProviderDisabled(String s) {
        trace();
    }

    public void startObserveLocationChanges(Context context) {
        if (subscribed)
            return;
        if (checkPermission(context)) {
            return;
        }
        subscribed = true;

        systemService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        Location location = currentLocation();
        Logger.logLocation(location);
    }

    private void stopObserveLocationChanges() {
        if (!subscribed)
            return;
        if (checkPermission(app())) {
            return;
        }
        subscribed = false;

        systemService.removeUpdates(this);
    }

    @Nullable
    public Location currentLocation() {
        if (checkPermission(app())) {
            Location location = new Location("");
            location.setLatitude(55.797287);
            location.setLongitude(37.536656);
            return location;
        }
        Location location = systemService.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null && Math.abs(location.getTime() - System.currentTimeMillis()) > 5 * 60 * 1000) {
            return null;
        }
        return location;
    }

    private boolean checkPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onAppStateChanged() {
        if (appState().isForeground()) {
            startObserveLocationChanges(app());
        } else {
            stopObserveLocationChanges();
        }
    }


    public interface LocationChangedEventHandler {
        void onLocationChanged(LocationAppService sender, Location args);
    }
}
