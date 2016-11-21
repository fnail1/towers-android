package ru.mail.my.towers.ui.popups;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.mail.my.towers.R;
import ru.mail.my.towers.ui.widgets.HighlightTargetDrawable;
import ru.mail.my.towers.utils.Utils;
import ru.mail.my.towers.service.LocationAppService;

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

    private PopupDialogResult dialogResult = PopupDialogResult.CANCEL;
    private Location targetLocation;

    public CreateTowerPopup(ViewGroup parent, IMapActivity activity) {
        this.parent = parent;
        this.activity = activity;
        popupView = LayoutInflater.from(this.parent.getContext()).inflate(R.layout.popup_create_tower, null);
        background = new HighlightTargetDrawable();
        ButterKnife.bind(this, popupView);
        popupView.setBackground(background);
    }

    @Override
    public void show() {
        dialogResult = PopupDialogResult.CANCEL;

        parent.addView(popupView);
        popupView.setScaleX(1);
        popupView.setScaleY(1);
        animateAppearance(caption, 100);

        animateAppearance(nameTitle, 130);
        animateAppearance(name, 150);

        animateAppearance(locationTitle, 180);
        animateAppearance(location, 200);

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

    public String getName() {
        return name.getText().toString();
    }

    public void setLocation(Location args) {
        if (args == null || Utils.isMockProvider(args)) {
            targetLocation = null;
            this.location.setText("Не определено");
        } else if ((System.currentTimeMillis() - args.getTime()) > 5 * 60 * 1000) {
            targetLocation = null;
            this.location.setText("Данные устарели");
        }
        targetLocation = args;
        this.location.setText(Utils.formatLocation(args));
    }

    public void setPOI(int x, int y, float radius) {
        background.setWindow(x, y, radius);
    }

    public void setCost(int cost) {
        this.cost.setText("" + cost);
    }

    @OnClick(R.id.positive)
    void onPositiveClick() {
        dialogResult = PopupDialogResult.POSITIVE;
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

    public PopupDialogResult getResult() {
        return dialogResult;
    }

    public Location getLocation() {
        return targetLocation;
    }

    @Override
    public void onLocationChanged(LocationAppService sender, Location args) {
        setLocation(args);
    }

}
