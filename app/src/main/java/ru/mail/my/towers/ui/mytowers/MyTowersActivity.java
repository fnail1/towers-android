package ru.mail.my.towers.ui.mytowers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.ui.BaseActivity;

public class MyTowersActivity extends BaseActivity {

    @BindView(R.id.list)
    RecyclerView list;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_towers);
        ButterKnife.bind(this);
        setTitle("Мои башни");

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new MyTowersListAdapter());
    }
}
