package jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader;

import android.nfc.NdefRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CuonaReaderSecureTag extends CuonaReaderTag {

    private static final String CUONA_TAG_DOMAIN = "conol.jp";
    private static final String CUONA_TAG_TYPE = "cuona";
    private static final byte CUONA_MAGIC_1 = 0x63;
    private static final byte CUONA_MAGIC_2 = 0x6f;
    private static final byte CUONA_MAGIC_3 = 0x05;

    private final byte[] deviceId;
    private final byte[] jsonData;

    private final int TAG_TYPE_UNKNOWN = 0;
    private final int TAG_TYPE_CUONA = 1;
    private final int TAG_TYPE_SEAL = 2;

    private CuonaReaderSecureTag(byte[] deviceId, byte[] content) {
        this.deviceId = deviceId;
        this.jsonData = Arrays.copyOfRange(content, 4, content.length);
    }

    @Override
    public byte[] getDeviceId() {
        return deviceId;
    }

    @Override
    public byte[] getJSONData() {
        return jsonData;
    }

    @Override
    public int getType() {
        if (deviceId.length == 7) {
            return TAG_TYPE_CUONA;
        } else if (deviceId.length == 9) {
            return TAG_TYPE_SEAL;
        } else {
            return TAG_TYPE_UNKNOWN;
        }
    }

    @Override
    public int getSecurityStrength() {
        return 256;
    }

    private static byte[] decrypt(int keyCode, byte[] deviceId, byte[] iv, byte[] encryptedcontent, int cuonaKeyCode, byte[] cuonaKey)
            throws GeneralSecurityException {

        byte[] keyData;
        if (keyCode == cuonaKeyCode) {
            keyData = cuonaKey;
        } else {
            // With master Key, uncomment the following line
            keyData = CuonaMasterKey.getKey(keyCode);
            // Without master key, uncomment the following line
//            return null;
        }

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(deviceId);
        byte[] key =  md.digest();
        if (key.length != cuonaKey.length) {
            throw new IllegalStateException("SHA-256 returns illegal length");
        }
        for (int i = 0; i < key.length; i++) {
            key[i] ^= keyData[i];
        }

        Cipher decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decryptor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));

        return decryptor.doFinal(encryptedcontent);
    }

    public static CuonaReaderSecureTag get(NdefRecord ndef, int cuonaKeyCode, byte[] cuonaKey) {
        if (ndef.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return null;
        }

        if (!new String(ndef.getType()).equals(CUONA_TAG_DOMAIN + ":" + CUONA_TAG_TYPE)) {
            return null;
        }

        byte[] payload = ndef.getPayload();
        if (payload.length < 7) {
            return null;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(payload);
        byte magic1 = (byte) bis.read();
        byte magic2 = (byte) bis.read();
        byte magic3 = (byte) bis.read();
        int kcLow = bis.read();
        int kcHigh = bis.read();
        int deviceIdLen = bis.read();
        int ivLen = bis.read();

        if (magic1 != CUONA_MAGIC_1 || magic2  != CUONA_MAGIC_2 || magic3 != CUONA_MAGIC_3) {
            return null;
        }

        int keyCode = (kcHigh << 8) + kcLow;

        int encryptedcontentLen = payload.length - (7 + deviceIdLen + ivLen);
        if (encryptedcontentLen < 0) {
            return null;
        }

        byte[] deviceId = new byte[deviceIdLen];
        byte[] iv = new byte[ivLen];
        byte[] encryptedcontent = new byte[encryptedcontentLen];

        try {
            bis.read(deviceId);
            bis.read(iv);
            bis.read(encryptedcontent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] content;
        try {
            content = decrypt(keyCode, deviceId, iv, encryptedcontent, cuonaKeyCode, cuonaKey);
        } catch (GeneralSecurityException e) {
            return null;
        }

        if (content == null) {
            return null;
        }

        if (content.length < 4 || content[0] != 'J' || content[1] != 'S' || content[2] != 'O'
                || content[3] != 'N') {
            return null;
        }

        return new CuonaReaderSecureTag(deviceId, content);
    }
}