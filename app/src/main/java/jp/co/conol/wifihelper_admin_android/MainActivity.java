package jp.co.conol.wifihelper_admin_android;

import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mScanBackgroundConstraintLayout;
    private ConstraintLayout mScanDialogConstraintLayout;
    private ConstraintLayout mConnectingProgressConstraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanBackgroundConstraintLayout = (ConstraintLayout) findViewById(R.id.ScanBackgroundConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) findViewById(R.id.scanDialogConstraintLayout);
        mConnectingProgressConstraintLayout = (ConstraintLayout) findViewById(R.id.connectingProgressConstraintLayout);
    }

    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }
}
