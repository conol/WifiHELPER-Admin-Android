package jp.co.conol.wifihelper_admin_lib.device_manager;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Masafumi_Ito on 2017/10/12.
 */

public class GetDevicesAsyncTask extends AsyncTask<Void, Void, List<String>> {

    private AsyncCallback mAsyncCallback = null;

    public interface AsyncCallback{
        void onSuccess(List<String> deviceIdList);
        void onFailure(Exception e);
    }

    public GetDevicesAsyncTask(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

    protected List<String> doInBackground(Void... params){

        String jsonString = httpGet("http://13.112.232.171/api/services/H7Pa7pQaVxxG.json");
        List<String> deviceIdList = new ArrayList<>();

        if(jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jRec = jsonArray.getJSONObject(i);
                    String deviceId = jRec.getString("device_id").replace(" ", "").toLowerCase();
                    deviceIdList.add(deviceId);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return deviceIdList;
    }

    @Override
    protected void onPostExecute(List<String> deviceIdList) {
        super.onPostExecute(deviceIdList);
        onSuccess(deviceIdList);
    }

    private void onSuccess(List<String> deviceIdList) {
        this.mAsyncCallback.onSuccess(deviceIdList);
    }

    private void onFailure(Exception e) {
        this.mAsyncCallback.onFailure(e);
    }

    private String httpGet(String urls){

        HttpURLConnection urlCon;
        InputStream in;

        try {
            urlCon = (HttpURLConnection) new URL(urls).openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.setDoInput(true);
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
            onFailure(e);
            return null;
        }
    }
}
