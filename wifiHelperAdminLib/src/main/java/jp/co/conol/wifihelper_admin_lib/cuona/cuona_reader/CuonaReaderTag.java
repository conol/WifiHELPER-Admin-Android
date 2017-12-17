package jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader;

import java.io.UnsupportedEncodingException;
import java.util.Formatter;

abstract public class CuonaReaderTag {

    abstract public byte[] getDeviceId();

    abstract public byte[] getJSONData();

    abstract public int getType();

    abstract public int getSecurityStrength();

    public String getDeviceIdString() {
        Formatter fmt = new Formatter();
        for (byte b: getDeviceId()) {
            fmt.format("%02X", b & 0xff);
        }
        return fmt.toString();
    }

    public String getJSONString() {
        try {
            return new String(getJSONData(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

 }
