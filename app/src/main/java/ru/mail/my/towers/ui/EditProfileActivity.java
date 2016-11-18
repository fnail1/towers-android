package ru.mail.my.towers.ui;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import retrofit2.Response;
import ru.mail.my.towers.R;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.model.UserInfo;
import ru.mail.my.towers.service.GameService;
import ru.mail.my.towers.toolkit.ThreadPool;

import static ru.mail.my.towers.TowersApp.api;
import static ru.mail.my.towers.TowersApp.game;

public class EditProfileActivity extends AppCompatActivity implements GameService.MyProfileEventHandler, GameService.UpdateMyProfileCallback {


    @BindView(R.id.name)
    EditText name;

    @BindView(R.id.color)
    EditText color;

    @BindView(R.id.color_sample)
    View colorSample;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ButterKnife.bind(this);
        setTitle(getString(R.string.edit_profile_title));
        color.addTextChangedListener(new MyColorFormatter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        game().myProfileEvent.add(this);
    }

    @Override
    protected void onPause() {
        game().myProfileEvent.remove(this);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveAndClose();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAndClose() {
        String newName = name.getText().toString();
        if (newName.length() < 3) {
            Toast.makeText(this, R.string.error_name_too_short, Toast.LENGTH_LONG).show();
            return;
        }

        int newColor;
        try {
            newColor = Integer.parseInt(color.getText().toString(), 16);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_color, Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.show();
        game().updateMyProfile(newName, newColor, this);

    }

    @Override
    public void onMyProfileChanged(UserInfo args) {
        runOnUiThread(() -> {
            name.setText(args.name);
            color.setText(Integer.toString(args.color, 16));
        });
    }


    private void onRequestError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            if (progressDialog != null)
                progressDialog.dismiss();
        });
    }

    @Override
    public void onUpdateMyProfileServerError(int code) {
        onRequestError(getString(R.string.error_server_error, code));
    }

    @Override
    public void onUpdateMyProfileCommonServerError() {
        onRequestError(getString(R.string.error_common_server_error));
    }

    @Override
    public void onUpdateMyProfileComplete() {
        runOnUiThread(() -> {
            if (progressDialog != null)
                progressDialog.dismiss();
            Toast.makeText(EditProfileActivity.this, R.string.save_profile_success, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onUpdateMyProfileNetworkError() {
        onRequestError(getString(R.string.error_network_error));
    }

    private class MyColorFormatter implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            int color = UserInfo.parseColor(EditProfileActivity.this.color.getText().toString()) & 0xffffff;

//            String hexColor = Integer.toHexString(color);
//            if (hexColor.length() > 6)
//                hexColor = hexColor.substring(0, 6);
//            else {
//                StringBuilder sb = new StringBuilder("000000");
//                sb.replace(6 - hexColor.length(), 6, hexColor);
//                hexColor = sb.toString();
//            }
//            Log.d("EditProfileActivity", hexColor);

            colorSample.setBackgroundColor(0xff000000 | color);
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
