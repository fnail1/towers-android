package ru.mail.my.towers.ui.notifications;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.ui.BaseActivity;
import ru.mail.my.towers.ui.mytowers.MyTowersListAdapter;

public class NotificationsActivity extends BaseActivity{
    @BindView(R.id.list)
    RecyclerView list;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        ButterKnife.bind(this);
        setTitle("Уведомления");

        list.setLayoutManager(new LinearLayoutManager(this));
        NotificationsAdapter adapter = new NotificationsAdapter();
        adapter.init();
        list.setAdapter(adapter);
    }
}
