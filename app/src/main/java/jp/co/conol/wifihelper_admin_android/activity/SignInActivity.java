package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.conol.wifihelper_admin_android.MyUtil;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_android.custom.ProgressDialog;
import jp.co.conol.wifihelper_admin_android.custom.SimpleAlertDialog;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.SignIn;
import jp.co.conol.wifihelper_admin_lib.cuona.WifiHelper;

public class SignInActivity extends AppCompatActivity {

    @BindView(R.id.signInMailEditText) EditText mSignInMailEditText;
    @BindView(R.id.signInPasswordEditText) EditText mSignInPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        // ログイン済みでオーナー情報が保存されていれば、次のページへ移動
        if(WifiHelper.hasToken(this)) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void onSignInButtonTapped(View view) {

        // 入力欄の文字列
        String emailString = mSignInMailEditText.getText().toString();
        String passwordString = mSignInPasswordEditText.getText().toString();

        // バリデーション
        if(MyUtil.Str.isBlank(emailString)) {
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
            if (MyUtil.Network.isEnable(this) || WifiHelper.isEnable(SignInActivity.this)) {

                // 読み込みダイアログを表示
                final ProgressDialog progressDialog = new ProgressDialog(SignInActivity.this);
                progressDialog.setMessage(getString(R.string.sign_in_progress_message));
                progressDialog.show();

                new WifiHelper(new WifiHelper.AsyncCallback() {
                    @Override
                    public void onSuccess(Object object) {

                        // 読み込みダイアログを非表示
                        progressDialog.dismiss();

                        if (object != null) {
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            new SimpleAlertDialog(SignInActivity.this, getString(R.string.error_common)).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("onFailure", e.toString());
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // 読み込みダイアログを非表示
                                progressDialog.dismiss();

                                new SimpleAlertDialog(SignInActivity.this, getString(R.string.error_common)).show();
                            }
                        });
                    }
                }).setContext(this).setSingIn(signIn).execute(WifiHelper.Task.SignIn);
            }
            // ネットに未接続の場合はエラー
            else {
                new SimpleAlertDialog(SignInActivity.this, getString(R.string.error_network_disable_sign_in)).show();
            }
        }
    }

    // 「このアプリについて」をタップした場合はアバウトページへ移動
    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }
}
