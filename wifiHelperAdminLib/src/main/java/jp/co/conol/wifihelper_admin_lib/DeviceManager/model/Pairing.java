package jp.co.conol.wifihelper_admin_lib.DeviceManager.model;


import jp.co.conol.wifihelper_admin_lib.Util;

/**
 * Created by m_ito on 2017/11/10.
 */

public class Pairing {

    private int service_id;
    private String[] device_ids;

    public Pairing(int serviceId, String[] deviceIds) {

        // デバイスIDをサーバーで使用する形式に変更
        for(int i = 0; i < deviceIds.length; i++) {
            deviceIds[i] = Util.Transform.deviceIdForServer(deviceIds[i]);
        }

        this.service_id = serviceId;
        this.device_ids = deviceIds;
    }
}
