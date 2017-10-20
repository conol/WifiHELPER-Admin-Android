package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;


import android.nfc.NdefRecord;

public class CNFCNDEF {

    private static final String CNFC_TAG_DOMAIN = "conol.co.jp";
    private static final String CNFC_TAG_TYPE = "cnfc_bt_manu_data";
    private static final byte CNFC_MAGIC_1 = 0x63;
    private static final byte CNFC_MAGIC_2 = 0x6f;
    private static final byte CNFC_MAGIC_3 = 0x01;

    private static final int CNFC_MAGIC_LENGTH = 3;
    private static final int CNFC_DEVICEID_LENGTH = 7;

    public static NdefRecord createRecord(byte[] deviceId, byte[] jsonData) {
        byte[] payload = new byte[jsonData.length + CNFC_MAGIC_LENGTH + CNFC_DEVICEID_LENGTH];
        payload[0] = CNFC_MAGIC_1;
        payload[1] = CNFC_MAGIC_2;
        payload[2] = CNFC_MAGIC_3;
        for (int i = 0; i < CNFC_DEVICEID_LENGTH; i++) {
            if (i < deviceId.length) {
                payload[i + CNFC_MAGIC_LENGTH] = deviceId[i];
            }
        }
        for (int i = 0; i < jsonData.length; i++) {
            payload[i + CNFC_MAGIC_LENGTH + CNFC_DEVICEID_LENGTH] = jsonData[i];
        }

        return NdefRecord.createExternal(CNFC_TAG_DOMAIN, CNFC_TAG_TYPE, payload);
    }

}
