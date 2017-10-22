package jp.co.conol.wifihelper_admin_lib.wifi_helper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.util.Date;

import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;

/**
 * Created by Masafumi_Ito on 2017/10/04.
 */

public class WifiStateWatcher extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

            // 変化前の状態を取得
            int previousState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // 変化後の状態を取得
            int currentState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // wifi接続時に期限が切れていたらAP削除
            if(currentState == WifiManager.WIFI_STATE_ENABLED) {

                // 有効期限の日時とSSIDを取得
                SharedPreferences pref = context.getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
                Date expireDateTime = new Date(pref.getLong("expireDateTime", -1));
                String ssid = pref.getString("ssid", null);

                // 現在の日時を取得
                Date currentDateTime = new Date(System.currentTimeMillis());

                if(expireDateTime.compareTo(currentDateTime) <= 0 && ssid != null) {

                    // wifiを解除
                    WifiHelper.deleteAccessPoint(context, ssid);
                    WifiHelper.tryDisconnect(context); // removeWifiSetting後に実行

                    // メッセージを表示
                    Toast.makeText(context, "Wifi期限切れてます", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

