package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;

import android.util.Log;

public class HexUtils {

    public static void logd(String msg, byte[] array) {
        StringBuilder sb =  new StringBuilder();
        sb.append(msg);
        sb.append(":");
        for (byte b: array) {
            String s = "0" + Integer.toHexString(((int) b) & 0xff);
            sb.append(" ");
            sb.append(s.substring(s.length() - 2));
        }
        Log.d("nfc", sb.toString());
    }

}
