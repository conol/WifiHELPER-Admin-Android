package jp.co.conol.wifihelper_admin_lib.DeviceManager.model;


import jp.co.conol.wifihelper_admin_lib.Util;

/**
 * Created by m_ito on 2017/11/10.
 */

public class Device {

    private int id;
    private String device_id;
    private String name;
    private String device_type;
    private String status;
    private boolean is_development;
    private Service[] services;
    private String created_at;
    private String updated_at;

    public int getId() {
        return id;
    }

    public String getDeviceId() {
        return Util.Transform.deviceIdForServer(device_id);
    }

    public String getName() {
        return name;
    }

    public String getDeviceType() {
        return device_type;
    }

    public String getStatus() {
        return status;
    }

    public boolean isIsDevelopment() {
        return is_development;
    }

    public Service[] getServices() {
        return services;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public String getUpdatedAt() {
        return updated_at;
    }
}
