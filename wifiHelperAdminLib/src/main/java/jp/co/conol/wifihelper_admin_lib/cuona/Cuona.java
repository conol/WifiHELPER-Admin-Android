package jp.co.conol.wifihelper_admin_lib.cuona;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderException;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderLegacyTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderSecureTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaBTDevice;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaWritableT2;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaWritableTag;

import static android.content.Context.BLUETOOTH_SERVICE;
import static android.content.Context.WIFI_SERVICE;

@SuppressLint("StaticFieldLeak")
public class Cuona extends AsyncTask<String[][], Void, JSONObject> {

    private SendLogCallback mSendLogCallback;
    private Context mContext;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilters;
    private String[][] mTechList;
    private CuonaReaderTag mCuonaReaderTag = null;
    private Intent mNfcTouchIntent = null;
    private String mReadLogMessage = null;  // 読み込み時のログメッセージ
    private String mWriteLogMessage = null; // 書き込み時のログメッセージ
    public static final int TAG_TYPE_UNKNOWN = 0;
    public static final int TAG_TYPE_CUONA   = 1;
    public static final int TAG_TYPE_SEAL    = 2;

    public interface SendLogCallback {
        void onSuccess(JSONObject responseJson);
        void onFailure(Exception e);
    }

    private Cuona(SendLogCallback sendLogCallback){
        this.mSendLogCallback = sendLogCallback;
    }

    public Cuona(Context context) throws NFCNotAvailableException {
        this.mContext = context;

        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (mNfcAdapter == null) {
            throw new NFCNotAvailableException();
        }

        mPendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);

        mIntentFilters = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        mTechList = new String[][] {
                // Typ2 2 Mifare UltraLight Seal, like NXP NTAG21x
                new String[] { NfcA.class.getName(), Ndef.class.getName() },

                // Type 4 Dynamic, like ST M24SRxx
                new String[] { NfcA.class.getName(), IsoDep.class.getName(), /*Ndef.class.getName()*/ },
        };

        // 本体に登録されているログを取得（2次元配列）
        Gson gson = new Gson();
        final SharedPreferences pref = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
        String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

        // ネットに繋がっていればログの送信
        if(savedLog != null && (isNetworkEnable() || isWifiEnable())) {
            new Cuona(new SendLogCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {

                    // 保存されているログは削除
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("savedLog", null);
                    editor.apply();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d("SendLogFailure", e.toString());
                }
            }).execute(savedLog);
        }
    }

    // NFCがオンになっているか確認
    public boolean isNfcEnabled() {
        return mNfcAdapter.isEnabled();
    }

    // Bluetoothがオンになっているか確認
    public boolean isBluetoothEnabled() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        return bluetoothManager != null && bluetoothManager.getAdapter().isEnabled();
    }

    // Bluetoothのオン・オフを設定
    public void setBluetoothEnable(boolean state) {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        if(bluetoothManager != null) {
            if (state) {
                bluetoothManager.getAdapter().enable();
            } else {
                bluetoothManager.getAdapter().disable();
            }
        }
    }

    // 開発用鍵が使われている否か
    // package-private
    static boolean isDevelopment() {
//        return Keys.customerId == 0;
        return false;
    }

    public void enableForegroundDispatch(Activity activity) {
        mNfcAdapter.enableForegroundDispatch(activity, mPendingIntent, mIntentFilters, mTechList);
    }

    public void disableForegroundDispatch(Activity activity) {
        try {
            mNfcAdapter.disableForegroundDispatch(activity);
        } catch (IllegalStateException e) {
            // Already disabled, ignore
        }
    }

//    public void setReadLogMessage(String message) {
//        mReadLogMessage = message;
//    }

