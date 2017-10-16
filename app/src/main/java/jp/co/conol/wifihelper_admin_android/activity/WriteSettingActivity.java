package jp.co.conol.wifihelper_admin_android.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_admin_android.MyUtil;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.corona.CoronaNfc;
import jp.co.conol.wifihelper_admin_lib.corona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.device_manager.GetDevicesAsyncTask;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;

public class WriteSettingActivity extends AppCompatActivity implements TextWatcher {

    private boolean isScanning = false;
    private Handler mScanDialogAutoCloseHandler = new Handler();
    private CoronaNfc mCoronaNfc;
    private int mWifiKind;
    List<String> mDeviceIds = new ArrayList<>();    // WifiHelperのサービスに登録されているデバイスのID一覧
    private EditText mSsidEditText;
    private EditText mPassEditText;
    private TextView mWepTextView;
    private TextView mWpaTextView;
    private TextView mNoneTextView;
    private Button mStartScanButton;
    private ConstraintLayout mExpireDateConstraintLayout;
    private TextView mExpireDateTextView;
    private ConstraintLayout mScanBackgroundConstraintLayout;
    private ConstraintLayout mScanDialogConstraintLayout;
    private final int PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_setting);

        mSsidEditText = (EditText) findViewById(R.id.ssidEditText);
        mPassEditText = (EditText) findViewById(R.id.passEditText);
        mWepTextView = (TextView) findViewById(R.id.wepTextView);
        mWpaTextView = (TextView) findViewById(R.id.wpaTextView);
        mNoneTextView = (TextView) findViewById(R.id.noneTextView);
        mStartScanButton = (Button) findViewById(R.id.startScanButton);
        mExpireDateConstraintLayout = (ConstraintLayout) findViewById(R.id.expireDateConstraintLayout);
        mExpireDateTextView = (TextView) findViewById(R.id.expireDateTextView);
        mScanBackgroundConstraintLayout = (ConstraintLayout) findViewById(R.id.ScanBackgroundConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) findViewById(R.id.scanDialogConstraintLayout);

        try {
            mCoronaNfc = new CoronaNfc(this);
        } catch (NFCNotAvailableException e) {
            Log.d("CoronaNfc", e.toString());
            finish();
        }

        // nfcから情報を取得
        Intent intent = getIntent();
        final String ssid = intent.getStringExtra("ssid");
        final String pass = intent.getStringExtra("pass");
        final int wifiKind = intent.getIntExtra("wifiKind", 1);
        final int expireDate = intent.getIntExtra("expireDate", -1);

        // nfcからの情報をセット
        mSsidEditText.setText(ssid);
        mPassEditText.setText(pass);
        mWifiKind = wifiKind;
        switch (wifiKind) {
            case 1:
                setWpaWifi();
                break;
            case 2:
                setWepWifi();
                break;
            default:
                setNoneWifi();
                break;
        }
        if(expireDate != -1) {
            mExpireDateTextView.setText(String.valueOf(expireDate) + getString(R.string.write_expire_date_option));
        }

        // ssidかpasswordが空欄ならスキャンを開始できないようにする
        setEnableStartScanButton(mSsidEditText.getText().toString());
        setEnableStartScanButton(mPassEditText.getText().toString());
        mSsidEditText.addTextChangedListener(this);
        mPassEditText.addTextChangedListener(this);

        // wifiの種類をクリックした場合
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
        final String[] expireDays = new String[365];
        for(int i = 0; i < expireDays.length; i++) {
            expireDays[i] = String.valueOf(i + 1) + getString(R.string.write_expire_date_option);
        }
        mExpireDateConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int defaultItem;
                if(mExpireDateTextView.getText() == getString(R.string.write_expire_date)) {
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
                            .setSingleChoiceItems(expireDays, defaultItem - 1, new DialogInterface.OnClickListener() {
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
                                        String checkedDate = String.valueOf(Integer.parseInt(checkedItems.get(0).toString()) + 1);
                                        mExpireDateTextView.setText(checkedDate + getString(R.string.write_expire_date_option));
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

        // スキャン画面が開いているときは、背景のタップを出来ないように設定
        mScanBackgroundConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return isScanning;
            }
        });
    }


    @Override
    protected void onNewIntent(final Intent intent) {
        // サーバーに登録されているデバイスIDを取得
        final Handler handler = new Handler();
        if (MyUtil.Network.isConnected(this)) {
            new GetDevicesAsyncTask(new GetDevicesAsyncTask.AsyncCallback() {
                @Override
                public void onSuccess(List<List<String>> deviceIdList) {

                    // 接続成功してもデバイスID一覧が無ければエラー
                    if(deviceIdList == null || deviceIdList.size() == 0) {
                        showAlertDialog();
                        return;
                    } else {
                        // デバイスIDのリストを作成
                        for(int i = 0; i < deviceIdList.size(); i++) {
                            mDeviceIds.add(deviceIdList.get(i).get(0));
                        }
                    }

                    // 入力されている設定の取得
                    final String ssid = String.valueOf(mSsidEditText.getText());
                    final String pass = String.valueOf(mPassEditText.getText());
                    final int expireDate = Integer.parseInt(mExpireDateTextView.getText().toString().replace(getString(R.string.write_expire_date_option), ""));

                    CNFCTag tag = mCoronaNfc.getWriteTagFromIntent(intent);

                    if (tag != null) {

                        // nfcに書き込むjson
                        String serviceIdString = WifiHelper.createJson(ssid, pass, mWifiKind, expireDate);
                        byte[] serviceId = serviceIdString.getBytes(StandardCharsets.UTF_8);

                        try {
                            tag.writeServiceID(serviceId);

                            Intent writeDoneIntent = new Intent(WriteSettingActivity.this, WriteDoneActivity.class);
                            startActivity(writeDoneIntent);

                            isScanning = false;
                            closeScanPage();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    showAlertDialog();
                }

                // デバイスID取得失敗でアラートを表示
                private void showAlertDialog() {
                    handler.post(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(WriteSettingActivity.this)
                                    .setMessage(getString(R.string.error_fail_get_device_ids))
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .show();
                        }
                    });
                }
            }).execute();
        }
        // ネットに未接続の場合はエラー
        else {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_network_disable))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        }
    }

    public void onStartScanButtonClicked(View view) {
        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要（wifi接続時）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 許可を求めるダイアログを表示
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                    PERMISSION_REQUEST_CODE
            );

        } else {
            if (!isScanning) {

                // nfc読み込み待機
                mCoronaNfc.enableForegroundDispatch(WriteSettingActivity.this);
                isScanning = true;
                openScanPage();

                // 60秒後に自動で閉じる
                mScanDialogAutoCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isScanning) {
                            cancelScan();
                        }
                    }
                }, 60000);
            }
        }
    }

    public void onCancelScanButtonClicked(View view) {
        if(isScanning) {
            cancelScan();

            // 60秒後に閉じる予約をキャンセル
            mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
        }
    }

    private void cancelScan() {
        // nfc読み込み待機を解除
        mCoronaNfc.disableForegroundDispatch(WriteSettingActivity.this);
        isScanning = false;
        closeScanPage();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK) {

            // 読み込み中に戻るタップでスキャン中止
            if(isScanning) {
                cancelScan();
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

    // 読み込み画面を非表示
    private void closeScanPage() {
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanBackgroundConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を非表示（背景は残す）
    private void closeScanDialog() {
        mScanDialogConstraintLayout.setVisibility(View.GONE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を非表示（背景）
    private void closeScanBackground() {
        mScanBackgroundConstraintLayout.setVisibility(View.GONE);
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out_slowly));
        mScanDialogAutoCloseHandler.removeCallbacksAndMessages(null);
    }

    // 読み込み画面を表示
    private void openScanPage() {
        mScanDialogConstraintLayout.setVisibility(View.VISIBLE);
        mScanBackgroundConstraintLayout.setVisibility(View.VISIBLE);
        mScanDialogConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom));
        mScanBackgroundConstraintLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slowly));
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    // ssidかpasswordが空欄ならスキャンを開始できないようにする
    @Override
    public void afterTextChanged(Editable editable) {
        String inputString = editable.toString();
        setEnableStartScanButton(inputString);
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
