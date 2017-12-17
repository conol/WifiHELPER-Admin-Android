package jp.co.conol.wifihelper_admin_android.custom;

import android.content.Context;
import android.support.v7.app.AlertDialog;

/**
 * Created by m_ito on 2017/12/14.
 */

public class SimpleAlertDialog {

    private AlertDialog mAlertDialog = null;

    public SimpleAlertDialog(Context context, String message) {
        mAlertDialog = new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public void show() {
        mAlertDialog.show();
    }
}
