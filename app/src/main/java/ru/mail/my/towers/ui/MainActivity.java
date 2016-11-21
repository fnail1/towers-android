package ru.mail.my.towers.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.mail.my.towers.R;
import ru.mail.my.towers.diagnostics.DebugUtils;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.service.Envelop;
import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.service.MapObjectsService;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.ui.popups.CreateTowerPopup;
import ru.mail.my.towers.ui.popups.IMapPopup;
import ru.mail.my.towers.ui.popups.PopupDialogResult;
import ru.mail.my.towers.ui.widgets.CirclesView;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.TowersApp.location;
import static ru.mail.my.towers.TowersApp.mapObjects;
import static ru.mail.my.towers.TowersApp.prefs;
import static ru.mail.my.towers.diagnostics.Logger.trace;

public class MainActivity extends BaseFragmentActivity implements OnMapReadyCallback, LocationAppService.LocationChangedEventHandler, GoogleMap.OnCameraMoveListener, MapObjectsService.MapObjectsLoadingCompleteEventHandler, IMapPopup.IMapActivity, GameService.GameMessageEventHandler {

    private static final int RC_LOCATION_PERMISSION = 101;
    private static final int RC_ACCESS_STORAGE_PERMISSION = 102;
    public static final int MIN_ZOOM_PREFERENCE = 10;
    public static final int ZOOM_SHOW_ME = 15;
    public static final int TOWERS_VISIBILITY_SCALE_MAX = 5000;

    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Location center = new Location("");
    private final Point point = new Point();
    private final Envelop mapEnv = new Envelop(0, 0, 0, 0);
    private final Stack<IMapPopup> popups = new Stack<>();

    private GoogleMap map;

    @BindView(R.id.map_objects)
    protected CirclesView mapObjectsView;

    @BindView(R.id.map_controls)
    View mapControls;

    @BindView(R.id.root)
    protected ViewGroup root;