//    public void setWriteLogMessage(String message) {
//        mWriteLogMessage = message;
//    }

    public int readType(Intent intent) throws CuonaReaderException {
        setCuonaReaderTag(intent, true);
        if(mCuonaReaderTag != null) {
            return mCuonaReaderTag.getType();
        } else {
            return Cuona.TAG_TYPE_UNKNOWN;
        }
    }

    public String readDeviceId(Intent intent) throws CuonaReaderException {
        setCuonaReaderTag(intent, true);
        if(mCuonaReaderTag != null) {
            return deviceIdForServer(mCuonaReaderTag.getDeviceIdString());
        } else {
            return null;
        }
    }

    // package-private
    String readJson(Intent intent, boolean sendLog) throws CuonaReaderException {
        setCuonaReaderTag(intent, sendLog);
        if(mCuonaReaderTag != null) {
            return mCuonaReaderTag.getJSONString();
        } else {
            return null;
        }
    }

    // package-private
    boolean writeJson(Context context, Intent intent, String json, String password) {
        CuonaWritableTag cuonaWritableTag = getWritableTagFromIntent(intent);
        if(cuonaWritableTag != null) {
            cuonaWritableTag.setCallback((CuonaWritableTag.Callback) context);
            if(password == null) password = null;
            cuonaWritableTag.writeJSON(json, password, Keys.keyCode, Keys.cuonaKey32B);
            return true;
        } else {
            return false;
        }
    }

    // CUONAタグ読み込みの準備
    private void setCuonaReaderTag(Intent intent, boolean sendLog) throws CuonaReaderException {

        // NFCタッチ時のインテントが変わった場合に更新
        if(mNfcTouchIntent == null || mNfcTouchIntent != intent ) {
            mNfcTouchIntent = intent;

            try {
                mCuonaReaderTag = getReadableTagFromIntent(mNfcTouchIntent);

                // サービスIDが書き込まれているか確認
                if(!Objects.equals(Constants.Urls.SERVICE_KEY, "") && mCuonaReaderTag != null) {
                    try {
                        JSONObject readJson = new JSONObject(mCuonaReaderTag.getJSONString());
                        if (!Objects.equals(readJson.getJSONObject(Constants.Urls.SERVICE_JSON_NAME).getString("id"), Constants.Urls.SERVICE_KEY )) {
                            mCuonaReaderTag = null;
                            return;
                        }
                    } catch (JSONException e) {
                        mCuonaReaderTag = null;
                        return;
                    }
                }

                // ログ送信
                if(sendLog) sendReadLog(mCuonaReaderTag);

            } catch (CuonaReaderException e) {
                e.printStackTrace();
                throw new CuonaReaderException(e);
            }
        }
    }

    // 読み込み時のログを作成
    private void sendReadLog(CuonaReaderTag cuonaReaderTag) {
        if(cuonaReaderTag != null) {
            String logMessage = "Read by Android";
            if(mReadLogMessage != null) logMessage = mReadLogMessage;
            createAndSendLog(cuonaReaderTag, logMessage);
        }
    }

    // 書き込み時のログを作成
    private void sendWriteLog(Tag tag) throws CuonaReaderException {
        if(tag != null) {

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                throw new CuonaReaderException("Not NDEF tag");
            }

            NdefMessage msg;
            try {
                ndef.connect();
                msg = ndef.getNdefMessage();
            } catch (IOException |FormatException e) {
                throw new CuonaReaderException(e);
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            NdefRecord[] records = msg.getRecords();
            for (NdefRecord rec: records) {
                CuonaReaderTag cuonaReaderTag = null;
                CuonaReaderSecureTag stag = CuonaReaderSecureTag.get(rec, Keys.keyCode, Keys.cuonaKey32B);
                if (stag != null) {
                    cuonaReaderTag = stag;
                }
                CuonaReaderLegacyTag ltag = CuonaReaderLegacyTag.get(rec);
                if (ltag != null) {
                    cuonaReaderTag = ltag;
                }

                if(cuonaReaderTag != null) {
                    String logMessage = "Write by Android";
                    if(mWriteLogMessage != null) logMessage = mWriteLogMessage;
                    createAndSendLog(cuonaReaderTag, logMessage);
                }
            }
        }
    }

    // 書き込みと読み込み時のログ作成・送信の共通部分
    private void createAndSendLog(CuonaReaderTag cuonaReaderTag, String message) {

        // 位置情報の取得
        String[] locationInfo = new LocationInfo(mContext).getCurrentLocation();
        String locationLog = "--";
        if(locationInfo != null) {
            locationLog = locationInfo[0] + "," + locationInfo[1];
        }

        // デバイスIDをサーバーで送信可能な形式に変換
        String deviceId = deviceIdForServer(cuonaReaderTag.getDeviceIdString());

        // 現在のログを作成
        String currentLog[] = {deviceId, locationLog, message};

        // 本体に登録されているログを取得（2次元配列）
        final Gson gson = new Gson();
        final SharedPreferences pref = mContext.getSharedPreferences("logs", Context.MODE_PRIVATE);
        String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

        // サーバーに送信するためのログを作成
        int toSendLogLength = 1;
        if (savedLog != null) {
            toSendLogLength += savedLog.length;
        }
        final String toSendLog[][] = new String[toSendLogLength][currentLog.length];
        toSendLog[0] = currentLog;
        if (savedLog != null) {
            System.arraycopy(savedLog, 0, toSendLog, 1, toSendLogLength - 1);
        }

        // ネットに繋がっていればログの送信
        if (isNetworkEnable() || isWifiEnable()) {

            new Cuona(new SendLogCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {

                    // 保存されているログは削除
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("savedLog", null);
                    editor.apply();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d("SendLogFailure", e.toString());

                    // 通信失敗でログを保存
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("savedLog", gson.toJson(toSendLog));
                    editor.apply();
                }
            }).execute(toSendLog);
        }
        // ネットに繋がっていなければログを保存
        else {
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("savedLog", gson.toJson(toSendLog));
            editor.apply();
        }
    }

    private CuonaReaderTag getReadableTagFromIntent(Intent intent) throws CuonaReaderException {
        if (intent.getAction() != null && !intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            return null;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return null;
        }

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new CuonaReaderException("Not NDEF tag");
        }

        NdefMessage msg;
        try {
            ndef.connect();
            msg = ndef.getNdefMessage();
        } catch (IOException |FormatException e) {
            throw new CuonaReaderException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(msg != null) {
            NdefRecord[] records = msg.getRecords();
            for (NdefRecord rec : records) {
                CuonaReaderSecureTag stag = CuonaReaderSecureTag.get(rec, Keys.keyCode, Keys.cuonaKey32B);
                if (stag != null) {
                    return stag;
                }
                CuonaReaderLegacyTag ltag = CuonaReaderLegacyTag.get(rec);
                if (ltag != null) {
                    return ltag;
                }
            }
        }

        throw new CuonaReaderException("Not Cuona tag");
    }

    private CuonaWritableTag getWritableTagFromIntent(Intent intent) {

        if (intent.getAction() != null && intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                MifareUltralight mul = MifareUltralight.get(tag);
                if (mul != null) {

                    // ログ送信
                    try {
                        sendWriteLog(tag);
                    } catch (CuonaReaderException e) {
                        e.printStackTrace();
                    }

                    return new CuonaWritableT2(mul);
                }

                IsoDep isoDep = IsoDep.get(tag);
                if (isoDep != null) {

                    // ログ送信
                    try {
                        sendWriteLog(tag);
                    } catch (CuonaReaderException e) {
                        e.printStackTrace();
                    }

                    return new CuonaBTDevice(mContext, isoDep);
                    //return new CuonaWritableT4(isoDep);
                }
            }
        }
        return null;
    }

    @Override
    protected JSONObject doInBackground(String[][]... params) {

        // 現在時間の作成
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(cal.getTimeZone());
        String currentDateTime = sdf.format(cal.getTime());

        // ログ送信用のjsonを作成
        JSONObject deviceLogsJson = new JSONObject();
        JSONArray deviceLogJsonArray = new JSONArray();
        try {
            for(int i = 0; i < params[0].length; i++) {
                JSONObject jsonOneData;
                jsonOneData = new JSONObject();
                jsonOneData.put("device_id", params[0][i][0]);
                jsonOneData.put("used_at", currentDateTime);
                jsonOneData.put("lat_lng", params[0][i][1]);
                jsonOneData.put("notes", params[0][i][2]);
                deviceLogJsonArray.put(jsonOneData);
            }

            deviceLogsJson.put("device_logs", deviceLogJsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // ログ送信先URL
        final String LOG_URL = Constants.Urls.LOG_URL;

        // サーバーにログを送信
        if(!Objects.equals(LOG_URL, "")) {
            return post(LOG_URL, deviceLogsJson.toString());
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(JSONObject responseJson) {
        super.onPostExecute(responseJson);
        onSuccess(responseJson);
    }

    private void onSuccess(JSONObject responseJson) {
        this.mSendLogCallback.onSuccess(responseJson);
    }

    private void onFailure(Exception e) {
        this.mSendLogCallback.onFailure(e);
    }

    // ログ送信用のPOST
    private JSONObject post(String logUrl, String body) {
        JSONObject responseJson = null;
        try {
            String buffer = "";
            HttpURLConnection con = null;
            URL url = new URL(logUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept-Language", "jp");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("X-USER-OS", "Android");
            con.setRequestProperty("X-USER-OS-VERSION", Build.VERSION.RELEASE);
            con.setRequestProperty("X-USER-MODEL", Build.MODEL);
            OutputStream os = con.getOutputStream();
            PrintStream ps = new PrintStream(os);
            ps.print(body);
            ps.close();

            // レスポンスを取得
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String responseJsonString = reader.readLine();
            responseJson = new JSONObject(responseJsonString);

            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ServerLog", e.toString());
            onFailure(e);
        }

        return responseJson;
    }

    // ネットワーク接続の確認
    private boolean isNetworkEnable() {
        if(mContext != null) {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = null;
            if (cm != null) info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } else {
            return false;
        }
    }

    // Wifi接続の確認
    private boolean isWifiEnable() {
        if(mContext != null) {
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
            return wifiManager != null && wifiManager.isWifiEnabled();
        } else {
            return false;
        }
    }

    // デバイスIDをサーバーに送信可能な形式に変換
    private String deviceIdForServer(String deviceId) {
        if(deviceId != null) {
            String deviceIdTmp = deviceId.replace(" ", "").toLowerCase();
            StringBuilder deviceIdToSend = new StringBuilder(deviceIdTmp);
            for (int i = 0; i < (deviceIdTmp.length() - 2) / 2; i++) {
                deviceIdToSend.insert((deviceIdTmp.length() - 2) - (2 * i), " ");
            }
            return deviceIdToSend.toString();
        } else {
            return null;
        }
    }
}
