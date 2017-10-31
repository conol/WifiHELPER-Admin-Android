package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import java.io.IOException;

public abstract class CuonaWritableTag {

    public abstract void writeJSON(String json) throws IOException;

}
