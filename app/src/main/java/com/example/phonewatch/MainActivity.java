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

/**
 * Activitatea principala a aplicatiei
 * Gestioneaza interfata principala si butoanele de salvare PIN si pornire/oprire monitorizare.
 */
public class MainActivity extends AppCompatActivity {
    private boolean isMonitoring = false;
    private PinManager pinManager;
    private Button btnToggle;
    private TextView tvStatus;

    private CountDownTimer countDownTimer;


     // Receptor pentru a primi actualizari de stare de la AntiTheftService.

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

        // Initializam managerul de PIN si incarcam starea curenta a sistemului.
        pinManager = new PinManager(this);
        isMonitoring = pinManager.isMonitoring();

        // Legam elementele de UI de XML
        btnToggle = findViewById(R.id.btn_toggle);
        tvStatus = findViewById(R.id.tv_status);
        Button btnSetPin = findViewById(R.id.btn_set_pin);

        // Setam actiunile pentru butoane.
        btnSetPin.setOnClickListener(v -> showSetPinDialog());
        btnToggle.setOnClickListener(v -> toggleAntiTheft());

        // Actualizam butoanele conform starii sistemului (oprit/pornit monitorizare).
        updateUI();
        
        // Cerem permisiunile necesare pentru notificari pentru Android 13+.
        checkAndRequestPermissions();
    }

    /**
     * Verifica daca aplicatia are permisiuni de a trimite notificari.
     */
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
        // Inregistram receptorul de stare cand aplicatia devine vizibila.
        IntentFilter filter = new IntentFilter(AntiTheftService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
        // Ne asiguram ca interfata este sincronizata.
        isMonitoring = pinManager.isMonitoring();
        updateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stergem receptorul cand aplicatia este oprit pentru a economisi bateria.
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException e) {
            // Receptorul nu a fost inregistrat.
        }
    }

    /**
     * Gestioneaza actiunea de pornire/oprire a monitorizarii.
     */
    private void toggleAntiTheft() {
        if (!isMonitoring) {
            showActivationConfirmation();
        } else {
            performToggle();
        }
    }

    /**
     * Afiseaza un pop-up de confirmare inaninte de pornirea senzorilor.
     */
    private void showActivationConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare activare")
                .setMessage("Ești sigur că știi PIN-ul setat? Vei avea nevoie de el pentru a opri alarma.")
                .setPositiveButton("Da, pornește", (dialog, which) -> performToggle())
                .setNeutralButton("Nu, modifică PIN", (dialog, which) -> showSetPinDialog())
                .setNegativeButton("Anulează", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Executa pornirea timer-ului sau oprirea senzorilor.
     */
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
            Toast.makeText(this, "Monitorizare oprită.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Porneste o numaratoare inversa de 5 secunde inainte de pornirea serviciilor de anti-furt.
     * Ofera utilizatorului timp sa puna telefonul pe o suprafata plana.
     */
    private void startActivationTimer() {
        btnToggle.setEnabled(false); // Blocam butonul in timpul numaratorii.

        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                tvStatus.setText("Activare în: " + secondsRemaining + " secunde...");
            }

            @Override
            public void onFinish() {
                isMonitoring = true;
                startAntiTheftService();
                updateUI();
                btnToggle.setEnabled(true);
                Toast.makeText(MainActivity.this, "Monitorizare pornită.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    /**
     * Porneste serviciul in Foreground.
     */
    private void startAntiTheftService() {
        Intent intent = new Intent(this, AntiTheftService.class);
        intent.setAction("START_MONITORING");
        ContextCompat.startForegroundService(this, intent);
    }

    /**
     * Oprirea serviciului
     */
    private void stopAntiTheftService() {
        Intent intent = new Intent(this, AntiTheftService.class);
        intent.setAction("STOP_MONITORING");
        startService(intent);
    }

    /**
     * Actualizeaza textele de pe ecran conform starii.
     */
    private void updateUI(){
        tvStatus.setText("Mod anti-furt: " + (isMonitoring ? "ACTIVAT" : "DEZACTIVAT"));
        btnToggle.setText(isMonitoring ? "Dezactivează anti-furt" : "Pornește anti-furt");
    }

    /**
     * Afiseaza dialogul de configurare PIN.
     */
    private void showSetPinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_pin, null);
        EditText etNewPin = dialogView.findViewById(R.id.et_new_pin);
        EditText etConfirmPin = dialogView.findViewById(R.id.et_confirm_pin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Setează PIN")
                .setView(dialogView)
                .setPositiveButton("Salvează", null)
                .setNegativeButton("Anulează", null)
                .create();

        dialog.show();

        // Validam datele introduse inainte de a le salva.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPin = etNewPin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (newPin.length() < 4) {
                Toast.makeText(this, "PIN-ul trebuie să aibă minim 4 cifre!", Toast.LENGTH_SHORT).show();
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
