package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CNFCT2WriterTag extends CoronaWriterTag {

    private MifareUltralight mul;

    public CNFCT2WriterTag(MifareUltralight mul) {
        this.mul = mul;
    }

    @Override
    public void writeJSON(String json) throws IOException {
        Log.i("nfc", "T2 detected");

        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] deviceId = mul.readPages(0);
        HexUtils.logd("readPages(0)", deviceId);

        mul.close();

        byte[] jsonData = json.getBytes(StandardCharsets.UTF_8);

        NdefRecord rec = CNFCNDEF.createRecord(deviceId, jsonData);
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
