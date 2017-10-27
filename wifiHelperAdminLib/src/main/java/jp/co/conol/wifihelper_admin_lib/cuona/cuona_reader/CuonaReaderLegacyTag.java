package jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader;

import android.nfc.NdefRecord;

import java.util.Arrays;

import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_CUONA;
import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_SEAL;
import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_UNKNOWN;

public class CuonaReaderLegacyTag extends CuonaReaderTag {

    private static final byte[] CNFC_MAGIC = { 0x63, 0x6f, 0x01 };
    private static final int CNFC_DEVICEID_LENGTH = 7;
    private static final String CONOL_EXT_MANU_DATA = "conol.co.jp:cnfc_bt_manu_data";

    private final byte[] deviceId;
    private final byte[] jsonData;

    private CuonaReaderLegacyTag(byte[] deviceId, byte[] jsonData) {
        this.deviceId = deviceId;
        this.jsonData = jsonData;
    }

    @Override
    public byte[] getDeviceId() {
        return deviceId;
    }

    @Override
    public byte[] getJSONData() {
        return jsonData;
    }

    public int getType() {
        if (deviceId  == null || deviceId.length == 0) {
            return TAG_TYPE_UNKNOWN;
        }
        if (deviceId[0] == 4) {
            return TAG_TYPE_SEAL;
        }
        if (deviceId[0] == 2) {
            return TAG_TYPE_CUONA;
        }
        return TAG_TYPE_UNKNOWN;
    }

    @Override
    public int getSecurityStrength() {
        return 0;
    }

    public static CuonaReaderLegacyTag get(NdefRecord ndef) {
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

        return new CuonaReaderLegacyTag(deviceId, jsonData);
    }

}
