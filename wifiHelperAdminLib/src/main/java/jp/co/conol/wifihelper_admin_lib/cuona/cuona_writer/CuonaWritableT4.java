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
    private static final int T4_CC_LENGTH = 15;
    private static final int T4_CC_WRITE_ACCESS_OFFSET = 0x0e;

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
    public void writeJSON(String json, String password) throws IOException {
        Log.i("nfc", "T4 detected");

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

    /*
    @Override
    public void protect(String newPassword, String oldPassword) throws IOException {
        byte[] newpw = new CUONAPassword(newPassword).getPasswordArray(T4_PASSWORD_LENGTH);
        byte[] oldpw = new CUONAPassword(oldPassword).getPasswordArray(T4_PASSWORD_LENGTH);

        if (!dep.isConnected()) {
            dep.connect();
        }
        try {
            sendAPDU(NDEFTagAppSelectAPDU);
            sendAPDU(CCSelectAPDU);
            byte[] CC = readBinary(0, T4_CC_LENGTH);
            if (CC[T4_CC_WRITE_ACCESS_OFFSET] != 0) {
                Log.d("nfc", "NFC Tag is write protected");
                throw new IOException("NFC Tag is write protected");
            }

            Log.d("nfc", "NFC Tag is not write protected");

            sendAPDU(NDEFFileSelectAPDU);
            sendWritePasswordVerify(newpw);
            sendAPDU(EnablePermanentStateAPDU);

        } finally {
            dep.close();
        }
    }

    @Override
    public void unprotect(String password) throws IOException {
        Log.d("nfc", "Unlock not supported for Type 4 tag");

        byte[] pw = new CUONAPassword(password).getPasswordArray(T4_PASSWORD_LENGTH);

        if (!dep.isConnected()) {
            dep.connect();
        }
        try {
            sendAPDU(NDEFTagAppSelectAPDU);
            sendAPDU(CCSelectAPDU);
            byte[] CC = readBinary(0, T4_CC_LENGTH);
            if (CC[T4_CC_WRITE_ACCESS_OFFSET] != 0) {
                Log.d("nfc", "NFC Tag is write protected");

                sendAPDU(NDEFFileSelectAPDU);
                sendWritePasswordVerify(pw);
                // !!! THIS NOT WORKS !!!
                // DisablePermanentStateAPDU must be issued from I2C bus
                sendAPDU(DisablePermanentStateAPDU);
                //sendAPDU(DisableWriteVerificationAPDU);

            } else {
                Log.d("nfc", "NFC Tag is not write protected");
                // Nothing to do
            }
        } finally {
            dep.close();
        }
    }
    */

    private final static byte[] NDEFTagAppSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x04, (byte) 0x00, // P1 P2
            (byte) 0x07, // Lc
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00,
            (byte) 0x85, (byte) 0x01, (byte) 0x01, // data
    };

    private final static byte[] CCSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x00, (byte) 0x0C, // P1 P2
            (byte) 0x02, // Lc
            (byte) 0xE1, (byte) 0x03, // data
    };

    private final static byte[] SystemFileSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x00, (byte) 0x0C, // P1 P2
            (byte) 0x02, // Lc
            (byte) 0xE1, (byte) 0x01, // data
    };

    private final static byte[] NDEFFileSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x00, (byte) 0x0C, // P1 P2
            (byte) 0x02, // Lc
            (byte) 0x00, (byte) 0x01, // data
    };

    private final static byte[] EnableWriteVerificationAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0x28, // INS
            (byte) 0x00, (byte) 0x02, // P1 P2
    };

    private final static byte[] DisableWriteVerificationAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0x26, // INS
            (byte) 0x00, (byte) 0x02, // P1 P2
    };

    private final static byte[] EnablePermanentStateAPDU = new byte[] {
            (byte) 0xa2, // CLA
            (byte) 0x28, // INS
            (byte) 0x00, (byte) 0x02, // P1 P2
    };

    private final static byte[] DisablePermanentStateAPDU = new byte[] {
            (byte) 0xa2, // CLA
            (byte) 0x26, // INS
            (byte) 0x00, (byte) 0x02, // P1 P2
    };

    private void sendAPDU(byte[] apdu) throws IOException {
        HexUtils.logd("T4 send", apdu);
        byte[] ans = dep.transceive(apdu);
        HexUtils.logd("T4 recv", ans);
        if (ans[0] != (byte) 0x90) {
            throw new IOException("NFC error");
        }
    }

    private final static byte[] ReadBinaryAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xB0, // INS
            (byte) 0x00, (byte) 0x00, // P1 P2 (Offset, placeholder)
            (byte) 0x00, // Le (Length, placeholder)
    };

    private byte[] readBinary(int offset, int length) throws IOException {
        byte[] apdu = Arrays.copyOf(ReadBinaryAPDU, ReadBinaryAPDU.length);
        apdu[2] = (byte) (offset >> 8);
        apdu[3] = (byte) offset;
        apdu[4] = (byte) length;
        HexUtils.logd("T4 send", apdu);
        byte[] ans = dep.transceive(apdu);
        HexUtils.logd("T4 recv", ans);
        if (ans[length] != (byte) 0x90) {
            throw new IOException("NFC error");
        }
        return Arrays.copyOf(ans, length);
    }

    private final static byte[] WritePasswordVerifyAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0x20, // INS
            (byte) 0x00, (byte) 0x02, // P1 P2
            (byte) 0x00, // Lc (Length, placeholder)
            // Password follows
    };

    private void sendWritePasswordVerify(byte[] password) throws IOException {
        byte[] apdu = Arrays.copyOf(WritePasswordVerifyAPDU,
                WritePasswordVerifyAPDU.length + password.length);
        apdu[WritePasswordVerifyAPDU.length - 1] = (byte) password.length;
        for (int i = 0; i < password.length; i++) {
            apdu[WritePasswordVerifyAPDU.length + i] = password[i];
        }
        HexUtils.logd("T4 send", apdu);
        byte[] ans = dep.transceive(apdu);
        HexUtils.logd("T4 recv", ans);
        if (ans[0] != (byte) 0x90) {
            throw new IOException("NFC error");
        }
    }
}

