package ru.mail.my.towers.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import retrofit2.Response;
import ru.mail.my.towers.R;
import ru.mail.my.towers.api.model.GsonAuthResponse;
import ru.mail.my.towers.api.model.GsonGetProfileResponse;
import ru.mail.my.towers.api.model.GsonRequestAuthResponse;
import ru.mail.my.towers.toolkit.ThreadPool;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.game;
import static ru.mail.my.towers.diagnostics.Logger.trace;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.phone)
    EditText phoneEdit;

    @BindView(R.id.code)
    EditText codeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        codeEdit.setEnabled(false);
        phoneEdit.requestFocus();
    }

    @OnEditorAction(R.id.phone)
    boolean onPhoneEditComplete(TextView textView, int i, KeyEvent keyEvent) {
        trace();
        String number = normalizePhoneNumber(textView.getText());
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH)
                .execute(() -> requestCode(number));
        return true;
    }

    private void requestCode(String phone) {
        try {
            Response<GsonRequestAuthResponse> response = api().requestAuth(phone, "rus").execute();
            if (HttpURLConnection.HTTP_OK != response.code()) {
                onRequestError(getString(R.string.error_server_error, response.code()));
                return;
            }
            GsonRequestAuthResponse body = response.body();
            if (!body.success) {
                onRequestError(getString(R.string.error_common_server_error));
                return;
            }
            runOnUiThread(() -> {
                codeEdit.setEnabled(true);
                codeEdit.requestFocus();
            });
        } catch (IOException e) {
            onRequestError(getString(R.string.error_network_error));
        }
    }

    private void onRequestError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            phoneEdit.setEnabled(true);
            codeEdit.setEnabled(false);
        });
    }


    @NonNull
    private String normalizePhoneNumber(CharSequence phone) {
        StringBuilder sb = new StringBuilder(phone.length());
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (Character.isDigit(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @OnEditorAction(R.id.code)
    boolean onCodeEditComplete(TextView textView, int i, KeyEvent keyEvent) {
        trace();
        codeEdit.setEnabled(false);
        String number = normalizePhoneNumber(phoneEdit.getText());
        String code = codeEdit.getText().toString();
        ThreadPool.SLOW_EXECUTORS.getExecutor(ThreadPool.Priority.HIGH)
                .execute(() -> checkCode(number, code));
        return true;
    }

    private void checkCode(String number, String code) {
        try {
            Response<GsonAuthResponse> response = api().auth(number, code).execute();
            if (HttpURLConnection.HTTP_OK != response.code()) {
                onRequestError(getString(R.string.error_server_error, response.code()));
                return;
            }
            GsonAuthResponse body = response.body();
            if (!body.success) {
                onRequestError(getString(R.string.error_common_server_error));
                return;
            }

            if (!body.loginData.isNewUser) {
                app().onLogin(number, body.loginData.token);
                Response<GsonGetProfileResponse> myProfile = api().getMyProfile().execute();
                game().updateMyProfile(myProfile.body().profile);
            } else {
                app().onLogin(number, body.loginData.token);
            }


            runOnUiThread(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });


        } catch (IOException e) {
            onRequestError(getString(R.string.error_network_error));
        }
    }

}
