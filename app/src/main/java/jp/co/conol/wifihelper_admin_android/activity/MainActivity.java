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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

            // Wifi情報を渡して移動
            Intent writeSettingIntent = new Intent(MainActivity.this, WriteSettingActivity.class);
            writeSettingIntent.putExtra("ssid", wifi.getSsid());
            writeSettingIntent.putExtra("pass", wifi.getPassword());
            writeSettingIntent.putExtra("wifiKind", wifi.getKind());
            writeSettingIntent.putExtra("expireDate", wifi.getDays());
            writeSettingIntent.putExtra("deviceType", deviceType);
            startActivity(writeSettingIntent);

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

    public void onLogoutButtonClicked(View view) {
        WifiHelper.deleteToken(this);
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
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
