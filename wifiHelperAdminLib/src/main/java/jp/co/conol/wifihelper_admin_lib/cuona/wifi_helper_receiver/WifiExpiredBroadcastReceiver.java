package jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import jp.co.conol.wifihelper_admin_lib.cuona.WifiHelper;

/**
 * Created by Masafumi_Ito on 2017/10/06.
 */

public class WifiExpiredBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        // ssidを取得
        SharedPreferences pref = context.getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
        String ssid = pref.getString("ssid", null);

        // wifiを解除
        WifiHelper.deleteAccessPoint(context, ssid);
        WifiHelper.tryDisconnect(context); // removeWifiSetting後に実行

        // メッセージを表示
        Toast.makeText(context, "Wi-Fi Helperで設定したWi-Fiの有効期限が切れました", Toast.LENGTH_LONG).show();
    }
}
