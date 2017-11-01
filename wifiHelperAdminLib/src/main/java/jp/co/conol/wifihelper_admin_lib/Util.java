package jp.co.conol.wifihelper_admin_lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/09/28.
 */

@SuppressWarnings("ALL")
public class Util {

    public static class SharedPref {

        /**
         * SharedPreferenceにjsonを保存
         * @param context
         * @param key 取り出すためのKey
         * @param jsonString 保存するjson
         */
        public static void save(Context context, String key, String jsonString) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putString(key ,jsonString).apply();
        }

        /**
         * SharedPreferenceからjsonを取得
         * @param context
         * @param key 取り出すためのKey
         */
        public static String get(Context context, String key) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            return pref.getString(key, null);
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
                if(appToken != null || appToken != "") con.setRequestProperty("Authorization", "Bearer " + appToken);
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
                if(appToken != null || appToken != "") con.setRequestProperty("Authorization", "Bearer " + appToken);
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
                if(appToken != null || appToken != "") con.setRequestProperty("Authorization", "Bearer " + appToken);
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
                if(appToken != null || appToken != "") con.setRequestProperty("Authorization", "Bearer " + appToken);
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
            String deviceIdTmp = deviceId.replace(" ", "").toLowerCase();
            StringBuilder deviceIdToSend = new StringBuilder(deviceIdTmp);
            for (int i = 0; i < 6; i++) {
                deviceIdToSend.insert((deviceIdToSend.length() - 2) - (2 * i), " ");
            }
            return deviceIdToSend.toString();
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
}
