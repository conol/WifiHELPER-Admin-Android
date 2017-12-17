package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.nfc.NdefRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class CuonaNDEF {

    private static final String CUONA_TAG_DOMAIN = "conol.jp";
    private static final String CUONA_TAG_TYPE = "cuona";
    private static final byte CUONA_MAGIC_1 = 0x63;
    private static final byte CUONA_MAGIC_2 = 0x6f;
    private static final byte CUONA_MAGIC_3 = 0x04;

    private static final SecureRandom secureRandom = new SecureRandom();

    static NdefRecord createRecord(byte[] deviceId, byte[] payload, byte[] cuonaKey)
            throws IOException {

        byte[] all = encrypt(deviceId, payload, cuonaKey);
        return NdefRecord.createExternal(CUONA_TAG_DOMAIN, CUONA_TAG_TYPE, all);
    }

    static byte[] encrypt(byte[] deviceId, byte[] payload, byte[] cuonaKey) throws IOException {
        byte[] iv;
        byte[] encryptedPayload;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(deviceId);
            byte[] key =  md.digest();
            if (key.length != cuonaKey.length) {
                throw new IllegalStateException("SHA-256 returns illegal length");
            }
            for (int i = 0; i < key.length; i++) {
                key[i] ^= cuonaKey[i];
            }

            Cipher aesEncryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesEncryptor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            encryptedPayload = aesEncryptor.doFinal(payload);
            iv = aesEncryptor.getIV();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        byte[] header = new byte[] {
                CUONA_MAGIC_1, CUONA_MAGIC_2, CUONA_MAGIC_3,
                (byte) deviceId.length, (byte) iv.length
        };

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(header);
        os.write(deviceId);
        os.write(iv);
        os.write(encryptedPayload);

        return os.toByteArray();
    }

}
