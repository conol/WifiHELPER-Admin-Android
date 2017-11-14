package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.DeviceManager;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.Owner;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.SignIn;
import jp.co.conol.wifihelper_admin_lib.Util;

public class SignInActivity extends AppCompatActivity {

    private EditText mSignInMailEditText;
    private EditText mSignInPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mSignInMailEditText = (EditText) findViewById(R.id.signInMailEditText);
        mSignInPasswordEditText = (EditText) findViewById(R.id.signInPasswordEditText);

//        // オーナー情報が保存されていれば、次のスキャンページへ移動
//        if(DeviceManager.hasToken(this)) {
//            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
//        }
    }

    public void onSignInButtonTapped(View view) {

        // 入力欄の文字列
        String emailString = mSignInMailEditText.getText().toString();
        String passwordString = mSignInPasswordEditText.getText().toString();

        // バリデーション
        if(Util.Str.isBlank(emailString)) {
            Toast.makeText(this, getString(R.string.validation_email_is_blank), Toast.LENGTH_SHORT).show();
        } else if(50 < emailString.length()) {
            Toast.makeText(this, getString(R.string.validation_email_too_long), Toast.LENGTH_SHORT).show();
        } else if(passwordString.length() < 6) {
            Toast.makeText(this, getString(R.string.validation_password_is_too_short), Toast.LENGTH_SHORT).show();
        } else if(50 < passwordString.length()) {
            Toast.makeText(this, getString(R.string.validation_password_too_long), Toast.LENGTH_SHORT).show();
        } else {

            // サインイン用オブジェクトを作成
            SignIn signIn = new SignIn(emailString, passwordString);

            // ログイン処理
            new DeviceManager(new DeviceManager.AsyncCallback() {
                @Override
                public void onSuccess(Object object) {
                    Owner owner = (Owner) object;

                    if(object != null) {
                        // スキャン画面へ移動
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(SignInActivity.this, getString(R.string.validation_not_correct), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("onFailure", e.toString());
                }
            }).setContext(this).setSingIn(signIn).execute(DeviceManager.Task.SignIn);
        }
    }

    // 「このアプリについて」をタップした場合はアバウトページへ移動
    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }
}
