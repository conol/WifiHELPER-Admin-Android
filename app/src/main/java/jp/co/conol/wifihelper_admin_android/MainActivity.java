package jp.co.conol.wifihelper_admin_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onAppAboutTextViewTapped(View view) {
        Intent intent = new Intent(this, AboutAppActivity.class);
        startActivity(intent);
    }
}
