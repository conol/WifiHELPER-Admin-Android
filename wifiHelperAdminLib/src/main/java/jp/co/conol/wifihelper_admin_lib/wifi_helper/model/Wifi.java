package jp.co.conol.wifihelper_admin_lib.wifi_helper.model;

/**
 * Created by Masafumi_Ito on 2017/10/10.
 */

public class Wifi {

    private String ssid;
    private String pass;
    private int kind;
    private Integer days;

    public Wifi(String ssid, String pass, int kind, int days) {
        this.ssid = ssid;
        this.pass = pass;
        this.kind = kind;
        this.days = days;
    }

    public String getSsid() {
        return ssid;
    }

    public String getPass() {
        return pass;
    }

    public int getKind() {
        return kind;
    }

    public Integer getDays() {
        return days;
    }
}
