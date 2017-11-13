package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by m_ito on 2017/11/09.
 */

public class CuonaWritableT4 extends CuonaWritableTag {

    private static final String TYPE_LEGACY = "conol.co.jp:cnfc_bt_manu_data";
    private static final byte[] LEGACY_MAGIC = { 0x63, 0x6f, 0x01 };
    private static final int LEGACY_DEVICEID_LENGTH = 7;

    private static final String TYPE_CUONA = "conol.jp:cuona";
    private static final byte[] CUONA_MAGIC = { 0x63, 0x6f, 0x04 };
    private static final int CUONA_DEVICEID_LENGTH_OFFSET = 3;
    private static final int CUONA_DEVICEID_OFFSET = 5;

    private static final int T4_PASSWORD_LENGTH = 128 / 8; // 16

    private IsoDep dep;

    public CuonaWritableT4(IsoDep dep) {
        this.dep = dep;
    }

    private byte[] getDeviceIdLegacy(NdefRecord rec) throws IOException {
        if (rec.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return null;
        }
        if (!new String(rec.getType()).equals(TYPE_LEGACY)) {
            return null;
        }
        byte[] payload = rec.getPayload();
        if (payload.length < LEGACY_MAGIC.length + LEGACY_DEVICEID_LENGTH) {
            return null;
        }
        for (int i = 0; i < LEGACY_MAGIC.length; i++) {
            if (payload[i] != LEGACY_MAGIC[i]) {
                return null;
            }
        }
        return Arrays.copyOfRange(payload, LEGACY_MAGIC.length,
                LEGACY_MAGIC.length + LEGACY_DEVICEID_LENGTH);
    }

    private byte[] getDeviceIdCuona(NdefRecord rec) throws IOException {
        if (rec.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return null;
        }
        if (!new String(rec.getType()).equals(TYPE_CUONA)) {
            return null;
        }
        byte[] payload = rec.getPayload();
        if (payload.length < CUONA_DEVICEID_OFFSET) {
            return null;
        }
        for (int i = 0; i < CUONA_MAGIC.length; i++) {
            if (payload[i] != CUONA_MAGIC[i]) {
                return null;
            }
        }
        int deviceIdLength = payload[CUONA_DEVICEID_LENGTH_OFFSET] & 0xff;
        if (payload.length < CUONA_DEVICEID_OFFSET + deviceIdLength) {
            return null;
        }

        return Arrays.copyOfRange(payload, CUONA_DEVICEID_OFFSET,
                CUONA_DEVICEID_OFFSET + deviceIdLength);
    }

    private byte[] getDeviceId(Ndef ndef) throws IOException {
        NdefMessage msg;
        try {
            msg = ndef.getNdefMessage();
        } catch (FormatException e) {
            throw new IOException(e);
        }

        NdefRecord[] recs = msg.getRecords();
        for (NdefRecord rec: recs) {
            byte[] deviceId = getDeviceIdLegacy(rec);
            if (deviceId != null) {
                return deviceId;
            }
            deviceId = getDeviceIdCuona(rec);
            if (deviceId != null) {
                return deviceId;
            }
        }
        return null;
    }

    @Override
    public void writeJSON(String json) throws IOException {
        Log.i("nfc", "T4 detected");
        //selectNDEFTagApp();
        Ndef ndef = Ndef.get(dep.getTag());
        if (ndef == null) {
            throw new IOException("Cannot get Ndef");
        }

        if (!ndef.isConnected()) {
            ndef.connect();
        }

        byte[] deviceId = getDeviceId(ndef);
        if  (deviceId == null) {
            throw new IOException("NFC type 4 tag is not formatted");
        }

        HexUtils.logd("Got deviceId", deviceId);

        byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);

        NdefRecord rec = CuonaNDEF.createRecord(deviceId, jsonData);
        NdefMessage msg = new NdefMessage(rec);

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

    @Override
    public void protect(byte[] newPassword, byte[] oldPassword) throws IOException {
        // TODO:
    }

    @Override
    public void unprotect(byte[] password) throws IOException {
        // TODO:
    }

    /*
    private final static byte[] NDEFTagAppSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x04, (byte) 0x00, // P1 P2
            (byte) 0x07, // Lc
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00,
            (byte) 0x85, (byte) 0x01, (byte) 0x01, // data
    };

    private void selectNDEFTagApp() throws IOException {

        if (!dep.isConnected()) {
            dep.connect();
        }

        HexUtils.logd("T4 send", NDEFTagAppSelectAPDU);
        byte[] ans = dep.transceive(NDEFTagAppSelectAPDU);
        HexUtils.logd("T4 recv", ans);

    }
    */

}

