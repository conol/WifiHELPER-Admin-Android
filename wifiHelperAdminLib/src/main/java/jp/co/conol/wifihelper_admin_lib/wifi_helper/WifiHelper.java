package jp.co.conol.wifihelper_admin_lib.wifi_helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.Owner;
import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_lib.cuona.CuonaException;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.receiver.WifiExpiredBroadcastReceiver;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/10/04.
 */

public class WifiHelper {

    private WifiManager mWifiManager;
    private int mNetworkId = -1;
    private String mSsid;
    public static final int WPA_WPA2PSK = 1;  // 暗号化方式がWPA/WPA2-PSK
    public static final int WEP         = 2;  // 暗号化方式がWEP
    public static final int FREE        = 3;  // 暗号化なし

    // コンストラクタ
    public WifiHelper(Context context, String ssid, String password, int kind, Integer days) {
        this.mSsid = ssid;

        // 接続期限日時の算出
        Calendar expirationDay = Calendar.getInstance();
        if(days != null && 1 <= days && days <= 365) {
            expirationDay.setTime(new Date(System.currentTimeMillis()));
            expirationDay.add(Calendar.DATE, days);
        } else {
            expirationDay = null;
        }

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

        // 接続処理
        switch (kind) {
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

        // wifiの有効期限を設定
        SharedPreferences pref = context.getSharedPreferences("wifiHelper", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(expirationDay != null) {

            // 有効期限の日時とSSIDを保存
            editor.putLong("expireDateTime", Util.Transform.calendarTodate(expirationDay).getTime());
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
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, expirationDay.getTimeInMillis(), pending);
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

                        return mWifiManager.enableNetwork(mNetworkId, true);
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
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            try {
                return wifiManager.disableNetwork(wifiInfo.getNetworkId());
            } catch (NullPointerException e) {
                Log.d("onFailure: ", e.toString());
            }
        }

        return false;
    }

    public int getNetworkId() {
        return mNetworkId;
    }

    public static boolean isEnable(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public static void setEnable(Context context, boolean state) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager != null) wifiManager.setWifiEnabled(state);
    }

    // 手動で設定したWifiは削除不可能（Android 6.0 以降）
    @SuppressWarnings("deprecation")
    public static boolean deleteAccessPoint(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);

        if(wifiManager != null) {
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
        }

        return false;
    }

    public static Wifi readWifiSetting(Intent intent, Cuona cuona) throws CuonaException {
        try {
            return parseJsonToObj(cuona.readJson(intent));
        } catch (CuonaException | JSONException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
    }

    public static void writeWifiSetting(Intent intent, Context context, Cuona cuona, Wifi wifi) throws CuonaException {
        try {
            String readString = cuona.readJsonNonLog(intent);

            // 本体に保存されているオーナー情報を取得
            Gson gson = new Gson();
            Owner owner = gson.fromJson(Util.SharedPref.get(context, "owner"), Owner.class);

            // 読み込んだjson
            JSONObject readJson = new JSONObject("{}");
            if(!Objects.equals(readString, "")){
                readJson = new JSONObject(readString);
            }

            // 読み込んだjsonからWifiHelperの情報を削除
            readJson.remove("wifi");

            // 読み込んだjsonに新しい情報を書き込む
            JSONObject writeJson = new JSONObject();
            writeJson.put("id", "H7Pa7pQaVxxG");
            writeJson.put("ssid", wifi.getSsid());
            writeJson.put("pass", wifi.getPassword());
            writeJson.put("kind", wifi.getKind());
            if(wifi.getDays() != null) writeJson.put("days", wifi.getDays());
            readJson.put("wifi", writeJson);
            cuona.writeJson(intent, readJson.toString(), owner.getDevicePassword());

        } catch (CuonaException | JSONException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
    }

    private static Wifi parseJsonToObj(String targetJson) throws JSONException {
        JSONObject jsonObject = new JSONObject(targetJson);
        JSONObject wifi = jsonObject.getJSONObject("wifi");

        // daysが設定されていない場合はnullにする
        Integer days ;
        try {
            days = wifi.getInt("days");
        } catch (Exception e) {
            days = null;
        }

        return new Wifi(
                wifi.getString("ssid"),
                wifi.getString("pass"),
                wifi.getInt("kind"),
                days
        );
    }

    public static boolean isAvailable(String targetJson) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(targetJson);
            jsonObject.getJSONObject("wifi");
            return true;
        } catch (JSONException e) {
            Log.e("WifiHelper", e.toString());
            return false;
        }
    }
}
