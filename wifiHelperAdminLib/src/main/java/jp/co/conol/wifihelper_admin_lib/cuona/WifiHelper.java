package jp.co.conol.wifihelper_admin_lib.cuona;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.SignIn;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.Owner;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderException;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model.Wifi;
import jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_receiver.WifiExpiredBroadcastReceiver;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/10/04.
 */

public class WifiHelper extends AsyncTask<WifiHelper.Task, Void, Object> {

    private AsyncCallback mAsyncCallback = null;
    private Context mContext = null;
    private SignIn mSignIn = null;
    private WifiManager mWifiManager;
    private int mNetworkId = -1;
    private String mSsid;
    public static final int WPA_WPA2PSK = 1;  // 暗号化方式がWPA/WPA2-PSK
    public static final int WEP         = 2;  // 暗号化方式がWEP
    public static final int FREE        = 3;  // 暗号化なし

    public enum Task {
        SignIn,                 // サインイン
        GetAvailableDevices,    // WifiHelperを利用可能なデバイス一覧を取得
        GetOwnersDevices        // オーナーがWifiHelperに利用可能なデバイス一覧を取得
    }

    public interface AsyncCallback {
        void onSuccess(Object obj);
        void onFailure(Exception e);
    }

    public WifiHelper setContext(Context context) {
        mContext = context;
        return this;
    }

    public WifiHelper setSingIn(SignIn singIn) {
        mSignIn = singIn;
        return this;
    }

