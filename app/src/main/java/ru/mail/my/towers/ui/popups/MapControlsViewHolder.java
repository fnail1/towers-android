package ru.mail.my.towers.ui.popups;

import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;

public class MapControlsViewHolder {
    @BindView(R.id.profile_lv)
    View profile_lv;
    @BindView(R.id.profile_xp)
    View profile_xp;
    @BindView(R.id.profile_hp)
    View profile_hp;
    @BindView(R.id.profile_ar)
    View profile_ar;
    @BindView(R.id.profile_gd)
    View profile_gd;

    public MapControlsViewHolder(View root) {
        ButterKnife.bind(this, root);
    }
}
