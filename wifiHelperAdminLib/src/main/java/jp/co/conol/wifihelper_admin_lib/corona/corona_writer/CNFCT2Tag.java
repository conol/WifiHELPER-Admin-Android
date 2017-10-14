package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;

public class CNFCT2Tag extends CNFCTag {

    private MifareUltralight mul;

    public CNFCT2Tag(MifareUltralight mul) {
        this.mul = mul;
    }

    @Override
    public void writeServiceID(byte[] serviceId) throws IOException {
        Log.i("nfc", "T2 detected");

        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] chipId = mul.readPages(0);
        HexUtils.logd("readPages(0)", chipId);

        int maxDataLength = 8 * (chipId[14] & 0xff);
        Log.d("nfc", "maxDataLength=" + maxDataLength);

        mul.close();

        byte[] data = new byte[maxDataLength];

        NdefRecord rec = CNFCNDEF.createRecord(chipId, serviceId);
        NdefMessage msg = new NdefMessage(rec);

        Ndef ndef = Ndef.get(mul.getTag());
        if (ndef == null) {
            throw new IOException("Cannot get Ndef");
        }

        if (!ndef.isConnected()) {
            ndef.connect();
        }

        if (ndef.isWritable()) {

            try {
                ndef.writeNdefMessage(msg);
            } catch (FormatException e) {
                throw new IOException(e);
            }
            Log.i("nfc", "Tag written!");

        } else {
            throw new IOException("Tag is not writable");
        }

    }

}
