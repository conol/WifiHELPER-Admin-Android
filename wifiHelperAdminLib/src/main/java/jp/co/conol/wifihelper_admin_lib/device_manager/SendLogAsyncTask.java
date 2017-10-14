package jp.co.conol.wifihelper_admin_lib.device_manager;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Masafumi_Ito on 2017/10/13.
 */

public class SendLogAsyncTask extends AsyncTask<String[][], Void, JSONObject> {

    private AsyncCallback mAsyncCallback = null;

    public interface AsyncCallback{
        void onSuccess(JSONObject responseJson);
        void onFailure(Exception e);
    }

    public SendLogAsyncTask(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

    @Override
    protected JSONObject doInBackground(String[][]... params) {

        // ログ送信用のjsonを作成
        JSONObject deviceLogsJson = new JSONObject();
        JSONArray deviceLogJsonArray = new JSONArray();
        try {
            for(int i = 0; i < params[0].length; i++) {
                JSONObject jsonOneData;
                jsonOneData = new JSONObject();
                jsonOneData.put("device_id", params[0][i][0]);
                jsonOneData.put("used_at", params[0][i][1]);
                jsonOneData.put("lat_lng", params[0][i][2]);
                jsonOneData.put("notes", params[0][i][3]);
                jsonOneData.put("shop", null);  // TODO 後に削除
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
