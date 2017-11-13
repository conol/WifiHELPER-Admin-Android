package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CuonaWritableT2 extends CuonaWritableTag {

    private static final int T2_DEVICE_ID_LENGTH = 9;

    private static final byte T2_CMD_GET_VERSION = 0x60;
    private static final byte T2_CMD_PWD_AUTH = 0x1b;

    private MifareUltralight mul;

    private byte[] deviceId;
    private boolean isNXPNTAG;
    private int configPage;
    private byte[] configPageData;
    private boolean isProtected;

    public CuonaWritableT2(MifareUltralight mul) {
        this.mul = mul;
    }

    private byte[] createNdefTLV(NdefMessage msg) {
        byte[] msgData = msg.toByteArray();
        byte[] tlv;
        int len = msgData.length;
        int p;
        if (msgData.length > 254) {
            tlv = new byte[len + 4 + 1];
            tlv[0] = 3;
            tlv[1] = (byte) 0xff;
            tlv[2] = (byte) (len >> 8);
            tlv[3] = (byte) len;
            p = 4;
        } else {
            tlv = new byte[len + 2 + 1];
            tlv[0] = 3;
            tlv[1] = (byte) len;
            p = 2;
        }
        System.arraycopy(msgData, 0, tlv, p, len);
        tlv[p + len] = (byte) 0xfe; // end mark
        return tlv;
    }

    private void t2prepare() throws IOException {
        Log.i("nfc", "T2 detected");
        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] page0 = mul.readPages(0);
        HexUtils.logd("readPages(0)", page0);
        deviceId = Arrays.copyOf(page0, T2_DEVICE_ID_LENGTH);

        byte[] version = mul.transceive(new byte[]{T2_CMD_GET_VERSION});
        HexUtils.logd("T2 Version", version);

        if (version[1] != 4 || version[2] != 4) {
            Log.e("nfc", "Not NXP NTAG chip, protection not supported");
            isNXPNTAG = false;
            return;
        }

        isNXPNTAG = true;

        int tagSize = page0[14] & 0xff; // CC2
        Log.d("nfc", "Size=" + tagSize);
        configPage = 0;
        if (tagSize == 0x12) {
            // NTAG213
            configPage = 0x29;
        } else if (tagSize == 0x3e) {
            // NTAG215
            configPage = 0x83;
        } else if (tagSize == 0x6d) {
            // NTAG216
            configPage = 0xe3;
        }

        if (configPage != 0) {
            configPageData = mul.readPages(configPage);
            HexUtils.logd("config", configPageData);

            int auth0 = configPageData[3] & 0xff;
            if (auth0 <= configPage) {
                Log.e("nfc", "Tag is protected");
                isProtected = true;
            } else {
                Log.e("nfc", "Tag is not protected");
                isProtected = false;
            }
        }

    }

    private void t2auth(byte[] password) throws IOException {
        if (password.length != 4) {
            throw new IllegalArgumentException("password must be 4 bytes");
        }
        byte[] cmd = new byte[] { T2_CMD_PWD_AUTH,
                password[0], password[1], password[2], password[3] };

        byte[] pack = mul.transceive(cmd);
        HexUtils.logd("PWD_AUTH: pack", pack);
    }

    @Override
    public void writeJSON(String json, String password) throws IOException {
        byte[] pw = password == null ? null
                : new CUONAPassword(password).getPasswordArray(4);

        t2prepare();

        try {

            if (pw != null) {
                if (!isNXPNTAG) {
                    throw new IOException("Not NXP NTAG, password protection not supported");
                }
                if (isProtected) {
                    t2auth(pw);
                }
                byte[] configPage1 = Arrays.copyOf(configPageData, 4);
                configPage1[3] = (byte) 0xff; // auth0
                mul.writePage(configPage, configPage1);
                Log.i("nfc", "auth0 written, tag is unprotected");
            }

            byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);

            NdefRecord rec = CuonaNDEF.createRecord(deviceId, jsonData);
            NdefMessage msg = new NdefMessage(rec);
            byte[] data = createNdefTLV(msg);

            int page = 4;
            for (int p = 0; p < data.length; p += 4) {
                byte[] block = Arrays.copyOfRange(data, p, p + 4);
                mul.writePage(page, block);
                page++;
            }
            Log.d("nfc", "All pages written");

            if (pw != null) {
                mul.writePage(configPage + 2, pw);
                Log.i("nfc", "password written");
                byte[] configPage1 = Arrays.copyOf(configPageData, 4);
                configPage1[3] = 0; // auth0
                mul.writePage(configPage, configPage1);
                Log.i("nfc", "auth0 written, tag is protected");
            }

        } finally {
            mul.close();
        }

    }
}
