package jp.co.conol.wifihelper_admin_lib.DeviceManager.model;

/**
 * Created by m_ito on 2017/11/10.
 */

public class Owner {

    private String app_token;
    private String device_password = "0 0 0 0";
    private int id;
    private String name;
    private String name_kana;
    private String staff;
    private String staff_kana;
    private String email;
    private String zip_code;
    private String address;
    private String phone_number;
    private String fax_number;
    private String notes;
    private String start_on;
    private int num_devices = 0;
    private int num_dev_devices = 0;
    private String payment_method;
    private int payment_period;
    private boolean activated;
    private String created_at;
    private String updated_at;
    private Service[] services;

    public String getAppToken() {
        return app_token;
    }

    public String getDevicePassword() {
        return device_password;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameKana() {
        return name_kana;
    }

    public String getStaff() {
        return staff;
    }

    public String getStaffKana() {
        return staff_kana;
    }

    public String getEmail() {
        return email;
    }

    public String getZipCode() {
        return zip_code;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public String getFaxNumber() {
        return fax_number;
    }

    public String getNotes() {
        return notes;
    }

    public String getStartOn() {
        return start_on;
    }

    public int getNumDevices() {
        return num_devices;
    }

    public int getNumDevDevices() {
        return num_dev_devices;
    }

    public String getPaymentMethod() {
        return payment_method;
    }

    public int getPaymentPeriod() {
        return payment_period;
    }

    public boolean isActivated() {
        return activated;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public String getUpdatedAt() {
        return updated_at;
    }

    public Service[] getServices() {
        return services;
    }
}
