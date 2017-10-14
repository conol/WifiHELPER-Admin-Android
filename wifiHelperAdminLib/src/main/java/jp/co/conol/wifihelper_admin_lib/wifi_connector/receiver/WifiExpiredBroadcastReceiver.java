package jp.co.conol.wifihelper_admin_lib.wifi_connector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import jp.co.conol.wifihelper_admin_lib.wifi_connector.WifiConnector;

/**
 * Created by Masafumi_Ito on 2017/10/06.
 */

public class WifiExpiredBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        // ssidを取得
        final String ssid = intent.getStringExtra("ssid");

        // wifiを解除
        WifiConnector.deleteAccessPoint(context, ssid);
        WifiConnector.tryDisconnect(context); // removeWifiSetting後に実行

        // メッセージを表示
        Toast.makeText(context, "Wifi期限切れ ", Toast.LENGTH_LONG).show();
    }
}
