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
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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

import java.io.IOException;
import java.util.Random;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.mail.my.towers.BuildConfig;
import ru.mail.my.towers.R;
import ru.mail.my.towers.diagnostics.DebugUtils;
import ru.mail.my.towers.gis.MapExtent;
import ru.mail.my.towers.model.Notification;
import ru.mail.my.towers.model.NotificationType;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.toolkit.ThreadPool;
import ru.mail.my.towers.ui.mytowers.MyTowersActivity;
import ru.mail.my.towers.ui.notifications.NotificationsActivity;
import ru.mail.my.towers.ui.popups.CreateTowerPopup;
import ru.mail.my.towers.ui.popups.IMapPopup;
import ru.mail.my.towers.ui.widgets.MapObjectsView;
import ru.mail.my.towers.utils.Utils;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.TowersApp.data;
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
    private static final int PROFILE_VALUE_NORMAL_COLOR = 0x7f000000;
    private static final int PROFILE_VALUE_INC_COLOR = 0x7fff0000;
    private static final int PROFILE_VALUE_DEC_COLOR = 0x7f0000ff;

    private final Location leftTopCorner = new Location("");
    private final Location rightBottomCorner = new Location("");
    private final Location center = new Location("");
    private final Point point = new Point();
    private final Stack<IMapPopup> popups = new Stack<>();
    private final Handler handler = new Handler();

    private GoogleMap map;

    @BindView(R.id.map_objects)
    protected MapObjectsView mapObjectsView;

    @BindView(R.id.map_controls)
    RelativeLayout mapControls;

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

    @BindView(R.id.all_notifications)
    ImageButton allNotifications;

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
    @BindView(R.id.tower_owner_info_text)
    TextView towerOwnerInfoText;

    @BindView(R.id.root)
    protected ViewGroup root;

    private Marker currentLocationMarker;
    private GoogleApiClient client;
    private boolean mapControlsVisible = true;
    private Tower selectedTower;


    private int displayedLevel = -1;
    private int displayedExp = -1;
    private int displayedHealth = -1;
    private int displayedArea = -1;
    private int displayedGold = -1;
    private CustomToastsEngine toastsEngine;


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

        allNotifications.setImageResource(R.drawable.ic_notifications_none);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs().getAccessToken() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else if (TextUtils.isEmpty(game().me.name) || game().me.color == UserInfo.INVALID_COLOR) {
            startActivity(new Intent(this, EditProfileActivity.class));
        }

        checkOrRequestPermissions(RC_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION);

        if (map != null) {
            onReady();
        }

        game().gameMessageEvent.add(this);
        game().myProfileEvent.add(this);
        game().geoDataChangedEvent.add(this);
        updateProfileLoop();

        toastsEngine = new CustomToastsEngine(mapControls);

//        handler.postDelayed(new Runnable() {
//            int counter = 1;
//            Random rnd = new Random();
//
//            @Override
//            public void run() {
//                toastsEngine.showMessage(String.valueOf(counter++), rnd.nextInt(10000), 0xff000000, 0xf7 + rnd.nextInt(0xffffff));
//
//                handler.postDelayed(this, 1000);
//            }
//        }, 1000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);

        CustomToastsEngine toastsEngine = this.toastsEngine;
        this.toastsEngine = null;
        toastsEngine.removeAll();

        game().gameMessageEvent.remove(this);
        game().myProfileEvent.remove(this);
        game().geoDataChangedEvent.remove(this);

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
        startActivity(new Intent(this, UserProfileActivity.class).putExtra(UserProfileActivity.PARAM_UID, selectedTower.owner));
    }

    @OnClick(R.id.logout)
    protected void onLogoutClick() {
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH).execute(() -> {
            try {
                api().logout().execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                app().onLogout();
                recreate();
            });
        });
    }

    @OnClick(R.id.my_towers_info)
    protected void onMyTowersInfoClick() {
        startActivity(new Intent(this, MyTowersActivity.class));
    }

    @OnClick(R.id.all_notifications)
    protected void onAllNotificationClick() {
        startActivity(new Intent(this, NotificationsActivity.class));
        allNotifications.setImageResource(R.drawable.ic_notifications_none);
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
    public void onGameNewMessage(Notification notification) {
        runOnUiThread(() -> {
            int foreColor;
            switch (notification.type) {
                case SUCCESS:
                    foreColor = Utils.getColor(this, R.color.colorNotificationSuccess);
                    break;
                case ERROR:
                    foreColor = Utils.getColor(this, R.color.colorNotificationError);
                    break;
                case INFO:
                    foreColor = Utils.getColor(this, R.color.colorNotificationInfo);
                    break;
                case ALARM:
                    foreColor = Utils.getColor(this, R.color.colorNotificationAlarm);
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(notification.type));
            }
            toastsEngine.showMessage(notification.message, 4000, foreColor, 0x7f000000);
            allNotifications.setImageResource(R.drawable.ic_notifications_active);
        });
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
                    repairTowerText.setText("REPAIR (" + tower.repairCost + " GD)");
                    animateAppearance(repairTower, 150);
                } else {
                    repairTower.setVisibility(View.GONE);
                }
                if (tower.level < game().me.currentLevel) {
                    upgradeTowerText.setText("UPGRADE (" + tower.updateCost + " GD)");
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
                towerOwnerInfoText.setText(data().users.select(selectedTower.owner).name);
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

    private void updateProfileLoop() {
        onMyProfileChanged(game().me);
        handler.postDelayed(this::updateProfileLoop, 1 * 1000);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onMyProfileChanged(UserInfo args) {
        runOnUiThread(() -> {
            int level = args.currentLevel + 1;
            updateProfileValue(profileLv, displayedLevel, level, "LV: " + level);
            displayedLevel = level;

            int exp = args.exp;
            updateProfileValue(profileXp, displayedExp, exp, "XP: " + exp + "/" + args.nextExp);
            displayedExp = exp;

            int health = args.currentHealth();
            updateProfileValue(profileHp, displayedHealth, health, "HP: " + health + "/" + args.health.max);
            displayedHealth = health;

            int area = (int) Math.round(args.area);
            updateProfileValue(profileAr, displayedArea, area, "AR: " + area);
            displayedArea = area;


            int gold = args.currentGold();
            updateProfileValue(profileGd, displayedGold, gold, "GD: " + gold);
            displayedGold = gold;

            buildTowerInfo.setText("" + args.createCost + " GD, +10 XP");
        });
    }

    public void updateProfileValue(TextView textView, int displayedValue, int newValue, String text) {
        if (displayedValue < 0) {
            textView.setText(text);
        } else if (displayedValue != newValue) {
            textView.setText(text);
            int color = PROFILE_VALUE_NORMAL_COLOR;
            int highlight;
            if (newValue > displayedValue) {
                highlight = PROFILE_VALUE_INC_COLOR;
                game().onGameNotification(text + " (+" + (newValue - displayedValue) + ") ", NotificationType.INFO);
            } else {
                highlight = PROFILE_VALUE_DEC_COLOR;
                game().onGameNotification(text + " (" + (newValue - displayedValue) + ") ", NotificationType.INFO);
            }
            textView.setTextColor(highlight);

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float alpha, Transformation t) {
                    super.applyTransformation(alpha, t);
                    textView.setTextColor(Utils.mulColors(highlight, color, alpha));
                }
            };
            animation.setDuration(1200);
            textView.startAnimation(animation);


        }
    }

    @Override
    public void onTowersGeoDataChanged(MapExtent extent) {
        runOnUiThread(this::onCameraMove);
    }
}
