package ru.mail.my.towers.ui.popups;

import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerUpdateAction;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.ui.widgets.HighlightTargetDrawable;
import ru.mail.my.towers.utils.Utils;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.data;
import static ru.mail.my.towers.TowersApp.game;

public class TowerInfoPopup implements IMapPopup, View.OnClickListener {
    private final ViewGroup parent;
    private final IMapPopup.IMapActivity activity;
    private final View popupView;
    private final HighlightTargetDrawable background;
    private final int padding;
    private Tower tower;

    @BindView(R.id.infos_container)
    View infos;
    @BindView(R.id.actions_container)
    View actions;
    @BindView(R.id.caption)
    TextView caption;
    @BindView(R.id.owner_title)
    TextView ownerTitle;
    @BindView(R.id.owner)
    TextView owner;
    @BindView(R.id.location_title)
    TextView locationTitle;
    @BindView(R.id.location)
    TextView location;
    @BindView(R.id.level_title)
    TextView levelTitle;
    @BindView(R.id.level)
    TextView level;
    @BindView(R.id.health_title)
    TextView healthTitle;
    @BindView(R.id.health)
    TextView health;
    @BindView(R.id.gold_title)
    TextView goldTitle;
    @BindView(R.id.gold)
    TextView gold;
    @BindView(R.id.repair)
    TextView repair;
    @BindView(R.id.attack)
    TextView attack;
    @BindView(R.id.destroy)
    TextView destroy;
    @BindView(R.id.upgrade)
    TextView upgrade;

    public TowerInfoPopup(ViewGroup parent, IMapPopup.IMapActivity activity) {
        this.parent = parent;
        this.activity = activity;
        popupView = LayoutInflater.from(this.parent.getContext()).inflate(R.layout.popup_tower_info, null);
        background = new HighlightTargetDrawable();
        ButterKnife.bind(this, popupView);
        popupView.setBackground(background);
        popupView.setOnClickListener(this);
        repair.setOnClickListener(this);
        destroy.setOnClickListener(this);
        attack.setOnClickListener(this);
        padding = parent.getResources().getDimensionPixelOffset(R.dimen.padding);
    }

    public void show(GoogleMap map, Tower tower) {
        this.tower = tower;
        parent.addView(popupView);

        Projection projection = map.getProjection();
        LatLng latLng = new LatLng(tower.lat, tower.lng);
        Point point = projection.toScreenLocation(latLng);

        int radius = parent.getResources().getDimensionPixelOffset(R.dimen.popup_poi_window_size);
        background.setWindow(point.x, point.y, radius);

//        RelativeLayout.LayoutParams poiLayout = (RelativeLayout.LayoutParams) poiAnchor.getLayoutParams();
//        poiLayout.leftMargin = point.x - radius;
//        poiLayout.topMargin = point.y - radius;
//        poiAnchor.setLayoutParams(poiLayout);

        if (tower.my) {
            String ownerName = game().me.name + " (Вы)";
            owner.setText(ownerName);
            attack.setVisibility(View.GONE);
            if (tower.health < tower.maxHealth) {
                repair.setEnabled(true);
                repair.setText("починить " + tower.repairCost + "$");
            } else {
                repair.setEnabled(false);
                repair.setText("починить");
            }
        } else {
            UserInfo owner = data().users.select(tower.owner);
            this.owner.setText(owner.name);
            repair.setVisibility(View.GONE);
            destroy.setVisibility(View.GONE);
        }
        caption.setText(tower.title);
        location.setText(Utils.formatLocation(latLng));
        level.setText(String.valueOf(tower.level));
        String healthStr = String.format(Locale.getDefault(), "%d / %d", tower.health, tower.maxHealth);
        health.setText(healthStr);
        gold.setText(String.valueOf(tower.goldGain) + '$');

        RelativeLayout.LayoutParams infosLayout = (RelativeLayout.LayoutParams) infos.getLayoutParams();
        infosLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
        infosLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        infosLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        infosLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);

        actions.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        RelativeLayout.LayoutParams actionLayout = (RelativeLayout.LayoutParams) actions.getLayoutParams();

        if (point.y < parent.getHeight() / 2) {
            if (point.x < parent.getWidth() / 2) {
                // I quoter
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                actionLayout.leftMargin = point.x + radius;
                actionLayout.topMargin = point.y - radius;
            } else {
                // II quoter
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                if (point.y > actions.getMeasuredHeight() + 2 * padding) {
                    actionLayout.leftMargin = parent.getWidth() - actions.getMeasuredWidth() - padding;
                    actionLayout.topMargin = point.y - radius - actions.getMeasuredHeight();
                } else {
                    actionLayout.leftMargin = point.x - radius - actions.getMeasuredWidth();
                    actionLayout.topMargin = point.y - radius - padding;
                }
            }
        } else {
            if (point.x < parent.getWidth() / 2) {
                // III quoter
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                actionLayout.leftMargin = point.x + radius;
                actionLayout.topMargin = point.y + radius;
            } else {
                // IV quoter
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                infosLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                actionLayout.leftMargin = point.x - radius - actions.getMeasuredWidth();
                actionLayout.topMargin = point.y + radius;
            }
            caption.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            infosLayout.topMargin = caption.getMeasuredHeight();
        }
        infos.setLayoutParams(infosLayout);
        actions.setLayoutParams(actionLayout);

        animateAppearance(ownerTitle, 100);
        animateAppearance(owner, 120);
        animateAppearance(locationTitle, 150);
        animateAppearance(location, 170);
        animateAppearance(levelTitle, 200);
        animateAppearance(level, 220);
        animateAppearance(healthTitle, 250);
        animateAppearance(health, 270);
        animateAppearance(goldTitle, 300);
        animateAppearance(gold, 320);
        animateAppearance(repair, 350);
        animateAppearance(attack, 350);
        animateAppearance(upgrade, 350);
        animateAppearance(destroy, 350);
    }

    @Override
    public void close() {
        animateDisappearance(ownerTitle);
        animateDisappearance(owner);
        animateDisappearance(locationTitle);
        animateDisappearance(location);
        animateDisappearance(levelTitle);
        animateDisappearance(level);
        animateDisappearance(healthTitle);
        animateDisappearance(health);
        animateDisappearance(goldTitle);
        animateDisappearance(gold);
        animateDisappearance(repair);
        animateDisappearance(attack);
        animateDisappearance(upgrade);
        animateDisappearance(destroy);

        popupView.animate()
                .alpha(0)
                .setDuration(500)
                .withEndAction(() -> {
                    parent.removeView(popupView);
                    activity.onPopupResult(this);
                });
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
    public void onClick(View v) {
        if (v == popupView) {
            close();
        } else if (v == repair) {
            api().updateTower(tower.serverId, TowerUpdateAction.repair);
        } else if (v == destroy) {

        } else if (v == attack) {

        }
    }
}
