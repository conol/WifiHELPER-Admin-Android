package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;

import java.io.IOException;

public abstract class CNFCWriterTag {

    public abstract void writeServiceID(byte[] serviceId) throws IOException;

}