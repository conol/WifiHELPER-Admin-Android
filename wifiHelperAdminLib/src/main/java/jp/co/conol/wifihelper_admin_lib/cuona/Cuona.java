package jp.co.conol.wifihelper_admin_lib.cuona;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PatternMatcher;
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
import java.util.TimeZone;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderLegacyTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderSecureTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader.CuonaReaderTag;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaWritableT2;
import jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer.CuonaWritableTag;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;


/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class Cuona {

    private Context context;
    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] intentFilters;
    private final String[][] techList;
    private String mReadLogMessage = "read!";
    private String mWriteLogMessage = "write!";
    public static final int TAG_TYPE_UNKNOWN = 0;
    public static final int TAG_TYPE_CUONA = 1;
    public static final int TAG_TYPE_SEAL = 2;

    public Cuona(Context context) throws NfcNotAvailableException {
        this.context = context;
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            throw new NfcNotAvailableException();
        }

        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);

        IntentFilter actionNdef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        actionNdef.addDataScheme("vnd.android.nfc");
        actionNdef.addDataPath("/conol.co.jp:corona", PatternMatcher.PATTERN_LITERAL);

        intentFilters = new IntentFilter[]{
                actionNdef
        };

        techList = new String[][]{
                new String[] { NfcA.class.getName(), MifareUltralight.class.getName() }
        };

        // 本体に登録されているログを取得（2次元配列）
        Gson gson = new Gson();
        final SharedPreferences pref = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
        String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

        // ネットに繋がっていればログの送信
        if(savedLog != null && (Util.Network.isConnected(context) || WifiHelper.isEnable(context))) {
            new SendLog(new SendLog.AsyncCallback() {
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

    public boolean isEnable() {
        return nfcAdapter.isEnabled();
    }

    public void enableForegroundDispatch(Activity activity) {
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techList);
    }

    public void disableForegroundDispatch(Activity activity) {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public int readType(Intent intent) throws CuonaException {
        CuonaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, false);
        } catch (CuonaException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
        if(tag != null) {
            return tag.getType();
        } else {
            return Cuona.TAG_TYPE_UNKNOWN;
        }
    }

    public String readDeviceId(Intent intent) throws CuonaException {
        CuonaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, false);
        } catch (CuonaException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
        if(tag != null) {
            return tag.getDeviceIdString();
        } else {
            return null;
        }
    }

    public String readJson(Intent intent) throws CuonaException {
        CuonaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, true);
        } catch (CuonaException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
        if(tag != null) {
            return tag.getJSONString();
        } else {
            return null;
        }
    }

    public void writeJson(Intent intent, String json) throws CuonaException {
        CuonaWritableTag tag;
        try {
            tag = getWriteTagFromIntent(intent);
            if(tag != null) tag.writeJson(json);
        } catch (CuonaException | IOException e) {
            e.printStackTrace();
            throw new CuonaException(e);
        }
    }

    public void setReadLogMessage(String message) {
        mReadLogMessage = message;
    }

    public void setWriteLogMessage(String message) {
        mWriteLogMessage = message;
    }

    private CuonaWritableTag getWriteTagFromIntent(Intent intent) throws CuonaException {

        // 書き込みの暗号化にRSA512を使用するか否か（useShortKey = true で使用）
        boolean useShortKey = false;

        // GPSの許可を確認（ログ送信用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }

        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)
                || intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                MifareUltralight mul = MifareUltralight.get(tag);
                if (mul != null) {

                    // 以下からログ送信に必要

                    Ndef ndef = Ndef.get(tag);
                    if (ndef == null) {
                        throw new CuonaException("Not NDEF tag");
                    }

                    NdefMessage msg;
                    try {
                        ndef.connect();
                        msg = ndef.getNdefMessage();
                    } catch (IOException | FormatException e) {
                        throw new CuonaException(e);
                    } finally {
                        try {
                            ndef.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(msg == null) throw new CuonaException("Can not available WifiHelper!!");

                    NdefRecord[] records = msg.getRecords();
                    for (NdefRecord rec : records) {

                        CuonaReaderTag cuonaReaderTag = null;

                        CuonaReaderSecureTag stag = CuonaReaderSecureTag.get(rec);
                        if (stag != null) {
                            cuonaReaderTag = stag;
                        }
                        CuonaReaderLegacyTag ltag = CuonaReaderLegacyTag.get(rec);
                        if (ltag != null) {
                            cuonaReaderTag = ltag;
                        }

                        if (cuonaReaderTag != null) {

                            // 位置情報の取得
                            GetLocation location = new GetLocation(context);
                            String locationInfo = "";
                            if(location.getCurrentLocation() != null) {
                                locationInfo = String.valueOf(location.getCurrentLocation().getLatitude()) + "," + String.valueOf(location.getCurrentLocation().getLongitude());
                            }

                            // デバイスIDをサーバーで送信可能な形式に変換
                            String deviceId = Util.Transform.deviceIdForServer(cuonaReaderTag.getDeviceIdString());

                            // 現在のログを作成
                            String currentLog[] = {deviceId, locationInfo, mReadLogMessage};

                            Gson gson = new Gson();

                            // 本体に登録されているログを取得（2次元配列）
                            final SharedPreferences pref = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
                            String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

                            // サーバーに送信するためのログを作成
                            int toSendLogLength = 1;
                            if(savedLog != null) {
                                toSendLogLength += savedLog.length;
                            }
                            String toSendLog[][] = new String[toSendLogLength][currentLog.length];
                            toSendLog[0] = currentLog;
                            if(savedLog != null) {
                                System.arraycopy(savedLog, 0, toSendLog, 1, toSendLogLength - 1);
                            }

                            // ネットに繋がっていればログの送信
                            if(Util.Network.isConnected(context) || WifiHelper.isEnable(context)) {
                                new SendLog(new SendLog.AsyncCallback() {
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
                                }).execute(toSendLog);
                            }
                            // ネットに繋がっていなければログを保存
                            else {
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("savedLog", gson.toJson(toSendLog));
                                editor.apply();
                            }

                            // NFCのタイプがシールの場合はRSA512を使用
                            if(cuonaReaderTag.getType() == Cuona.TAG_TYPE_SEAL) useShortKey = true;
                        }
                    }



                    return new CuonaWritableT2(mul, useShortKey);
                }
            }
        }
        return null;
    }

    private CuonaReaderTag getReadTagFromIntent(Intent intent, boolean sendLog) throws CuonaException {
        // GPSの許可がなければwifiに接続できないため、事前に確認（ログ送信にも使用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }

        if (!intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) &&
                !intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            return null;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return null;
        }

        // 以下からログ送信に必要

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new CuonaException("Not NDEF tag");
        }

        NdefMessage msg;
        try {
            ndef.connect();
            msg = ndef.getNdefMessage();
        } catch (IOException | FormatException e) {
            Log.d("CuonaException", e.toString());
            throw new CuonaException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(msg == null) throw new CuonaException("Can not available WifiHelper!!");

        NdefRecord[] records = msg.getRecords();
        for (NdefRecord rec : records) {

            CuonaReaderTag cuonaReaderTag = null;

            CuonaReaderSecureTag stag = CuonaReaderSecureTag.get(rec);
            if (stag != null) {
                cuonaReaderTag = stag;
            }
            CuonaReaderLegacyTag ltag = CuonaReaderLegacyTag.get(rec);
            if (ltag != null) {
                cuonaReaderTag = ltag;
            }

            if (cuonaReaderTag != null) {

                if (sendLog) {

                    // 位置情報の取得
                    GetLocation location = new GetLocation(context);
                    String locationInfo = "";
                    if (location.getCurrentLocation() != null) {
                        locationInfo = String.valueOf(location.getCurrentLocation().getLatitude()) + "," + String.valueOf(location.getCurrentLocation().getLongitude());
                    }

                    // デバイスIDをサーバーで送信可能な形式に変換
                    String deviceId = Util.Transform.deviceIdForServer(cuonaReaderTag.getDeviceIdString());

                    // 現在のログを作成
                    String currentLog[] = {deviceId, locationInfo, mReadLogMessage};

                    // 本体に登録されているログを取得（2次元配列）
                    Gson gson = new Gson();
                    final SharedPreferences pref = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
                    String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

                    // サーバーに送信するためのログを作成
                    int toSendLogLength = 1;
                    if (savedLog != null) {
                        toSendLogLength += savedLog.length;
                    }
                    String toSendLog[][] = new String[toSendLogLength][currentLog.length];
                    toSendLog[0] = currentLog;
                    if (savedLog != null) {
                        System.arraycopy(savedLog, 0, toSendLog, 1, toSendLogLength - 1);
                    }

                    // ネットに繋がっていればログの送信
                    if (Util.Network.isConnected(context) || WifiHelper.isEnable(context)) {
                        new SendLog(new SendLog.AsyncCallback() {
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
                        }).execute(toSendLog);
                    }
                    // ネットに繋がっていなければログを保存
                    else {
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("savedLog", gson.toJson(toSendLog));
                        editor.apply();
                    }
                }

                return cuonaReaderTag;
            }
        }

        throw new CuonaException("Not Cuona tag");
    }

    public static class SendLog extends AsyncTask<String[][], Void, JSONObject> {

        private AsyncCallback mAsyncCallback = null;

        public interface AsyncCallback{
            void onSuccess(JSONObject responseJson);
            void onFailure(Exception e);
        }

        public SendLog(AsyncCallback asyncCallback){
            this.mAsyncCallback = asyncCallback;
        }

        @Override
        protected JSONObject doInBackground(String[][]... params) {

            // 現在時間の作成
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
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

            // サーバーにログを送信
            JSONObject responseJson = null;
            try {
                String buffer = "";
                HttpURLConnection con = null;
                URL url = new URL("http://13.112.232.171/api/device_logs/H7Pa7pQaVxxG.json");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "jp");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream os = con.getOutputStream();
                PrintStream ps = new PrintStream(os);
                ps.print(deviceLogsJson.toString());
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

        @Override
        protected void onPostExecute(JSONObject responseJson) {
            super.onPostExecute(responseJson);
            onSuccess(responseJson);
        }

        private void onSuccess(JSONObject responseJson) {
            this.mAsyncCallback.onSuccess(responseJson);
        }

        private void onFailure(Exception e) {
            this.mAsyncCallback.onFailure(e);
        }
    }
}
