package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import jp.co.conol.wifihelper_admin_android.R;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class WriteDoneActivity extends AppCompatActivity {

    private TextView mSsidTextView;
    private TextView mPassTextView;
    private TextView mTypeTextView;
    private TextView mDaysTextView;
    private ImageView mCoronaImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_done);

        mSsidTextView = (TextView) findViewById(R.id.ssidTextView);
        mPassTextView = (TextView) findViewById(R.id.passTextView);
        mTypeTextView = (TextView) findViewById(R.id.typeTextView);
        mDaysTextView = (TextView) findViewById(R.id.daysTextView);
        mCoronaImageView = (ImageView) findViewById(R.id.coronaImageView);

        // 設定の取得
        Intent intent = getIntent();
        final String ssid = intent.getStringExtra("ssid");
        final String pass = intent.getStringExtra("pass");
        final int wifiKind = intent.getIntExtra("wifiKind", 1);
        final int expireDate = intent.getIntExtra("expireDate", 0);
        final int deviceType = intent.getIntExtra("deviceType", 2);

        // 設定の表示
        mSsidTextView.setText(ssid);
        mPassTextView.setText(pass);
        switch (wifiKind) {
            case 1:
                mTypeTextView.setText(getString(R.string.write_kind_wpa));
                break;
            case 2:
                mTypeTextView.setText(getString(R.string.write_kind_wep));
                break;
            default:
                mTypeTextView.setText(getString(R.string.write_kind_none));
                break;
        }
        if(expireDate != 0) {
            mDaysTextView.setText(String.valueOf(expireDate) + getString(R.string.write_expire_date_option));
        } else {
            mDaysTextView.setText(getString(R.string.write_expire_date_unlimited));
        }
        if(deviceType == 1) {
            mCoronaImageView.setImageResource(R.drawable.img_corona);
        } else {
            mCoronaImageView.setImageResource(R.drawable.img_nfc);
        }
    }

    public void onBackToTopButtonClicked(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
