package ru.mail.my.towers.ui.popups;

import android.graphics.Point;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.mail.my.towers.R;
import ru.mail.my.towers.service.LocationAppService;
import ru.mail.my.towers.ui.widgets.HighlightTargetDrawable;
import ru.mail.my.towers.utils.Utils;

import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.TowersApp.location;

public class CreateTowerPopup implements IMapPopup, LocationAppService.LocationChangedEventHandler {

    private final ViewGroup parent;
    private final IMapActivity activity;
    private final View popupView;
    private final HighlightTargetDrawable background;

    @BindView(R.id.caption)
    TextView caption;

    @BindView(R.id.name_title)
    TextView nameTitle;

    @BindView(R.id.name)
    EditText name;

    @BindView(R.id.location_title)
    TextView locationTitle;

    @BindView(R.id.location)
    TextView location;

    @BindView(R.id.cost_title)
    TextView costTitle;

    @BindView(R.id.cost)
    TextView cost;

    @BindView(R.id.positive)
    TextView positive;

    @BindView(R.id.negative)
    TextView negative;

    private double latitude;
    private double longitude;

    public CreateTowerPopup(ViewGroup parent, IMapActivity activity) {
        this.parent = parent;
        this.activity = activity;
        popupView = LayoutInflater.from(this.parent.getContext()).inflate(R.layout.popup_tower_create, null);
        background = new HighlightTargetDrawable();
        ButterKnife.bind(this, popupView);
        popupView.setBackground(background);
    }

    public void show(GoogleMap map) {
        Point point = new Point(parent.getWidth() / 2, parent.getHeight() / 2);
        LatLng latLng = map.getProjection().fromScreenLocation(point);
        setCost(game().me.createCost);
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        setLocation(location);
        setName(game().me.name + " (Башня " + (game().me.towersCount + 1) + ")");
        background.setWindow(point.x, point.y, (float) parent.getResources().getDimensionPixelOffset(R.dimen.popup_poi_window_size));

        parent.addView(popupView);

        animateAppearance(caption, 100);

        animateAppearance(nameTitle, 130);
        animateAppearance(name, 150);

        animateAppearance(locationTitle, 180);
        animateAppearance(this.location, 200);

        animateAppearance(costTitle, 230);
        animateAppearance(cost, 250);

        animateAppearance(negative, 200);
        animateAppearance(positive, 250);

//        location().locationChangedEvent.add(this);
        onLocationChanged(location(), location().currentLocation());
    }

    @Override
    public void close() {
//        location().locationChangedEvent.remove(this);
        animateDisappearance(caption);
        animateDisappearance(nameTitle);
        animateDisappearance(name);
        animateDisappearance(locationTitle);
        animateDisappearance(location);
        animateDisappearance(costTitle);
        animateDisappearance(cost);
        animateDisappearance(positive);
        animateDisappearance(negative);

        popupView.animate()
                .alpha(0)
                .setDuration(150)
                .withEndAction(() -> {
                    parent.removeView(popupView);
                    activity.onPopupResult(this);
                });
    }

    public void setName(String value) {
        name.setText(value);
    }

    public void setLocation(Location args) {
        if (args == null || Utils.isMockProvider(args)) {
            latitude = longitude = Double.NaN;
            this.location.setText("Не определено");
            positive.setEnabled(false);
        } else if ((System.currentTimeMillis() - args.getTime()) > 5 * 60 * 1000) {
            latitude = longitude = Double.NaN;
            this.location.setText("Данные устарели");
            positive.setEnabled(false);
        } else {
            latitude = args.getLatitude();
            longitude = args.getLongitude();
            this.location.setText(Utils.formatLocation(latitude, longitude));
            positive.setEnabled(true);
        }
    }

    public void setCost(int cost) {
        this.cost.setText("" + cost);
    }

    @OnClick(R.id.positive)
    void onPositiveClick() {
        game().createTower(latitude, longitude, name.getText().toString());
        close();
    }

    @OnClick(R.id.negative)
    void onNegativeClick() {
        close();
    }

    private void animateAppearance(View view, int duration) {
        view.setAlpha(0);
        view.setTranslationX(parent.getWidth());
        view.setTranslationY(0);
        view.animate()
                .alpha(1)
                .translationX(0)
                .setDuration(duration);
    }

    private void animateDisappearance(View view) {
        view.animate()
                .alpha(0)
                .translationY(-parent.getHeight())
                .translationX(-parent.getWidth() / 2)
                .setDuration(300);
    }

    @Override
    public void onLocationChanged(LocationAppService sender, Location args) {
        setLocation(args);
    }

}
