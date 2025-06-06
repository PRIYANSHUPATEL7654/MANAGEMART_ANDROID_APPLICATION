package com.managemart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanQRActivity extends AppCompatActivity {

    Button btnScanQR;
    TextView tvScanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        btnScanQR = findViewById(R.id.btnScanQR);
        tvScanResult = findViewById(R.id.tvScanResult);

        // Load previously saved scan result (if any)
        SharedPreferences prefs = getSharedPreferences("ScanData", MODE_PRIVATE);
        String savedData = prefs.getString("qr_result", null);

        if (savedData != null) {
            tvScanResult.setText("Scanned Data: " + savedData);
        }

        // Start QR Scan when button is clicked
        btnScanQR.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(ScanQRActivity.this);
            integrator.setPrompt("Scan a QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String scannedData = result.getContents();
            tvScanResult.setText("Scanned Data: " + scannedData);

            // Save scanned data to SharedPreferences
            SharedPreferences.Editor editor = getSharedPreferences("ScanData", MODE_PRIVATE).edit();
            editor.putString("qr_result", scannedData);
            editor.apply();
        }
    }
}
