package com.freetime.geoweather;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DonateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        Button btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> {
            Intent intent = new Intent(DonateActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
