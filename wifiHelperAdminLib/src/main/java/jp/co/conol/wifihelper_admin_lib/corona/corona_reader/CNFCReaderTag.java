package jp.co.conol.wifihelper_admin_lib.corona.corona_reader;

import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Formatter;

public class CNFCReaderTag {

    private static final byte[] CNFC_MAGIC = { 0x63, 0x6f, 0x01 };
    private static final int CNFC_DEVICEID_LENGTH = 7;
    private static final String CONOL_EXT_MANU_DATA = "conol.co.jp:cnfc_bt_manu_data";

    private final byte[] chipId;
    private final byte[] serviceId;

    private CNFCReaderTag(byte[] chipId, byte[] serviceId) {
        this.chipId = chipId;
        this.serviceId = serviceId;
    }

    public byte[] getChipId() {
        return chipId;
    }

    public byte[] getServiceId() {
        return serviceId;
    }

    public String getChipIdString() {
        Formatter fmt = new Formatter();
        for (byte b: chipId) {
            fmt.format("%02X", b & 0xff);
        }
        return fmt.toString();
    }

    public String getServiceIdString() {
        try {
            return new String(serviceId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static CNFCReaderTag get(NdefRecord ndef) {
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

        byte[] chipId = Arrays.copyOfRange(payload, CNFC_MAGIC.length,
                CNFC_MAGIC.length + CNFC_DEVICEID_LENGTH);
        byte[] serviceId = Arrays.copyOfRange(payload, CNFC_MAGIC.length + CNFC_DEVICEID_LENGTH,
                payload.length);

        return new CNFCReaderTag(chipId, serviceId);
    }
}
