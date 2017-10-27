package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;


import android.nfc.NdefRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CuonaNDEF {

    private static final String CUONA_TAG_DOMAIN = "conol.jp";
    private static final String CUONA_TAG_TYPE = "cuona";
    private static final byte CUONA_MAGIC_1 = 0x63;
    private static final byte CUONA_MAGIC_2 = 0x6f;
    private static final byte CUONA_MAGIC_3_AES256_RSA2048 = 0x02;
    private static final byte CUONA_MAGIC_3_AES256_RSA512 = 0x03;

    private static final int CUONA_T2_DEVICEID_LENGTH = 9;

    private static final int SYMMETRIC_KEY_LENGTH = 32; // 256 bits

    private static final SecureRandom secureRandom = new SecureRandom();

    static NdefRecord createRecord(byte[] deviceId, byte[] payload, boolean useShortKey)
            throws IOException {
        byte[] encryptedSymKey;
        byte[] iv;
        byte[] encryptedPayload;

        try {
            // Setup RSA key
            byte[] privateKeyData = useShortKey ? Keys.privateKeyData_512
                    : Keys.privateKeyData_2048;
            KeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyData);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);

            // Generate AES256 key
            byte[] symKey = new byte[SYMMETRIC_KEY_LENGTH];
            secureRandom.nextBytes(symKey);

            // Encrypt AES key with RSA key
            Cipher rsaEncryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaEncryptor.init(Cipher.ENCRYPT_MODE, privateKey);
            encryptedSymKey = rsaEncryptor.doFinal(symKey);

            // Encrypt payload with AES key
            Cipher aesEncryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesEncryptor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(symKey, "AES"));
            encryptedPayload = aesEncryptor.doFinal(payload);
            iv = aesEncryptor.getIV();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        byte eslenLow = (byte) (encryptedSymKey.length & 0xff);
        byte eslenHigh = (byte) (encryptedSymKey.length >> 8);

        byte[] header = new byte[] {
                CUONA_MAGIC_1, CUONA_MAGIC_2,
                useShortKey ? CUONA_MAGIC_3_AES256_RSA512 : CUONA_MAGIC_3_AES256_RSA2048,
                CUONA_T2_DEVICEID_LENGTH, eslenLow, eslenHigh, (byte) iv.length, 0
        };

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(header);
        os.write(deviceId, 0, CUONA_T2_DEVICEID_LENGTH);
        os.write(encryptedSymKey);
        os.write(iv);
        os.write(encryptedPayload);

        byte[] all = os.toByteArray();
        return NdefRecord.createExternal(CUONA_TAG_DOMAIN, CUONA_TAG_TYPE, all);
    }

}
