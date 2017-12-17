package jp.co.conol.wifihelper_admin_android.custom;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_android.R;

/**
 * Created by m_ito on 2017/12/14.
 */

public class CuonaUtil {

    // Bluetoothがオフの場合はダイアログを表示
    public static void checkBluetoothSetting(Context context, final Cuona cuona) {
        if(!cuona.isBluetoothEnabled()) {
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.bluetooth_dialog))
                    .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cuona.setBluetoothEnable(true);
                        }
                    })
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show();
        }
    }

    // nfcがオフの場合はダイアログを表示
    public static void checkNfcSetting(final Context context, Cuona cuona) {
        if(!cuona.isNfcEnabled()) {
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.nfc_dialog))
                    .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            context.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                        }
                    })
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show();
        }
    }

    // 位置情報の取得許可のダイアログを表示
    public static void checkAccessCoarseLocationPermission(Activity activity, final int PERMISSION_REQUEST_CODE) {

        // Android6.0以上はACCESS_COARSE_LOCATIONの許可が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 許可されていない場合
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // 許可を求めるダイアログを表示
                ActivityCompat.requestPermissions(activity,
                        new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }
}
