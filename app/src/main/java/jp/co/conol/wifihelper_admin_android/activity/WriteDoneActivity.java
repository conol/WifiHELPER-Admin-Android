package jp.co.conol.wifihelper_admin_android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import jp.co.conol.wifihelper_admin_android.R;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class WriteDoneActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_done);
    }

    public void onBackToTopButtonClicked(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
