package com.example.phonewatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activitatea care se deschide atunci cand alarma este activata.
 * Aceasta blocheaza interfata si cere introducerea PIN-ului pentru a opri alarma.
 */
public class AlarmActivity extends Activity {
    private PinManager pinManager;
    private EditText pinInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Permitem activitatii sa apara peste ecranul blocat
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_alarm);
        
        pinManager = new PinManager(this);
        pinInput = findViewById(R.id.pin_input);

        Button btnStop = findViewById(R.id.btn_stop);
        // La apasarea butonului verifica pinul introdus de utilizator este corect.
        btnStop.setOnClickListener(v -> checkPin());
    }

    /**
     * Verifica pin-ul introdus de utilizator.
     * Daca este corect inchide ecranul de alarma.
     */
    private void checkPin() {
        String entered = pinInput.getText().toString().trim();

        if (entered.equals(pinManager.getPin())) {
            // Daca pin-ul este corect, oprim serviciul de anti-furt
            Intent stopIntent = new Intent(this, AntiTheftService.class);
            stopIntent.setAction("STOP_MONITORING");
            startService(stopIntent);
            
            // Inchidem aceasta activitate
            finish();
        } else {
            // Daca pin-ul e gresit, afisam un mesaj de eroare.
            Toast.makeText(this, "PIN incorect!", Toast.LENGTH_SHORT).show();
            pinInput.setText("");
        }
    }
    
    /**
     * Blocam butonul de back pentru ca "hotul" sa nu poata iesi usor din activitatea de alarma.
     */
    @Override
    public void onBackPressed() {
        // Nu apelăm super.onBackPressed(), deci butonul nu face nimic
        Toast.makeText(this, "Introdu PIN-ul pentru a opri!", Toast.LENGTH_SHORT).show();
    }
}
