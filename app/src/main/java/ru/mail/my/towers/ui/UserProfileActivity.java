package ru.mail.my.towers.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.model.UserInfo;

import static ru.mail.my.towers.TowersApp.data;

public class UserProfileActivity extends BaseActivity {
    public static final String PARAM_UID = "uid";

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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ButterKnife.bind(this);

        UserInfo userInfo = data().users.select(getIntent().getLongExtra(PARAM_UID, -1));
        setTitle(userInfo.name);

        profileLv.setText("LV: " + (userInfo.currentLevel + 1));
        profileXp.setText("XP: " + userInfo.exp);
        profileHp.setText("HP: " + userInfo.health.current + "/" + userInfo.health.max);
        profileAr.setText("AR: " + Math.round(userInfo.area));
        profileGd.setText("GD: " + userInfo.gold.current);
    }
}
