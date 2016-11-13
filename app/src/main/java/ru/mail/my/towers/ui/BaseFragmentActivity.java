package ru.mail.my.towers.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import static ru.mail.my.towers.TowersApp.appState;
import static ru.mail.my.towers.diagnostics.Logger.traceUi;


public class BaseFragmentActivity extends FragmentActivity {
    protected boolean resumed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        traceUi(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        traceUi(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        traceUi(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        traceUi(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        traceUi(this);
        appState().setTopActivity(this);
        super.onResume();
        resumed = true;
    }

    @Override
    protected void onPause() {
        resumed = false;
        traceUi(this);
        appState().resetTopActivity(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        traceUi(this);
        super.onBackPressed();
    }


}
