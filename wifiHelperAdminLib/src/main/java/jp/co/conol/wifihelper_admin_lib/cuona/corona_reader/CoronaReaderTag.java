package jp.co.conol.wifihelper_admin_lib.cuona.corona_reader;

import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Formatter;

public class CoronaReaderTag {

    private static final int TAG_TYPE_UNKNOWN = 0;
    private static final int TAG_TYPE_CORONA = 1;
    private static final int TAG_TYPE_SEAL = 2;

    private static final byte[] CNFC_MAGIC = { 0x63, 0x6f, 0x01 };
    private static final int CNFC_DEVICEID_LENGTH = 7;
    private static final String CONOL_EXT_MANU_DATA = "conol.co.jp:cnfc_bt_manu_data";

    private final byte[] deviceId;
    private final byte[] jsonData;

    private CoronaReaderTag(byte[] deviceId, byte[] jsonData) {
        this.deviceId = deviceId;
        this.jsonData = jsonData;
    }

    public byte[] getDeviceIdData() {
        return deviceId;
    }

    public byte[] getJsonData() {
        return jsonData;
    }

    public String getDeviceIdString() {
        Formatter fmt = new Formatter();
        for (byte b: deviceId) {
            fmt.format("%02X", b & 0xff);
        }
        return fmt.toString();
    }

    public int getType() {
        if (deviceId  == null || deviceId.length == 0) {
            return TAG_TYPE_UNKNOWN;
        }
        if (deviceId[0] == 4) {
            return TAG_TYPE_SEAL;
        }
        if (deviceId[0] == 2) {
            return TAG_TYPE_CORONA;
        }
        return TAG_TYPE_UNKNOWN;
    }

    public String getJsonString() {
        try {
            return new String(jsonData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static CoronaReaderTag get(NdefRecord ndef) {
        if (ndef.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return null;
        }

        if (!new String(ndef.getType()).equals(CONOL_EXT_MANU_DATA)) {
            return null;
        }

        byte[] payload = ndef.getPayload();
        if (payload.length < CNFC_MAGIC.length + CNFC_DEVICEID_LENGTH ||
                payload[0] != CNFC_MAGIC[0] || payload[1] != CNFC_MAGIC[1] ||
                payload[2] != CNFC_MAGIC[2]) {
            return null;
        }

        byte[] deviceId = Arrays.copyOfRange(payload, CNFC_MAGIC.length,
                CNFC_MAGIC.length + CNFC_DEVICEID_LENGTH);
        byte[] jsonData = Arrays.copyOfRange(payload, CNFC_MAGIC.length + CNFC_DEVICEID_LENGTH,
                payload.length);

        return new CoronaReaderTag(deviceId, jsonData);
    }
}
