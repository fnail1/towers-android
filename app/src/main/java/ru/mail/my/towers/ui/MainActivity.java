package ru.mail.my.towers.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import ru.mail.my.towers.R;
import ru.mail.my.towers.service.Envelop;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.service.MapObjectsService;
import ru.mail.my.towers.ui.widgets.CirclesView;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.TowersApp.location;
import static ru.mail.my.towers.TowersApp.mapObjects;
import static ru.mail.my.towers.TowersApp.prefs;

public class MainActivity extends BaseFragmentActivity implements OnMapReadyCallback, LocationAppService.LocationChangedEventHandler, GoogleMap.OnCameraMoveListener, MapObjectsService.MapObjectsLoadingCompleteEventHandler {

    private static final int RC_LOCATION_PERMISSION = 101;
    public static final int MIN_ZOOM_PREFERENCE = 10;
    public static final int TOWERS_VISIBILITY_SCALE_MAX = 5000;

    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Location center = new Location("");
    private final Point point = new Point();
    private final Envelop mapEnv = new Envelop(0, 0, 0, 0);

    private GoogleMap map;
    private CirclesView mapObjectsView;

    private Marker currentLocationMarker;
    private boolean showTowers;
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapObjectsView = (CirclesView) findViewById(R.id.map_objects);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            location().locationChangedEvent.add(this);
        }
        checkOrRequestPermissions(RC_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION);


        if (prefs().getAccessToken() == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (TextUtils.isEmpty(game().me.name)) {
            startActivity(new Intent(this, EditProfileActivity.class));
        }
        mapObjects().loadingCompleteEvent.add(this);
    }

    @Override
    protected void onPause() {
        mapObjects().loadingCompleteEvent.remove(this);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (!(grantResults[i] == PackageManager.PERMISSION_GRANTED))
                continue;

            appState().onPermissionGranted(permissions[i]);
            switch (requestCode) {
                case RC_LOCATION_PERMISSION:
                    location().startObserveLocationChanges(app());
                    break;
            }
        }
    }

    public boolean checkOrRequestPermissions(int reqCode, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, permission))
            return true;

        appState().addPermissionRequested(permission);
        String[] permissions = {permission};
        ActivityCompat.requestPermissions(this, permissions, reqCode);

        return false;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
//        map.setMinZoomPreference(MIN_ZOOM_PREFERENCE);
        onLocationChanged(location(), location().currentLocation());
        if (resumed) {
            location().locationChangedEvent.add(this);
        }
        map.setOnCameraMoveListener(this);
    }

    @Override
    public void onLocationChanged(LocationAppService sender, Location args) {
        if (args == null) {
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
                currentLocationMarker = null;
            }
            return;
        }

        LatLng pos = new LatLng(args.getLatitude(), args.getLongitude());
        if (currentLocationMarker == null) {
            MarkerOptions currentLocation = new MarkerOptions().position(pos).title("You are here");
            currentLocationMarker = map.addMarker(currentLocation);
        }

        currentLocationMarker.setPosition(pos);
        if (appState().displayCurrentLocation()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, MIN_ZOOM_PREFERENCE));
        }
    }

    @Override
    public void onCameraMove() {
        Projection projection = map.getProjection();
        point.set(0, 0);
        LatLng latLng = projection.fromScreenLocation(point);
        leftTopCorner.setLongitude(latLng.longitude);
        leftTopCorner.setLatitude(latLng.latitude);

        point.set(mapObjectsView.getWidth() / 2, mapObjectsView.getHeight() / 2);
        latLng = projection.fromScreenLocation(point);
        center.setLongitude(latLng.longitude);
        center.setLatitude(latLng.latitude);

        point.set(mapObjectsView.getWidth(), mapObjectsView.getHeight());
        latLng = projection.fromScreenLocation(point);
        rightBottomCorner.setLongitude(latLng.longitude);
        rightBottomCorner.setLatitude(latLng.latitude);

        showTowers = leftTopCorner.distanceTo(rightBottomCorner) <= TOWERS_VISIBILITY_SCALE_MAX;

        mapEnv.set(leftTopCorner.getLatitude(),
                leftTopCorner.getLongitude(),
                rightBottomCorner.getLatitude(),
                rightBottomCorner.getLongitude());

        mapObjects().loadMapObjects(leftTopCorner.getLatitude(), leftTopCorner.getLongitude(),
                rightBottomCorner.getLatitude(), rightBottomCorner.getLongitude());
    }

    @Override
    public void onMapObjectsLoadingComplete(MapObjectsService.MapObjectsLoadingCompleteEventArgs args) {
        if (!mapEnv.intersect(args.envelop))
            return;

        runOnUiThread(() -> mapObjectsView.onCameraMove(map, args.towers));
    }

}
