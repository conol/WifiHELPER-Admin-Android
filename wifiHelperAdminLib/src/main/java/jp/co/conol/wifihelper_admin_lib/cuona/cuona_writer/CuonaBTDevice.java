package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class CuonaBTDevice extends CuonaWritableTag implements CuonaBTCallback {

    private static final String TYPE_LEGACY = "conol.co.jp:cnfc_bt_manu_data";
    private static final byte[] LEGACY_MAGIC = {0x63, 0x6f, 0x01};
    private static final int LEGACY_DEVICEID_LENGTH = 7;

    private static final String TYPE_CUONA = "conol.jp:cuona";
    private static final byte[] CUONA_MAGIC = {0x63, 0x6f, 0x04};
    private static final int CUONA_DEVICEID_LENGTH_OFFSET = 3;
    private static final int CUONA_DEVICEID_OFFSET = 5;
    private static final int CUONA_PASSWORD_LENGTH = 16;

    private static final byte PWCMD_ENTER_ADMIN = 0;
    private static final byte PWCMD_SET_PASSWORD = 1;

    private final IsoDep dep;
    private final Context context;
    private CuonaBTConnection btConnection;

    private byte[] zeroPassword = new byte[CUONA_PASSWORD_LENGTH];
    private byte[] savedPassword;
    private byte[] encryptedData;

    enum BtState {
        NONE, ERROR,
        WJSON_CONNECT, WJSON_LOGIN, WJSON_DATA_WRITE, WJSON_PROTECT, WJSON_DISCONNECT,
        UNPRO_CONNECT, UNPRO_LOGIN, UNPRO_PROTECT, UNPRO_DISCONNECT,
    }

    private BtState btState = BtState.NONE;

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
        for (NdefRecord rec : recs) {
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

    private byte[] connectBT() throws IOException {
        final Ndef ndef = Ndef.get(dep.getTag());
        if (ndef == null) {
            throw new IOException("Cannot get Ndef");
        }

        if (!ndef.isConnected()) {
            ndef.connect();
        }

        byte[] deviceId = getDeviceId(ndef);
        if (deviceId == null) {
            throw new IOException("NFC type 4 tag is not formatted");
        }

        HexUtils.logd("Got deviceId", deviceId);

        btConnection = CuonaBTConnection.getInstance(context, deviceId, this);
        btConnection.startScanning();

        return deviceId;
    }

    private void sendPWProtect(byte cmd, byte[] pw) {
        byte[] data = new byte[pw.length + 1];
        data[0] = cmd;
        System.arraycopy(pw, 0, data, 1, pw.length);
        btConnection.writeRequest(CuonaBTConnection.CUONA_CHAR_UUID_PWPROTECT, data);
    }

    public CuonaBTDevice(Context context, IsoDep dep) {
        this.context = context;
        this.dep = dep;
    }

    private void commandDone() {
    }

    @Override
    public void writeJSON(String json, String password, byte[] cuonaKey) {
        if (password == null) {
            savedPassword = zeroPassword;
        } else {
            savedPassword = new CUONAPassword(password).getPasswordArray(CUONA_PASSWORD_LENGTH);
        }

        try {
            byte[] deviceId = connectBT();
            btState = BtState.WJSON_CONNECT;

            byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);
            encryptedData = CuonaNDEF.encrypt(deviceId, jsonData, cuonaKey);
            HexUtils.logd("encryptedData", encryptedData);
        } catch (IOException e) {
            e.printStackTrace();
            btState = BtState.ERROR;
            updateState(State.ERROR);
        }
    }

    // CuonaBTCallback interface

    @Override
    public void onCuonaBTConnected() {
        //btConnection.readRequest(CuonaBTConnection.CUONA_CHAR_UUID_SYSTEM_STATUS);
        if (btState == BtState.WJSON_CONNECT) {
            sendPWProtect(PWCMD_ENTER_ADMIN, zeroPassword);
            btState = BtState.WJSON_LOGIN;
        } else if (btState == BtState.UNPRO_CONNECT) {
            sendPWProtect(PWCMD_ENTER_ADMIN, savedPassword);
            btState = BtState.UNPRO_LOGIN;
        }
        updateState(State.BT_CONNECTED);
    }

    @Override
    public void onCuonaBTDisconnected() {
        if (btState  == BtState.WJSON_DISCONNECT || btState ==  BtState.UNPRO_DISCONNECT) {
            updateState(State.SUCCESS);
        } else if (btState == BtState.ERROR) {
            // error already reported
        } else {
            // unexpected disconnect
            updateState(State.ERROR);
        }
    }

    @Override
    public void onCuonaBTRead(UUID uuid, boolean success, byte[] data) {
        if (success) {
            if (uuid.equals(CuonaBTConnection.CUONA_CHAR_UUID_SYSTEM_STATUS)) {
                HexUtils.logd("system status: ", data);
            }
        } else {
            btState = BtState.ERROR;
            updateState(State.ERROR);
        }
    }

    @Override
    public void onCuonaBTWrite(UUID uuid, boolean success) {
        if (success) {
            if (uuid.equals(CuonaBTConnection.CUONA_CHAR_UUID_PWPROTECT)) {
                if (btState == BtState.WJSON_LOGIN) {
                    btConnection.writeRequest(CuonaBTConnection.CUONA_CHAR_UUID_NFC_DATA,
                            encryptedData);
                    btState = BtState.WJSON_DATA_WRITE;
                } else if (btState == BtState.WJSON_PROTECT) {
                    // done
                    btConnection.disconnectRequest();
                    btState = BtState.WJSON_DISCONNECT;
                } else if (btState == BtState.UNPRO_LOGIN) {
                    sendPWProtect(PWCMD_SET_PASSWORD, zeroPassword);
                    btState = BtState.UNPRO_PROTECT;
                } else if (btState == BtState.UNPRO_PROTECT) {
                    // done
                    btConnection.disconnectRequest();
                    btState = BtState.UNPRO_DISCONNECT;
                }
            } else if (uuid.equals(CuonaBTConnection.CUONA_CHAR_UUID_NFC_DATA)) {
                sendPWProtect(PWCMD_SET_PASSWORD, savedPassword);
                btState = BtState.UNPRO_PROTECT;
            }
        } else {
            btState = BtState.ERROR;
            updateState(State.ERROR);
        }
    }

}
