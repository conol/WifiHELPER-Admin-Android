package jp.co.conol.wifihelper_admin_lib.wifi_helper.model;

/**
 * Created by Masafumi_Ito on 2017/10/10.
 */

public class Wifi {

    private String ssid;
    private String password;
    private int kind;
    private Integer days;

    public Wifi(String ssid, String password, int kind, Integer days) {
        this.ssid = ssid;
        this.password = password;
        this.kind = kind;
        this.days = days;
    }

    public String getSsid() {
        return ssid;
    }

    public String getPassword() {
        return password;
    }

    public int getKind() {
        return kind;
    }

    public Integer getDays() {
        return days;
    }
}
