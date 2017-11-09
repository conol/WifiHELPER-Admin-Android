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

    public CuonaWritableT2(MifareUltralight mul) {
        this.mul = mul;
    }

    public void writeJSON(String json) throws IOException {
        Log.i("nfc", "T2 detected");

        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] page0 = mul.readPages(0);
        HexUtils.logd("readPages(0)", page0);
        byte[] deviceId = Arrays.copyOf(page0, T2_DEVICE_ID_LENGTH);

        mul.close();

        byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);

        NdefRecord rec = CuonaNDEF.createRecord(deviceId, jsonData);
        NdefMessage msg = new NdefMessage(rec);

        Ndef ndef = Ndef.get(mul.getTag());
        if (ndef == null) {
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
            throw new IOException("Tag is not writable");
        }

    }

    void t2_protect_work(byte[] page0) throws IOException {
        ///////////////////
        byte[] version = mul.transceive(new byte[] { T2_CMD_GET_VERSION  });
        HexUtils.logd("T2 Version", version);
        ///////////////////

        int tagSize = page0[14] & 0xff;
        Log.d("nfc", "Size=" + tagSize);
        int configPage = 0;
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
            byte[] config = mul.readPages(configPage);
            HexUtils.logd("config", config);
        }

        byte[] pack = mul.transceive(new byte[] { T2_CMD_PWD_AUTH,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff });
        //byte[] pack = mul.transceive(new byte[] { T2_CMD_PWD_AUTH, 0, 0, 0, 0 });
        HexUtils.logd("pack", pack);

    }


}
