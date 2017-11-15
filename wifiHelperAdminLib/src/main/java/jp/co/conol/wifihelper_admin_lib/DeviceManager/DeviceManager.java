package jp.co.conol.wifihelper_admin_lib.DeviceManager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.Device;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.Owner;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.Pairing;
import jp.co.conol.wifihelper_admin_lib.DeviceManager.model.SignIn;
import jp.co.conol.wifihelper_admin_lib.Util;

/**
 * Created by m_ito on 2017/11/10.
 */

public class DeviceManager extends AsyncTask<DeviceManager.Task, Void, Object> {

    private AsyncCallback mAsyncCallback = null;
    private Context mContext = null;
    private String mAppToken = null;
    private int mServiceId = 0;
    private String mDeviceId = null;
    private SignIn mSignIn = null;
    private Pairing mPairing = null;

    public enum Task {
        SignIn,                 // サインイン
        GetOwner,               // オーナー情報取得
        GetDevicesForOwner,     // オーナーが利用可能なデバイス一覧取得
        GetDevicesForService,   // サービスに紐づくデバイス一覧取得
        Pairing,                // デバイスとサービスの紐付け
        Release,                // デバイスとサービスの紐付け解除
    }

    public interface AsyncCallback {
        void onSuccess(Object obj);
        void onFailure(Exception e);
    }

    public DeviceManager(AsyncCallback asyncCallback){
        this.mAsyncCallback = asyncCallback;
    }

    // オーナー情報が保存されているか否かを返す関数
    public static boolean hasToken(Context context) {
        // オーナー情報の取得
        Gson gson = new Gson();
        Owner owner = gson.fromJson(Util.SharedPref.get(context, "owner"), Owner.class);

        return owner != null;
    }

    public DeviceManager setContext(Context context) {
        mContext = context;
        return this;
    }

    public DeviceManager setServiceId(int serviceId) {
        mServiceId = serviceId;
        return this;
    }

    public DeviceManager setAppToken(String appToken) {
        mAppToken = appToken;
        return this;
    }

    public DeviceManager setDeviceId(String deviceId) {
        mDeviceId = Util.Transform.deviceIdForServer(deviceId); // サーバーで送信可能な形式に変換
        return this;
    }

    public DeviceManager setSingIn(SignIn singIn) {
        mSignIn = singIn;
        return this;
    }

    public DeviceManager setPairing(Pairing pairing) {
        mPairing = pairing;
        return this;
    }

    @Override
    protected Object doInBackground(Task... params) {

        Gson gson = new Gson();
        String endPoint = "http://13.112.232.171";

        // サーバーに送信用するjsonをURLを作成
        String apiUrl = null;
        String requestJsonString = null;
        JSONObject json = null;
        String responseJsonString = null;
        Type type = null;

        try {
            switch (params[0]) {

                // サインイン
                case SignIn:
                    apiUrl = "/api/owners/sign_in.json";
                    requestJsonString = gson.toJson(mSignIn);
                    responseJsonString = Util.Http.post(endPoint + apiUrl, null, requestJsonString);
//                    type = new TypeToken<Owner>(){}.getType();
                    type = JSONObject.class;

                    // オーナー情報を端末に保存
                    Util.SharedPref.save(mContext, "owner", gson.toJson((Owner)gson.fromJson(responseJsonString, type)));
                    break;

                // オーナー情報取得
                case GetOwner:
                    apiUrl = "/api/owners/owner.json";
                    responseJsonString = Util.Http.get(endPoint + apiUrl, mAppToken);
                    type = new TypeToken<Owner>(){}.getType();
                    break;

                // オーナーが利用可能なデバイス一覧取得
                case GetDevicesForOwner:
                    // オーナー情報の取得
                    Owner owner = gson.fromJson(Util.SharedPref.get(mContext, "owner"), Owner.class);

                    apiUrl = "/api/owners/devices.json";
                    responseJsonString = Util.Http.get(endPoint + apiUrl, owner.getAppToken());
                    type = new TypeToken<ArrayList<Device>>(){}.getType();
                    break;

//                // サービスに紐づくデバイス一覧取得
//                case GetDevicesForService:
//
//                    String serviceKey = "";
//                    switch (mServiceId) {
//
//                        // Favor
//                        case 1:
//                            serviceKey = "UXbfYJ6SXm8G";
//                            break;
//
//                        // Wi-Fi HELPER
//                        case 2:
//                            serviceKey = "H7Pa7pQaVxxG";
//                            break;
//
//                        // Rounds
//                        case 3:
//                            serviceKey = "yhNuCERUMM58";
//                            break;
//
//                        default:
//                            break;
//                    }
//
//                    apiUrl = "/api/services/" + serviceKey + ".json";
//                    responseJsonString = Util.Http.get(endPoint + apiUrl, null);
//                    type = new TypeToken<ArrayList<Device>>(){}.getType();
//                    break;

                // デバイスとサービスの紐付け
                case Pairing:
                    apiUrl = "/api/owners/devices/pairing.json";
                    requestJsonString = gson.toJson(mPairing);
                    responseJsonString = Util.Http.put(endPoint + apiUrl, mAppToken, requestJsonString);
                    type = new TypeToken<ArrayList<Device>>(){}.getType();
                    break;

                // デバイスとサービスの紐付け解除
                case Release:
                    apiUrl = "/api/owners/devices/" + mDeviceId.replace(" ", "%20") + "/release.json";
                    responseJsonString = Util.Http.patch(endPoint + apiUrl, mAppToken, null);
                    type = new TypeToken<ArrayList<Device>>(){}.getType();
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
}
