package jp.co.conol.wifihelper_admin_lib.cuona.cuona_writer;

import android.os.Handler;

public abstract class CuonaWritableTag {

    public interface Callback {
        void tagWriteStateChanged(State state);
    }

    public enum State {
        SUCCESS, ERROR, BT_CONNECTED
    }

    private Handler handler;
    private Callback callback;

    public abstract void writeJSON(String json, String password, int keyCode, byte[] cuonaKey);

    public void setCallback(Callback callback) {
        this.handler = new Handler();
        this.callback = callback;
    }

    protected void updateState(final State state) {
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.tagWriteStateChanged(state);
                }
            });
        }
    }

}
