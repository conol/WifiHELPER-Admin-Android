package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_admin_android.custom.ProgressDialog;
import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_lib.cuona.NFCNotAvailableException;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderException;
import jp.co.conol.wifihelper_admin_android.MyUtil;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_android.custom.CuonaUtil;
import jp.co.conol.wifihelper_admin_android.custom.ScanCuonaDialog;
import jp.co.conol.wifihelper_admin_android.custom.SimpleAlertDialog;
import jp.co.conol.wifihelper_admin_lib.cuona.WifiHelper;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.Wifi;

public class MainActivity extends AppCompatActivity {

    private ScanCuonaDialog mScanCuonaDialog;
    private Cuona mCuona;
    private List<String> mAvailableDeviceIdList = new ArrayList<>();    // WifiHelperのサービスに登録されているデバイスのID一覧
    private final int PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mCuona = new Cuona(this);
        } catch (NFCNotAvailableException e) {
            Log.d("Cuona", e.toString());
            finish();
        }

        // CUONAスキャンダイアログのインスタンスを生成
        mScanCuonaDialog = new ScanCuonaDialog(MainActivity.this, mCuona, 60000, false);

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        CuonaUtil.checkAccessCoarseLocationPermission(this, PERMISSION_REQUEST_CODE);

        // Bluetoothがオフの場合はダイアログを表示
        CuonaUtil.checkBluetoothSetting(this, mCuona);

        // nfcがオフの場合はダイアログを表示
        CuonaUtil.checkNfcSetting(this, mCuona);

        // サーバーに登録されているデバイスIDを取得
        if (MyUtil.Network.isEnable(this) || WifiHelper.isEnable(MainActivity.this)) {

            // 読み込みダイアログを表示
            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.progress_message));
            progressDialog.show();

            new WifiHelper(new WifiHelper.AsyncCallback() {
                @Override
                public void onSuccess(Object object) {
                    mAvailableDeviceIdList = (List<String>) object;

                    // 読み込みダイアログを非表示
                    progressDialog.dismiss();

                    // デバイス情報の取得失敗でエラーダイアログを表示
                    if(mAvailableDeviceIdList == null || mAvailableDeviceIdList.size() == 0) {
                        new SimpleAlertDialog(MainActivity.this, getString(R.string.error_fail_get_device_ids)).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // 読み込みダイアログを非表示
                            progressDialog.dismiss();

                            new SimpleAlertDialog(MainActivity.this, getString(R.string.error_fail_get_device_ids)).show();
                        }
                    });
                }
            }).execute(WifiHelper.Task.GetAvailableDevices);
        }
        // ネットに未接続の場合はエラー
        else {
            new SimpleAlertDialog(MainActivity.this, getString(R.string.error_network_disable)).show();
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

            // nfc読み込み処理実行
            int deviceType;
            String deviceId;
            Wifi wifi;
            try {
                deviceType = mCuona.readType(intent);
                deviceId = mCuona.readDeviceId(intent);
                wifi = WifiHelper.readWifiSetting(intent, mCuona);
            } catch (CuonaReaderException e) {
                e.printStackTrace();
                new SimpleAlertDialog(MainActivity.this, getString(R.string.error_incorrect_touch_nfc)).show();
                mScanCuonaDialog.dismiss();
                return;
            }

            // サーバーに登録されているWifiHelper利用可能なデバイスに、タッチされたNFCが含まれているか否か確認
            if(mAvailableDeviceIdList != null && deviceId != null) {
                if (!mAvailableDeviceIdList.contains(deviceId)) {
                    new SimpleAlertDialog(MainActivity.this, getString(R.string.error_not_exist_in_devise_ids)).show();
                }
                // 利用可能なら次のページへWifi情報を渡して移動
                else {
                    Intent writeSettingIntent = new Intent(MainActivity.this, WriteSettingActivity.class);
                    writeSettingIntent.putExtra("ssid", wifi.getSsid());
                    writeSettingIntent.putExtra("pass", wifi.getPassword());
                    writeSettingIntent.putExtra("wifiKind", wifi.getKind());
                    writeSettingIntent.putExtra("expireDate", wifi.getDays());
                    writeSettingIntent.putExtra("deviceType", deviceType);
                    startActivity(writeSettingIntent);
                }
            } else {
                new SimpleAlertDialog(MainActivity.this, getString(R.string.error_incorrect_touch_nfc)).show();
            }

            mScanCuonaDialog.dismiss();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mScanCuonaDialog.isShowing()) {
                mScanCuonaDialog.dismiss();
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
}
