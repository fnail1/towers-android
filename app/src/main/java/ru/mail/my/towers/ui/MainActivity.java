package ru.mail.my.towers.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.mail.my.towers.BuildConfig;
import ru.mail.my.towers.R;
import ru.mail.my.towers.diagnostics.DebugUtils;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.ui.popups.CreateTowerPopup;
import ru.mail.my.towers.ui.popups.IMapPopup;
import ru.mail.my.towers.ui.widgets.MapObjectsView;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.TowersApp.location;
import static ru.mail.my.towers.TowersApp.prefs;
import static ru.mail.my.towers.diagnostics.Logger.trace;

public class MainActivity extends BaseFragmentActivity
        implements OnMapReadyCallback,
                   LocationAppService.LocationChangedEventHandler,
                   GoogleMap.OnCameraMoveListener,
//                   MapObjectsService.MapObjectsLoadingCompleteEventHandler,
                   IMapPopup.IMapActivity,
                   GameService.GameMessageEventHandler,
                   MapObjectsView.MapObjectClickListener, GameService.MyProfileEventHandler,
                   GameService.TowersGeoDataChanged {

    private static final int RC_LOCATION_PERMISSION = 101;
    private static final int RC_ACCESS_STORAGE_PERMISSION = 102;
    public static final int MIN_ZOOM_PREFERENCE = 10;
    public static final int ZOOM_SHOW_ME = 18;
    public static final int TOWERS_VISIBILITY_SCALE_MAX = 5000;

    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Location center = new Location("");
    private final Point point = new Point();
    private final Stack<IMapPopup> popups = new Stack<>();

    private GoogleMap map;

    @BindView(R.id.map_objects)
    protected MapObjectsView mapObjectsView;

    @BindView(R.id.map_controls)
    View mapControls;

    @BindView(R.id.profile_lv)
    TextView profileLv;
    @BindView(R.id.profile_xp)
    TextView profileXp;
    @BindView(R.id.profile_hp)
    TextView profileHp;
    @BindView(R.id.profile_ar)
    TextView profileAr;
    @BindView(R.id.profile_gd)
    TextView profileGd;
    @BindView(R.id.build_tower_info)
    TextView buildTowerInfo;

    @BindView(R.id.settings_panel)
    View settingsPanel;

    @BindView(R.id.restore_location)
    View setLocation;

    @BindView(R.id.tower_controls)
    View towerControls;
    @BindView(R.id.destroy_tower)
    View destroyTower;
    @BindView(R.id.repair_tower)
    View repairTower;
    @BindView(R.id.repair_tower_text)
    TextView repairTowerText;
    @BindView(R.id.upgrade_tower)
    View upgradeTower;
    @BindView(R.id.upgrade_tower_text)
    TextView upgradeTowerText;
    @BindView(R.id.attack_tower)
    View attackTower;
    @BindView(R.id.tower_owner_info)
    View towerOwnerInfo;

    @BindView(R.id.root)
    protected ViewGroup root;

    private Marker currentLocationMarker;
    private GoogleApiClient client;
    private boolean mapControlsVisible = true;
    private Tower selectedTower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        mapObjectsView.setMapObjectClickListener(this);

        if (BuildConfig.DEBUG) {
            mapObjectsView.setMapObjectLongClickListener(this::onMapObjectsViewLongClick);
        }
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
//        mapObjects().loadingCompleteEvent.add(this);
        game().gameMessageEvent.add(this);
        game().myProfileEvent.add(this);
        game().geoDataChangedEvent.add(this);
        onMyProfileChanged(game().me);
    }

    @Override
    protected void onPause() {
        game().gameMessageEvent.remove(this);
        game().myProfileEvent.remove(this);
        game().geoDataChangedEvent.remove(this);

//        mapObjects().loadingCompleteEvent.remove(this);

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
        } else if (settingsPanel.getVisibility() != View.GONE) {
            settingsPanel.setVisibility(View.GONE);
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
        onCameraMove();
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
        center.setTime(System.currentTimeMillis());

        point.set(mapObjectsView.getWidth() + 50, mapObjectsView.getHeight() + 50);
        latLng = projection.fromScreenLocation(point);
        rightBottomCorner.setLongitude(latLng.longitude);
        rightBottomCorner.setLatitude(latLng.latitude);


        mapObjectsView.onCameraMove(map);

        for (IMapPopup popup : popups) {
            if (popup instanceof CreateTowerPopup)
                ((CreateTowerPopup) popup).setLocation(center);
        }
    }

    @OnClick(R.id.settings)
    protected void onSettingsClick(View view) {
        settingsPanel.setVisibility(View.VISIBLE);
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
        Location gpsLocation = location().currentLocation();
        if (gpsLocation == null) {
            Toast.makeText(this, "Местоположение не определено", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng mapLocation = map.getCameraPosition().target;
        if (Math.abs(mapLocation.latitude - gpsLocation.getLatitude()) >= Double.MIN_NORMAL ||
                Math.abs(mapLocation.longitude - gpsLocation.getLongitude()) >= Double.MIN_NORMAL) {
            onCurrentLocationClick();
        }

        game().createTower(gpsLocation, game().me.name + " (Башня " + (game().me.towersCount + 1) + ")");
    }

    @OnClick(R.id.import_data)
    protected void onImportDataClick() {
        if (checkOrRequestPermissions(RC_ACCESS_STORAGE_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            DebugUtils.importFile(this, Environment.getExternalStorageDirectory());
    }

    private void onMapObjectsViewLongClick(int x, int y) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(50);

        LatLng latLng = map.getProjection().fromScreenLocation(new Point(x, y));
        location().setFakeLocation(latLng.latitude, latLng.longitude);
        setLocation.setEnabled(true);
        Toast.makeText(this, "Установлено текущее местоположение " + latLng, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.restore_location)
    protected void onRestoreLocationClick() {
        location().restoreLocation();
        onCurrentLocationClick();
        settingsPanel.setVisibility(View.GONE);
    }

    @OnClick(R.id.destroy_tower)
    protected void onDestroyTowerClick() {
        game().destroyTower(selectedTower);
    }

    @OnClick(R.id.repair_tower)
    protected void onRepairTowerClick() {

    }

    @OnClick(R.id.upgrade_tower)
    protected void onUpgradeTowerClick() {
        game().upgradeTower(selectedTower);
    }

    @OnClick(R.id.attack_tower)
    protected void onAttackTowerClick() {

    }

    @OnClick(R.id.tower_owner_info)
    protected void onTowerOwnerInfoClick() {
        startActivity(new Intent(this,UserProfileActivity.class).putExtra(UserProfileActivity.PARAM_UID, selectedTower.owner));
    }

    @Override
    public void onPopupResult(IMapPopup popup) {
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

    @Override
    public void onMapSelectionChanged(Tower tower) {
        selectedTower = tower;
        towerControls.animate().cancel();

        if (tower == null) {
            if (towerControls.getVisibility() != View.GONE) {
                animateDisappearance(destroyTower);
                animateDisappearance(repairTower);
                animateDisappearance(upgradeTower);
                animateDisappearance(attackTower);
                animateDisappearance(towerOwnerInfo);
                towerControls.animate()
                        .alpha(0)
                        .setDuration(500)
                        .withEndAction(() -> {
                            towerControls.setAlpha(1);
                            towerControls.setVisibility(View.GONE);
                        });
            }
        } else {
            if (towerControls.getVisibility() == View.GONE) {
                towerControls.setVisibility(View.VISIBLE);
            }
            if (tower.my) {
                animateAppearance(destroyTower, 100);
                if (tower.health < tower.maxHealth) {
                    repairTowerText.setText("" + tower.repairCost + " GD");
                    animateAppearance(repairTower, 150);
                } else {
                    repairTower.setVisibility(View.GONE);
                }
                if (tower.level < game().me.currentLevel) {
                    upgradeTowerText.setText("" + tower.updateCost + " GD");
                    animateAppearance(upgradeTower, 200);
                } else {
                    upgradeTower.setVisibility(View.GONE);
                }
                attackTower.setVisibility(View.GONE);
                towerOwnerInfo.setVisibility(View.GONE);

            } else {
                destroyTower.setVisibility(View.GONE);
                repairTower.setVisibility(View.GONE);
                upgradeTower.setVisibility(View.GONE);

                animateAppearance(attackTower, 100);
                animateAppearance(towerOwnerInfo, 150);
            }
        }
    }


    private void animateAppearance(View view, int duration) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0);
        view.setTranslationX(mapObjectsView.getWidth());
        view.setTranslationY(0);
        view.animate()
                .alpha(1)
                .translationX(0)
                .setDuration(duration);
    }

    private void animateDisappearance(View view) {
        view.animate()
                .alpha(0)
                .translationY(-mapObjectsView.getHeight())
                .translationX(-mapObjectsView.getWidth() / 2)
                .setDuration(300);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onMyProfileChanged(UserInfo args) {
        runOnUiThread(() -> {
            profileLv.setText("LV: " + (args.currentLevel + 1));
            profileXp.setText("XP: " + args.exp + "/" + args.nextExp);
            profileHp.setText("HP: " + args.health.current + "/" + args.health.max);
            profileAr.setText("AR: " + Math.round(args.area));
            profileGd.setText("GD: " + args.gold.current);
            buildTowerInfo.setText("" + args.createCost + " GD, +10 XP");
        });
    }

    @Override
    public void onTowersGeoDataChanged(MapExtent extent) {
        runOnUiThread(this::onCameraMove);
    }
}
