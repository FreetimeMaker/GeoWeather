package com.freetime.geoweather;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DonateActivity extends AppCompatActivity {

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen-Modus aktivieren
        hideSystemUI();
        setContentView(R.layout.activity_donate);

        Button btnDonateOxa = findViewById(R.id.btnDonateOxa);
        btnDonateOxa.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, OxaPayActivity.class);
            startActivity(intent);
        });

        Button btnDonateCoin = findViewById(R.id.btnDonateCoin);
        btnDonateCoin.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, CoinbaseActivity.class);
            startActivity(intent);
        });

        Button btnDonateBTC = findViewById(R.id.btnDonateBTC);
        btnDonateBTC.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, BitcoinActivity.class);
            startActivity(intent);
        });

        Button btnDonateETH = findViewById(R.id.btnDonateETH);
        btnDonateETH.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, EthereumActivity.class);
            startActivity(intent);
        });

        Button btnDonateUSDT = findViewById(R.id.btnDonateUSDT);
        btnDonateUSDT.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, USDT_Activity.class);
            startActivity(intent);
        });

        Button btnDonateUSDC = findViewById(R.id.btnDonateUSDC);
        btnDonateUSDC.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, USDC_Activity.class);
            startActivity(intent);
        });
        
        Button btnDonateSHIB = findViewById(R.id.btnDonateSHIB);
        btnDonateSHIB.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, ShibActivity.class);
            startActivity(intent);
        });

        Button btnDonateDOGE = findViewById(R.id.btnDonateDOGE);
        btnDonateDOGE.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, DogeActivity.class);
            startActivity(intent);
        });

        Button btnDonateTRON = findViewById(R.id.btnDonateTRON);
        btnDonateTRON.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, TronActivity.class);
            startActivity(intent);
        });

        Button btnDonateLTC = findViewById(R.id.btnDonateLTC);
        btnDonateLTC.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, LTC_Activity.class);
            startActivity(intent);
        });

        Button btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
