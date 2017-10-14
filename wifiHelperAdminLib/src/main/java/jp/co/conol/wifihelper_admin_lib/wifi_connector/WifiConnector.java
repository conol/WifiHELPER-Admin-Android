package jp.co.conol.wifihelper_admin_lib.wifi_connector;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.wifi_connector.receiver.WifiExpiredBroadcastReceiver;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/10/04.
 */

public class WifiConnector {

    private WifiManager mWifiManager;
    private int mNetworkId = -1;
    private String mSsid;
    public static final int WPA_WPA2PSK = 1;  // 暗号化方式がWPA/WPA2-PSK
    public static final int WEP         = 2;  // 暗号化方式がWEP
    public static final int FREE        = 3;  // 暗号化なし

    // コンストラクタ
    public WifiConnector(Context context, String ssid, String password, int encMethod, Calendar calendarExpire) {
        this.mSsid = ssid;

        // wifi設定用インスタンス
        mWifiManager  = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiConfiguration config = new WifiConfiguration();

        // ssidが既に登録済みの場合、端末内から設定を取得（要Wifi接続）
        if(mWifiManager.getConfiguredNetworks() != null) {
            for (WifiConfiguration configInDevice : mWifiManager.getConfiguredNetworks()) {
                if (configInDevice.SSID.equals('"' + ssid + '"')) {
                    mNetworkId = configInDevice.networkId;  // networkIdの取得
                    break;
                }
            }
        }

        // ssidが端末に登録されていない場合、接続処理を続行
        if(mNetworkId == -1) {

            switch (encMethod) {
                case FREE:
                    freeConnect(config);
                    break;
                case WEP:
                    wepConnect(config, password);
                    break;
                case WPA_WPA2PSK:
                    wpaConnect(config, password);
                    break;
                default:
                    Log.d("onFailure: ", "Wifiの暗号化方式設定が正しくありません");
                    break;
            }
        }

        // wifiの有効期限を設定
        SharedPreferences pref = context.getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(calendarExpire != null) {

            // 有効期限の日時とSSIDを保存
            editor.putLong("expireDateTime", Util.Transform.calendarTodate(calendarExpire).getTime());
            editor.putString("ssid", mSsid);
            editor.apply();

            // アラームを受信するレシーバーを作成
            Intent alarmIntent = new Intent(context.getApplicationContext(), WifiExpiredBroadcastReceiver.class);
            alarmIntent.putExtra("ssid", mSsid);
            PendingIntent pending = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // アラームをセットする
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendarExpire.getTimeInMillis(), pending);
        } else {
            editor.clear().apply();
        }
    }

    @SuppressWarnings("deprecation")
    private void freeConnect(WifiConfiguration config) {
        config.SSID = "\"" + mSsid + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedAuthAlgorithms.clear();
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        mNetworkId = mWifiManager.addNetwork(config); // 失敗した場合は-1

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mWifiManager.saveConfiguration();
        }
        mWifiManager.updateNetwork(config);
    }

    @SuppressWarnings("deprecation")
    private void wepConnect(WifiConfiguration config, String password) {
        config.SSID = "\"" + mSsid + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.wepKeys[0] = "\"" + password + "\"";
        config.wepTxKeyIndex = 0;

        mNetworkId = mWifiManager.addNetwork(config); // 失敗した場合は-1

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mWifiManager.saveConfiguration();
        }
        mWifiManager.updateNetwork(config);
    }

    @SuppressWarnings("deprecation")
    private void wpaConnect(WifiConfiguration config, String password) {

        config.SSID = "\"" + mSsid + "\"";
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.preSharedKey = "\"" + password + "\"";

        mNetworkId = mWifiManager.addNetwork(config); // 失敗した場合は-1

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mWifiManager.saveConfiguration();
        }
        mWifiManager.updateNetwork(config);
    }

    public boolean tryConnect() {

        // WiFi機能が無効の状態で呼び出されるとSSID検索の所でnullとなるので念のため例外処理を行なう
        try {

            // ssidの検索を開始
            mWifiManager.startScan();
            for (ScanResult result : mWifiManager.getScanResults()) {

                // Android4.2以降よりダブルクォーテーションが付いてくるので除去
                String resultSSID = result.SSID.replace("\"", "");

                if (resultSSID.equals(mSsid)) {

                    // 接続を行う
                    if (mNetworkId > 0) {

                        // 先に既存接続先を無効にする
                        for (WifiConfiguration confExist : mWifiManager.getConfiguredNetworks()) {
                            mWifiManager.enableNetwork(confExist.networkId, false);
                        }

                        mWifiManager.enableNetwork(mNetworkId, true);
                        return true;
                    }
                    break;
                }
            }
        } catch (NullPointerException e) {
            Log.d("onFailure: ", e.toString());
        }

        return false;
    }

    public static boolean tryDisconnect(Context context) {
        try {
            WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            return wifiManager.disableNetwork(wifiInfo.getNetworkId());
        } catch (NullPointerException e) {
            Log.d("onFailure: ", e.toString());
        }

        return false;
    }

    public int getNetworkId() {
        return mNetworkId;
    }

    public static boolean isEnable(Context context) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void setEnable(Context context, boolean state) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }

    // 手動で設定したWifiは削除不可能（Android 6.0 以降）
    @SuppressWarnings("deprecation")
    public static boolean deleteAccessPoint(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);

        List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations != null) {
            for (WifiConfiguration config : configurations) {
                if (config.SSID.equals('"' + ssid + '"')) {
                    return wifiManager.removeNetwork(config.networkId);
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            wifiManager.saveConfiguration();
        }

        return false;
    }
}
