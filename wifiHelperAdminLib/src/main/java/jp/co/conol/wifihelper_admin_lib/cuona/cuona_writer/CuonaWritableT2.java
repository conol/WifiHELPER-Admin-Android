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

    private void t2prepare() throws IOException {
        Log.i("nfc", "T2 detected");
        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] page0 = mul.readPages(0);
        HexUtils.logd("readPages(0)", page0);
        deviceId = Arrays.copyOf(page0, T2_DEVICE_ID_LENGTH);

        byte[] version = mul.transceive(new byte[] { T2_CMD_GET_VERSION  });
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

        if  (configPage != 0) {
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


    public void writeJSON(String json) throws IOException {
        t2prepare();
        mul.close();

        byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);

        NdefRecord rec = CuonaNDEF.createRecord(deviceId, jsonData);
        NdefMessage msg = new NdefMessage(rec);

        Ndef ndef = Ndef.get(mul.getTag());
        if (ndef == null) {
            Log.e("nfc", "Cannot get Ndef");
            throw new IOException("Cannot get Ndef");
        }

        if (!ndef.isConnected()) {
            ndef.connect();
        }

        if (ndef.isWritable()) {

            try {
                ndef.writeNdefMessage(msg);
            } catch (FormatException e) {
                throw new IOException(e);
            }
            Log.i("nfc", "Tag written!");

        } else {
            Log.e("nfc", "Tag is not writable");
            throw new IOException("Tag is not writable");
        }

    }

    @Override
    public void protect(byte[] newPassword, byte[] oldPassword) throws IOException {

        newPassword = Arrays.copyOf(newPassword, 4);
        oldPassword = Arrays.copyOf(oldPassword, 4);

        t2prepare();

        if (!isNXPNTAG) {
            throw new IOException("Not NXP NTAG, password protection not supported");
        }

        if (isProtected && oldPassword != null) {
            t2auth(oldPassword);
        }

        mul.writePage(configPage + 2, newPassword);
        Log.i("nfc", "password written");
        configPageData[3] = 0; // auth0
        byte[] configPage1 = Arrays.copyOf(configPageData, 4);
        mul.writePage(configPage, configPage1);
        Log.i("nfc", "auth0 written");

        mul.close();
    }

    @Override
    public void unprotect(byte[] password) throws IOException {
        password = Arrays.copyOf(password, 4);

        t2prepare();

        if (!isNXPNTAG) {
            throw new IOException("Not NXP NTAG, password protection not supported");
        }

        if (isProtected && password != null) {
            t2auth(password);
        }

        configPageData[3] = (byte) 0xff; // auth0
        byte[] configPage1 = Arrays.copyOf(configPageData, 4);
        mul.writePage(configPage, configPage1);
        Log.i("nfc", "auth0 written");

        mul.close();
    }


}
