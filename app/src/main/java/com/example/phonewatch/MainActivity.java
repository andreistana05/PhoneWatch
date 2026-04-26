package com.example.phonewatch;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pornim serviciul de monitorizare a senzorilor
        Intent serviceIntent = new Intent(this, AntiTheftService.class);
        startService(serviceIntent);
    }
}
