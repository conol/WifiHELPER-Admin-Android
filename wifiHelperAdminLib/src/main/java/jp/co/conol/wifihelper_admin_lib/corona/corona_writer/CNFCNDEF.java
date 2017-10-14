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

    public static NdefRecord createRecord(byte[] chipId, byte[] serviceId) {
        byte[] payload = new byte[serviceId.length + CNFC_MAGIC_LENGTH + CNFC_DEVICEID_LENGTH];
        payload[0] = CNFC_MAGIC_1;
        payload[1] = CNFC_MAGIC_2;
        payload[2] = CNFC_MAGIC_3;
        for (int i = 0; i < CNFC_DEVICEID_LENGTH; i++) {
            if (i < chipId.length) {
                payload[i + CNFC_MAGIC_LENGTH] = chipId[i];
            }
        }
        for (int i = 0; i < serviceId.length; i++) {
            payload[i + CNFC_MAGIC_LENGTH + CNFC_DEVICEID_LENGTH] = serviceId[i];
        }

        return NdefRecord.createExternal(CNFC_TAG_DOMAIN, CNFC_TAG_TYPE, payload);
    }

}
