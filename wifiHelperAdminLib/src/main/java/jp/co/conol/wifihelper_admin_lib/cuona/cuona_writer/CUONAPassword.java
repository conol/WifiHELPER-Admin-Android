package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import java.util.Arrays;

class CUONAPassword {

    private final byte[] pwData;

    CUONAPassword(String str) {
        String[] s = str.trim().split("\\s+");
        pwData = new byte[s.length];
        for (int i = 0; i < s.length; i++) {
            int n = Integer.parseInt(s[i]);
            if (n < 0 || n > 255) {
                throw new NumberFormatException("Numbers must be in range 0..255");
            }
            pwData[i] = (byte) n;
        }
    }

    byte[] getPasswordArray(int length) {
        return Arrays.copyOf(pwData, length);
    }

}