    public WifiHelper(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

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
            editor.putLong("expireDateTime", calendarToDate(expirationDay).getTime());
            editor.putString("ssid", mSsid);
            editor.apply();

            // アラームを受信するレシーバーを作成
            Intent alarmIntent = new Intent(context.getApplicationContext(), WifiExpiredBroadcastReceiver.class);
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

    public static Wifi readWifiSetting(Intent intent, Cuona cuona) throws CuonaReaderException {
        try {
            JSONObject jsonObject = new JSONObject(cuona.readJson(intent, true));
            JSONObject wifi = jsonObject.getJSONObject("wifi");

            // daysが設定されていない場合はnullにする
            Integer days;
            try {
                days = wifi.getInt("days");
            } catch (Exception e) {
                days = null;
            }

            // ssidが設定されていない場合は空白にする
            String ssid;
            try {
                ssid = wifi.getString("ssid");
            } catch (Exception e) {
                ssid = "";
            }

            // パスワードが設定されていない場合は空白にする
            String password;
            try {
                password = wifi.getString("pass");
            } catch (Exception e) {
                password = "";
            }

            // Wifiの種類が設定されていない場合はWPA_WPA2PSKにする
            int kind;
            try {
                kind = wifi.getInt("kind");
            } catch (Exception e) {
                kind = WifiHelper.WPA_WPA2PSK;
            }

            return new Wifi(ssid, password, kind, days);

        } catch (JSONException e) {
            e.printStackTrace();
            throw new CuonaReaderException(e);
        }
    }

    public static void writeWifiSetting(Intent intent, Context context, Cuona cuona, Wifi wifi) {
        try {
            // 本体に保存されているオーナー情報を取得
            String devicePassword = getString(context, "devicePassword");

            // 書き込まれているjsonを読み込み
            String readString = cuona.readJson(intent, false);
            JSONObject readJson = new JSONObject("{}");
            if(!Objects.equals(readString, "") && readString != null){
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
            cuona.writeJson(context, intent, readJson.toString(), devicePassword);

        } catch (JSONException|CuonaReaderException e) {
            e.printStackTrace();
        }
    }

    // 書き込みに必要なパスワード情報とAppTokenが保存されているか否かを返す関数
    public static boolean hasToken(Context context) {
        return getString(context, "devicePassword") != null && getString(context, "appToken") != null;
    }

    @Override
    protected Object doInBackground(Task... params) {

        Gson gson = new Gson();
        String endPoint = Constants.Urls.CDMS_URL;

        // サーバーに送信用するjsonをURLを作成
        String apiUrl = null;
        String requestJsonString = null;
        JSONObject json = null;
        String responseJsonString = null;
        Type type = null;
        boolean isDevelopment = false;

        try {
            switch (params[0]) {

                // サインイン
                case SignIn:
                    apiUrl = "/api/owners/sign_in.json";
                    requestJsonString = gson.toJson(mSignIn);
                    responseJsonString = post(endPoint + apiUrl, null, requestJsonString);

                    // 保存用のオーナー情報
                    OwnerPrivate ownerPrivate = gson.fromJson(responseJsonString, OwnerPrivate.class);
                    type = new TypeToken<Owner>(){}.getType();

                    // 書き込みに必要なパスワード情報を端末に保存
                    saveString(mContext, "devicePassword", ownerPrivate.getDevicePassword());

                    // AppTokenを本体に保存
                    saveString(mContext, "appToken", ownerPrivate.getAppToken());
                    break;

                // WifiHelperを利用可能なデバイス一覧を取得
                case GetAvailableDevices:
                    String appToken = null;
                    isDevelopment = Cuona.isDevelopment();
                    if(isDevelopment) {
                        apiUrl = "/api/admins/devices.json";
                        appToken = "J5B4o9y2iJTbckKfxsLsKq23";
                    } else {
                        apiUrl = "/api/services/" + Constants.Urls.SERVICE_KEY + ".json";
                    }
                    responseJsonString = get(endPoint + apiUrl, appToken);
                    type = new TypeToken<ArrayList<String>>(){}.getType();

                    List<String> availableDeviceIdList = new ArrayList<>();
                    if(responseJsonString != null) {
                        try {
                            JSONArray jsonArray = new JSONArray(responseJsonString);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jRec = jsonArray.getJSONObject(i);
                                if(isDevelopment && !jRec.getBoolean("is_development")) continue;   // 開発版鍵のときはCUONA開発版のデバイスID全てを取得
                                String deviceId = jRec.getString("device_id");
                                availableDeviceIdList.add(deviceId);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // デバイスのオブジェクト一覧のjsonをデバイスIDのオブジェクト一覧に変更
                    responseJsonString = gson.toJson(availableDeviceIdList);
                    break;

                // オーナーがWifiHelperに利用可能なデバイス一覧を取得
                case GetOwnersDevices:
                    isDevelopment = Cuona.isDevelopment();
                    if(isDevelopment) {
                        apiUrl = "/api/owners/devices.json";
                    } else {
                        apiUrl = "/api/owners/devices.json?service_key=H7Pa7pQaVxxG";
                    }
                    responseJsonString = get(endPoint + apiUrl, getString(mContext, "appToken"));
                    type = new TypeToken<ArrayList<String>>(){}.getType();

                    List<String> ownersDeviceIdList = new ArrayList<>();
                    if(responseJsonString != null) {
                        try {
                            JSONArray jsonArray = new JSONArray(responseJsonString);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jRec = jsonArray.getJSONObject(i);
                                if(isDevelopment && !jRec.getBoolean("is_development")) continue;   // 開発版鍵のときはCUONA開発版のデバイスID全てを取得
                                String deviceId = jRec.getString("device_id");
                                ownersDeviceIdList.add(deviceId);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // デバイスのオブジェクト一覧のjsonをデバイスIDのオブジェクト一覧に変更
                    responseJsonString = gson.toJson(ownersDeviceIdList);
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            Log.e("onFailure", e.toString());
            onFailure(e);
        }

        return gson.fromJson(responseJsonString, type);
    }

    @Override
    protected void onPostExecute(Object obj) {
        super.onPostExecute(obj);
        onSuccess(obj);
    }

    private void onSuccess(Object obj) {
        this.mAsyncCallback.onSuccess(obj);
    }

    private void onFailure(Exception e) {
        this.mAsyncCallback.onFailure(e);
    }

    // サインイン用のPOSTリクエスト
    private String post(String url, String appToken, String body) {
        String responseJsonString = null;
        try {
            String buffer = "";
            HttpURLConnection con = null;
            URL urlTmp = new URL(url);
            con = (HttpURLConnection) urlTmp.openConnection();
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept-Language", "jp");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if(appToken != null && !Objects.equals(appToken, "")) con.setRequestProperty("Authorization", "Bearer " + appToken);
            OutputStream os = con.getOutputStream();
            PrintStream ps = new PrintStream(os);
            ps.print(body);
            ps.close();

            // レスポンスを取得
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            responseJsonString = reader.readLine();

            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HttpException", e.toString());
        }

        return responseJsonString;
    }

    // デバイス一覧取得用のGETリクエスト
    protected String get(String url, String appToken) {
        HttpURLConnection urlCon;
        InputStream in;

        try {
            urlCon = (HttpURLConnection) new URL(url).openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.setDoInput(true);
            urlCon.setRequestProperty("Authorization", "Bearer " + appToken);
            urlCon.connect();

            String str_json;
            in = urlCon.getInputStream();
            InputStreamReader objReader = new InputStreamReader(in);
            BufferedReader objBuf = new BufferedReader(objReader);
            StringBuilder strBuilder = new StringBuilder();
            String sLine;
            while((sLine = objBuf.readLine()) != null){
                strBuilder.append(sLine);
            }
            str_json = strBuilder.toString();
            in.close();

            return str_json;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HttpException", e.toString());
            return null;
        }
    }

    // SharedPreferenceに文字列を保存
    private static void saveString(Context context, String key, String string) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putString(key ,string).apply();
    }

    // SharedPreferenceからjsonを取得
    private static String getString(Context context, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(key, null);
    }

    private Date calendarToDate(Calendar calendar){
        return calendar.getTime();
    }
}
