package jp.co.conol.wifihelper_admin_lib.cuona;

/**
 * Created by Masafumi_Ito on 2017/10/14.
 */


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

class LocationInfo implements LocationListener {

    private Context mContext;
    private LocationManager mLocationManager;

    LocationInfo(Context context) {
        this.mContext = context;
    }

    String[] getCurrentLocation() {
        Location location = null;

        // LocationManagerを取得
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        // Accuracy
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // PowerRequirement
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        // ロケーションプロバイダ
        String provider = mLocationManager.getBestProvider(criteria, true);

        // 位置情報の通知するための最小時間間隔（ミリ秒）
        final long minTime = 0;

        // 位置情報を通知するための最小距離間隔（メートル）
        final long minDistance = 1;

        // LocationListenerの登録
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(provider, minTime, minDistance, this);
            location = mLocationManager.getLastKnownLocation(provider);
            if(location == null){
                mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if(location == null){
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        // 現在地の配列 ["緯度", "軽度"]
        String[] currentLocation = null;
        if(location != null) {
            currentLocation = new String[]{String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())};
        }

        return currentLocation;
    }

    void stopGetLocation() {
        mLocationManager.removeUpdates((LocationListener) this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location",""+location.getLatitude()+ ":"+ location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
}
