package jp.co.conol.wifihelper_admin_lib.corona;

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
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CoronaReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCT2WriterTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CoronaWriterTag;
import jp.co.conol.wifihelper_admin_lib.device_manager.GetLocation;
import jp.co.conol.wifihelper_admin_lib.device_manager.SendLogAsyncTask;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.WifiHelper;


/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class Corona {

    private Context context;
    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] intentFilters;
    private final String[][] techList;
    private String mReadLogMessage = "read!";
    private String mWriteLogMessage = "write!";
    public static final int TAG_TYPE_UNKNOWN = 0;
    public static final int TAG_TYPE_CORONA = 1;
    public static final int TAG_TYPE_SEAL = 2;

    public Corona(Context context) throws NfcNotAvailableException {
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
        actionNdef.addDataPath("/conol.co.jp:cnfc_bt_manu_data", PatternMatcher.PATTERN_LITERAL);

        intentFilters = new IntentFilter[]{
                actionNdef
        };

        techList = new String[][]{
                new String[]{NfcA.class.getName(), IsoDep.class.getName()},
                new String[]{NfcA.class.getName(), MifareUltralight.class.getName()}
        };

        // 本体に登録されているログを取得（2次元配列）
        Gson gson = new Gson();
        final SharedPreferences pref = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
        String savedLog[][] = gson.fromJson(pref.getString("savedLog", null), String[][].class);

        // ネットに繋がっていればログの送信
        if(savedLog != null && (Util.Network.isConnected(context) || WifiHelper.isEnable(context))) {
            new SendLogAsyncTask(new SendLogAsyncTask.AsyncCallback() {
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

    public int readType(Intent intent) throws CoronaException {
        CoronaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, false);
        } catch (CoronaException e) {
            e.printStackTrace();
            throw new CoronaException(e);
        }
        if(tag != null) {
            return tag.getType();
        } else {
            return Corona.TAG_TYPE_UNKNOWN;
        }
    }

    public String readDeviceId(Intent intent) throws CoronaException {
        CoronaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, false);
        } catch (CoronaException e) {
            e.printStackTrace();
            throw new CoronaException(e);
        }
        if(tag != null) {
            return tag.getDeviceIdString();
        } else {
            return null;
        }
    }

    public String readJson(Intent intent) throws CoronaException {
        CoronaReaderTag tag;
        try {
            tag = getReadTagFromIntent(intent, true);
        } catch (CoronaException e) {
            e.printStackTrace();
            throw new CoronaException(e);
        }
        if(tag != null) {
            return tag.getJsonString();
        } else {
            return null;
        }
    }

    public void writeJson(Intent intent, String json) throws CoronaException {
        CoronaWriterTag tag;
        try {
            tag = getWriteTagFromIntent(intent);
            if(tag != null) tag.writeJson(json);
        } catch (CoronaException | IOException e) {
            e.printStackTrace();
            throw new CoronaException(e);
        }
    }

    public void setReadLogMessage(String message) {
        mReadLogMessage = message;
    }

    public void setWriteLogMessage(String message) {
        mWriteLogMessage = message;
    }

    private CoronaWriterTag getWriteTagFromIntent(Intent intent) throws CoronaException {
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
                /*
                IsoDep isoDep = IsoDep.get(tag);
                if (isoDep != null) {
                    return new CNFCT4WriterTag(isoDep);
                }
                */
                MifareUltralight mul = MifareUltralight.get(tag);
                if (mul != null) {

                    // 以下からログ送信に必要

                    Ndef ndef = Ndef.get(tag);
                    if (ndef == null) {
                        throw new CoronaException("Not NDEF tag");
                    }

                    NdefMessage msg;
                    try {
                        ndef.connect();
                        msg = ndef.getNdefMessage();
                    } catch (IOException | FormatException e) {
                        throw new CoronaException(e);
                    } finally {
                        try {
                            ndef.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(msg == null) throw new CoronaException("Can not available WifiHelper!!");

                    NdefRecord[] records = msg.getRecords();
                    for (NdefRecord rec : records) {
                        CoronaReaderTag t = CoronaReaderTag.get(rec);
                        if (t != null) {

                            // 現在時間の作成
                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                            sdf.setTimeZone(cal.getTimeZone());
                            String currentDateTime = sdf.format(cal.getTime());

                            // 位置情報の取得
                            GetLocation location = new GetLocation(context);
                            String locationInfo = "";
                            if(location.getCurrentLocation() != null) {
                                locationInfo = String.valueOf(location.getCurrentLocation().getLatitude()) + "," + String.valueOf(location.getCurrentLocation().getLongitude());
                            }

                            // デバイスIDに空白を入れる
                            // TODO 16進数でなくなるとこの部分削除
                            String str = t.getDeviceIdString();
                            StringBuilder sb = new StringBuilder(str);
                            sb.insert(12," ");
                            sb.insert(10," ");
                            sb.insert(8," ");
                            sb.insert(6," ");
                            sb.insert(4," ");
                            sb.insert(2," ");

                            Gson gson = new Gson();

                            // 現在のログを作成
                            String currentLog[] = {sb.toString().toLowerCase(), currentDateTime, locationInfo, mWriteLogMessage};

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
                                new SendLogAsyncTask(new SendLogAsyncTask.AsyncCallback() {
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
                    }

                    return new CNFCT2WriterTag(mul);
                }
            }
        }
        return null;
    }

    private CoronaReaderTag getReadTagFromIntent(Intent intent, boolean sendLog) throws CoronaException {
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
            throw new CoronaException("Not NDEF tag");
        }

        NdefMessage msg;
        try {
            ndef.connect();
            msg = ndef.getNdefMessage();
        } catch (IOException | FormatException e) {
            Log.d("CoronaException", e.toString());
            throw new CoronaException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(msg == null) throw new CoronaException("Can not available WifiHelper!!");

        NdefRecord[] records = msg.getRecords();
        for (NdefRecord rec : records) {
            CoronaReaderTag t = CoronaReaderTag.get(rec);
            if (t != null) {

                if (sendLog) {

                    // 現在時間の作成
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    sdf.setTimeZone(cal.getTimeZone());
                    String currentDateTime = sdf.format(cal.getTime());

                    // 位置情報の取得
                    GetLocation location = new GetLocation(context);
                    String locationInfo = "";
                    if (location.getCurrentLocation() != null) {
                        locationInfo = String.valueOf(location.getCurrentLocation().getLatitude()) + "," + String.valueOf(location.getCurrentLocation().getLongitude());
                    }

                    // デバイスIDに空白を入れる
                    // TODO 16進数でなくなるとこの部分削除
                    String str = t.getDeviceIdString();
                    StringBuilder sb = new StringBuilder(str);
                    sb.insert(12, " ");
                    sb.insert(10, " ");
                    sb.insert(8, " ");
                    sb.insert(6, " ");
                    sb.insert(4, " ");
                    sb.insert(2, " ");

                    // 現在のログを作成
                    String currentLog[] = {sb.toString().toLowerCase(), currentDateTime, locationInfo, mReadLogMessage};

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
                        new SendLogAsyncTask(new SendLogAsyncTask.AsyncCallback() {
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

                return t;
            }
        }

        throw new CoronaException("Not Corona tag");
    }
}
