package jp.co.conol.wifihelper_admin_android.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jp.co.conol.wifihelper_admin_android.R;
import jp.co.conol.wifihelper_admin_lib.wifi_helper.model.Wifi;

public class WriteSettingActivity extends AppCompatActivity {

    private EditText mSsidEditText;
    private EditText mPassEditText;
    private TextView mWepTextView;
    private TextView mWpaTextView;
    private TextView mNoneTextView;
    private ConstraintLayout mExpireDateConstraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_setting);

        mSsidEditText = (EditText) findViewById(R.id.ssidEditText);
        mPassEditText = (EditText) findViewById(R.id.passEditText);
        mWepTextView = (TextView) findViewById(R.id.wepTextView);
        mWpaTextView = (TextView) findViewById(R.id.wpaTextView);
        mNoneTextView = (TextView) findViewById(R.id.noneTextView);
        mExpireDateConstraintLayout = (ConstraintLayout) findViewById(R.id.expireDateConstraintLayout);

        // nfcから情報を取得
        Intent intent = getIntent();
        String ssid = intent.getStringExtra("ssid");
        String pass = intent.getStringExtra("pass");
        int wifiKind = intent.getIntExtra("wifiKind", 1);

        // nfcからの情報をセット
        mSsidEditText.setText(ssid);
        mPassEditText.setText(pass);
        switch (wifiKind) {
            case 1:
                setWpaWifi();
                break;
            case 2:
                setWepWifi();
                break;
            default:
                setNoneWifi();
                break;
        }

        // wifiの種類をクリックした場合
        View.OnClickListener wifiKindClickListener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(view == mWpaTextView) {
                    setWpaWifi();
                } else if(view == mWepTextView) {
                    setWepWifi();
                } else {
                    setNoneWifi();
                }
            }
        };
        mWepTextView.setOnClickListener(wifiKindClickListener);
        mWpaTextView.setOnClickListener(wifiKindClickListener);
        mNoneTextView.setOnClickListener(wifiKindClickListener);

        // 期限日付選択時のダイアログ
        final String[] expireDays = new String[365];
        for(int i = 0; i < expireDays.length; i++) {
            expireDays[i] = String.valueOf(i + 1) + "日";
        }
        final int defaultItem = 0;
        final List<Integer> checkedItems = new ArrayList<>();
        checkedItems.add(defaultItem);
        mExpireDateConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // タップした時
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mExpireDateConstraintLayout.setAlpha(0.5f);
                }
                // 離した時
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mExpireDateConstraintLayout.setAlpha(1f);
                    new AlertDialog.Builder(WriteSettingActivity.this)
                            .setTitle(getString(R.string.write_expire_date_title))
                            .setSingleChoiceItems(expireDays, defaultItem, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkedItems.clear();
                                    checkedItems.add(which);
                                }
                            })
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!checkedItems.isEmpty()) {
                                        Log.d("checkedItem:", "" + checkedItems.get(0));
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                }
                return false;
            }
        });

    }

    private void setWepWifi() {
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_enable);
        mWepTextView.setTextColor(Color.WHITE);
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_disable);
        mWpaTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_disable);
        mNoneTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
    }

    private void setWpaWifi() {
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_disable);
        mWepTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_enable);
        mWpaTextView.setTextColor(Color.WHITE);
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_disable);
        mNoneTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
    }

    private void setNoneWifi() {
        mWepTextView.setBackgroundResource(R.drawable.style_wep_button_disable);
        mWepTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mWpaTextView.setBackgroundResource(R.drawable.style_wpa_button_disable);
        mWpaTextView.setTextColor(ContextCompat.getColor(WriteSettingActivity.this, R.color.darkGray));
        mNoneTextView.setBackgroundResource(R.drawable.style_none_button_enable);
        mNoneTextView.setTextColor(Color.WHITE);
    }





}