    private Marker currentLocationMarker;
    private boolean showTowers;
    private GoogleApiClient client;
    private boolean mapControlsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            onReady();
        }
        checkOrRequestPermissions(RC_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION);


        if (prefs().getAccessToken() == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (TextUtils.isEmpty(game().me.name) || game().me.color == UserInfo.INVALID_COLOR) {
            startActivity(new Intent(this, EditProfileActivity.class));
        }
        mapObjects().loadingCompleteEvent.add(this);
        game().gameMessageEvent.add(this);
    }

    @Override
    protected void onPause() {
        game().gameMessageEvent.remove(this);
        mapObjects().loadingCompleteEvent.remove(this);

        while (!popups.isEmpty())
            popups.pop().close();
        restoreMapControls(false);

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
                case RC_ACCESS_STORAGE_PERMISSION:
                    DebugUtils.importFile(this, Environment.getExternalStorageDirectory());
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!popups.isEmpty()) {
            IMapPopup popup = popups.pop();
            popup.close();
        } else {
            super.onBackPressed();
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
        trace();
        map = googleMap;
//        map.setMinZoomPreference(MIN_ZOOM_PREFERENCE);
        map.setOnCameraMoveListener(this);
        if (resumed) {
            onReady();
        }
    }

    private void onReady() {
        trace();
        location().locationChangedEvent.add(this);
//        onCameraMove();
//        onLocationChanged(location(), location().currentLocation());
    }

    @Override
    public void onLocationChanged(LocationAppService sender, Location args) {
        trace();
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
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, ZOOM_SHOW_ME));
            onCameraMove();
        }
    }

    @Override
    public void onCameraMove() {
        trace();
        Projection projection = map.getProjection();
        point.set(-50, -50);
        LatLng latLng = projection.fromScreenLocation(point);
        leftTopCorner.setLongitude(latLng.longitude);
        leftTopCorner.setLatitude(latLng.latitude);

        point.set(mapObjectsView.getWidth() / 2, mapObjectsView.getHeight() / 2);
        latLng = projection.fromScreenLocation(point);
        center.setLongitude(latLng.longitude);
        center.setLatitude(latLng.latitude);
//
//        point.set(0, mapObjectsView.getHeight());
//        latLng = projection.fromScreenLocation(point);
//        Location leftBottom = new Location("");
//        leftBottom.setLatitude(latLng.latitude);
//        leftBottom.setLongitude(latLng.longitude);
//
//        point.set(mapObjectsView.getWidth(), 0);
//        latLng = projection.fromScreenLocation(point);
//        Location rightTop = new Location("");
//        rightTop.setLatitude(latLng.latitude);
//        rightTop.setLongitude(latLng.longitude);
//
        point.set(mapObjectsView.getWidth() + 50, mapObjectsView.getHeight() + 50);
        latLng = projection.fromScreenLocation(point);
        rightBottomCorner.setLongitude(latLng.longitude);
        rightBottomCorner.setLatitude(latLng.latitude);

        float distance = leftTopCorner.distanceTo(rightBottomCorner);
        showTowers = distance <= TOWERS_VISIBILITY_SCALE_MAX;

        mapEnv.set(leftTopCorner.getLatitude(),
                leftTopCorner.getLongitude(),
                rightBottomCorner.getLatitude(),
                rightBottomCorner.getLongitude());

        mapObjects().loadMapObjects(leftTopCorner.getLatitude(), leftTopCorner.getLongitude(),
                rightBottomCorner.getLatitude(), rightBottomCorner.getLongitude());

        for (IMapPopup popup : popups) {
            if (popup instanceof CreateTowerPopup)
                ((CreateTowerPopup) popup).setLocation(center);
        }
    }

    @Override
    public void onMapObjectsLoadingComplete(MapObjectsService.MapObjectsLoadingCompleteEventArgs args) {
        trace();
        if (!mapEnv.intersect(args.envelop))
            return;

        Tower[] towers = args.towers.toArray(new Tower[args.towers.size()]);
        runOnUiThread(() -> mapObjectsView.onCameraMove(map, towers));
    }

    @OnClick(R.id.settings)
    protected void onSettingsClick(View view) {
        if (checkOrRequestPermissions(RC_ACCESS_STORAGE_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            DebugUtils.importFile(this, Environment.getExternalStorageDirectory());
    }

    @OnClick(R.id.current_location)
    protected void onCurrentLocationClick() {
        Location location = location().currentLocation();
        if (location == null) {
            Toast.makeText(this, "Местоположение не определено", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, ZOOM_SHOW_ME));
        onCameraMove();
    }

    @OnClick(R.id.build_tower)
    protected void onBuildTowerClick() {
        LatLng latLng = map.getProjection().fromScreenLocation(new Point(mapObjectsView.getWidth() / 2, mapObjectsView.getHeight() / 2));


        CreateTowerPopup popup = new CreateTowerPopup(root, this);
        popup.setCost(game().me.createCost);
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        popup.setLocation(location);
        popup.setName(game().me.name + " (Башня " + (data().towers().countOfMy() + 1) + ")");
        popups.add(popup);
        popup.show();

        hideMapControls();
    }

    @Override
    public void onPopupResult(IMapPopup popup) {
        if (popup instanceof CreateTowerPopup) {
            CreateTowerPopup createTowerPopup = (CreateTowerPopup) popup;
            if (createTowerPopup.getResult() == PopupDialogResult.POSITIVE) {
                game().createTower(createTowerPopup.getLocation(), createTowerPopup.getName());
            }
        }
        popups.remove(popup);
        restoreMapControls(true);
    }

    private void restoreMapControls(boolean animated) {
        if (popups.isEmpty() && !mapControlsVisible) {
            mapControlsVisible = true;
            if (animated) {
                mapControls.animate()
                        .alpha(1)
                        .setDuration(200);
            } else {
                mapControls.setAlpha(1);
            }
        }
    }


    private void hideMapControls() {
        if (!popups.isEmpty() && mapControlsVisible) {
            mapControlsVisible = false;
            mapControls.animate()
                    .alpha(0)
                    .setDuration(200);
        }
    }

    @Override
    public void onGameNewMessage(String args) {
        runOnUiThread(() -> Toast.makeText(this, args, Toast.LENGTH_SHORT).show());
    }
}
