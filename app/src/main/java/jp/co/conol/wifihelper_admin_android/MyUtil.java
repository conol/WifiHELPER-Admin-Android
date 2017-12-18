package jp.co.conol.wifihelper_admin_android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/09/28.
 */

@SuppressWarnings("ALL")
public class MyUtil {

    public static class Str {

        /**
         * 文字列が、空白のみ、null、"" ならtrue それ以外は false
         * @param string
         * @param boolean
         */
        public static boolean isBlank(String string) {
            if(string == null) return true;
            String stringTmp = string.replace(" ", "");

            int stringTmpLength = stringTmp.length();

            if(stringTmpLength == 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class SharedPref {

        /**
         * SharedPreferenceに文字列を保存
         * @param context
         * @param key 取り出すためのKey
         * @param string 保存するstring
         */
        public static void saveString(Context context, String key, String string) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putString(key ,string).apply();
        }

        /**
         * SharedPreferenceからjsonを取得
         * @param context
         * @param key 取り出すためのKey
         */
        public static String getString(Context context, String key) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            return pref.getString(key, null);
        }

        /**
         * SharedPreferenceに文字列を保存
         * @param context
         * @param key 取り出すためのKey
         * @param value 保存するint
         */
        public static void saveInt(Context context, String key, int value) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putInt(key ,value).apply();
        }

        /**
         * SharedPreferenceからjsonを取得
         * @param context
         * @param key 取り出すためのKey
         * @param defaultValue 取り出し失敗時の値
         */
        public static int getInt(Context context, String key, int defaultValue) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            return pref.getInt(key, defaultValue);
        }

        /**
         * SharedPreferenceに真偽値を保存
         * @param context
         * @param key 取り出すためのKey
         * @param bool 保存する真偽値
         */
        public static void saveBoolean(Context context, String key, boolean bool) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putBoolean(key ,bool).apply();
        }

        /**
         * SharedPreferenceからjsonを取得
         * @param context
         * @param key 取り出すためのKey
         * @param defaultValue 取り出し失敗時の値
         */
        public static boolean getBoolean(Context context, String key, boolean defaultValue) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            return pref.getBoolean(key, defaultValue);
        }
    }

    public static class Network {

        /**
         * ネットワークに接続されているかをチェック
         * @param context
         * @return 接続：true / 未接続：false
         */
        public static boolean isEnable(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();

            return info != null && info.isConnected();
        }
    }

    public static class Wifi {

        /**
         * Wifiに接続されているかをチェック
         * @param context
         * @return 接続：true / 未接続：false
         */
        public static boolean isEnable(Context context) {
            WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
            return wifiManager.isWifiEnabled();
        }
    }

    public static class Http {

        public static String get(String url, String appToken) {
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

        public static String post(String url, String appToken, String body) {
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

        public static String patch(String url, String appToken, String body) {
            String responseJsonString = null;
            try {
                String buffer = "";
                HttpURLConnection con = null;
                URL urlTmp = new URL(url);
                con = (HttpURLConnection) urlTmp.openConnection();
                con.setRequestMethod("POST");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "jp");
                con.setRequestProperty("X-HTTP-Method", "PATCH"); // Microsoft
                con.setRequestProperty("X-HTTP-Method-Override", "PATCH");  // Google/GData
                con.setRequestProperty("X-Method-Override", "PATCH");
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

        public static String put(String url, String appToken, String body) {
            String responseJsonString = null;
            try {
                String buffer = "";
                HttpURLConnection con = null;
                URL urlTmp = new URL(url);
                con = (HttpURLConnection) urlTmp.openConnection();
                con.setRequestMethod("PUT");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "jp");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
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

        public static String delete(String url, String appToken) {
            String responseJsonString = null;
            try {
                String buffer = "";
                HttpURLConnection con = null;
                URL urlTmp = new URL(url);
                con = (HttpURLConnection) urlTmp.openConnection();
                con.setRequestMethod("DELETE");
                con.setInstanceFollowRedirects(false);
                con.setRequestProperty("Accept-Language", "jp");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if(appToken != null && !Objects.equals(appToken, "")) con.setRequestProperty("Authorization", "Bearer " + appToken);
                con.getResponseCode();

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


    }

    public static class Transform {

        // デバイスIDをサーバーに送信可能な形式に変換
        public static String deviceIdForServer(String deviceId) {
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

        /**
         * Date型 -> Calendar型
         * @param date
         * @return calendar
         */
        public static Calendar dateToCalender(Date date){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        }

        /**
         * Calendar型 -> Date型
         * @param calendar
         * @return date
         */
        public static Date calendarTodate(Calendar calendar){
            Date date = calendar.getTime();
            return date;
        }
    }

    public static class Aes {

        /**
         * AES128暗号化
         * @param key 鍵（16文字の文字列）
         * @param target 暗号化したい文字列
         * @return 暗号化した文字列
         */
        public static String encrypt(String key, String target) throws Exception{
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sKey);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(target.getBytes());
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        }

        /**
         * AES128復号
         * @param key 鍵（16文字の文字列）
         * @param target 復号したい文字列
         * @return 復号した文字列
         */
        public static String decrypt(String key, String encryptedStr) throws Exception {
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(), "AES");
            byte[] encrypted = Base64.decode(encryptedStr, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sKey);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        }
    }

    public static class App {

        /**
         * Return Statusbar size.
         * @param activity Activity
         * @return Statusbar size
         */
        public static int getStatusBarHeight(Activity activity){
            int height;
            Resources myResources = activity.getResources();
            int idStatusBarHeight = myResources.getIdentifier("status_bar_height", "dimen", "android");
            if (idStatusBarHeight > 0) {
                height = activity.getResources().getDimensionPixelSize(idStatusBarHeight);
            }else{
                height = 0;
            }
            return height;
        }

        /**
         * 背景のタッチを禁止
         * @param view 前面のView
         * @param state ture: タッチできる false:タッチできない
         */
        public static void enableTouchBackground(View view, final boolean state) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return !state;
                }
            });
        }
    }
}
