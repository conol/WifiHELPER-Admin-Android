package jp.co.conol.wifihelper_admin_android.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_admin_android.MyUtil;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_lib.cuona.CuonaException;
import jp.co.conol.wifihelper_admin_lib.cuona.NfcNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.GetAvailableDevices;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;

public class MainActivity extends AppCompatActivity {

    Handler mScanDialogAutoCloseHandler = new Handler();
    private Cuona mCuona;
    private boolean isScanning = false;
    List<String> mDeviceIds = new ArrayList<>();    // WifiHelperのサービスに登録されているデバイスのID一覧
    private final int PERMISSION_REQUEST_CODE = 1000;
    private ConstraintLayout mScanBackgroundConstraintLayout;
    private ConstraintLayout mScanDialogConstraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanBackgroundConstraintLayout = (ConstraintLayout) findViewById(R.id.ScanBackgroundConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) findViewById(R.id.scanDialogConstraintLayout);

        try {
            mCuona = new Cuona(this);
        } catch (NfcNotAvailableException e) {
            Log.d("Cuona", e.toString());
            finish();
        }

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 許可されていない場合
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // 許可を求めるダイアログを表示
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_CODE
                );
            }
        }

        // nfcがオフの場合はダイアログを表示
        if(!mCuona.isEnable()) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.nfc_dialog))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if(isScanning) {

            // サーバーに登録されているデバイスIDを取得
            final Handler handler = new Handler();
            if (MyUtil.Network.isConnected(this) || WifiHelper.isEnable(MainActivity.this)) {
                new GetAvailableDevices(new GetAvailableDevices.AsyncCallback() {
                    @Override
                    public void onSuccess(List<String> deviceIdList) {

                        // 接続成功してもデバイスID一覧が無ければエラー
                        if(deviceIdList == null || deviceIdList.size() == 0) {
                            showAlertDialog();
                            return;
                        } else {
                            // デバイスIDのリストを作成
                            mDeviceIds.addAll(deviceIdList);
                        }

                        // nfc読み込み処理実行
                        int deviceType;
                        String deviceId;
                        try {
                            deviceType = mCuona.readType(intent);
                            deviceId = mCuona.readDeviceId(intent);
                        } catch (CuonaException e) {
                            Log.d("CuonaReader", e.toString());
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(getString(R.string.error_not_exist_in_devise_ids))
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .show();
                            return;
                        }

                        // サーバーに登録されているWifiHelper利用可能なデバイスに、タッチされたNFCが含まれているか否か確認
                        if(mDeviceIds != null && deviceId != null) {

                            if (!mDeviceIds.contains(deviceId)) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(getString(R.string.error_not_exist_in_devise_ids))
                                        .setPositiveButton(getString(R.string.ok), null)
                                        .show();
                            }
                            // 含まれていれば処理を進める
                            else {
                                try {
                                    // Wifi設定情報を取得
                                    final Wifi wifi = WifiHelper.readWifiSetting(intent, mCuona);

                                    Intent writeSettingIntent = new Intent(MainActivity.this, WriteSettingActivity.class);
                                    writeSettingIntent.putExtra("ssid", wifi.getSsid());
                                    writeSettingIntent.putExtra("pass", wifi.getPassword());
                                    writeSettingIntent.putExtra("wifiKind", wifi.getKind());
                                    writeSettingIntent.putExtra("expireDate", wifi.getDays());
                                    writeSettingIntent.putExtra("deviceType", deviceType);
                                    startActivity(writeSettingIntent);
                                    isScanning = false;
                                    closeScanPage();
                                }
                                // 読み込んだnfcがWifiHelperに未対応の場合
                                catch (CuonaException e) {
                                    e.printStackTrace();
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(getString(R.string.error_read_service_failed))
                                            .setPositiveButton(getString(R.string.ok), null)
                                            .show();
                                }
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
                                new AlertDialog.Builder(MainActivity.this)
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

        }
        // nfcがオフの場合はダイアログを表示
        else if(!mCuona.isEnable()) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.nfc_dialog))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        } else {
            if (!isScanning) {

                // nfc読み込み待機
                mCuona.enableForegroundDispatch(MainActivity.this);
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
        mCuona.disableForegroundDispatch(MainActivity.this);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {

            // パーミッションを許可しない場合
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.grant_permission, Toast.LENGTH_LONG).show();
            }
        }
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
}
