package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import java.util.UUID;

interface CuonaBTCallback {

    void onCuonaBTConnected();
    void onCuonaBTDisconnected();
    void onCuonaBTRead(UUID uuid, boolean success, byte[] data);
    void onCuonaBTWrite(UUID uuid, boolean success);

}
