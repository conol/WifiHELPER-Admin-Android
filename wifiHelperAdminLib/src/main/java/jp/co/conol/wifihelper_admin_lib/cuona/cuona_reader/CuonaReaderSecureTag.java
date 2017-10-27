package jp.co.conol.wifihelper_admin_lib.cuona.cuona_reader;

import android.nfc.NdefRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_CUONA;
import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_SEAL;
import static jp.co.conol.wifihelper_admin_lib.cuona.Cuona.TAG_TYPE_UNKNOWN;

public class CuonaReaderSecureTag extends CuonaReaderTag {

    private static final String CUONA_TAG_DOMAIN = "conol.jp";
    private static final String CUONA_TAG_TYPE = "cuona";
    private static final byte CUONA_MAGIC_1 = 0x63;
    private static final byte CUONA_MAGIC_2 = 0x6f;
    private static final byte CUONA_MAGIC_3_AES256_RSA2048 = 0x02;
    private static final byte CUONA_MAGIC_3_AES256_RSA512 = 0x03;

    private final byte[] deviceId;
    private final byte[] jsonData;
    private final boolean useShortKey;

    private CuonaReaderSecureTag(byte[] deviceId, byte[] content, boolean useShortKey) {
        this.deviceId = deviceId;
        this.useShortKey = useShortKey;
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
        if (useShortKey) {
            return 512;
        } else {
            return 2048;
        }
    }

    private static byte[] decrypt(boolean useShortKey, byte[] encryptedSymKey, byte[] iv,
                                  byte[] encryptedcontent) throws GeneralSecurityException {

        byte[] publicKeyData = useShortKey ? Keys.publicKeyData_512 : Keys.publicKeyData_2048;

        KeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyData);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);

        Cipher rsaDecryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaDecryptor.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] symKey = rsaDecryptor.doFinal(encryptedSymKey);

        Cipher aesDecryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesDecryptor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(symKey, "AES"),
                new IvParameterSpec(iv));
        byte[] content = aesDecryptor.doFinal(encryptedcontent);

        return content;
    }

    public static CuonaReaderSecureTag get(NdefRecord ndef) {
        if (ndef.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
            return null;
        }

        if (!new String(ndef.getType()).equals(CUONA_TAG_DOMAIN + ":" + CUONA_TAG_TYPE)) {
            return null;
        }

        byte[] payload = ndef.getPayload();
        if (payload.length < 8) {
            return null;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(payload);
        byte magic1 = (byte) bis.read();
        byte magic2 = (byte) bis.read();
        byte magic3 = (byte) bis.read();
        int deviceIdLen = bis.read();
        int eslenLow = bis.read();
        int eslenHigh = bis.read();
        int ivLen = bis.read();
        int reserved = bis.read();

        if (magic1 != CUONA_MAGIC_1 || magic2  != CUONA_MAGIC_2 || reserved != 0) {
            return null;
        }

        boolean useShortKey;
        if  (magic3 == CUONA_MAGIC_3_AES256_RSA512) {
            useShortKey = true;
        } else if  (magic3 == CUONA_MAGIC_3_AES256_RSA2048) {
            useShortKey = false;
        } else {
            return null;
        }

        int encryptedSymKeyLen = (eslenHigh << 8) + eslenLow;
        int encryptedcontentLen = payload.length - (8 + deviceIdLen + ivLen + encryptedSymKeyLen);
        if (encryptedcontentLen < 0) {
            return null;
        }

        byte[] deviceId = new byte[deviceIdLen];
        byte[] encryptedSymKey = new byte[encryptedSymKeyLen];
        byte[] iv = new byte[ivLen];
        byte[] encryptedcontent = new byte[encryptedcontentLen];

        try {
            bis.read(deviceId);
            bis.read(encryptedSymKey);
            bis.read(iv);
            bis.read(encryptedcontent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] content;
        try {
            content = decrypt(useShortKey, encryptedSymKey, iv, encryptedcontent);
        } catch (GeneralSecurityException e) {
            return null;
        }

        if (content.length < 4 || content[0] != 'J' || content[1] != 'S' || content[2] != 'O'
                || content[3] != 'N') {
            return null;
        }

        return new CuonaReaderSecureTag(deviceId, content, useShortKey);
    }
}
