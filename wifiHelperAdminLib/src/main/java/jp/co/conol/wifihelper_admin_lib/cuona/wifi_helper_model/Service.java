package jp.co.conol.wifihelper_admin_lib.cuona.wifi_helper_model;

/**
 * Created by m_ito on 2017/11/10.
 */

public class Service {

    private int id = 0;
    private int service_id = 0;
    private String service_key = null;
    private String name = null;
    private int num_devices = 0;
    private boolean is_using_sdk = false;
    private String start_on = null;
    private int contract_period = 0;
    private String created_at = null;
    private String updated_at = null;

    public int getId() {
        return id;
    }

    public int getServiceId() {
        return service_id;
    }

    public String getServiceKey() {
        return service_key;
    }

    public String getName() {
        return name;
    }

    public int getNumDevices() {
        return num_devices;
    }

    public boolean isIsUsingSdk() {
        return is_using_sdk;
    }

    public String getStart_on() {
        return start_on;
    }

    public int getContractPeriod() {
        return contract_period;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public String getUpdatedAt() {
        return updated_at;
    }
}
