package jp.co.conol.wifihelper_admin_lib.wifi_helper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;

/**
 * Created by Masafumi_Ito on 2017/10/06.
 */

public class WifiExpiredBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        // ssidを取得
        final String ssid = intent.getStringExtra("ssid");

        // wifiを解除
        WifiHelper.deleteAccessPoint(context, ssid);
        WifiHelper.tryDisconnect(context); // removeWifiSetting後に実行

        // メッセージを表示
        Toast.makeText(context, "Wi-Fi Helperで設定したWi-Fiの有効期限が切れました", Toast.LENGTH_LONG).show();
    }
}
