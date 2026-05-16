package com.example.phonewatch;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private boolean isMonitoring = false;
    private PinManager pinManager;
    private Button btnToggle;
    private TextView tvStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, AntiTheftService.class));

        btnToggle = findViewById(R.id.btn_toggle);
        tvStatus = findViewById(R.id.tv_status);
        pinManager = new PinManager(this);
        Button btnSetPin = findViewById(R.id.btn_set_pin);

        btnSetPin.setOnClickListener(v -> showSetPinDialog());
        btnToggle.setOnClickListener(v -> toggleAntiTheft());
    }

    private void toggleAntiTheft() {
        isMonitoring = !isMonitoring;

        Intent intent = new Intent(this, AntiTheftService.class);
        intent.setAction(isMonitoring ? "START_MONITORING" : "STOP_MONITORING");
        startService(intent);
        tvStatus.setText("Mod anti-furt: " + (isMonitoring ? "ACTIVAT" : "DEZACTIVAT"));
        btnToggle.setText(isMonitoring ? "Dezactiveaza anti-furt" : "Activeaza anti-furt");
    }
    private void showSetPinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_pin, null);
        EditText etNewPin = dialogView.findViewById(R.id.et_new_pin);
        EditText etConfirmPin = dialogView.findViewById(R.id.et_confirm_pin);

        new AlertDialog.Builder(this)
                .setTitle("Seteaza PIN")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    String newPin = etNewPin.getText().toString().trim();
                    String confirmPin = etConfirmPin.getText().toString().trim();

                    if (newPin.length() < 4) {
                        Toast.makeText(this, "PIN-ul trebuie sa aiba minim 4 cifre!", Toast.LENGTH_SHORT).show();
                    } else if (!newPin.equals(confirmPin)){
                        Toast.makeText(this, "PIN-urile nu coincid!", Toast.LENGTH_SHORT).show();
                    } else {
                        pinManager.savePin(newPin);
                        Toast.makeText(this, "PIN salvat!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }
}
