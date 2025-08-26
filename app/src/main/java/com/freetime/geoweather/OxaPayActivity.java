package com.freetime.geoweather;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OxaPayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oxa_pay);

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://pay.oxapay.com/13038067");

        Button btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> {
            Intent intent = new Intent(OxaPayActivity.this, DonateActivity.class);
            startActivity(intent);
        });
    }
}