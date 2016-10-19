package com.krava.vkmedia.presentation.view.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.MvpAppCompatActivity;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.presenter.LoginPresenter;
import com.krava.vkmedia.presentation.view.LoginView;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

/**
 * Created by krava2008 on 13.06.16.
 */

public class LoginActivity extends MvpAppCompatActivity implements LoginView {
    @InjectPresenter
    LoginPresenter presenter;

    public static final int CUSTOM_LOGIN_CODE = 1602;
    private ProgressDialog progress;
    private EditText authPass;
    private EditText authLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VKSdk.isLoggedIn()){
            startMainActivity();
        }
        setContentView(R.layout.activity_login);

        authPass = (EditText)findViewById(R.id.auth_pass);
        authLogin = (EditText)findViewById(R.id.auth_login);

        this.progress = new ProgressDialog(this);
        this.progress.setIndeterminate(true);
        this.progress.setMessage(getString(R.string.label_loading));
        this.progress.setCancelable(false);

        findViewById(R.id.auth_login_btn).setOnClickListener(v -> {
            if(checkInternetConnection(LoginActivity.this)) {
                doCustomLogin(
                        authPass.getText().toString(),
                        authLogin.getText().toString()
                );
            }else{
                showAlert(R.string.error_title_connection_error, R.string.error_message_connection_error);
            }
        });
    }

    public void showProgress(){
        this.progress.show();
    }

    public void doCustomLogin(String pass, String login){
        pass = pass.trim();
        login = login.trim();
        if(!pass.equals("") && !login.equals("")) {
            showProgress();
            Intent tabActivity = new Intent(this, AuthTransActivity.class);
            tabActivity.putExtra("login", login);
            tabActivity.putExtra("pass", pass);
            startActivityForResult(tabActivity, LoginActivity.CUSTOM_LOGIN_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        VKCallback<VKAccessToken> callback = new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                startMainActivity();
            }

            @Override
            public void onError(VKError error) {
                showAlert(R.string.auth_error, R.string.error_message_unknown);
            }
        };
        if(requestCode == CUSTOM_LOGIN_CODE){
            this.progress.dismiss();
            if(resultCode == RESULT_OK){
                startMainActivity();
            }else if(resultCode == RESULT_CANCELED){
                showAlert(R.string.auth_error, R.string.label_error_login);
            }
        }else if (!VKSdk.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void startMainActivity(){
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainActivity);
        finish();
    }

    private void showAlert(int title, int messege){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this, R.style.MyDialogTheme);
        builder.setTitle(getString(title));
        builder.setMessage(getString(messege));
        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                (dialog, which) -> {
                    // positive button logic
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkInternetConnection(Context context){
        ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (cm == null) {
            return false;
        }
        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnected() &&
                cm.getActiveNetworkInfo().isAvailable();
    }
}
