package jp.co.conol.wifihelper_admin_android.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.conol.wifihelper_admin_android.MyUtil;
import jp.co.conol.wifihelper_admin_android.custom.CuonaUtil;
import jp.co.conol.wifihelper_admin_android.custom.ProgressDialog;
import jp.co.conol.wifihelper_admin_android.custom.ScanCuonaDialog;
import jp.co.conol.wifihelper_admin_android.custom.SimpleAlertDialog;
import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_lib.cuona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.cuona.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderException;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaWritableTag;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.Wifi;

public class WriteSettingActivity extends AppCompatActivity implements TextWatcher, CuonaWritableTag.Callback {

    private ScanCuonaDialog mScanCuonaDialog;
    private ProgressDialog mProgressDialog;
    private Cuona mCuona;
    private String mSsid;
    private String mPassword;
    private int mWifiKind;
    private Integer mExpireDate;
    private int mDeviceType;
    List<String> mOwnersDeviceIdList = new ArrayList<>();    // WifiHelperのサービスに登録されているオーナーのデバイスID一覧
    @BindView(R.id.ssidEditText) EditText mSsidEditText;
    @BindView(R.id.passEditText) EditText mPassEditText;
    @BindView(R.id.wepTextView) TextView mWepTextView;
    @BindView(R.id.wpaTextView) TextView mWpaTextView;
    @BindView(R.id.noneTextView) TextView mNoneTextView;
    @BindView(R.id.signInButtonConstrainLayout) Button mStartScanButton;
    @BindView(R.id.expireDateConstraintLayout) ConstraintLayout mExpireDateConstraintLayout;
    @BindView(R.id.expireDateTextView) TextView mExpireDateTextView;
    @BindView(R.id.coronaImageView) ImageView mCoronaImageView;
    private final int PERMISSION_REQUEST_CODE = 1000;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_setting);
        ButterKnife.bind(this);

        try {
            mCuona = new Cuona(this);
        } catch (NFCNotAvailableException e) {
            Log.d("Cuona", e.toString());
            finish();
        }

        // CUONAスキャンダイアログのインスタンスを生成
        mScanCuonaDialog = new ScanCuonaDialog(WriteSettingActivity.this, mCuona, 60000, false);

        // 前のページからNFCに書き込まれている情報を受け取る
        Intent intent = getIntent();
        final String ssid = intent.getStringExtra("ssid");
        final String pass = intent.getStringExtra("pass");
        final int wifiKind = intent.getIntExtra("wifiKind", WifiHelper.WPA_WPA2PSK);
        final int expireDate = intent.getIntExtra("expireDate", 0);
        mDeviceType = intent.getIntExtra("deviceType", Cuona.TAG_TYPE_UNKNOWN);

        // 前のページから受け取ったNFCに書き込まれている情報を画面に反映
        mSsidEditText.setText(ssid);
        mPassEditText.setText(pass);
        mWifiKind = wifiKind;
        switch (wifiKind) {
            case WifiHelper.WPA_WPA2PSK:
                setWpaWifi();
                break;
            case WifiHelper.WEP:
                setWepWifi();
                break;
            default:
                setNoneWifi();
                break;
        }
        if(expireDate != 0) {
            mExpireDateTextView.setText(String.valueOf(expireDate) + getString(R.string.write_expire_date_option));
        } else {
            mExpireDateTextView.setText(getString(R.string.write_expire_date_unlimited));
        }
        if(mDeviceType == Cuona.TAG_TYPE_SEAL) {
            mCoronaImageView.setImageResource(R.drawable.ic_nfc);
        } else {
            mCoronaImageView.setImageResource(R.drawable.img_cuona);
        }

        // ssidかpasswordが空欄ならスキャンを開始できないようにする
        setEnableStartScanButton(mSsidEditText.getText().toString());
        setEnableStartScanButton(mPassEditText.getText().toString());
        mSsidEditText.addTextChangedListener(this);
        mPassEditText.addTextChangedListener(this);

        // wifiの種類をクリックした場合、クリックによってボタンの表示を切り替え
        View.OnClickListener wifiKindClickListener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(view == mWpaTextView) {
                    setWpaWifi();
                } else if(view == mWepTextView) {
                    setWepWifi();
                } else {
                    setNoneWifi();
                }
            }
        };
        mWepTextView.setOnClickListener(wifiKindClickListener);
        mWpaTextView.setOnClickListener(wifiKindClickListener);
        mNoneTextView.setOnClickListener(wifiKindClickListener);

        // 期限日付選択時のダイアログ
        final String[] expireDays = new String[366];
        for(int i = 0; i < expireDays.length; i++) {
            if(i == 0) {
                expireDays[i] = getString(R.string.write_expire_date_unlimited);
            } else {
                expireDays[i] = String.valueOf(i) + getString(R.string.write_expire_date_option);
            }
        }
        mExpireDateConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int defaultItem;
                if(mExpireDateTextView.getText() == getString(R.string.write_expire_date)
                        || mExpireDateTextView.getText() == getString(R.string.write_expire_date_unlimited)) {
                    defaultItem = 0;
                } else {
                    defaultItem = Integer.parseInt(mExpireDateTextView.getText().toString().replace(getString(R.string.write_expire_date_option), ""));
                }
                final List<Integer> checkedItems = new ArrayList<>();
                checkedItems.add(defaultItem);

                // タップした時
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mExpireDateConstraintLayout.setAlpha(0.5f);
                }
                // 離した時
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mExpireDateConstraintLayout.setAlpha(1f);
                    new AlertDialog.Builder(WriteSettingActivity.this)
                            .setTitle(getString(R.string.write_expire_date_title))
                            .setSingleChoiceItems(expireDays, defaultItem, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkedItems.clear();
                                    checkedItems.add(which);
                                }
                            })
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!checkedItems.isEmpty()) {
                                        String checkedDate = String.valueOf(Integer.parseInt(checkedItems.get(0).toString()));
                                        if(checkedDate.equals("0")) {
                                            mExpireDateTextView.setText(getString(R.string.write_expire_date_unlimited));
                                        } else {
                                            mExpireDateTextView.setText(checkedDate + getString(R.string.write_expire_date_option));
                                        }
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                } else {
                    mExpireDateConstraintLayout.setAlpha(1f);
                }
                return false;
            }
        });

        // サーバーに登録されているオーナーがWifiHelperに利用可能なデバイスIDを取得
        final Gson gson = new Gson();
        if (MyUtil.Network.isEnable(this)) {

            // 読み込みダイアログを表示
            final ProgressDialog progressDialog = new ProgressDialog(WriteSettingActivity.this);
            progressDialog.setMessage(getString(R.string.progress_message));
            progressDialog.show();

            new WifiHelper(new WifiHelper.AsyncCallback() {
                @Override
                public void onSuccess(Object object) {
                    mOwnersDeviceIdList = (List<String>) object;

                    // 読み込みダイアログを非表示
                    progressDialog.dismiss();

                    // デバイス情報の取得失敗でエラーダイアログを表示
                    if(mOwnersDeviceIdList == null || mOwnersDeviceIdList.size() == 0) {
                        new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_not_exist_devise)).show();
                    } else {
                        MyUtil.SharedPref.saveString(WriteSettingActivity.this, "ownersDeviceIdList", gson.toJson(mOwnersDeviceIdList));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // 読み込みダイアログを非表示
                            progressDialog.dismiss();

                            new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_not_exist_devise)).show();
                        }
                    });
                }
            }).setContext(WriteSettingActivity.this).execute(WifiHelper.Task.GetOwnersDevices);
        }
        // ネットに未接続の場合はエラー
        else {
            mOwnersDeviceIdList = gson.fromJson(MyUtil.SharedPref.getString(WriteSettingActivity.this, "ownersDeviceIdList"), new TypeToken<ArrayList<String>>(){}.getType());
            if(mOwnersDeviceIdList == null || mOwnersDeviceIdList.size() == 0) {
                new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_network_disable)).show();
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCuona != null) mCuona.enableForegroundDispatch(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCuona != null) mCuona.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if(mScanCuonaDialog.isShowing()) {

            // プログレスダイアログを表示
            mProgressDialog = new ProgressDialog(WriteSettingActivity.this);
            mProgressDialog.setMessage(getString(R.string.writing_progress_message));
            mProgressDialog.show();

            // スキャンダイアログを非表示
            mScanCuonaDialog.dismiss();

            // 入力されている設定の取得
            mSsid = String.valueOf(mSsidEditText.getText());
            mPassword = String.valueOf(mPassEditText.getText());
            if (!mExpireDateTextView.getText().toString().equals(getString(R.string.write_expire_date_unlimited))) {
                mExpireDate = Integer.parseInt(mExpireDateTextView.getText().toString().replace(getString(R.string.write_expire_date_option), ""));
            } else {
                mExpireDate = null;
            }

            // nfc読み込み処理実行（デバイスIDを取得）
            String deviceId = null;
            try {
                deviceId = mCuona.readDeviceId(intent);
            } catch (CuonaReaderException e) {
                e.printStackTrace();
                new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_not_exist_in_devise_ids)).show();
                mScanCuonaDialog.dismiss();
                return;
            }

            // WifiHelperのサービスに登録されているオーナーのデバイスに、タッチされたNFCが含まれているか否か確認
            if (!mOwnersDeviceIdList.contains(deviceId)) {
                new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_not_owners)).show();
            }
            // Wifi情報の書き込み
            else {
                WifiHelper.writeWifiSetting(intent, WriteSettingActivity.this, mCuona, new Wifi(mSsid, mPassword, mWifiKind, mExpireDate));
            }
        }
    }

    @Override
    public void tagWriteStateChanged(CuonaWritableTag.State state) {

        if (state == CuonaWritableTag.State.SUCCESS) {

            // 次のページへWifi情報を渡して移動
            if(mProgressDialog.isShowing()) {
                Intent writeDoneIntent = new Intent(WriteSettingActivity.this, WriteDoneActivity.class);
                writeDoneIntent.putExtra("ssid", mSsid);
                writeDoneIntent.putExtra("pass", mPassword);
                writeDoneIntent.putExtra("wifiKind", mWifiKind);
                writeDoneIntent.putExtra("expireDate", mExpireDate);
                writeDoneIntent.putExtra("deviceType", mDeviceType);
                startActivity(writeDoneIntent);

                // プログレスダイアログを非表示
                mProgressDialog.dismiss();
            }

        } else if (state == CuonaWritableTag.State.ERROR) {

            // プログレスダイアログを非表示
            mProgressDialog.dismiss();

            new SimpleAlertDialog(WriteSettingActivity.this, getString(R.string.error_write_setting)).show();
        }
    }

    public void onStartScanButtonClicked(View view) {
        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        CuonaUtil.checkAccessCoarseLocationPermission(this, PERMISSION_REQUEST_CODE);

        // Bluetoothがオフの場合はダイアログを表示
        CuonaUtil.checkBluetoothSetting(this, mCuona);

        // nfcがオフの場合はダイアログを表示
        CuonaUtil.checkNfcSetting(this, mCuona);

        // BluetoothとNFCが許可されている場合処理を進める
        if(mCuona.isBluetoothEnabled() && mCuona.isNfcEnabled()) {
            mScanCuonaDialog.show();
        }
    }

    public void onSsidEditViewClearClicked(View view) {
        mSsidEditText.setText("");
    }

    public void onPassEditViewClearClicked(View view) {
        mPassEditText.setText("");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mScanCuonaDialog != null && mScanCuonaDialog.isShowing()) {
                mScanCuonaDialog.dismiss();
            } else if(mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            } else {
                finish();
            }
        }
        return false;
    }

    private void setWpaWifi() {
        mWifiKind = 1;
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_disable);
        mWepTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_enable);
        mWpaTextView.setTextColor(Color.WHITE);
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_disable);
        mNoneTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
    }

    private void setWepWifi() {
        mWifiKind = 2;
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_enable);
        mWepTextView.setTextColor(Color.WHITE);
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_disable);
        mWpaTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_disable);
        mNoneTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
    }

    private void setNoneWifi() {
        mWifiKind = 3;
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_disable);
        mWepTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_disable);
        mWpaTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_enable);
        mNoneTextView.setTextColor(Color.WHITE);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String inputString = editable.toString();

        // ssidかpasswordが空欄ならスキャンを開始できないようにする
        setEnableStartScanButton(inputString);

        // 50文字入力されたら通知（それ以上は入力不可）
        if(50 == inputString.length()) {
            Toast.makeText(this, getString(R.string.edit_text_limit_toast), Toast.LENGTH_LONG).show();
        }
    }

    private void setEnableStartScanButton(String string) {
        if(string.length() == 0) {
            mStartScanButton.setEnabled(false);
            mStartScanButton.setAlpha(0.5f);
        } else {
            mStartScanButton.setEnabled(true);
            mStartScanButton.setAlpha(1f);
        }
    }
}
