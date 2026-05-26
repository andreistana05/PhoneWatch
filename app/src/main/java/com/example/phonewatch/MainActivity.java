package com.example.phonewatch;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    private CountDownTimer countDownTimer;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AntiTheftService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                isMonitoring = pinManager.isMonitoring();
                updateUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pinManager = new PinManager(this);
        isMonitoring = pinManager.isMonitoring();

        btnToggle = findViewById(R.id.btn_toggle);
        tvStatus = findViewById(R.id.tv_status);
        Button btnSetPin = findViewById(R.id.btn_set_pin);

        btnSetPin.setOnClickListener(v -> showSetPinDialog());
        btnToggle.setOnClickListener(v -> toggleAntiTheft());

        updateUI();
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AntiTheftService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        // Sync state in case it changed while stopped
        isMonitoring = pinManager.isMonitoring();
        updateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    private void toggleAntiTheft() {
        if (!isMonitoring) {
            showActivationConfirmation();
        } else {
            performToggle();
        }
    }

    private void showActivationConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare activare")
                .setMessage("Esti sigur ca stii PIN-ul setat? Vei avea nevoie de el pentru a opri alarma.")
                .setPositiveButton("Da, porneste", (dialog, which) -> performToggle())
                .setNeutralButton("Nu, modifica PIN", (dialog, which) -> showSetPinDialog())
                .setNegativeButton("Anuleaza", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void performToggle() {
        if(!isMonitoring) {
            startActivationTimer();
        } else {
            isMonitoring = false;
            if(countDownTimer != null) {
                countDownTimer.cancel();
            }
            stopAntiTheftService();
            updateUI();
            Toast.makeText(this, "Monitorizare oprita.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startActivationTimer() {
        btnToggle.setEnabled(false);

        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                tvStatus.setText("Activare in: " + secondsRemaining + " secunde...");
            }

            @Override
            public void onFinish() {
                isMonitoring = true;
                startAntiTheftService();
                updateUI();
                btnToggle.setEnabled(true);
                Toast.makeText(MainActivity.this, "Monitorizare pornita.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void startAntiTheftService() {
        Intent intent = new Intent(this, AntiTheftService.class);
        intent.setAction("START_MONITORING");
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopAntiTheftService() {
        Intent intent = new Intent(this, AntiTheftService.class);
        intent.setAction("STOP_MONITORING");
        startService(intent);
    }

    private void updateUI(){
        tvStatus.setText("Mod anti-furt: " + (isMonitoring ? "ACTIVAT" : "DEZACTIVAT"));
        btnToggle.setText(isMonitoring ? "Dezactiveaza anti-furt" : "Porneste anti-furt");
    }

    private void showSetPinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_pin, null);
        EditText etNewPin = dialogView.findViewById(R.id.et_new_pin);
        EditText etConfirmPin = dialogView.findViewById(R.id.et_confirm_pin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Seteaza PIN")
                .setView(dialogView)
                .setPositiveButton("Salveaza", null)
                .setNegativeButton("Anuleaza", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPin = etNewPin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (newPin.length() < 4) {
                Toast.makeText(this, "PIN-ul trebuie sa aiba minim 4 cifre!", Toast.LENGTH_SHORT).show();
            } else if (!newPin.equals(confirmPin)){
                Toast.makeText(this, "PIN-urile nu coincid!", Toast.LENGTH_SHORT).show();
            } else {
                pinManager.savePin(newPin);
                Toast.makeText(this, "PIN salvat!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }
}
