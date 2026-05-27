package com.example.phonewatch;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager pentru datele persistente ale aplicatiei
 * Se ocupa de salvarea si recuperarea pin-ului
 */
public class PinManager {
    // Numele fisierului in care se vor salva datele legate de PIN
    private static final String PREFS_NAME = "phonewatch_prefs";
    private static final String KEY_PIN = "user_pin";
    private static final String DEFAULT_PIN = "1234";
    private static final String KEY_MONITORING = "is_monitoring";

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        // Initializam sistemul Shared Preferences pentru stocarea locala privata de date.
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returneaza pin-ul salvat sau cel implicit (1234) daca nu a fost salvat niciunul.
     */
    public String getPin(){
        return prefs.getString(KEY_PIN, DEFAULT_PIN);
    }

    /**
     * Salveaza noul pin in sistem.
     */
    public void savePin(String pin){
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    /**
     * Verifica daca modul anti-furt a fost activ la ultima folosire a aplicatiei.
     */
    public boolean isMonitoring() {
        return prefs.getBoolean(KEY_MONITORING, false);
    }

    /**
     * Salveaza starea monitorizarii.
     */
    public void setMonitoring(boolean active) {
        prefs.edit().putBoolean(KEY_MONITORING, active).apply();
    }
}
