package jp.co.conol.wifihelper_admin_lib.wifi_helper;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;


/**
 * Created by Masafumi_Ito on 2017/10/10.
 */

public class WifiHelper {

    public static String createJson(String ssid, String pass, int kind, Integer days) {
        return "{\"wifi\":{\"ssid\":\"" + ssid + "\",\"pass\":\"" + pass + "\",\"kind\":" + kind + ",\"days\":" + days + "}}";
    }

    public static Wifi parseJsonToObj(String targetJson) throws JSONException {
        JSONObject jsonObject = new JSONObject(targetJson);
        JSONObject wifi = jsonObject.getJSONObject("wifi");

        return new Wifi(
                wifi.getString("ssid"),
                wifi.getString("pass"),
                wifi.getInt("kind"),
                wifi.getInt("days")
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
