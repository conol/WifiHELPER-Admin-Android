package jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by m_ito on 2018/02/04.
 */

class CuonaMasterKey {

    private static final byte[] seed = {
            (byte) 0x0B, (byte) 0xF2, (byte) 0x44, (byte) 0xBC, (byte) 0x5B,
            (byte) 0xC2, (byte) 0xBA, (byte) 0xF1, (byte) 0xFE, (byte) 0x2B,
            (byte) 0x2D, (byte) 0xDC, (byte) 0x4B, (byte) 0x73, (byte) 0xF0,
            (byte) 0x18, (byte) 0x95, (byte) 0x23, (byte) 0x13, (byte) 0x3A,
            (byte) 0x40, (byte) 0x43, (byte) 0x7B, (byte) 0xBF, (byte) 0xBD,
            (byte) 0x6D, (byte) 0xD4, (byte) 0xDF, (byte) 0xAA, (byte) 0x19,
            (byte) 0x4B, (byte) 0x32,
    };

    public static byte[] getKey(int keyCode) {
        if (keyCode < 0 || keyCode >= 65536) {
            throw new IllegalArgumentException("bad customerId");
        }

        byte kcHigh = (byte) (keyCode >> 8);
        byte kcLow = (byte) keyCode;

        byte[] data = Arrays.copyOf(seed, seed.length);
        data[24] = kcHigh;
        data[28] = kcLow;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
        md.reset();
        return md.digest(data);
    }

}

