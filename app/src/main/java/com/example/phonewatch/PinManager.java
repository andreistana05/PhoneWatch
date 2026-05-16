package com.example.phonewatch;

import android.content.Context;
import android.content.SharedPreferences;
public class PinManager {
    private static final String PREFS_NAME = "phonewatch_prefs";
    private static final String KEY_PIN = "user_pin";
    private static final String DEFAULT_PIN = "1234";

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getPin(){
        return prefs.getString(KEY_PIN, DEFAULT_PIN);
    }

    public void savePin(String pin){
        prefs.edit().putString(KEY_PIN, pin).apply();
    }
}
