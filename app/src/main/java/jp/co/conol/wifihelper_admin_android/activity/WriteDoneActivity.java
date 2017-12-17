package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_lib.cuona.WifiHelper;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class WriteDoneActivity extends AppCompatActivity {

    @BindView(R.id.ssidTextView) TextView mSsidTextView;
    @BindView(R.id.passTextView) TextView mPassTextView;
    @BindView(R.id.typeTextView) TextView mTypeTextView;
    @BindView(R.id.daysTextView) TextView mDaysTextView;
    @BindView(R.id.coronaImageView) ImageView mCoronaImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_done);
        ButterKnife.bind(this);

        // 設定の取得
        Intent intent = getIntent();
        final String ssid = intent.getStringExtra("ssid");
        final String pass = intent.getStringExtra("pass");
        final int wifiKind = intent.getIntExtra("wifiKind", WifiHelper.WPA_WPA2PSK);
        final int expireDate = intent.getIntExtra("expireDate", 0);
        final int deviceType = intent.getIntExtra("deviceType", Cuona.TAG_TYPE_UNKNOWN);

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
        if(deviceType == Cuona.TAG_TYPE_SEAL) {
            mCoronaImageView.setImageResource(R.drawable.ic_nfc);
        } else {
            mCoronaImageView.setImageResource(R.drawable.img_cuona);
        }
    }

    public void onBackToTopButtonClicked(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
